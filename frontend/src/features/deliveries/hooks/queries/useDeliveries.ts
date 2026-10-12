import { getDeliveries } from "@/api";
import { useQuery } from "@tanstack/react-query";

export function useDeliveries() {
  return useQuery({
    queryKey: ["analytics", "deliveries"],
    queryFn: getDeliveries,
    refetchInterval: 2000,
  });
}
