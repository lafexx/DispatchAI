import {
  useDriver,
  useDriverAnalytics,
} from "@/features/drivers/hooks";

import { StatusBadge } from "@/components";
import { DriverAnalyticsCard } from "./DriverAnalyticsCard";

interface DriverDetailsProps {
  driverId: string;
}

export function DriverDetails({
  driverId,
}: DriverDetailsProps) {
  const driver = useDriver(driverId);
  const analytics = useDriverAnalytics(driverId);

  if (driver.isLoading) {
    return (
      <div className="text-sm text-zinc-500">
        Loading driver...
      </div>
    );
  }

  if (driver.isError || !driver.data) {
    return (
      <div className="rounded-xl border border-red-900 bg-red-950/30 p-6 text-red-300">
        Failed to load driver.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm text-zinc-500">
              Driver
            </p>

            <h1 className="mt-1 text-xl font-semibold text-zinc-100">
              {driver.data.name}
            </h1>

            <p className="mt-2 font-mono text-xs text-zinc-500">
              {driver.data.id}
            </p>
          </div>

          <StatusBadge
            status={driver.data.availability}
          />
        </div>
      </div>

      {analytics.isLoading && (
        <div className="text-sm text-zinc-500">
          Loading performance data...
        </div>
      )}

      {analytics.isError && (
        <div className="rounded-xl border border-red-900 bg-red-950/30 p-5 text-sm text-red-300">
          Failed to load driver analytics.
        </div>
      )}

      {analytics.data && (
        <DriverAnalyticsCard
          analytics={analytics.data}
        />
      )}
    </div>
  );
}
