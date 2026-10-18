import { getDrivers } from "@/api";
import { useQuery } from "@tanstack/react-query";

export function useDrivers() {
  return useQuery({
    queryKey: ["fleet", "drivers"],
    queryFn: getDrivers,
    refetchInterval: 5000,
  });
}
