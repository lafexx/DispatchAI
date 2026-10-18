import { useDelayDelivery } from "@/features/deliveries/hooks";
import { Modal, Notice } from "@/components/UI";
export function DelayDeliveryDialog({
  deliveryId,
  open,
  onClose,
}: {
  deliveryId: string;
  open: boolean;
  onClose: () => void;
}) {
  const mutation = useDelayDelivery();
  return (
    <Modal
      title="Report a delay"
      description="Record the expected delay and the reason so the operation can respond."
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
                delayMinutes: Number(form.get("minutes")),
                reason: String(form.get("reason")).trim(),
              },
            },
            { onSuccess: onClose },
          );
        }}
      >
        <label className="field">
          Expected delay (minutes)
          <input
            name="minutes"
            type="number"
            min={1}
            max={2147483647}
            step={1}
            defaultValue={15}
            required
            disabled={mutation.isPending}
          />
        </label>
        <label className="field">
          Reason
          <textarea
            name="reason"
            rows={3}
            required
            maxLength={1000}
            placeholder="e.g. Heavy traffic near the destination"
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
            Cancel
          </button>
          <button
            className="button button-primary"
            type="submit"
            disabled={mutation.isPending}
          >
            {mutation.isPending ? "Saving…" : "Save delay"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
