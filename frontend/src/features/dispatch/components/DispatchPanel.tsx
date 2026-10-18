import { useState } from "react";
import { Link } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getAssignment, getDispatchEligibility, setParcelWeight } from "@/api";
import { useDispatchAdvisor } from "@/features/agents/hooks";
import { useCreateAssignment, useAutoAssign } from "@/features/dispatch/hooks";
import { useSourceDelivery, refreshOperations } from "@/lib/queries";
import { Panel, Notice, Loading, Avatar } from "@/components/UI";
import { Icon } from "@/components/Icon";
import { StatusBadge } from "@/components/StatusBadge";
import { shortId, duration } from "@/lib/format";
export function DispatchPanel({ deliveryId }: { deliveryId: string }) {
  const source = useSourceDelivery(deliveryId);
  const client = useQueryClient();
  const [driverId, setDriverId] = useState<string | null>(null);
  const [vehicleId, setVehicleId] = useState<string | null>(null);
  const [submittedId, setSubmittedId] = useState<string | null>(null);
  const [weight, setWeight] = useState("");
  const assignment = useQuery({
    queryKey: ["dispatch", "assignments", submittedId],
    queryFn: () => getAssignment(submittedId!),
    enabled: !!submittedId,
    refetchInterval: (q) => (q.state.data?.status === "ACTIVE" ? 2000 : false),
  });
  const waiting =
    !!submittedId &&
    source.data?.status === "PENDING" &&
    (!assignment.data || assignment.data.status === "ACTIVE");
  const eligibility = useQuery({
    queryKey: ["dispatch", "eligibility", deliveryId],
    queryFn: () => getDispatchEligibility(deliveryId),
    enabled: source.data?.status === "PENDING" && !waiting,
    refetchInterval: 5000,
  });
  const advisor = useDispatchAdvisor();
  const manual = useCreateAssignment();
  const automatic = useAutoAssign();
  const weigh = useMutation({
    mutationFn: (value: number) => setParcelWeight(deliveryId, value),
    onSuccess: async (result) => {
      client.setQueryData(["delivery", deliveryId], result);
      advisor.reset();
      setDriverId(null);
      setVehicleId(null);
      setWeight("");
      await refreshOperations(client);
    },
  });
  const busy =
    weigh.isPending ||
    advisor.isPending ||
    manual.isPending ||
    automatic.isPending ||
    waiting;
  const drivers = eligibility.data?.availableDrivers ?? [];
  const vehicles = eligibility.data?.eligibleVehicles ?? [];
  const validDriver = drivers.some((d) => d.id === driverId);
  const validVehicle = vehicles.some((v) => v.id === vehicleId);
  const ready =
    !!eligibility.data &&
    !eligibility.isError &&
    drivers.length > 0 &&
    vehicles.length > 0;
  const stale =
    !!advisor.data &&
    (!drivers.some((d) => d.id === advisor.data?.recommendedDriverId) ||
      !vehicles.some((v) => v.id === advisor.data?.recommendedVehicleId) ||
      advisor.data.parcelWeightKg !== eligibility.data?.parcelWeightKg);
  const onAssigned = (result: { id: string }) => {
    setSubmittedId(result.id);
    void refreshOperations(client);
  };
  if (source.isPending)
    return (
      <Panel>
        <Loading label="Loading delivery…" />
      </Panel>
    );
  if (!source.data)
    return (
      <Notice tone="error">
        {source.error?.message ?? "Delivery not found"}
      </Notice>
    );
  if (source.data.status !== "PENDING")
    return (
      <Panel title={`Delivery #${shortId(deliveryId)}`}>
        <div className="panel-body-topless stack">
          <StatusBadge status={source.data.status} />
          <Notice tone="success">
            This delivery is no longer waiting for dispatch.
          </Notice>
          <Link className="button self-start" to={`/deliveries/${deliveryId}`}>
            View delivery <Icon name="arrow" size={14} />
          </Link>
        </div>
      </Panel>
    );
  return (
    <Panel
      title={`Assign delivery #${shortId(deliveryId)}`}
      subtitle="Choose resources yourself, request advice, or assign the best available fit."
    >
      <div className="dispatch-section">
        <form
          className="flex items-end gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            if (!busy) weigh.mutate(Number(weight));
          }}
        >
          <label className="field flex-1">
            Parcel weight (kg)
            <input
              aria-label="Parcel weight in kilograms"
              type="number"
              min="0.01"
              max="99999999.99"
              step="0.01"
              required
              value={weight}
              placeholder={String(
                source.data.parcelWeightKg ?? "Enter the actual weight",
              )}
              onChange={(e) => setWeight(e.target.value)}
              disabled={busy}
            />
          </label>
          <button className="button" type="submit" disabled={busy || !weight}>
            Save weight
          </button>
        </form>
        {weigh.isError && (
          <div className="mt-3">
            <Notice tone="error">{weigh.error.message}</Notice>
          </div>
        )}
      </div>
      {waiting ? (
        <div className="dispatch-section stack">
          <Notice tone="info">
            <span className="flex items-center gap-2">
              <span className="spinner" />
              Assignment submitted. Waiting for delivery confirmation.
            </span>
          </Notice>
          {assignment.isError && (
            <>
              <Notice tone="warning">
                Assignment status could not be refreshed. Check its status
                before submitting again.
              </Notice>
              <button
                className="button self-start"
                onClick={() => void assignment.refetch()}
              >
                Refresh assignment status
              </button>
            </>
          )}
        </div>
      ) : (
        <>
          {assignment.data?.status === "REJECTED" && (
            <div className="dispatch-section">
              <Notice tone="warning">
                The previous assignment was rejected or expired. Select an
                available pair and retry.
              </Notice>
            </div>
          )}
          <div className="dispatch-section">
            {eligibility.isError && (
              <Notice tone="warning">{eligibility.error.message}</Notice>
            )}
            {eligibility.isPending && (
              <Loading label="Finding eligible resources…" />
            )}
            {eligibility.data && (
              <>
                <div className="section-label">
                  <span>01 / Select a driver</span>
                  <span className="muted">{drivers.length} available</span>
                </div>
                {drivers.length ? (
                  <div className="candidate-grid">
                    {drivers.map((d) => (
                      <button
                        type="button"
                        aria-pressed={driverId === d.id}
                        key={d.id}
                        className={`candidate ${driverId === d.id ? "selected" : ""}`}
                        disabled={busy || eligibility.isError}
                        onClick={() => setDriverId(d.id)}
                      >
                        <Avatar name={d.name} small />
                        <div>
                          <p>{d.name}</p>
                          <small>
                            {advisor.data?.recommendedDriverId === d.id
                              ? "AI recommended"
                              : `#${shortId(d.id)}`}
                          </small>
                        </div>
                        {driverId === d.id && (
                          <Icon
                            name="check"
                            size={16}
                            className="ml-auto text-mint"
                          />
                        )}
                      </button>
                    ))}
                  </div>
                ) : (
                  <p className="muted text-xs">
                    No drivers are currently available.
                  </p>
                )}
              </>
            )}
          </div>
          <div className="dispatch-section">
            <div className="section-label">
              <span>02 / Select a vehicle</span>
              <span className="muted">{vehicles.length} eligible</span>
            </div>
            <label className="field">
              <span className="sr-only">Eligible vehicle</span>
              <select
                aria-label="Eligible vehicle"
                value={validVehicle ? vehicleId! : ""}
                onChange={(e) => setVehicleId(e.target.value || null)}
                disabled={busy || !ready}
              >
                <option value="">Select an available vehicle</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.registration} · {v.type} · {v.capacityKg} kg capacity
                  </option>
                ))}
              </select>
              <small>
                Capacity must meet the{" "}
                {source.data.parcelWeightKg ?? "recorded"} kg parcel weight.
              </small>
            </label>
            {eligibility.data && !vehicles.length && (
              <div className="stack mt-3">
                <p className="text-xs text-amber">
                  No available vehicle can carry this parcel.
                </p>
                <Link
                  className="button self-start"
                  to="/fleet?view=vehicles&create=vehicle"
                >
                  <Icon name="plus" size={14} /> Add vehicle
                </Link>
                <Link className="link-button" to="/fleet?view=vehicles">
                  Review vehicle availability
                </Link>
              </div>
            )}
          </div>
          <div className="dispatch-section stack">
            <div className="flex flex-wrap gap-2">
              <button
                className="button"
                disabled={busy || !ready}
                onClick={() =>
                  advisor.mutate(deliveryId, {
                    onSuccess: (result) => {
                      setDriverId(result.recommendedDriverId);
                      setVehicleId(result.recommendedVehicleId);
                    },
                  })
                }
              >
                <Icon name="spark" size={15} />
                {advisor.isPending ? "Comparing resources…" : "Get AI advice"}
              </button>
              <button
                className="button button-primary"
                disabled={busy || !ready || !validDriver || !validVehicle}
                onClick={() =>
                  manual.mutate(
                    { deliveryId, driverId: driverId!, vehicleId },
                    { onSuccess: onAssigned },
                  )
                }
              >
                {manual.isPending ? "Assigning…" : "Assign selected pair"}
                <Icon name="arrow" size={14} />
              </button>
              <button
                className="button button-ghost"
                disabled={busy || !ready}
                onClick={() =>
                  automatic.mutate(
                    { deliveryId, vehicleId: null },
                    { onSuccess: onAssigned },
                  )
                }
              >
                {automatic.isPending ? "Assigning…" : "Auto assign best fit"}
              </button>
            </div>
            <p className="text-xs muted">
              AI advice requires your assignment action. Auto assign chooses a
              fitting available pair immediately.
            </p>
            {(manual.isError || automatic.isError || advisor.isError) && (
              <Notice tone="error">
                {manual.error?.message ??
                  automatic.error?.message ??
                  advisor.error?.message}
              </Notice>
            )}
            {advisor.data && (
              <div className="advice-card">
                <div className="flex justify-between items-center">
                  <h3 className="flex items-center gap-2">
                    <Icon name="spark" size={16} />
                    Dispatch recommendation
                  </h3>
                  <span className="text-xs muted">Review before assigning</span>
                </div>
                <p>
                  <strong className="text-zinc-100">
                    {advisor.data.recommendedDriverName}
                  </strong>{" "}
                  with{" "}
                  <strong className="text-zinc-100">
                    {advisor.data.recommendedVehicleRegistration}
                  </strong>
                </p>
                <p>{advisor.data.reasoning}</p>
                <div className="stat-row">
                  <span>Capacity match</span>
                  <strong>
                    {advisor.data.parcelWeightKg} /{" "}
                    {advisor.data.vehicleCapacityKg} kg
                  </strong>
                </div>
                {stale && (
                  <div className="mt-3">
                    <Notice tone="warning">
                      This recommendation is no longer current. Request fresh
                      advice.
                    </Notice>
                  </div>
                )}
                <details className="mt-4">
                  <summary className="cursor-pointer text-xs text-zinc-300">
                    Compare driver evidence
                  </summary>
                  <div className="table-scroll mt-3">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Driver</th>
                          <th>Assigned</th>
                          <th>Completed</th>
                          <th>Cancelled</th>
                          <th>Delayed</th>
                          <th>Avg transit</th>
                        </tr>
                      </thead>
                      <tbody>
                        {advisor.data.candidates.map((c) => (
                          <tr key={c.driver.id}>
                            <td>{c.driver.name}</td>
                            <td>{c.analytics?.totalAssigned ?? "—"}</td>
                            <td>{c.analytics?.completed ?? "—"}</td>
                            <td>{c.analytics?.cancelled ?? "—"}</td>
                            <td>{c.analytics?.delayed ?? "—"}</td>
                            <td>
                              {c.analytics && c.analytics.completed > 0
                                ? duration(
                                  c.analytics.averageTransitTimeSeconds,
                                )
                                : "No history"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </details>
              </div>
            )}
          </div>
        </>
      )}
    </Panel>
  );
}

