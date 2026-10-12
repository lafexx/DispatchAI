import type {
  AssignmentResponse,
  AutoAssignRequest,
  CreateAssignmentRequest,
} from "./dispatch.contracts";

const BASE_URL = "/dispatch-api/api/dispatch";

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
      `Dispatch request failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<T>;
}

export function createAssignment(
  input: CreateAssignmentRequest,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(
    `${BASE_URL}/assignments`,
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );
}

export function autoAssign(
  input: AutoAssignRequest,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(
    `${BASE_URL}/assignments/auto`,
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );
}

export function getAssignment(
  assignmentId: string,
): Promise<AssignmentResponse> {
  return request<AssignmentResponse>(
    `${BASE_URL}/assignments/${assignmentId}`,
  );
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
