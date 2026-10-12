import { useMutation, useQueryClient } from "@tanstack/react-query";
import { completeDelivery } from "@/api";

export function useCompleteDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (deliveryId: string) =>
      completeDelivery(deliveryId),

    onSuccess: async (_, deliveryId) => {
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
