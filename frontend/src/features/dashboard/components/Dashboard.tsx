import {
  AgentPanel,
  DeliveryTable,
  DriverTable,
  MetricCard,
} from "@/components";

import {
  useDeliveries,
  useDeliverySummary,
} from "@/features/deliveries/hooks";

import {
  useDrivers,
} from "@/features/drivers/hooks";

function percentage(value: number) {
  return `${(value * 100).toFixed(1)}%`;
}

export function Dashboard() {
  const summary = useDeliverySummary();
  const deliveries = useDeliveries();
  const drivers = useDrivers();

  const isLoading =
    summary.isLoading ||
    deliveries.isLoading ||
    drivers.isLoading;

  if (isLoading) {
    return (
      <div className="flex min-h-64 items-center justify-center text-sm text-zinc-500">
        Loading operational data...
      </div>
    );
  }

  if (
    summary.isError ||
    deliveries.isError ||
    drivers.isError
  ) {
    return (
      <div className="rounded-xl border border-red-900 bg-red-950/30 p-6 text-red-300">
        Failed to load dashboard data.
      </div>
    );
  }

  const summaryData = summary.data;
  const deliveryData = deliveries.data ?? [];
  const driverData = drivers.data ?? [];

  const activeDeliveries =
    (summaryData?.assigned ?? 0) +
    (summaryData?.inTransit ?? 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-zinc-100">
          Operations dashboard
        </h1>

        <p className="mt-1 text-sm text-zinc-500">
          Live delivery, fleet, analytics, and agent
          state.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Total deliveries"
          value={summaryData?.total ?? 0}
        />

        <MetricCard
          label="Active deliveries"
          value={activeDeliveries}
          description="Assigned or currently in transit"
        />

        <MetricCard
          label="Completed"
          value={summaryData?.completed ?? 0}
          description={
            summaryData
              ? `${percentage(summaryData.completionRate)} completion rate`
              : undefined
          }
        />

        <MetricCard
          label="Delayed"
          value={summaryData?.delayed ?? 0}
          description={
            summaryData
              ? `${summaryData.averageDelayMinutes.toFixed(1)} min average`
              : undefined
          }
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[2fr_1fr]">
        <DeliveryTable
          deliveries={deliveryData}
        />

        <div className="grid gap-4 sm:grid-cols-3 xl:grid-cols-1">
          <MetricCard
            label="Completion rate"
            value={
              summaryData
                ? percentage(
                  summaryData.completionRate,
                )
                : "—"
            }
          />

          <MetricCard
            label="Cancellation rate"
            value={
              summaryData
                ? percentage(
                  summaryData.cancellationRate,
                )
                : "—"
            }
          />

          <MetricCard
            label="Delay rate"
            value={
              summaryData
                ? percentage(
                  summaryData.delayRate,
                )
                : "—"
            }
          />
        </div>
      </div>

      <DriverTable drivers={driverData} />

      <AgentPanel />
    </div>
  );
}
