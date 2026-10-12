import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  autoAssign,
  type AutoAssignRequest,
} from "@/api";

export function useAutoAssign() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (
      request: AutoAssignRequest,
    ) => autoAssign(request),

    onSuccess: async (_, request) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: [
            "analytics",
            "deliveries",
            request.deliveryId,
          ],
        }),

        queryClient.invalidateQueries({
          queryKey: [
            "analytics",
            "deliveries",
          ],
        }),

        queryClient.invalidateQueries({
          queryKey: [
            "analytics",
            "summary",
          ],
        }),

        queryClient.invalidateQueries({
          queryKey: [
            "fleet",
            "drivers",
          ],
        }),

        queryClient.invalidateQueries({
          queryKey: [
            "fleet",
            "drivers",
            "available",
          ],
        }),
      ]);
    },
  });
}
