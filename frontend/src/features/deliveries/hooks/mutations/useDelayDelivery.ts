import { useMutation, useQueryClient } from "@tanstack/react-query";
import { delayDelivery, type DelayDeliveryRequest } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useDelayDelivery() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({
      deliveryId,
      input,
    }: {
      deliveryId: string;
      input: DelayDeliveryRequest;
    }) => delayDelivery(deliveryId, input),
    onSuccess: () => refreshOperations(client),
  });
}
