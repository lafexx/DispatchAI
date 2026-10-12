import { useMutation } from "@tanstack/react-query";
import { queryDispatchAdvisor } from "@/api";

export function useDispatchAdvisor() {
  return useMutation({
    mutationFn: (deliveryId: string) =>
      queryDispatchAdvisor(deliveryId),
  });
}
