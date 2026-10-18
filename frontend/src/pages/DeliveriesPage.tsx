import { useSearchParams } from "react-router";
import { useDeliveries } from "@/features/deliveries/hooks";
import { useDrivers } from "@/features/drivers/hooks";
import { useCustomers } from "@/lib/queries";
import {
  PageHeader,
  Panel,
  Notice,
  Loading,
  EmptyState,
} from "@/components/UI";
import { DeliveryTable } from "@/components/DeliveryTable";
import { Icon } from "@/components/Icon";
import { CreateDeliveryModal } from "@/features/deliveries/components/CreateDeliveryModal";
import { activeStatus } from "@/lib/format";
const filters = [
  ["all", "All"],
  ["PENDING", "Pending"],
  ["ASSIGNED", "Assigned"],
  ["IN_TRANSIT", "In transit"],
  ["COMPLETED", "Completed"],
  ["CANCELLED", "Cancelled"],
  ["delayed", "Active delays"],
];
export function DeliveriesPage() {
  const [params, setParams] = useSearchParams();
  const query = useDeliveries();
  const drivers = useDrivers();
  const customers = useCustomers();
  const term = params.get("q") ?? "";
  const status = params.get("status") ?? "all";
  const names = new Map(
    [...(drivers.data ?? []), ...(customers.data ?? [])].map((x) => [
      x.id,
      x.name,
    ]),
  );
  const all = query.data ?? [];
  const matchesStatus = (s: string, d: (typeof all)[number]) =>
    s === "all" || s === "delayed"
      ? s === "all" || (d.delayed && activeStatus(d.status))
      : d.status === s;
  const filtered = [...all]
    .filter(
      (d) =>
        matchesStatus(status, d) &&
        [
          d.deliveryId,
          d.customerId,
          d.driverId ?? "",
          names.get(d.customerId) ?? "",
          names.get(d.driverId ?? "") ?? "",
        ].some((value) => value.toLowerCase().includes(term.toLowerCase())),
    )
    .sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""));
  const count = Math.max(1, Math.ceil(filtered.length / 12));
  const page = Math.max(1, Math.min(count, Number(params.get("page")) || 1));
  function update(key: string, value: string) {
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      if (value) next.set(key, value);
      else next.delete(key);
      if (key !== "page") next.delete("page");
      return next;
    });
  }
  return (
    <div className="stack">
      <PageHeader
        eyebrow="DELIVERY OPERATIONS"
        title="Deliveries"
        description="Find a delivery, review its progress, and keep work moving."
        actions={
          <button
            className="button button-primary"
            onClick={() => update("create", "1")}
          >
            <Icon name="plus" size={16} />
            New delivery
          </button>
        }
      />
      {query.isError && (
        <Notice tone="warning">
          {query.data
            ? "Delivery updates are temporarily unavailable. Showing the last fetched list."
            : query.error.message}
        </Notice>
      )}
      <Panel>
        <div className="toolbar">
          <div className="search-field">
            <Icon name="search" size={16} />
            <input
              aria-label="Filter deliveries"
              placeholder="Search ID, customer or driver…"
              value={term}
              onChange={(e) => update("q", e.target.value)}
            />
          </div>
          <span className="muted text-xs">
            {filtered.length} matching deliveries
          </span>
        </div>
        <div className="toolbar">
          <div className="filter-tabs">
            {filters.map(([key, label]) => (
              <button
                key={key}
                className={`filter-tab ${status === key ? "active" : ""}`}
                aria-pressed={status === key}
                onClick={() => update("status", key)}
              >
                {label}
                <span>{all.filter((d) => matchesStatus(key, d)).length}</span>
              </button>
            ))}
          </div>
        </div>
      </Panel>
      {query.isPending ? (
        <Loading />
      ) : !query.data ? (
        <Panel>
          <EmptyState
            title="Delivery list unavailable"
            description="Try refreshing before reviewing deliveries."
          />
        </Panel>
      ) : (
        <>
          <DeliveryTable
            deliveries={filtered.slice((page - 1) * 12, page * 12)}
            drivers={drivers.data}
            customers={customers.data}
            title=""
          />
          <div className="table-footer" style={{ borderTop: 0, paddingTop: 0 }}>
            <span>
              {filtered.length
                ? `${(page - 1) * 12 + 1}–${Math.min(page * 12, filtered.length)} of ${filtered.length}`
                : "0 deliveries"}
            </span>
            <div className="flex items-center gap-3">
              <button
                className="button button-small"
                disabled={page <= 1}
                onClick={() => update("page", String(page - 1))}
              >
                Previous
              </button>
              <span>
                Page {page} of {count}
              </span>
              <button
                className="button button-small"
                disabled={page >= count}
                onClick={() => update("page", String(page + 1))}
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
      {params.get("create") === "1" && (
        <CreateDeliveryModal open onClose={() => update("create", "")} />
      )}
    </div>
  );
}
