import type {
  CreateDriverRequest,
  DriverResponse,
  SetDriverAvailabilityRequest,
} from "./fleet.contracts";

const BASE_URL = "/fleet-api/api/drivers";

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
      `Fleet request failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<T>;
}

export function getDrivers(): Promise<DriverResponse[]> {
  return request<DriverResponse[]>(BASE_URL);
}

export function getDriver(
  driverId: string,
): Promise<DriverResponse> {
  return request<DriverResponse>(
    `${BASE_URL}/${driverId}`,
  );
}

export function getAvailableDrivers(): Promise<DriverResponse[]> {
  return request<DriverResponse[]>(
    `${BASE_URL}/available`,
  );
}

export function createDriver(
  input: CreateDriverRequest,
): Promise<DriverResponse> {
  return request<DriverResponse>(BASE_URL, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function setDriverAvailability(
  driverId: string,
  input: SetDriverAvailabilityRequest,
): Promise<DriverResponse> {
  return request<DriverResponse>(
    `${BASE_URL}/${driverId}/availability`,
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );
}
