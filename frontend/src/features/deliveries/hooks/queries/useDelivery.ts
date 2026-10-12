import { useQuery } from "@tanstack/react-query";
import { getDeliveryAnalytics } from "@/api";

export function useDelivery(deliveryId?: string) {
  return useQuery({
    queryKey: ["analytics", "deliveries", deliveryId],
    queryFn: () => getDeliveryAnalytics(deliveryId!),
    enabled: Boolean(deliveryId),
    refetchInterval: 2000,
  });
}
