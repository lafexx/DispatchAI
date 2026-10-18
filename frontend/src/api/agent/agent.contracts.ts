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
  recommendedVehicleId: string;
  recommendedVehicleRegistration: string;
  parcelWeightKg: number;
  vehicleCapacityKg: number;
  candidates: {
    driver: import("../fleet/fleet.contracts").DriverResponse;
    analytics:
      | import("../analytics/analytics.contracts").DriverAnalyticsResponse
      | null;
  }[];
  eligibleVehicles: import("../dispatch/dispatch.contracts").EligibleVehicle[];
}

export interface DispatchAdviceRequest {
  deliveryId: string;
}
