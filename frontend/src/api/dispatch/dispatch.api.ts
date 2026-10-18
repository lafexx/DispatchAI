import { requestJson } from "../http";
import type {
  AssignmentResponse,
  AutoAssignRequest,
  CreateAssignmentRequest,
} from "./dispatch.contracts";

const BASE_URL = "/dispatch-api/api/dispatch";

const request = requestJson;

export function createAssignment(
  input: CreateAssignmentRequest,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(`${BASE_URL}/assignments`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function autoAssign(
  input: AutoAssignRequest,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(`${BASE_URL}/assignments/auto`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getAssignment(
  assignmentId: string,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(`${BASE_URL}/assignments/${assignmentId}`);
}

export function getDeliveryAssignment(
  deliveryId: string,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(
    `${BASE_URL}/deliveries/${deliveryId}/assignment`,
  );
}

export function getDriverAssignments(
  driverId: string,
): Promise<AssignmentResponse[]> {
  return request<AssignmentResponse[]>(
    `${BASE_URL}/drivers/${driverId}/assignments`,
  );
}

export function getDispatchEligibility(deliveryId: string) {
  return request<import("./dispatch.contracts").DispatchEligibilityResponse>(
    `${BASE_URL}/deliveries/${deliveryId}/eligibility`,
  );
}
