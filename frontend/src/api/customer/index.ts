import { requestJson } from "../http";
export interface CustomerResponse {
  id: string;
  name: string;
  email: string;
  phone: string | null;
}
export const getCustomers = () =>
  requestJson<CustomerResponse[]>("/customer-api/api/customers");
export const createCustomer = (input: {
  name: string;
  email: string;
  phone?: string;
}) =>
  requestJson<CustomerResponse>("/customer-api/api/customers", {
    method: "POST",
    body: JSON.stringify(input),
  });
