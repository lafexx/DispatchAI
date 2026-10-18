#!/usr/bin/env python3
"""Creates isolated demo records. No LLM calls unless --ai is provided."""
import json
import os
import sys
import time
import uuid
import urllib.request
import urllib.error
urls = {name: os.environ.get(name.upper()+'_URL', 'http://localhost:'+str(port)).rstrip('/')
        for name, port in [('delivery',8081),('fleet',8082),('dispatch',8083),('customer',8084),('agent',8086)]}
def call(service, method, path, body=None, expected=200):
    request = urllib.request.Request(urls[service]+path, method=method,
        data=None if body is None else json.dumps(body).encode(), headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(request,timeout=75) as response:
            status=response.status; raw=response.read()
    except urllib.error.HTTPError as error:
        status=error.code; raw=error.read()
    assert status==expected,(service,path,status,raw.decode())
    return json.loads(raw) if raw else None
def get(service,path): return call(service,'GET',path)
def wait(service,path,predicate):
    deadline=time.monotonic()+45
    while time.monotonic()<deadline:
        value=get(service,path)
        if predicate(value): return value
        time.sleep(0.5)
    raise AssertionError('Timed out waiting for '+path)
run=uuid.uuid4().hex[:10]
customer=call('customer','POST','/api/customers',{'name':'Capacity test '+run,'email':run+'@example.com','phone':'0400000000'},201)
driver=call('fleet','POST','/api/drivers',{'name':'Capacity driver '+run},201)
call('fleet','POST',f"/api/drivers/{driver['id']}/availability",{'available':True})
vehicles=[]
for index,capacity in enumerate([50,100,500]):
    v=call('fleet','POST','/api/vehicles',{'registration':f'CAP-{run}-{index}','type':'VAN','capacityKg':capacity},201)
    call('fleet','POST',f"/api/vehicles/{v['id']}/availability",{'available':True}); vehicles.append(v)
create={'customerId':customer['id'],'pickupAddress':'12 King St','destinationAddress':'45 George St','parcelWeightKg':100}
call('delivery','POST','/api/deliveries',dict(create,parcelWeightKg=0),400)
delivery=call('delivery','POST','/api/deliveries',create,201)
id=delivery['id']
context=get('dispatch',f'/api/dispatch/deliveries/{id}/eligibility')
eligible={v['id'] for v in context['eligibleVehicles']}
assert vehicles[0]['id'] not in eligible and vehicles[1]['id'] in eligible
call('delivery','PATCH',f'/api/deliveries/{id}/parcel-weight',{'parcelWeightKg':150})
assert vehicles[1]['id'] not in {v['id'] for v in get('dispatch',f'/api/dispatch/deliveries/{id}/eligibility')['eligibleVehicles']}
call('delivery','PATCH',f'/api/deliveries/{id}/parcel-weight',{'parcelWeightKg':100})
assignment={'deliveryId':id,'driverId':driver['id'],'vehicleId':vehicles[0]['id']}
call('dispatch','POST','/api/dispatch/assignments',assignment,409)
assert get('fleet',f"/api/drivers/{driver['id']}")['availability']=='AVAILABLE'
# Fleet also enforces the boundary if a caller bypasses Dispatch's candidate filtering.
reservation={'deliveryId':id,'driverId':driver['id'],'vehicleId':vehicles[1]['id'],'parcelWeightKg':100.01}
call('fleet','PUT','/api/reservations/'+str(uuid.uuid4()),reservation,409)
assert get('fleet',f"/api/vehicles/{vehicles[1]['id']}")['available']
assigned=call('dispatch','POST','/api/dispatch/assignments',dict(assignment,vehicleId=vehicles[1]['id']),201)
wait('delivery',f'/api/deliveries/{id}',lambda d:d['status']=='ASSIGNED')
call('delivery','PATCH',f'/api/deliveries/{id}/parcel-weight',{'parcelWeightKg':150},409)
assert not get('fleet',f"/api/vehicles/{vehicles[1]['id']}")['available']
call('delivery','POST',f'/api/deliveries/{id}/cancel',{'reason':'CUSTOMER_REQUEST','notes':'Capacity smoke test'})
wait('dispatch','/api/dispatch/assignments/'+assigned['id'],lambda a:a['status']=='CANCELLED')
wait('fleet',f"/api/drivers/{driver['id']}",lambda d:d['availability']=='AVAILABLE')
wait('fleet',f"/api/vehicles/{vehicles[1]['id']}",lambda v:v['available'])
print('Capacity, weight validation, assignment acceptance and resource release passed')
if '--ai' in sys.argv:
    pending=call('delivery','POST','/api/deliveries',create,201)
    advice=call('agent','POST','/api/agent/dispatch/advice',{'deliveryId':pending['id']})
    assert advice['vehicleCapacityKg']>=advice['parcelWeightKg']
    assert advice['recommendedDriverId'] in {c['driver']['id'] for c in advice['candidates']}
    assert advice['recommendedVehicleId'] in {v['id'] for v in advice['eligibleVehicles']}
    assert get('delivery','/api/deliveries/'+pending['id'])['status']=='PENDING'
    print('AI returned an eligible pair and did not assign it')
print('Created customer='+customer['id']+' delivery='+id)
