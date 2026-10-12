import type { DriverResponse } from "@/api";
import { StatusBadge } from "./StatusBadge";

interface DriverTableProps {
  drivers: DriverResponse[];
}

function shortId(id: string) {
  return `${id.slice(0, 8)}…`;
}

export function DriverTable({
  drivers,
}: DriverTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900">
      <div className="border-b border-zinc-800 px-5 py-4">
        <h2 className="font-semibold text-zinc-100">
          Fleet
        </h2>

        <p className="mt-1 text-sm text-zinc-500">
          Current driver availability.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-zinc-800 bg-zinc-950/40 text-zinc-400">
            <tr>
              <th className="px-5 py-3 font-medium">
                Driver
              </th>

              <th className="px-5 py-3 font-medium">
                ID
              </th>

              <th className="px-5 py-3 font-medium">
                Availability
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-zinc-800">
            {drivers.map((driver) => (
              <tr
                key={driver.id}
                className="text-zinc-300"
              >
                <td className="px-5 py-4 font-medium text-zinc-200">
                  {driver.name}
                </td>

                <td className="px-5 py-4 font-mono text-xs">
                  {shortId(driver.id)}
                </td>

                <td className="px-5 py-4">
                  <StatusBadge
                    status={driver.availability}
                  />
                </td>
              </tr>
            ))}

            {drivers.length === 0 && (
              <tr>
                <td
                  colSpan={3}
                  className="px-5 py-8 text-center text-zinc-500"
                >
                  No drivers found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
