export const number = (value: number) => new Intl.NumberFormat().format(value);
export const percent = (value: number) => `${(value * 100).toFixed(1)}%`;
export const shortId = (id: string | null | undefined) =>
  id ? id.slice(0, 8).toUpperCase() : "—";
export function duration(seconds: number | null | undefined) {
  if (seconds == null || !Number.isFinite(seconds) || seconds <= 0) return "—";
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  if (seconds < 3600) return `${(seconds / 60).toFixed(1)}m`;
  return `${(seconds / 3600).toFixed(1)}h`;
}
export function dateTime(value: string | null | undefined) {
  return value
    ? new Date(value).toLocaleString([], {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "—";
}
export function timeLabel(value: string) {
  return new Date(value).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}
export function initials(name: string) {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((p) => p[0])
      .join("")
      .toUpperCase() || "?"
  );
}
export const activeStatus = (status: string) =>
  ["PENDING", "ASSIGNED", "IN_TRANSIT"].includes(status);
