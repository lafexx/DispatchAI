import { useNavigate } from "react-router";
import type { DeliveryAnalyticsState } from "@/api";
import { StatusBadge } from "./StatusBadge";

interface DeliveryTableProps {
  deliveries: DeliveryAnalyticsState[];
}

export function DeliveryTable({
  deliveries,
}: DeliveryTableProps) {
  const navigate = useNavigate();

  return (
    <div className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900">
      <div className="border-b border-zinc-800 px-5 py-4">
        <h2 className="font-semibold text-zinc-100">
          Deliveries
        </h2>

        <p className="mt-1 text-sm text-zinc-500">
          Current state from the analytics projection.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-zinc-800 bg-zinc-950/40 text-zinc-400">
            <tr>
              <th className="px-5 py-3 font-medium">
                Delivery
              </th>

              <th className="px-5 py-3 font-medium">
                Status
              </th>

              <th className="px-5 py-3 font-medium">
                Driver
              </th>

              <th className="px-5 py-3 font-medium">
                Delay
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-zinc-800">
            {deliveries.map((delivery) => (
              <tr
                key={delivery.deliveryId}
                onClick={() =>
                  navigate(
                    `/deliveries/${delivery.deliveryId}`,
                  )
                }
                className="cursor-pointer text-zinc-300 transition hover:bg-zinc-800/50"
              >
                <td className="px-5 py-4 font-mono text-xs">
                  {delivery.deliveryId.slice(0, 8)}…
                </td>

                <td className="px-5 py-4">
                  <StatusBadge
                    status={delivery.status}
                  />
                </td>

                <td className="px-5 py-4 font-mono text-xs">
                  {delivery.driverId
                    ? `${delivery.driverId.slice(0, 8)}…`
                    : "—"}
                </td>

                <td className="px-5 py-4">
                  {delivery.delayed
                    ? `${delivery.delayMinutes ?? 0} min`
                    : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
