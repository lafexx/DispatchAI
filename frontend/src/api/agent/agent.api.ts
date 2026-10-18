import { postJson, requestJson } from "../http";
import type {
  AgentRequest,
  AgentResponse,
  DispatchAdviceRequest,
  DispatchAdviceResponse,
} from "./agent.contracts";

const BASE_URL = "/agent-api/api/agent";

function postAgentRequest(
  url: string,
  input: AgentRequest,
): Promise<AgentResponse> {
  return requestJson<AgentResponse>(url, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function queryOperationsAgent(message: string): Promise<AgentResponse> {
  return postAgentRequest(`${BASE_URL}/operations/query`, { message });
}

export function queryDispatchAdvisor(
  deliveryId: string,
): Promise<DispatchAdviceResponse> {
  return postJson<DispatchAdviceResponse, DispatchAdviceRequest>(
    "/agent-api/api/agent/dispatch/advice",
    { deliveryId },
  );
}
