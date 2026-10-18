import { useQuery, type QueryClient } from "@tanstack/react-query";
import { getCustomers, getVehicles, getDelivery } from "@/api";
export const useCustomers = () =>
  useQuery({
    queryKey: ["customer", "all"],
    queryFn: getCustomers,
    refetchInterval: 15000,
  });
export const useVehicles = () =>
  useQuery({
    queryKey: ["fleet", "vehicles"],
    queryFn: getVehicles,
    refetchInterval: 5000,
  });
export const useSourceDelivery = (id: string) =>
  useQuery({
    queryKey: ["delivery", id],
    queryFn: () => getDelivery(id),
    refetchInterval: 5000,
    retry: 1,
  });
export async function refreshOperations(client: QueryClient) {
  await Promise.all(
    ["analytics", "fleet", "dispatch", "delivery"].map((key) =>
      client.invalidateQueries({ queryKey: [key] }),
    ),
  );
}
