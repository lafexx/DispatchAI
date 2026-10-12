import type { DriverAnalyticsResponse } from "@/api";

interface DriverAnalyticsCardProps {
  analytics: DriverAnalyticsResponse;
}

function seconds(value: number) {
  if (value <= 0) {
    return "—";
  }

  if (value < 60) {
    return `${value.toFixed(2)}s`;
  }

  return `${(value / 60).toFixed(2)}m`;
}

export function DriverAnalyticsCard({
  analytics,
}: DriverAnalyticsCardProps) {
  const metrics = [
    {
      label: "Total assigned",
      value: analytics.totalAssigned,
    },
    {
      label: "Completed",
      value: analytics.completed,
    },
    {
      label: "Cancelled",
      value: analytics.cancelled,
    },
    {
      label: "Delayed",
      value: analytics.delayed,
    },
    {
      label: "Avg delivery time",
      value: seconds(
        analytics.averageDeliveryTimeSeconds,
      ),
    },
    {
      label: "Avg transit time",
      value: seconds(
        analytics.averageTransitTimeSeconds,
      ),
    },
  ];

  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-900">
      <div className="border-b border-zinc-800 px-5 py-4">
        <h2 className="font-semibold text-zinc-100">
          Driver performance
        </h2>

        <p className="mt-1 text-sm text-zinc-500">
          Aggregated from the Kafka Streams driver
          performance projection.
        </p>
      </div>

      <div className="grid gap-px bg-zinc-800 sm:grid-cols-2 lg:grid-cols-3">
        {metrics.map((metric) => (
          <div
            key={metric.label}
            className="bg-zinc-900 p-5"
          >
            <p className="text-xs uppercase tracking-wide text-zinc-500">
              {metric.label}
            </p>

            <p className="mt-2 text-xl font-semibold text-zinc-100">
              {metric.value}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
