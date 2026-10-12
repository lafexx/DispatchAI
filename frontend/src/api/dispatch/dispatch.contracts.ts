export type AssignmentStatus =
  | "ACTIVE"
  | "COMPLETED"
  | "CANCELLED";

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
