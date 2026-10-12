interface MetricCardProps {
  label: string;
  value: string | number;
  description?: string;
}

export function MetricCard({
  label,
  value,
  description,
}: MetricCardProps) {
  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-900 p-5">
      <p className="text-sm font-medium text-zinc-400">
        {label}
      </p>

      <p className="mt-2 text-3xl font-semibold tracking-tight text-zinc-100">
        {value}
      </p>

      {description && (
        <p className="mt-1 text-sm text-zinc-500">
          {description}
        </p>
      )}
    </div>
  );
}
