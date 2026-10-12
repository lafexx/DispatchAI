import { useState } from "react";

import {
  useCompleteDelivery,
  useStartDelivery,
} from "@/features/deliveries/hooks";

import type { AnalyticsDeliveryStatus } from "@/api";

import { CancelDeliveryDialog } from "./CancelDeliveryDialog";
import { DelayDeliveryDialog } from "./DelayDeliveryDialog";

interface DeliveryActionsProps {
  deliveryId: string;
  status: AnalyticsDeliveryStatus;
  delayed: boolean;
}

export function DeliveryActions({
  deliveryId,
  status,
  delayed,
}: DeliveryActionsProps) {
  const [delayDialogOpen, setDelayDialogOpen] =
    useState(false);

  const [cancelDialogOpen, setCancelDialogOpen] =
    useState(false);

  const startDelivery = useStartDelivery();
  const completeDelivery = useCompleteDelivery();

  const active =
    status === "PENDING" ||
    status === "ASSIGNED" ||
    status === "IN_TRANSIT";

  return (
    <>
      <div className="flex flex-wrap gap-3">
        {status === "ASSIGNED" && (
          <button
            type="button"
            onClick={() =>
              startDelivery.mutate(deliveryId)
            }
            disabled={startDelivery.isPending}
            className="rounded-lg bg-zinc-100 px-4 py-2 text-sm font-medium text-zinc-950 hover:bg-white disabled:opacity-50"
          >
            {startDelivery.isPending
              ? "Starting..."
              : "Start delivery"}
          </button>
        )}

        {status === "IN_TRANSIT" && (
          <button
            type="button"
            onClick={() =>
              completeDelivery.mutate(deliveryId)
            }
            disabled={completeDelivery.isPending}
            className="rounded-lg bg-emerald-400 px-4 py-2 text-sm font-medium text-zinc-950 hover:bg-emerald-300 disabled:opacity-50"
          >
            {completeDelivery.isPending
              ? "Completing..."
              : "Complete delivery"}
          </button>
        )}

        {active && !delayed && (
          <button
            type="button"
            onClick={() =>
              setDelayDialogOpen(true)
            }
            className="rounded-lg border border-amber-800 bg-amber-950/40 px-4 py-2 text-sm font-medium text-amber-300 hover:bg-amber-950"
          >
            Mark delayed
          </button>
        )}

        {active && (
          <button
            type="button"
            onClick={() =>
              setCancelDialogOpen(true)
            }
            className="rounded-lg border border-red-900 bg-red-950/30 px-4 py-2 text-sm font-medium text-red-300 hover:bg-red-950/60"
          >
            Cancel delivery
          </button>
        )}
      </div>

      {(startDelivery.isError ||
        completeDelivery.isError) && (
          <p className="mt-3 text-sm text-red-400">
            Delivery operation failed.
          </p>
        )}

      <DelayDeliveryDialog
        deliveryId={deliveryId}
        open={delayDialogOpen}
        onClose={() =>
          setDelayDialogOpen(false)
        }
      />

      <CancelDeliveryDialog
        deliveryId={deliveryId}
        open={cancelDialogOpen}
        onClose={() =>
          setCancelDialogOpen(false)
        }
      />
    </>
  );
}
