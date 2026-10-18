#!/usr/bin/env python3
"""Dependency-free source audit; this is not a substitute for Java compilation."""
import argparse
import collections
from pathlib import Path
import re
import sys

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('root', nargs='?', default=str(Path(__file__).resolve().parents[1]))
root = Path(parser.parse_args().root).resolve()
errors = []
index = {}
by_service = collections.defaultdict(dict)
main_types = {}
for path in sorted(root.glob('*-service/src/**/*.java')):
    source = path.read_text()
    match = re.search(r'^package ([\w.]+);', source, re.M)
    if not match:
        errors.append(f'Missing package: {path.relative_to(root)}')
        continue
    package = match.group(1)
    service = path.relative_to(root).parts[0]
    name = package + '.' + path.stem
    is_main = '/src/main/java/' in str(path)
    source_root = root / service / ('src/main/java' if is_main else 'src/test/java')
    if str(path.relative_to(source_root)) != name.replace('.', '/') + '.java':
        errors.append(f'Package/path mismatch: {path.relative_to(root)}')
    if name in index:
        errors.append(f'Duplicate type: {name}')
    index[name] = path
    by_service[service][path.stem] = name
    if is_main:
        main_types[name] = source
        if '/domain/' in str(path) and re.search(r'com\.dispatchai\.\w+\.(presentation|application|infrastructure)\.', source):
            errors.append(f'Domain depends on an outer layer: {path.relative_to(root)}')
        if '@RestController' in source and '.presentation' not in package:
            errors.append(f'Controller outside presentation: {name}')
        if re.search(r'extends (?:JpaRepository|CrudRepository)', source) and not package.endswith('.infrastructure.persistence.repository'):
            errors.append(f'Repository outside persistence.repository: {name}')
        if 'class ' + path.stem in source and path.stem.endswith('Exception') and not ('.domain.exception' in package or '.client.' in package and package.endswith('.exception')):
            errors.append(f'Exception outside the agreed exception packages: {name}')
