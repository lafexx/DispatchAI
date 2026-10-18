import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { getDeliveryAssignment } from "@/api";
import { useDelivery } from "@/features/deliveries/hooks";
import { useDrivers } from "@/features/drivers/hooks";
import { useSourceDelivery, useCustomers, useVehicles } from "@/lib/queries";
import { Panel, Notice, Loading, PageHeader } from "@/components/UI";
import { StatusBadge } from "@/components/StatusBadge";
import { Icon } from "@/components/Icon";
import { DeliveryActions } from "./DeliveryActions";
import { DispatchPanel } from "@/features/dispatch/components/DispatchPanel";
import { dateTime, shortId } from "@/lib/format";
export function DeliveryDetails({ deliveryId }: { deliveryId: string }) {
  const source = useSourceDelivery(deliveryId);
  const projection = useDelivery(deliveryId);
  const drivers = useDrivers();
  const customers = useCustomers();
  const vehicles = useVehicles();
  const assignment = useQuery({
    queryKey: ["dispatch", "delivery-assignment", deliveryId],
    queryFn: () => getDeliveryAssignment(deliveryId),
    enabled: !!source.data?.assignedDriverId,
    refetchInterval: 5000,
  });
  if (source.isPending) return <Loading label="Loading delivery details…" />;
  if (!source.data)
    return (
      <Notice tone="error">
        {source.error?.message ?? "Delivery not found"}{" "}
        <Link className="link-button ml-3" to="/deliveries">
          Back to deliveries
        </Link>
      </Notice>
    );
  const d = source.data;
  const p = projection.data;
  const driver = drivers.data?.find((x) => x.id === d.assignedDriverId);
  const customer = customers.data?.find((x) => x.id === d.customerId);
  const vehicle = vehicles.data?.find(
    (x) => x.id === assignment.data?.vehicleId,
  );
  const stage = ["PENDING", "ASSIGNED", "IN_TRANSIT", "COMPLETED"].indexOf(
    d.status,
  );
  const timeline = [
    { label: "Delivery created", at: p?.createdAt, done: true },
    {
      label: "Driver assigned",
      at: p?.assignedAt,
      done: stage >= 1 || !!p?.assignedAt,
    },
    {
      label: "In transit",
      at: p?.startedAt,
      done: stage >= 2 || !!p?.startedAt,
    },
    ...(d.status === "CANCELLED"
      ? [{ label: "Cancelled", at: p?.cancelledAt, done: true }]
      : [
          {
            label: "Delivery completed",
            at: p?.completedAt,
            done: d.status === "COMPLETED",
          },
        ]),
  ];
  return (
    <div className="stack">
      <PageHeader
        eyebrow="DELIVERY DETAILS"
        title={`Delivery #${shortId(deliveryId)}`}
        description={
          customer
            ? `${customer.name} · ${customer.email}`
            : `Customer #${shortId(d.customerId)}`
        }
        actions={
          <>
            <StatusBadge status={d.status} />
            <Link to="/deliveries" className="button button-ghost">
              <Icon name="back" size={14} />
              All deliveries
            </Link>
          </>
        }
      />
      {source.isError && (
        <Notice tone="warning">
          Delivery state could not be refreshed. Displaying the last fetched
          details.
        </Notice>
      )}
      {projection.isError && (
        <Notice tone="info">
          Delivery details are available. Timeline statistics are still loading
          or temporarily unavailable.
        </Notice>
      )}
      <div className="detail-grid">
        <div className="stack">
          <Panel
            title="Route & parcel"
            subtitle="Pickup, destination and delivery requirements"
          >
            <div className="panel-body-topless">
              <div className="route-address">
                <span className="route-marker">A</span>
                <div>
                  <label>Pickup address</label>
                  <strong>{d.pickupAddress}</strong>
                </div>
              </div>
              <div className="route-address destination">
                <span className="route-marker">B</span>
                <div>
                  <label>Destination address</label>
                  <strong>{d.destinationAddress}</strong>
                </div>
              </div>
              <div className="stat-row mt-3">
                <span>Parcel weight</span>
                <strong>
                  {d.parcelWeightKg == null
                    ? "Weight required"
                    : `${d.parcelWeightKg} kg`}
                </strong>
              </div>
              <div className="stat-row">
                <span>Driver</span>
                <strong>
                  {driver ? (
                    <Link
                      className="text-blue"
                      to={`/fleet?driver=${driver.id}`}
                    >
                      {driver.name}
                    </Link>
                  ) : d.assignedDriverId ? (
                    shortId(d.assignedDriverId)
                  ) : (
                    "Unassigned"
                  )}
                </strong>
              </div>
              <div className="stat-row">
                <span>Vehicle</span>
                <strong>
                  {vehicle
                    ? `${vehicle.registration} · ${vehicle.capacityKg} kg capacity`
                    : assignment.data?.vehicleId
                      ? shortId(assignment.data.vehicleId)
                      : "Unassigned"}
                </strong>
              </div>
              {assignment.isError && (
                <p className="text-xs muted mt-3">
                  Assignment details are temporarily unavailable.
                </p>
              )}
              <div className="stat-row">
                <span>Delivery ID</span>
                <strong
                  className="mono"
                  style={{ overflowWrap: "anywhere", maxWidth: "70%" }}
                >
                  {deliveryId}
                </strong>
              </div>
            </div>
          </Panel>
          {d.delayed && (
            <Panel
              title="Reported delay"
              action={
                <span className="status-badge status-pending">
                  <Icon name="clock" size={12} />
                  {d.delayMinutes ?? "—"} min
                </span>
              }
            >
              <div className="panel-body-topless">
                <p className="text-sm text-zinc-300 leading-7">
                  {d.delayReason ?? "No reason recorded"}
                </p>
                <p className="text-xs muted mt-3">
                  Latest report {dateTime(p?.lastDelayAt)}
                </p>
              </div>
            </Panel>
          )}
          {d.status === "PENDING" && <DispatchPanel deliveryId={deliveryId} />}
          <Panel title="Delivery actions">
            <div className="panel-body-topless">
              <DeliveryActions
                deliveryId={deliveryId}
                status={d.status}
                delayed={d.delayed}
              />
              {["COMPLETED", "CANCELLED"].includes(d.status) && (
                <p className="text-xs muted">
                  This delivery has finished. No further lifecycle actions are
                  available.
                </p>
              )}
            </div>
          </Panel>
        </div>
        <Panel
          title="Delivery timeline"
          subtitle="Milestones recorded for this delivery"
        >
          <div className="timeline">
            {timeline.map((item) => (
              <div
                key={item.label}
                className={`timeline-item ${item.done ? "done" : ""} ${item.label === "Cancelled" ? "cancelled" : ""}`}
              >
                <span className="timeline-step">
                  {item.done ? (
                    <Icon
                      name={item.label === "Cancelled" ? "close" : "check"}
                      size={11}
                    />
                  ) : (
                    <span
                      style={{
                        width: 4,
                        height: 4,
                        background: "currentColor",
                        borderRadius: 4,
                      }}
                    />
                  )}
                </span>
                <div>
                  <h3>{item.label}</h3>
                  <p>
                    {item.at
                      ? dateTime(item.at)
                      : item.done
                        ? "Timestamp updating…"
                        : "Not reached"}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </Panel>
      </div>
    </div>
  );
}
