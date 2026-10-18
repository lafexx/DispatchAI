export type AssignmentStatus =
  | "ACTIVE"
  | "COMPLETED"
  | "CANCELLED"
  | "REJECTED";

export interface AssignmentResponse {
  id: string;
  deliveryId: string;
  driverId: string;
  vehicleId: string | null;
  status: AssignmentStatus;

  assignedAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
}

export interface CreateAssignmentRequest {
  deliveryId: string;
  driverId: string;
  vehicleId?: string | null;
}

export interface AutoAssignRequest {
  deliveryId: string;
  vehicleId?: string | null;
}

export interface EligibleVehicle {
  id: string;
  registration: string;
  type: string;
  capacityKg: number;
  available: boolean;
}
export interface DispatchEligibilityResponse {
  deliveryId: string;
  parcelWeightKg: number;
  availableDrivers: import("../fleet/fleet.contracts").DriverResponse[];
  eligibleVehicles: EligibleVehicle[];
}
