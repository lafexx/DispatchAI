import { requestJson } from "../http";
import type {
  CancelDeliveryRequest,
  CreateDeliveryRequest,
  DelayDeliveryRequest,
  DeliveryResponse,
} from "./delivery.contracts";

const BASE_URL = "/delivery-api/api/deliveries";

const request = requestJson;

export function getDelivery(deliveryId: string): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(`${BASE_URL}/${deliveryId}`);
}

export function createDelivery(
  input: CreateDeliveryRequest,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(BASE_URL, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function startDelivery(deliveryId: string): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(`${BASE_URL}/${deliveryId}/start`, {
    method: "POST",
  });
}

export function completeDelivery(
  deliveryId: string,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(`${BASE_URL}/${deliveryId}/complete`, {
    method: "POST",
  });
}

export function delayDelivery(
  deliveryId: string,
  input: DelayDeliveryRequest,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(`${BASE_URL}/${deliveryId}/delay`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function cancelDelivery(
  deliveryId: string,
  input: CancelDeliveryRequest,
): Promise<DeliveryResponse> {
  return request<DeliveryResponse>(`${BASE_URL}/${deliveryId}/cancel`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function setParcelWeight(deliveryId: string, parcelWeightKg: number) {
  return request<DeliveryResponse>(`${BASE_URL}/${deliveryId}/parcel-weight`, {
    method: "PATCH",
    body: JSON.stringify({ parcelWeightKg }),
  });
}
