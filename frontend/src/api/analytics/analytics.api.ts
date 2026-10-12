import type {
  DeliveryAnalyticsState,
  DeliverySummaryResponse,
  DriverAnalyticsResponse,
} from "./analytics.contracts";

const BASE_URL = "/analytics-api/api/analytics/deliveries";

async function request<T>(url: string): Promise<T> {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(
      `Analytics request failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<T>;
}

export function getDeliverySummary(): Promise<DeliverySummaryResponse> {
  return request<DeliverySummaryResponse>(`${BASE_URL}/summary`);
}

export function getDeliveries(): Promise<DeliveryAnalyticsState[]> {
  return request<DeliveryAnalyticsState[]>(BASE_URL);
}

export function getDeliveryAnalytics(
  deliveryId: string,
): Promise<DeliveryAnalyticsState> {
  return request<DeliveryAnalyticsState>(
    `${BASE_URL}/${deliveryId}`,
  );
}

export function getDriverAnalytics(
  driverId: string,
): Promise<DriverAnalyticsResponse> {
  return request<DriverAnalyticsResponse>(
    `${BASE_URL}/drivers/${driverId}`,
  );
}
