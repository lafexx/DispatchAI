import { useQuery } from "@tanstack/react-query";
import { getDeliverySummary } from "@/api";

export function useDeliverySummary() {
  return useQuery({
    queryKey: ["analytics", "summary"],
    queryFn: getDeliverySummary,
    refetchInterval: 2000,
  });
}
