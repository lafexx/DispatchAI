import { requestJson } from "../http";
import type {
  CreateDriverRequest,
  DriverResponse,
  SetDriverAvailabilityRequest,
  VehicleResponse,
  CreateVehicleRequest,
} from "./fleet.contracts";
const BASE = "/fleet-api/api";
export const getDrivers = () =>
  requestJson<DriverResponse[]>(`${BASE}/drivers`);
export const getDriver = (id: string) =>
  requestJson<DriverResponse>(`${BASE}/drivers/${id}`);
export const getAvailableDrivers = () =>
  requestJson<DriverResponse[]>(`${BASE}/drivers/available`);
export const createDriver = (input: CreateDriverRequest) =>
  requestJson<DriverResponse>(`${BASE}/drivers`, {
    method: "POST",
    body: JSON.stringify(input),
  });
export const setDriverAvailability = (
  id: string,
  input: SetDriverAvailabilityRequest,
) =>
  requestJson<DriverResponse>(`${BASE}/drivers/${id}/availability`, {
    method: "POST",
    body: JSON.stringify(input),
  });
export const getVehicles = () =>
  requestJson<VehicleResponse[]>(`${BASE}/vehicles`);
export const createVehicle = (input: CreateVehicleRequest) =>
  requestJson<VehicleResponse>(`${BASE}/vehicles`, {
    method: "POST",
    body: JSON.stringify(input),
  });
export const setVehicleAvailability = (id: string, available: boolean) =>
  requestJson<VehicleResponse>(`${BASE}/vehicles/${id}/availability`, {
    method: "POST",
    body: JSON.stringify({ available }),
  });
