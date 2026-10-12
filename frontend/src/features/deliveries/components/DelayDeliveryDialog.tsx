import { useState } from "react";
import { useDelayDelivery } from "@/features/deliveries/hooks";

interface DelayDeliveryDialogProps {
  deliveryId: string;
  open: boolean;
  onClose: () => void;
}

export function DelayDeliveryDialog({
  deliveryId,
  open,
  onClose,
}: DelayDeliveryDialogProps) {
  const [delayMinutes, setDelayMinutes] = useState(15);
  const [reason, setReason] = useState("");

  const delayDelivery = useDelayDelivery();

  if (!open) {
    return null;
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    if (delayMinutes <= 0 || !reason.trim()) {
      return;
    }

    delayDelivery.mutate(
      {
        deliveryId,
        input: {
          delayMinutes,
          reason: reason.trim(),
        },
      },
      {
        onSuccess: () => {
          setDelayMinutes(15);
          setReason("");
          onClose();
        },
      },
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-6">
        <div>
          <h2 className="text-lg font-semibold text-zinc-100">
            Mark delivery delayed
          </h2>

          <p className="mt-1 text-sm text-zinc-500">
            Record the expected delay and operational reason.
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="mt-6 space-y-4"
        >
          <div>
            <label
              htmlFor="delay-minutes"
              className="text-sm font-medium text-zinc-300"
            >
              Delay
            </label>

            <div className="mt-2 flex items-center gap-2">
              <input
                id="delay-minutes"
                type="number"
                min={1}
                value={delayMinutes}
                onChange={(event) =>
                  setDelayMinutes(Number(event.target.value))
                }
                className="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 outline-none focus:border-zinc-500"
              />

              <span className="text-sm text-zinc-500">
                minutes
              </span>
            </div>
          </div>

          <div>
            <label
              htmlFor="delay-reason"
              className="text-sm font-medium text-zinc-300"
            >
              Reason
            </label>

            <textarea
              id="delay-reason"
              value={reason}
              onChange={(event) =>
                setReason(event.target.value)
              }
              placeholder="Heavy traffic"
              rows={3}
              className="mt-2 w-full resize-none rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-zinc-500"
            />
          </div>

          {delayDelivery.isError && (
            <p className="text-sm text-red-400">
              Failed to mark delivery as delayed.
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={delayDelivery.isPending}
              className="rounded-lg border border-zinc-700 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-800 disabled:opacity-50"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={
                delayDelivery.isPending ||
                delayMinutes <= 0 ||
                !reason.trim()
              }
              className="rounded-lg bg-amber-400 px-4 py-2 text-sm font-medium text-zinc-950 hover:bg-amber-300 disabled:opacity-50"
            >
              {delayDelivery.isPending
                ? "Saving..."
                : "Mark delayed"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
