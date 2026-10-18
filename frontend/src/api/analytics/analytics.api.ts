import { requestJson } from "../http";
import type {
  DeliveryAnalyticsState,
  DeliverySummaryResponse,
  DriverAnalyticsResponse,
} from "./analytics.contracts";

const BASE_URL = "/analytics-api/api/analytics/deliveries";

const request = requestJson;

export function getDeliverySummary(): Promise<DeliverySummaryResponse> {
  return request<DeliverySummaryResponse>(`${BASE_URL}/summary`);
}

export function getDeliveries(): Promise<DeliveryAnalyticsState[]> {
  return request<DeliveryAnalyticsState[]>(BASE_URL);
}

export function getDeliveryAnalytics(
  deliveryId: string,
): Promise<DeliveryAnalyticsState> {
  return request<DeliveryAnalyticsState>(`${BASE_URL}/${deliveryId}`);
}

export function getDriverAnalytics(
  driverId: string,
): Promise<DriverAnalyticsResponse> {
  return request<DriverAnalyticsResponse>(`${BASE_URL}/drivers/${driverId}`);
}

export function getAnalyticsHistory(
  metric: "throughput" | "delay-rate",
  minutes = 60,
) {
  return request<import("./analytics.contracts").TimeBucketResponse[]>(
    `${BASE_URL}/${metric}?minutes=${minutes}`,
  );
}
