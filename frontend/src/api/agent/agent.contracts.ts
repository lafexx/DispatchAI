export interface AgentRequest {
  message: string;
}

export interface AgentResponse {
  response: string;
}

export interface DispatchAdviceResponse {
  recommendedDriverId: string;
  recommendedDriverName: string;
  reasoning: string;
}

export interface DispatchAdviceRequest {
  deliveryId: string;
}
