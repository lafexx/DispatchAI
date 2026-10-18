import { Link } from "react-router";
import type {
  DeliveryAnalyticsState,
  DriverResponse,
  CustomerResponse,
} from "@/api";
import { shortId, dateTime } from "@/lib/format";
import { StatusBadge } from "./StatusBadge";
import { Panel, EmptyState, Avatar } from "./UI";
import { Icon } from "./Icon";
export function DeliveryTable({
  deliveries,
  drivers = [],
  customers = [],
  title = "Deliveries",
  subtitle = "",
  action,
}: {
  deliveries: DeliveryAnalyticsState[];
  drivers?: DriverResponse[];
  customers?: CustomerResponse[];
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
}) {
  const names = new Map(drivers.map((d) => [d.id, d.name]));
  const customerNames = new Map(customers.map((c) => [c.id, c.name]));
  return (
    <Panel
      title={title || undefined}
      subtitle={subtitle || undefined}
      action={action}
    >
      {deliveries.length === 0 ? (
        <EmptyState
          title="No deliveries to show"
          description="Try a different filter, or create a new delivery."
        />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Delivery</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Driver</th>
                <th>Delay</th>
                <th>Created</th>
                <th>
                  <span className="sr-only">Open delivery</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {deliveries.map((d) => {
                const name = d.driverId ? names.get(d.driverId) : null;
                return (
                  <tr key={d.deliveryId}>
                    <td>
                      <Link
                        to={`/deliveries/${d.deliveryId}`}
                        className="mono cell-primary"
                      >
                        #{shortId(d.deliveryId)}
                      </Link>
                    </td>
                    <td>
                      {customerNames.get(d.customerId) ?? shortId(d.customerId)}
                    </td>
                    <td>
                      <StatusBadge status={d.status} />
                    </td>
                    <td>
                      {name ? (
                        <span className="person-cell">
                          <Avatar name={name} small />
                          {name}
                        </span>
                      ) : (
                        <span className="muted">
                          {d.driverId ? shortId(d.driverId) : "Unassigned"}
                        </span>
                      )}
                    </td>
                    <td>
                      {d.delayed ? (
                        <span className="text-amber">
                          {d.delayMinutes == null
                            ? "Reported"
                            : `${d.delayMinutes} min`}
                        </span>
                      ) : (
                        <span className="muted">—</span>
                      )}
                    </td>
                    <td className="muted">{dateTime(d.createdAt)}</td>
                    <td>
                      <Link
                        to={`/deliveries/${d.deliveryId}`}
                        aria-label={`Open delivery ${shortId(d.deliveryId)}`}
                        className="icon-button"
                        style={{ padding: 5, background: "transparent" }}
                      >
                        <Icon name="arrow" size={14} />
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}
