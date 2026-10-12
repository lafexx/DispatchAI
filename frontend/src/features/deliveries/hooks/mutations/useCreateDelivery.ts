import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createDelivery } from "@/api";
import type { CreateDeliveryRequest } from "@/api";

export function useCreateDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: CreateDeliveryRequest) =>
      createDelivery(input),

    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["analytics", "deliveries"],
        }),
        queryClient.invalidateQueries({
          queryKey: ["analytics", "summary"],
        }),
      ]);
    },
  });
}
