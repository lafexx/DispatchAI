import { useMutation, useQueryClient } from "@tanstack/react-query";
import { delayDelivery } from "@/api";
import type { DelayDeliveryRequest } from "@/api";

interface DelayDeliveryVariables {
  deliveryId: string;
  input: DelayDeliveryRequest;
}

export function useDelayDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      deliveryId,
      input,
    }: DelayDeliveryVariables) =>
      delayDelivery(deliveryId, input),

    onSuccess: async (_, { deliveryId }) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["analytics", "deliveries"],
        }),
        queryClient.invalidateQueries({
          queryKey: ["analytics", "deliveries", deliveryId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["analytics", "summary"],
        }),
      ]);
    },
  });
}
