export type DriverAvailability =
  | "AVAILABLE"
  | "UNAVAILABLE";

export interface DriverResponse {
  id: string;
  name: string;
  availability: DriverAvailability;
}

export interface CreateDriverRequest {
  name: string;
}

export interface SetDriverAvailabilityRequest {
  available: boolean;
}
