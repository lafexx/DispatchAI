import { useState } from "react";

import {
  useAvailableDrivers,
} from "@/features/drivers/hooks";

import {
  useDispatchAdvisor,
} from "@/features/agents/hooks";

import {
  useAutoAssign,
  useCreateAssignment,
} from "@/features/dispatch/hooks";

interface DispatchPanelProps {
  deliveryId: string;
}

function Spinner() {
  return (
    <span
      className="
        h-4 w-4
        animate-spin
        rounded-full
        border-2
        border-current
        border-t-transparent
      "
    />
  );
}

export function DispatchPanel({
  deliveryId,
}: DispatchPanelProps) {
  const [
    selectedDriverId,
    setSelectedDriverId,
  ] = useState<string | null>(null);

  const [
    assignmentSubmitted,
    setAssignmentSubmitted,
  ] = useState(false);

  const availableDrivers =
    useAvailableDrivers();

  const advisor =
    useDispatchAdvisor();

  const manualAssignment =
    useCreateAssignment();

  const automaticAssignment =
    useAutoAssign();

  const recommendedDriverId =
    advisor.data?.recommendedDriverId;

  const busy =
    advisor.isPending ||
    manualAssignment.isPending ||
    automaticAssignment.isPending ||
    assignmentSubmitted;

  function requestRecommendation() {
    advisor.mutate(deliveryId, {
      onSuccess: (result) => {
        setSelectedDriverId(
          result.recommendedDriverId,
        );
      },
    });
  }

  function assignSelectedDriver() {
    if (!selectedDriverId) {
      return;
    }

    manualAssignment.mutate(
      {
        deliveryId,
        driverId: selectedDriverId,
        vehicleId: null,
      },
      {
        onSuccess: () => {
          setAssignmentSubmitted(true);
        },
      },
    );
  }

  function automaticallyAssign() {
    automaticAssignment.mutate(
      {
        deliveryId,
        vehicleId: null,
      },
      {
        onSuccess: () => {
          setAssignmentSubmitted(true);
        },
      },
    );
  }

  return (
    <section className="rounded-xl border border-zinc-800 bg-zinc-900">
      <div className="border-b border-zinc-800 px-6 py-4">
        <h2 className="font-semibold text-zinc-100">
          Dispatch
        </h2>

        <p className="mt-1 text-sm text-zinc-500">
          Select a driver manually, request an
          AI-assisted recommendation, or let the
          dispatch service assign automatically.
        </p>
      </div>

      <div className="space-y-6 p-6">
        {assignmentSubmitted ? (
          <div className="flex items-center gap-3 rounded-lg border border-zinc-700 bg-zinc-950 p-4">
            <Spinner />

            <div>
              <p className="text-sm font-medium text-zinc-200">
                Assignment created
              </p>

              <p className="mt-1 text-xs text-zinc-500">
                Waiting for the delivery state to
                update...
              </p>
            </div>
          </div>
        ) : (
          <>
            <div>
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium text-zinc-300">
                  Available drivers
                </h3>

                <span className="text-xs text-zinc-600">
                  {availableDrivers.data?.length ??
                    0}{" "}
                  available
                </span>
              </div>

              {availableDrivers.isLoading && (
                <div className="mt-4 flex items-center gap-2 text-sm text-zinc-500">
                  <Spinner />
                  Loading available drivers...
                </div>
              )}

              {availableDrivers.isError && (
                <p className="mt-4 text-sm text-red-400">
                  Failed to load available drivers.
                </p>
              )}

              <div className="mt-3 space-y-2">
                {availableDrivers.data?.map(
                  (driver) => {
                    const selected =
                      selectedDriverId ===
                      driver.id;

                    const recommended =
                      recommendedDriverId ===
                      driver.id;

                    return (
                      <button
                        key={driver.id}
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          setSelectedDriverId(
                            driver.id,
                          )
                        }
                        className={[
                          "flex w-full items-center justify-between gap-4 rounded-lg border p-4 text-left transition",
                          selected
                            ? "border-zinc-500 bg-zinc-800"
                            : "border-zinc-800 bg-zinc-950 hover:border-zinc-700 hover:bg-zinc-900",
                          busy
                            ? "cursor-not-allowed opacity-60"
                            : "",
                        ].join(" ")}
                      >
                        <div className="min-w-0">
                          <p className="font-medium text-zinc-200">
                            {driver.name}
                          </p>

                          <p className="mt-1 truncate font-mono text-xs text-zinc-600">
                            {driver.id}
                          </p>
                        </div>

                        <div className="flex items-center gap-2">
                          {recommended && (
                            <span className="rounded-full border border-emerald-800 bg-emerald-950 px-2.5 py-1 text-xs text-emerald-300">
                              Recommended
                            </span>
                          )}

                          {selected && (
                            <span className="text-xs text-zinc-400">
                              Selected
                            </span>
                          )}
                        </div>
                      </button>
                    );
                  },
                )}
              </div>

              {availableDrivers.data?.length ===
                0 && (
                  <p className="mt-4 text-sm text-zinc-500">
                    No drivers are currently
                    available.
                  </p>
                )}
            </div>

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                disabled={busy}
                onClick={requestRecommendation}
                className="
                  flex items-center gap-2
                  rounded-lg
                  border border-zinc-700
                  px-4 py-2
                  text-sm font-medium
                  text-zinc-300
                  hover:bg-zinc-800
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                "
              >
                {advisor.isPending && (
                  <Spinner />
                )}

                {advisor.isPending
                  ? "Analysing drivers..."
                  : "Get AI recommendation"}
              </button>

              <button
                type="button"
                disabled={
                  busy ||
                  !selectedDriverId
                }
                onClick={assignSelectedDriver}
                className="
                  flex items-center gap-2
                  rounded-lg
                  bg-zinc-100
                  px-4 py-2
                  text-sm font-medium
                  text-zinc-950
                  hover:bg-white
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                "
              >
                {manualAssignment.isPending && (
                  <Spinner />
                )}

                {manualAssignment.isPending
                  ? "Assigning driver..."
                  : "Assign selected driver"}
              </button>

              <button
                type="button"
                disabled={busy}
                onClick={automaticallyAssign}
                className="
                  flex items-center gap-2
                  rounded-lg
                  border border-zinc-700
                  bg-zinc-800
                  px-4 py-2
                  text-sm font-medium
                  text-zinc-300
                  hover:bg-zinc-700
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                "
              >
                {automaticAssignment.isPending && (
                  <Spinner />
                )}

                {automaticAssignment.isPending
                  ? "Selecting driver..."
                  : "Auto assign"}
              </button>
            </div>

            {advisor.data && (
              <div className="rounded-lg border border-zinc-800 bg-zinc-950 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">
                    AI recommendation
                  </p>

                  <span className="text-xs text-zinc-600">
                    Human approval required
                  </span>
                </div>

                <p className="mt-3 font-medium text-zinc-200">
                  {
                    advisor.data
                      .recommendedDriverName
                  }
                </p>

                <p className="mt-2 font-mono text-xs text-zinc-600">
                  {
                    advisor.data
                      .recommendedDriverId
                  }
                </p>

                <p className="mt-3 text-sm leading-6 text-zinc-400">
                  {advisor.data.reasoning}
                </p>
              </div>
            )}

            {advisor.isError && (
              <div className="rounded-lg border border-red-900 bg-red-950/30 p-4 text-sm text-red-300">
                Failed to get an AI
                recommendation.
              </div>
            )}

            {(manualAssignment.isError ||
              automaticAssignment.isError) && (
                <div className="rounded-lg border border-red-900 bg-red-950/30 p-4 text-sm text-red-300">
                  Driver assignment failed.
                </div>
              )}
          </>
        )}
      </div>
    </section>
  );
}
