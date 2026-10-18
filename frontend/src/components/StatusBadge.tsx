const labels: Record<string, string> = {
  PENDING: "Pending",
  ASSIGNED: "Assigned",
  IN_TRANSIT: "In transit",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  AVAILABLE: "Available",
  UNAVAILABLE: "Unavailable",
  REJECTED: "Rejected",
  ACTIVE: "Active",
};
export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <i />
      {labels[status] ?? status}
    </span>
  );
}
