import { useState } from "react";
import { useSearchParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createDriver,
  createVehicle,
  setVehicleAvailability,
  type VehicleResponse,
} from "@/api";
import { useDrivers } from "@/features/drivers/hooks";
import { useVehicles, refreshOperations } from "@/lib/queries";
import {
  PageHeader,
  Panel,
  Notice,
  Loading,
  Modal,
  Avatar,
  EmptyState,
} from "@/components/UI";
import { Icon } from "@/components/Icon";
import { MetricCard } from "@/components/MetricCard";
import { StatusBadge } from "@/components/StatusBadge";
import { DriverDetails } from "@/features/drivers/components/DriverDetails";
import { shortId, number } from "@/lib/format";
function ResourceModal({
  kind,
  onClose,
  onCreated,
}: {
  kind: "driver" | "vehicle";
  onClose: () => void;
  onCreated: (id: string, close?: boolean) => void;
}) {
  const client = useQueryClient();
  const [ready, setReady] = useState(true);
  const [savedVehicle, setSavedVehicle] = useState<VehicleResponse | null>(
    null,
  );
  const mutation = useMutation({
    mutationFn: async (form: FormData): Promise<{ id: string }> => {
      if (kind === "driver")
        return createDriver({ name: String(form.get("name")).trim() });
      const vehicle =
        savedVehicle ??
        (await createVehicle({
          registration: String(form.get("registration")).trim(),
          type: String(form.get("type")) as VehicleResponse["type"],
          capacityKg: Number(form.get("capacity")),
        }));
      setSavedVehicle(vehicle);
      onCreated(vehicle.id);
      void refreshOperations(client);
      if (ready) {
        const updated = await setVehicleAvailability(vehicle.id, true);
        if (!updated.available)
          throw new Error(
            "Vehicle saved, but it is currently assigned. Its availability preference has been recorded.",
          );
      }
      return vehicle;
    },
    onSuccess: async (value) => {
      await refreshOperations(client);
      onCreated(value.id, true);
    },
  });
  return (
    <Modal
      open
      title={kind === "driver" ? "Add a driver" : "Add a vehicle"}
      description={
        kind === "driver"
          ? "New drivers start unavailable. Mark them available when they are ready for dispatch."
          : "Set the vehicle's capacity and choose whether it is ready for dispatch."
      }
      onClose={onClose}
      busy={mutation.isPending}
    >
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          if (!mutation.isPending)
            mutation.mutate(new FormData(e.currentTarget));
        }}
      >
        {kind === "driver" ? (
          <label className="field">
            Driver name
            <input
              name="name"
              required
              maxLength={150}
              disabled={mutation.isPending}
            />
          </label>
        ) : (
          <>
            <label className="field">
              Registration
              <input
                name="registration"
                required
                maxLength={80}
                disabled={mutation.isPending || !!savedVehicle}
              />
            </label>
            <div className="form-grid">
              <label className="field">
                Vehicle type
                <select
                  name="type"
                  defaultValue="VAN"
                  disabled={mutation.isPending || !!savedVehicle}
                >
                  <option value="CAR">Car</option>
                  <option value="VAN">Van</option>
                  <option value="TRUCK">Truck</option>
                </select>
              </label>
              <label className="field">
                Capacity (kg)
                <input
                  name="capacity"
                  type="number"
                  min="0.01"
                  max="99999999.99"
                  step="0.01"
                  required
                  disabled={mutation.isPending || !!savedVehicle}
                />
              </label>
            </div>
            <label className="flex items-center gap-3 text-sm">
              <input
                type="checkbox"
                checked={ready}
                onChange={(e) => setReady(e.target.checked)}
                disabled={mutation.isPending || !!savedVehicle}
              />
              Make available for dispatch
            </label>
          </>
        )}
        {mutation.isError && (
          <Notice tone="error">
            {savedVehicle
              ? `${savedVehicle.registration} was created. Availability could not be confirmed: `
              : ""}
            {mutation.error.message}
          </Notice>
        )}
        <div className="form-actions">
          <button
            className="button"
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
          >
            {savedVehicle ? "Close" : "Cancel"}
          </button>
          <button
            className="button button-primary"
            type="submit"
            disabled={mutation.isPending}
          >
            {mutation.isPending
              ? "Saving…"
              : kind === "driver"
                ? "Add driver"
                : savedVehicle
                  ? "Retry availability"
                  : "Add vehicle"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
export function FleetPage() {
  const drivers = useDrivers();
  const vehicles = useVehicles();
  const client = useQueryClient();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState("");
  const requestedModal = params.get("create");
  const modal =
    requestedModal === "driver" || requestedModal === "vehicle"
      ? requestedModal
      : null;
  function openModal(kind: "driver" | "vehicle") {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      next.set("create", kind);
      return next;
    });
  }
  function closeModal() {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      next.delete("create");
      return next;
    });
  }
  const [feedback, setFeedback] = useState("");
  const view = params.get("view") === "vehicles" ? "vehicles" : "drivers";
  const availability = useMutation({
    mutationFn: ({ id, available }: { id: string; available: boolean }) =>
      setVehicleAvailability(id, available),
    onSuccess: async (result, request) => {
      setFeedback(
        request.available && !result.available
          ? "Availability preference saved. This vehicle remains unavailable while assigned."
          : `${result.registration}: availability updated.`,
      );
      await refreshOperations(client);
    },
  });
  const driverData = drivers.data ?? [];
  const vehicleData = vehicles.data ?? [];
  const readyDrivers = driverData.filter(
    (d) => d.availability === "AVAILABLE",
  ).length;
  const readyVehicles = vehicleData.filter((v) => v.available).length;
  const filtered = driverData.filter((d) =>
    `${d.name} ${d.id}`.toLowerCase().includes(search.toLowerCase()),
  );
  const filteredVehicles = vehicleData.filter((v) =>
    `${v.registration} ${v.type}`.toLowerCase().includes(search.toLowerCase()),
  );
  const selected =
    filtered.find((d) => d.id === params.get("driver"))?.id ?? filtered[0]?.id;
  return (
    <div className="stack">
      <PageHeader
        eyebrow="PEOPLE & RESOURCES"
        title="Fleet"
        description="Keep drivers and vehicles ready for the next delivery."
        actions={
          <>
            <button className="button" onClick={() => openModal("driver")}>
              <Icon name="plus" size={16} /> Add driver
            </button>
            <button
              className="button button-primary"
              onClick={() => openModal("vehicle")}
            >
              <Icon name="plus" size={16} /> Add vehicle
            </button>
          </>
        }
      />
      <div className="grid-four">
        <MetricCard
          label="Total drivers"
          value={drivers.data ? number(driverData.length) : "—"}
          description="Registered in the fleet"
          icon="users"
          tone="blue"
        />
        <MetricCard
          label="Drivers ready"
          value={drivers.data ? readyDrivers : "—"}
          description="Available for a new assignment"
          icon="check"
        />
        <MetricCard
          label="Total vehicles"
          value={vehicles.data ? number(vehicleData.length) : "—"}
          description="Cars, vans and trucks"
          icon="truck"
          tone="purple"
        />
        <MetricCard
          label="Vehicles ready"
          value={vehicles.data ? readyVehicles : "—"}
          description="Available for capacity matching"
          icon="weight"
          tone="amber"
        />
      </div>
      <div className="flex items-center justify-between gap-4">
        <div className="filter-tabs">
          {["drivers", "vehicles"].map((tab) => (
            <button
              key={tab}
              className={`filter-tab ${view === tab ? "active" : ""}`}
              onClick={() => {
                setSearch("");
                setParams(tab === "vehicles" ? { view: "vehicles" } : {});
              }}
            >
              {tab === "drivers" ? "Drivers" : "Vehicles"}
              <span>
                {tab === "drivers" ? driverData.length : vehicleData.length}
              </span>
            </button>
          ))}
        </div>
        <div className="search-field">
          <Icon name="search" size={15} />
          <input
            aria-label={`Search ${view}`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={
              view === "drivers"
                ? "Search driver name or ID…"
                : "Search registration or type…"
            }
          />
        </div>
      </div>
      {view === "drivers" ? (
        <>
          {drivers.isError && (
            <Notice tone="warning">Drivers could not be refreshed.</Notice>
          )}
          {drivers.isPending ? (
            <Loading label="Loading drivers…" />
          ) : !drivers.data ? (
            <Panel>
              <EmptyState
                title="Driver directory unavailable"
                description="Try refreshing in a moment."
                icon="users"
              />
            </Panel>
          ) : (
            <div className="fleet-layout">
              <Panel
                title="Driver directory"
                subtitle={`${filtered.length} drivers`}
              >
                {filtered.length ? (
                  filtered.map((d) => (
                    <button
                      key={d.id}
                      className={`queue-item ${selected === d.id ? "active" : ""}`}
                      onClick={() => setParams({ driver: d.id })}
                    >
                      <span className="person-cell">
                        <Avatar name={d.name} small />
                        <span>
                          <strong className="text-xs">{d.name}</strong>
                          <small className="mono">{shortId(d.id)}</small>
                        </span>
                      </span>
                      <div className="mt-3">
                        <StatusBadge status={d.availability} />
                      </div>
                    </button>
                  ))
                ) : (
                  <EmptyState title="No drivers found" icon="users" />
                )}
              </Panel>
              {selected ? (
                <DriverDetails key={selected} driverId={selected} />
              ) : (
                <Panel>
                  <EmptyState
                    title="Build your driver team"
                    description="Add a driver to view availability and performance."
                    icon="users"
                  />
                </Panel>
              )}
            </div>
          )}
        </>
      ) : (
        <>
          {vehicles.isError && (
            <Notice tone="warning">Vehicles could not be refreshed.</Notice>
          )}
          {feedback && <Notice tone="info">{feedback}</Notice>}
          {availability.isError && (
            <Notice tone="error">{availability.error.message}</Notice>
          )}
          {vehicles.isPending ? (
            <Loading label="Loading vehicles…" />
          ) : !vehicles.data ? (
            <Panel>
              <EmptyState
                title="Vehicle directory unavailable"
                description="Try refreshing in a moment."
                icon="truck"
              />
            </Panel>
          ) : (
            <Panel
              title="Vehicle directory"
              subtitle="Availability reflects manual readiness and current assignments"
            >
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Vehicle</th>
                      <th>Type</th>
                      <th>Capacity</th>
                      <th>Availability</th>
                      <th>Readiness</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredVehicles.map((v) => (
                      <tr key={v.id}>
                        <td>
                          <span className="cell-primary">{v.registration}</span>
                          <span className="cell-secondary mono">
                            {shortId(v.id)}
                          </span>
                        </td>
                        <td>{v.type}</td>
                        <td>{number(v.capacityKg)} kg</td>
                        <td>
                          <StatusBadge
                            status={v.available ? "AVAILABLE" : "UNAVAILABLE"}
                          />
                        </td>
                        <td>
                          <div className="flex gap-2">
                            <button
                              className="button button-small"
                              disabled={availability.isPending}
                              onClick={() =>
                                availability.mutate({
                                  id: v.id,
                                  available: true,
                                })
                              }
                              aria-label={`Mark ${v.registration} available`}
                            >
                              Available
                            </button>
                            <button
                              className="button button-small button-ghost"
                              disabled={availability.isPending}
                              onClick={() =>
                                availability.mutate({
                                  id: v.id,
                                  available: false,
                                })
                              }
                              aria-label={`Mark ${v.registration} unavailable`}
                            >
                              Unavailable
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {filteredVehicles.length === 0 && (
                  <EmptyState
                    title={
                      vehicleData.length
                        ? "No matching vehicles"
                        : "Add your first vehicle"
                    }
                    description={
                      vehicleData.length
                        ? "Try a different search."
                        : "Set capacity and availability so dispatch can match parcels."
                    }
                    icon="truck"
                    action={
                      <button
                        className="button button-primary"
                        onClick={() => openModal("vehicle")}
                      >
                        Add vehicle
                      </button>
                    }
                  />
                )}
              </div>
            </Panel>
          )}
        </>
      )}
      {modal && (
        <ResourceModal
          key={modal}
          kind={modal}
          onClose={closeModal}
          onCreated={(id, close) => {
            setSearch("");
            setParams((previous) => {
              const next = new URLSearchParams(previous);
              if (close) next.delete("create");
              if (modal === "driver") {
                next.delete("view");
                next.set("driver", id);
              } else {
                next.set("view", "vehicles");
                next.delete("driver");
              }
              return next;
            });
          }}
        />
      )}
    </div>
  );
}

