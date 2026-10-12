import { StatusBadge } from "@/components";

import {
  useDelivery,
} from "@/features/deliveries/hooks";

import { DeliveryActions } from "./DeliveryActions";
import { DispatchPanel } from "@/features/dispatch/components";

interface DeliveryDetailsProps {
  deliveryId: string;
}

function formatDate(value: string | null) {
  if (!value) {
    return "—";
  }

  return new Date(value).toLocaleString();
}

export function DeliveryDetails({
  deliveryId,
}: DeliveryDetailsProps) {
  const delivery = useDelivery(deliveryId);

  if (delivery.isLoading) {
    return (
      <div className="text-sm text-zinc-500">
        Loading delivery...
      </div>
    );
  }

  if (delivery.isError || !delivery.data) {
    return (
      <div className="rounded-xl border border-red-900 bg-red-950/30 p-6 text-red-300">
        Failed to load delivery.
      </div>
    );
  }

  const data = delivery.data;

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-zinc-500">
          Delivery details
        </p>

        <h1 className="mt-1 font-mono text-xl font-semibold text-zinc-100">
          {data.deliveryId}
        </h1>
      </div>

      <div className="rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm text-zinc-500">
              Current status
            </p>

            <div className="mt-2">
              <StatusBadge status={data.status} />
            </div>
          </div>
        </div>

        <dl className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <Detail
            label="Driver"
            value={data.driverId ?? "Unassigned"}
            mono
          />

          <Detail
            label="Delayed"
            value={data.delayed ? "Yes" : "No"}
          />

          <Detail
            label="Delay"
            value={
              data.delayMinutes != null
                ? `${data.delayMinutes} minutes`
                : "—"
            }
          />

          <Detail
            label="Delay reason"
            value={data.delayReason ?? "—"}
          />

          <Detail
            label="Created"
            value={formatDate(data.createdAt)}
          />

          <Detail
            label="Assigned"
            value={formatDate(data.assignedAt)}
          />

          <Detail
            label="Started"
            value={formatDate(data.startedAt)}
          />

          <Detail
            label="Completed"
            value={formatDate(data.completedAt)}
          />
        </dl>
      </div>

      {data.status === "PENDING" ? (
        <DispatchPanel
          deliveryId={deliveryId}
        />
      ) : (
        <div className="rounded-xl border border-zinc-800 bg-zinc-900 p-6">
          <h2 className="font-semibold text-zinc-100">
            Delivery actions
          </h2>

          <div className="mt-4">
            <DeliveryActions
              deliveryId={deliveryId}
              status={data.status}
              delayed={data.delayed}
            />
          </div>
        </div>
      )}
    </div>
  );
}

interface DetailProps {
  label: string;
  value: string;
  mono?: boolean;
}

function Detail({
  label,
  value,
  mono,
}: DetailProps) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-zinc-500">
        {label}
      </dt>

      <dd
        className={[
          "mt-1 break-all text-sm text-zinc-200",
          mono ? "font-mono" : "",
        ].join(" ")}
      >
        {value}
      </dd>
    </div>
  );
}
