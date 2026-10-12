import type {
  CancelDeliveryRequest,
  CreateDeliveryRequest,
  DelayDeliveryRequest,
  DeliveryResponse,
} from "./delivery.contracts";

const BASE_URL = "/delivery-api/api/deliveries";

async function request<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    throw new Error(
      `Delivery request failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<T>;
}

export function getDelivery(
  deliveryId: string,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(
    `${BASE_URL}/${deliveryId}`,
  );
}

export function createDelivery(
  input: CreateDeliveryRequest,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(BASE_URL, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function startDelivery(
  deliveryId: string,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(
    `${BASE_URL}/${deliveryId}/start`,
    {
      method: "POST",
    },
  );
}

export function completeDelivery(
  deliveryId: string,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(
    `${BASE_URL}/${deliveryId}/complete`,
    {
      method: "POST",
    },
  );
}

export function delayDelivery(
  deliveryId: string,
  input: DelayDeliveryRequest,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(
    `${BASE_URL}/${deliveryId}/delay`,
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );
}

export function cancelDelivery(
  deliveryId: string,
  input: CancelDeliveryRequest,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(
    `${BASE_URL}/${deliveryId}/cancel`,
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );
}
