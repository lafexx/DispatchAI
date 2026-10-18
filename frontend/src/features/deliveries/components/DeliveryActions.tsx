import { useState } from "react";
import {
  useCompleteDelivery,
  useStartDelivery,
} from "@/features/deliveries/hooks";
import type { AnalyticsDeliveryStatus } from "@/api";
import { CancelDeliveryDialog } from "./CancelDeliveryDialog";
import { DelayDeliveryDialog } from "./DelayDeliveryDialog";
import { Notice } from "@/components/UI";
import { Icon } from "@/components/Icon";
export function DeliveryActions({
  deliveryId,
  status,
  delayed,
}: {
  deliveryId: string;
  status: AnalyticsDeliveryStatus;
  delayed: boolean;
}) {
  const [delayOpen, setDelayOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const start = useStartDelivery();
  const complete = useCompleteDelivery();
  const active = ["PENDING", "ASSIGNED", "IN_TRANSIT"].includes(status);
  const busy = start.isPending || complete.isPending;
  return (
    <>
      <div className="flex flex-wrap gap-2">
        {status === "ASSIGNED" && (
          <button
            className="button button-primary"
            disabled={busy}
            onClick={() => start.mutate(deliveryId)}
          >
            <Icon name="truck" size={15} />
            {start.isPending ? "Starting…" : "Start delivery"}
          </button>
        )}
        {status === "IN_TRANSIT" && (
          <button
            className="button button-primary"
            disabled={busy}
            onClick={() => complete.mutate(deliveryId)}
          >
            <Icon name="check" size={15} />
            {complete.isPending ? "Completing…" : "Complete delivery"}
          </button>
        )}
        {active && !delayed && (
          <button
            className="button"
            disabled={busy}
            onClick={() => setDelayOpen(true)}
          >
            <Icon name="clock" size={15} />
            Report delay
          </button>
        )}
        {active && (
          <button
            className="button button-danger"
            disabled={busy}
            onClick={() => setCancelOpen(true)}
          >
            Cancel delivery
          </button>
        )}
      </div>
      {(start.isError || complete.isError) && (
        <div className="mt-3">
          <Notice tone="error">
            {start.error?.message ?? complete.error?.message}
          </Notice>
        </div>
      )}
      <DelayDeliveryDialog
        deliveryId={deliveryId}
        open={delayOpen}
        onClose={() => setDelayOpen(false)}
      />
      <CancelDeliveryDialog
        deliveryId={deliveryId}
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
      />
    </>
  );
}
