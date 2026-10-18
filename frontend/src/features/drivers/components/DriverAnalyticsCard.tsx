import type { DriverAnalyticsResponse } from "@/api";
import { Panel } from "@/components/UI";
import { duration, percent } from "@/lib/format";
export function DriverAnalyticsCard({
  analytics: a,
}: {
  analytics: DriverAnalyticsResponse;
}) {
  const metrics = [
    { label: "Total assigned", value: a.totalAssigned },
    { label: "Completed", value: a.completed },
    { label: "Cancelled", value: a.cancelled },
    { label: "Reported delays", value: a.delayed },
    {
      label: "Avg delivery time",
      value: a.completed ? duration(a.averageDeliveryTimeSeconds) : "—",
    },
    {
      label: "Avg transit time",
      value: a.completed ? duration(a.averageTransitTimeSeconds) : "—",
    },
  ];
  return (
    <Panel
      title="Driver performance"
      subtitle={
        a.totalAssigned
          ? `${percent(a.completed / a.totalAssigned)} completion rate · ${a.totalAssigned} assigned deliveries`
          : "No assignments recorded yet"
      }
    >
      <div className="performance-grid">
        {metrics.map((m) => (
          <div key={m.label} className="performance-cell">
            <label>{m.label}</label>
            <strong>{m.value}</strong>
          </div>
        ))}
      </div>
    </Panel>
  );
}
