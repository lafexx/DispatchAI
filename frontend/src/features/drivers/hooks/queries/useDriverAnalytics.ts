import { getDriverAnalytics } from "@/api";
import { useQuery } from "@tanstack/react-query";

export function useDriverAnalytics(driverId?: string) {
  return useQuery({
    queryKey: ["analytics", "drivers", driverId],
    queryFn: () => getDriverAnalytics(driverId!),
    enabled: Boolean(driverId),
    refetchInterval: 5000,
  });
}
