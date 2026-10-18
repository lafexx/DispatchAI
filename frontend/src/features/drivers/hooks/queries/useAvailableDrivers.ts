import { useQuery } from "@tanstack/react-query";
import { getAvailableDrivers } from "@/api";

export function useAvailableDrivers() {
  return useQuery({
    queryKey: ["fleet", "drivers", "available"],
    queryFn: getAvailableDrivers,
    refetchInterval: 5000,
  });
}
