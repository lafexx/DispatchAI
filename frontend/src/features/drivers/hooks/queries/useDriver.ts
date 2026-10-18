import { useQuery } from "@tanstack/react-query";
import { getDriver } from "@/api";

export function useDriver(driverId?: string) {
  return useQuery({
    queryKey: ["fleet", "drivers", driverId],
    queryFn: () => getDriver(driverId!),
    enabled: Boolean(driverId),
    refetchInterval: 5000,
  });
}
