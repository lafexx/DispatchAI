export type DriverAvailability = "AVAILABLE" | "UNAVAILABLE";

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

export interface VehicleResponse {
  id: string;
  registration: string;
  type: "CAR" | "VAN" | "TRUCK";
  capacityKg: number;
  available: boolean;
}
export interface CreateVehicleRequest {
  registration: string;
  type: VehicleResponse["type"];
  capacityKg: number;
}
