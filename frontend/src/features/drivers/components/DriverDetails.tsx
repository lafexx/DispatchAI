import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router";
import { setDriverAvailability } from "@/api";
import { useDriver, useDriverAnalytics } from "@/features/drivers/hooks";
import { useDeliveries } from "@/features/deliveries/hooks";
import { refreshOperations } from "@/lib/queries";
import { StatusBadge } from "@/components/StatusBadge";
import { Panel, Notice, Loading, Avatar, EmptyState } from "@/components/UI";
import { dateTime, shortId } from "@/lib/format";
import { DriverAnalyticsCard } from "./DriverAnalyticsCard";
export function DriverDetails({ driverId }: { driverId: string }) {
  const driver = useDriver(driverId);
  const analytics = useDriverAnalytics(driverId);
  const deliveries = useDeliveries();
  const client = useQueryClient();
  const [feedback, setFeedback] = useState("");
  const availability = useMutation({
    mutationFn: (available: boolean) =>
      setDriverAvailability(driverId, { available }),
    onSuccess: async (result, requested) => {
      setFeedback(
        requested && result.availability !== "AVAILABLE"
          ? "Availability preference saved. This driver remains unavailable while assigned."
          : "Availability updated.",
      );
      await refreshOperations(client);
    },
  });
  if (driver.isPending) return <Loading label="Loading driver…" />;
  if (!driver.data)
    return (
      <Notice tone="error">
        {driver.error?.message ?? "Driver unavailable"}
      </Notice>
    );
  const history = [...(deliveries.data ?? [])]
    .filter((d) => d.driverId === driverId)
    .sort((a, b) => (b.assignedAt ?? "").localeCompare(a.assignedAt ?? ""))
    .slice(0, 6);
  return (
    <div className="stack">
      <Panel>
        <div className="panel-body">
          <div className="flex items-center justify-between gap-4">
            <div className="person-cell">
              <Avatar name={driver.data.name} />
              <div>
                <h2 style={{ fontSize: 18 }}>{driver.data.name}</h2>
                <span className="cell-secondary mono">{driverId}</span>
              </div>
            </div>
            <StatusBadge status={driver.data.availability} />
          </div>
          <div className="flex gap-2 mt-5">
            <button
              className="button button-small"
              disabled={availability.isPending}
              onClick={() => availability.mutate(true)}
            >
              Mark available
            </button>
            <button
              className="button button-ghost button-small"
              disabled={availability.isPending}
              onClick={() => availability.mutate(false)}
            >
              Mark unavailable
            </button>
          </div>
          {feedback && (
            <p className="text-xs muted mt-3" role="status">
              {feedback}
            </p>
          )}
          {availability.isError && (
            <div className="mt-3">
              <Notice tone="error">{availability.error.message}</Notice>
            </div>
          )}
        </div>
      </Panel>
      {analytics.isError && (
        <Notice tone="warning">
          Driver performance is temporarily unavailable.
        </Notice>
      )}
      {analytics.data ? (
        <DriverAnalyticsCard analytics={analytics.data} />
      ) : analytics.isPending ? (
        <Loading label="Loading performance…" />
      ) : null}
      <Panel
        title="Recent assignments"
        subtitle="Latest recorded delivery assignments"
      >
        {deliveries.isError ? (
          <div className="panel-body">
            <Notice tone="warning">Assignment history is unavailable.</Notice>
          </div>
        ) : history.length ? (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Delivery</th>
                  <th>State</th>
                  <th>Assigned</th>
                </tr>
              </thead>
              <tbody>
                {history.map((d) => (
                  <tr key={d.deliveryId}>
                    <td>
                      <Link className="mono" to={`/deliveries/${d.deliveryId}`}>
                        #{shortId(d.deliveryId)}
                      </Link>
                    </td>
                    <td>
                      <StatusBadge status={d.status} />
                    </td>
                    <td>{dateTime(d.assignedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No assignments yet"
            description="Recorded assignments will appear here."
            icon="dispatch"
          />
        )}
      </Panel>
    </div>
  );
}
