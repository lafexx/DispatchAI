import { useMutation, useQueryClient } from "@tanstack/react-query";
import { completeDelivery } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useCompleteDelivery() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (deliveryId: string) => completeDelivery(deliveryId),
    onSuccess: () => refreshOperations(client),
  });
}
