export type DeliveryStatus =
  | "PENDING"
  | "ASSIGNED"
  | "IN_TRANSIT"
  | "COMPLETED"
  | "CANCELLED";

export interface DeliveryResponse {
  id: string;
  customerId: string;
  assignedDriverId: string | null;

  pickupAddress: string;
  destinationAddress: string;

  status: DeliveryStatus;

  delayed: boolean;
  delayMinutes: number | null;
  delayReason: string | null;

  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDeliveryRequest {
  customerId: string;
  pickupAddress: string;
  destinationAddress: string;
}

export interface DelayDeliveryRequest {
  delayMinutes: number;
  reason: string;
}

export type CancellationReason =
  | "CUSTOMER_REQUEST"
  | "DRIVER_UNAVAILABLE"
  | "OPERATIONAL"
  | string;

export interface CancelDeliveryRequest {
  reason: CancellationReason;
  notes?: string;
}
