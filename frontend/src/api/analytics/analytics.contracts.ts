export type AnalyticsDeliveryStatus =
  | "PENDING"
  | "ASSIGNED"
  | "IN_TRANSIT"
  | "COMPLETED"
  | "CANCELLED";

export interface DeliveryAnalyticsState {
  deliveryId: string;
  customerId: string;
  driverId: string | null;

  status: AnalyticsDeliveryStatus;

  delayed: boolean;
  delayMinutes: number | null;
  delayReason: string | null;

  createdAt: string | null;
  assignedAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
}

export interface DeliverySummaryResponse {
  total: number;
  pending: number;
  assigned: number;
  inTransit: number;
  completed: number;
  cancelled: number;
  delayed: number;

  averageDelayMinutes: number;
  averageAssignmentTimeSeconds: number;
  averageDeliveryTimeSeconds: number;
  averageTransitTimeSeconds: number;

  completionRate: number;
  cancellationRate: number;
  delayRate: number;
}

export interface DriverAnalyticsResponse {
  driverId: string;
  totalAssigned: number;
  completed: number;
  cancelled: number;
  delayed: number;

  averageDeliveryTimeSeconds: number;
  averageTransitTimeSeconds: number;
}
