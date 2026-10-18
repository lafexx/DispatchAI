import { useSearchParams, Link } from "react-router";
import { useDeliveries } from "@/features/deliveries/hooks";
import { useCustomers } from "@/lib/queries";
import {
  PageHeader,
  Panel,
  Loading,
  Notice,
  EmptyState,
} from "@/components/UI";
import { StatusBadge } from "@/components/StatusBadge";
import { Icon } from "@/components/Icon";
import { DispatchPanel } from "@/features/dispatch/components/DispatchPanel";
import { shortId, dateTime } from "@/lib/format";
export function DispatchPage() {
  const query = useDeliveries();
  const customers = useCustomers();
  const [params, setParams] = useSearchParams();
  const requested = params.get("delivery");
  const queue = [...(query.data ?? [])]
    .filter((d) => d.status === "PENDING")
    .sort((a, b) => (a.createdAt ?? "").localeCompare(b.createdAt ?? ""));
  const selected = requested ?? queue[0]?.deliveryId;
  const names = new Map(customers.data?.map((c) => [c.id, c.name]));
  return (
    <div className="stack">
      <PageHeader
        eyebrow="RESOURCE COORDINATION"
        title="Dispatch"
        description="Work through the pending queue with capacity-aware assignments and optional AI advice."
        actions={
          <span className="button button-ghost">
            <Icon name="package" size={15} />
            {query.data ? queue.length : "—"} pending
          </span>
        }
      />
      {query.isError && (
        <Notice tone="warning">
          The dispatch queue could not be refreshed. Showing the last fetched
          data.
        </Notice>
      )}
      {query.isPending ? (
        <Loading label="Loading dispatch queue…" />
      ) : !query.data ? (
        <Panel>
          <EmptyState
            title="Dispatch queue unavailable"
            description="Use refresh to try loading the queue again."
            icon="dispatch"
          />
        </Panel>
      ) : !queue.length && !requested ? (
        <Panel>
          <EmptyState
            title="The queue is clear"
            description="New pending deliveries will appear here. You can also create a delivery now."
            icon="check"
            action={
              <Link className="button button-primary" to="/deliveries?create=1">
                <Icon name="plus" size={15} />
                New delivery
              </Link>
            }
          />
        </Panel>
      ) : (
        <div className="dispatch-layout">
          <Panel title="Pending deliveries" subtitle="Oldest deliveries first">
            {queue.map((d) => (
              <button
                key={d.deliveryId}
                className={`queue-item ${selected === d.deliveryId ? "active" : ""}`}
                onClick={() => setParams({ delivery: d.deliveryId })}
              >
                <div className="flex justify-between items-center">
                  <strong className="mono">#{shortId(d.deliveryId)}</strong>
                  <StatusBadge status="PENDING" />
                </div>
                <small>
                  {names.get(d.customerId) ?? shortId(d.customerId)}
                </small>
                <small>Created {dateTime(d.createdAt)}</small>
                {d.delayed && (
                  <small className="text-amber">
                    Delay reported · {d.delayMinutes ?? "—"} min
                  </small>
                )}
              </button>
            ))}
            {!queue.length && (
              <EmptyState title="No pending deliveries" icon="check" />
            )}
          </Panel>
          <div className="stack">
            {selected && <DispatchPanel key={selected} deliveryId={selected} />}
            <Link className="link-button" to="/deliveries">
              View all deliveries <Icon name="arrow" size={13} />
            </Link>
            {requested &&
              !queue.some((d) => d.deliveryId === requested) &&
              queue.length > 0 && (
                <button
                  className="button self-start"
                  onClick={() => setParams({ delivery: queue[0].deliveryId })}
                >
                  Next pending delivery <Icon name="arrow" size={14} />
                </button>
              )}
          </div>
        </div>
      )}
    </div>
  );
}
