import { useState } from "react";
import { useCancelDelivery } from "@/features/deliveries/hooks";
import type { CancellationReason } from "@/api";

interface CancelDeliveryDialogProps {
  deliveryId: string;
  open: boolean;
  onClose: () => void;
}

export function CancelDeliveryDialog({
  deliveryId,
  open,
  onClose,
}: CancelDeliveryDialogProps) {
  const [reason, setReason] =
    useState<CancellationReason>("CUSTOMER_REQUEST");

  const [notes, setNotes] = useState("");

  const cancelDelivery = useCancelDelivery();

  if (!open) {
    return null;
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    cancelDelivery.mutate(
      {
        deliveryId,
        input: {
          reason,
          notes: notes.trim() || undefined,
        },
      },
      {
        onSuccess: () => {
          setNotes("");
          onClose();
        },
      },
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <h2 className="text-lg font-semibold text-zinc-100">
          Cancel delivery
        </h2>

        <p className="mt-1 text-sm text-zinc-500">
          Cancelling the delivery will terminate its active
          lifecycle.
        </p>

        <form
          onSubmit={handleSubmit}
          className="mt-6 space-y-4"
        >
          <div>
            <label
              htmlFor="cancellation-reason"
              className="text-sm font-medium text-zinc-300"
            >
              Reason
            </label>

            <select
              id="cancellation-reason"
              value={reason}
              onChange={(event) =>
                setReason(
                  event.target.value as CancellationReason,
                )
              }
              className="mt-2 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 outline-none focus:border-zinc-500"
            >
              <option value="CUSTOMER_REQUEST">
                Customer request
              </option>

              <option value="DRIVER_UNAVAILABLE">
                Driver unavailable
              </option>

              <option value="OPERATIONAL">
                Operational
              </option>
            </select>
          </div>

          <div>
            <label
              htmlFor="cancellation-notes"
              className="text-sm font-medium text-zinc-300"
            >
              Notes
            </label>

            <textarea
              id="cancellation-notes"
              value={notes}
              onChange={(event) =>
                setNotes(event.target.value)
              }
              rows={3}
              placeholder="Optional additional information"
              className="mt-2 w-full resize-none rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-zinc-500"
            />
          </div>

          {cancelDelivery.isError && (
            <p className="text-sm text-red-400">
              Failed to cancel delivery.
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={cancelDelivery.isPending}
              className="rounded-lg border border-zinc-700 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-800 disabled:opacity-50"
            >
              Back
            </button>

            <button
              type="submit"
              disabled={cancelDelivery.isPending}
              className="rounded-lg bg-red-500 px-4 py-2 text-sm font-medium text-white hover:bg-red-400 disabled:opacity-50"
            >
              {cancelDelivery.isPending
                ? "Cancelling..."
                : "Cancel delivery"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
