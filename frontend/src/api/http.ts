export class ApiRequestError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
  }
}
export async function requestJson<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as {
      message?: string;
      detail?: string;
    } | null;
    throw new ApiRequestError(
      body?.message ??
        body?.detail ??
        `Request failed (${response.status}). Please try again.`,
      response.status,
    );
  }
  return response.json() as Promise<T>;
}
export const getJson = <T>(url: string, init?: RequestInit) =>
  requestJson<T>(url, init);
export const postJson = <TResponse, TBody = unknown>(
  url: string,
  body?: TBody,
  init?: RequestInit,
) =>
  requestJson<TResponse>(url, {
    ...init,
    method: "POST",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
