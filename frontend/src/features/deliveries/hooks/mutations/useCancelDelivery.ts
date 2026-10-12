import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelDelivery } from "@/api";
import type { CancelDeliveryRequest } from "@/api";

interface CancelDeliveryVariables {
  deliveryId: string;
  input: CancelDeliveryRequest;
}

export function useCancelDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      deliveryId,
      input,
    }: CancelDeliveryVariables) =>
      cancelDelivery(deliveryId, input),

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