for name, path in index.items():
    source = path.read_text()
    package = name.rsplit('.', 1)[0]
    service = path.relative_to(root).parts[0]
    for imported in re.findall(r'^import (?:static )?(com\.dispatchai\.[\w.*]+);', source, re.M):
        if imported.endswith('.*'):
            target = imported[:-2]
            if not any(other == target or other.startswith(target + '.') for other in index):
                errors.append(f'Unresolved project wildcard: {imported} in {path.relative_to(root)}')
        elif imported not in index and not any(imported.startswith(other + '.') for other in index):
            errors.append(f'Unresolved project import: {imported} in {path.relative_to(root)}')
    imports = set(re.findall(r'^import (com\.dispatchai\.[\w.]+);', source, re.M))
    wildcards = set(re.findall(r'^import (com\.dispatchai\.[\w.]+)\.\*;', source, re.M))
    scrubbed = re.sub(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|/\*[\s\S]*?\*/|//[^\n]*', '', source)
    scrubbed = re.sub(r'^\s*(?:package|import) [^;]+;', '', scrubbed, flags=re.M)
    for simple, target in by_service[service].items():
        if simple == path.stem or not re.search(r'(?<![\w.])' + re.escape(simple) + r'\b', scrubbed):
            continue
        target_package = target.rsplit('.', 1)[0]
        if target_package != package and target not in imports and target_package not in wildcards:
            errors.append(f'Possibly missing import: {target} in {path.relative_to(root)}')

enums = {name.rsplit('.', 1)[1] for name, source in main_types.items() if re.search(r'\benum\s+\w+', source)}
def fields(name):
    source = main_types[name]
    match = re.search(r'\brecord\s+\w+\s*\(', source)
    if not match:
        raise ValueError('Not a record: ' + name)
    start = match.end()
    depth = 1
    end = start
    while depth:
        if source[end] == '(':
            depth += 1
        elif source[end] == ')':
            depth -= 1
        end += 1
    signature = re.sub(r'@[\w.]+(?:\([^()]*\))?\s*', '', source[start:end-1])
    result = {}
    # Current contracts contain single-argument List<T>; no generic maps or nested commas.
    for declaration in signature.split(','):
        parts = declaration.strip().rsplit(None, 1)
        if len(parts) != 2:
            raise ValueError('Cannot parse record field: ' + declaration)
        kind, field = parts
        kind = kind.rsplit('.', 1)[-1]
        if '<' in kind:
            kind = 'List'
        if kind in enums:
            kind = 'String'
        result[field] = kind
    return result

contract_checks = 0
def compare(provider, *consumers):
    global contract_checks
    for consumer in consumers:
        contract_checks += 1
        if fields(provider) != fields(consumer):
            errors.append(f'Contract drift: {provider} -> {consumer}: {fields(provider)} != {fields(consumer)}')
base = 'com.dispatchai.'
event = '.infrastructure.messaging.event.'
for event_name, consumers in [
    ('DeliveryCreatedEvent', ['dispatch','analytics']),
    ('DeliveryAssignedEvent', ['fleet','customer','analytics']),
    ('DeliveryStartedEvent', ['customer','analytics']),
    ('DeliveryDelayedEvent', ['customer','analytics']),
    ('DeliveryCompletedEvent', ['dispatch','fleet','customer','analytics']),
    ('DeliveryCancelledEvent', ['dispatch','fleet','customer','analytics']),
]:
    compare(base+'delivery'+event+event_name, *(base+s+event+event_name for s in consumers))
compare(base+'dispatch'+event+'DriverAssignedEvent', base+'delivery'+event+'DriverAssignedEvent')
compare(base+'delivery'+event+'AssignmentRejectedEvent', base+'dispatch'+event+'AssignmentRejectedEvent')
compare(base+'delivery.presentation.dto.DeliveryResponse',
        base+'dispatch.infrastructure.client.delivery.dto.DeliveryResponse', base+'agent.infrastructure.client.delivery.dto.DeliveryResponse')
compare(base+'fleet.presentation.dto.DriverResponse',
        base+'dispatch.infrastructure.client.fleet.dto.DriverResponse', base+'agent.infrastructure.client.fleet.dto.DriverResponse')
compare(base+'fleet.presentation.dto.VehicleResponse',
        base+'dispatch.infrastructure.client.fleet.dto.VehicleResponse', base+'agent.infrastructure.client.fleet.dto.VehicleResponse')
compare(base+'fleet.presentation.dto.ReservationRequest',
        base+'dispatch.infrastructure.client.fleet.dto.ReservationRequest', base+'delivery.infrastructure.client.fleet.dto.ReservationRequest')
compare(base+'fleet.presentation.dto.ReservationResponse',
        base+'dispatch.infrastructure.client.fleet.dto.ReservationResponse', base+'delivery.infrastructure.client.fleet.dto.ReservationResponse')
compare(base+'dispatch.presentation.dto.DispatchEligibilityResponse', base+'agent.infrastructure.client.dispatch.dto.DispatchEligibilityResponse')
compare(base+'analytics.domain.model.DeliveryAnalyticsState', base+'agent.infrastructure.client.analytics.dto.DeliveryAnalyticsResponse')
for name in ['DriverAnalyticsResponse','DeliverySummaryResponse']:
    compare(base+'analytics.presentation.dto.'+name, base+'agent.infrastructure.client.analytics.dto.'+name)
for service in ['fleet','dispatch','customer','agent','analytics']:
    compare(base+'delivery.presentation.error.ApiError', base+service+'.presentation.error.ApiError')
print(f'Audited {len(index)} Java main/test types and {contract_checks} producer/consumer contract pairs')
for error in errors:
    print(error, file=sys.stderr)
if errors:
    sys.exit(1)
print('Package paths, imports, domain boundaries, repository/controller locations and contract fields agree')
