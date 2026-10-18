import { useCancelDelivery } from "@/features/deliveries/hooks";
import { Modal, Notice } from "@/components/UI";
import type { CancellationReason } from "@/api";
export function CancelDeliveryDialog({
  deliveryId,
  open,
  onClose,
}: {
  deliveryId: string;
  open: boolean;
  onClose: () => void;
}) {
  const mutation = useCancelDelivery();
  return (
    <Modal
      title="Cancel delivery"
      description="This ends the delivery and releases its assigned resources. It cannot be restarted."
      open={open}
      onClose={onClose}
      busy={mutation.isPending}
    >
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          if (mutation.isPending) return;
          const form = new FormData(e.currentTarget);
          mutation.mutate(
            {
              deliveryId,
              input: {
                reason: String(form.get("reason")) as CancellationReason,
                notes: String(form.get("notes")).trim() || undefined,
              },
            },
            { onSuccess: onClose },
          );
        }}
      >
        <label className="field">
          Reason
          <select
            name="reason"
            defaultValue="CUSTOMER_REQUEST"
            disabled={mutation.isPending}
          >
            <option value="CUSTOMER_REQUEST">Customer request</option>
            <option value="DRIVER_UNAVAILABLE">Driver unavailable</option>
            <option value="OPERATIONAL">Operational</option>
          </select>
        </label>
        <label className="field">
          Notes
          <textarea
            name="notes"
            rows={3}
            maxLength={1000}
            placeholder="Optional context for the cancellation"
            disabled={mutation.isPending}
          />
        </label>
        {mutation.isError && (
          <Notice tone="error">{mutation.error.message}</Notice>
        )}
        <div className="form-actions">
          <button
            className="button"
            type="button"
            disabled={mutation.isPending}
            onClick={onClose}
          >
            Keep delivery
          </button>
          <button
            className="button button-danger"
            type="submit"
            disabled={mutation.isPending}
          >
            {mutation.isPending ? "Cancelling…" : "Confirm cancellation"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
