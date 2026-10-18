import { useMutation, useQueryClient } from "@tanstack/react-query";
import { autoAssign, type AutoAssignRequest } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useAutoAssign() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: AutoAssignRequest) => autoAssign(input),
    onSuccess: () => refreshOperations(client),
  });
}
