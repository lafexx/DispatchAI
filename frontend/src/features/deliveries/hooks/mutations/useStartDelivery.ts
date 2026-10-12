import { useMutation, useQueryClient } from "@tanstack/react-query";
import { startDelivery } from "@/api";

export function useStartDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (deliveryId: string) =>
      startDelivery(deliveryId),

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
