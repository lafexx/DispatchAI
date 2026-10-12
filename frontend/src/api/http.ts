export async function getJson<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      Accept: "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    throw new Error(
      `GET ${url} failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<T>;
}

export async function postJson<TResponse, TBody = unknown>(
  url: string,
  body?: TBody,
  init?: RequestInit,
): Promise<TResponse> {
  const response = await fetch(url, {
    ...init,
    method: "POST",

    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...init?.headers,
    },

    body:
      body === undefined
        ? undefined
        : JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(
      `POST ${url} failed: ${response.status} ${response.statusText}`,
    );
  }

  return response.json() as Promise<TResponse>;
}
