import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createDelivery, type CreateDeliveryRequest } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useCreateDelivery() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateDeliveryRequest) => createDelivery(input),
    onSuccess: () => refreshOperations(client),
  });
}
