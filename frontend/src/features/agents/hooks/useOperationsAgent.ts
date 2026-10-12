import { useMutation } from "@tanstack/react-query";
import { queryOperationsAgent } from "@/api";

export function useOperationsAgent() {
  return useMutation({
    mutationFn: (message: string) =>
      queryOperationsAgent(message),
  });
}
