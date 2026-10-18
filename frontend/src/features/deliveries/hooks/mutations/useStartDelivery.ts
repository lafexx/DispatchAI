import { useMutation, useQueryClient } from "@tanstack/react-query";
import { startDelivery } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useStartDelivery() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (deliveryId: string) => startDelivery(deliveryId),
    onSuccess: () => refreshOperations(client),
  });
}
