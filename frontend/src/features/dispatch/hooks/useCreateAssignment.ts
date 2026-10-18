import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createAssignment, type CreateAssignmentRequest } from "@/api";
import { refreshOperations } from "@/lib/queries";
export function useCreateAssignment() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateAssignmentRequest) => createAssignment(input),
    onSuccess: () => refreshOperations(client),
  });
}
