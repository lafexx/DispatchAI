interface StatusBadgeProps {
  status: string;
}

const styles: Record<string, string> = {
  PENDING:
    "border-zinc-700 bg-zinc-800 text-zinc-300",

  ASSIGNED:
    "border-blue-900 bg-blue-950 text-blue-300",

  IN_TRANSIT:
    "border-amber-900 bg-amber-950 text-amber-300",

  COMPLETED:
    "border-emerald-900 bg-emerald-950 text-emerald-300",

  CANCELLED:
    "border-red-900 bg-red-950 text-red-300",

  AVAILABLE:
    "border-emerald-900 bg-emerald-950 text-emerald-300",

  UNAVAILABLE:
    "border-zinc-700 bg-zinc-800 text-zinc-400",
};

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span
      className={[
        "inline-flex rounded-full border px-2.5 py-1",
        "text-xs font-medium",
        styles[status] ??
        "border-zinc-700 bg-zinc-800 text-zinc-300",
      ].join(" ")}
    >
      {status.replaceAll("_", " ")}
    </span>
  );
}
