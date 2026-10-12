import { useState } from "react";

import { useDrivers } from "@/features/drivers/hooks";
import { DriverDetails } from "@/features/drivers/components";
import { StatusBadge } from "@/components";

export function DriversPage() {
  const drivers = useDrivers();

  const [selectedDriverId, setSelectedDriverId] =
    useState<string | null>(null);

  if (drivers.isLoading) {
    return (
      <main className="min-h-screen bg-zinc-950 text-zinc-100">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          Loading drivers...
        </div>
      </main>
    );
  }

  if (drivers.isError) {
    return (
      <main className="min-h-screen bg-zinc-950 text-zinc-100">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          Failed to load drivers.
        </div>
      </main>
    );
  }

  const data = drivers.data ?? [];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Drivers
        </h1>

        <p className="mt-1 text-sm text-zinc-500">
          Fleet availability and aggregated driver
          performance.
        </p>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[320px_1fr]">
        <div className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900">
          {data.map((driver) => (
            <button
              key={driver.id}
              type="button"
              onClick={() =>
                setSelectedDriverId(driver.id)
              }
              className={[
                "flex w-full items-center justify-between gap-4 border-b border-zinc-800 px-4 py-4 text-left last:border-b-0",
                selectedDriverId === driver.id
                  ? "bg-zinc-800"
                  : "hover:bg-zinc-800/50",
              ].join(" ")}
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-zinc-200">
                  {driver.name}
                </p>

                <p className="mt-1 truncate font-mono text-xs text-zinc-600">
                  {driver.id}
                </p>
              </div>

              <StatusBadge
                status={driver.availability}
              />
            </button>
          ))}

          {data.length === 0 && (
            <div className="p-6 text-center text-sm text-zinc-500">
              No drivers found.
            </div>
          )}
        </div>

        <div>
          {selectedDriverId ? (
            <DriverDetails
              driverId={selectedDriverId}
            />
          ) : (
            <div className="flex min-h-64 items-center justify-center rounded-xl border border-dashed border-zinc-800 text-sm text-zinc-600">
              Select a driver to view performance.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
