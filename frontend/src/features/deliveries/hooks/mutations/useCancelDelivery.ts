import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelDelivery, type CancelDeliveryRequest } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useCancelDelivery() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({
      deliveryId,
      input,
    }: {
      deliveryId: string;
      input: CancelDeliveryRequest;
    }) => cancelDelivery(deliveryId, input),
    onSuccess: () => refreshOperations(client),
  });
}
