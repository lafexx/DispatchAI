import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  createAssignment,
  type CreateAssignmentRequest,
} from "@/api";

export function useCreateAssignment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (
      request: CreateAssignmentRequest,
    ) => createAssignment(request),

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
