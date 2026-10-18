import { Icon, type IconName } from "./Icon";
export function MetricCard({
  label,
  value,
  description,
  icon = "chart",
  tone = "mint",
}: {
  label: string;
  value: string | number;
  description?: string;
  icon?: IconName;
  tone?: "mint" | "blue" | "amber" | "purple";
}) {
  return (
    <div className={`metric-card tone-${tone}`}>
      <div className="metric-label">
        <span>{label}</span>
        <span className="metric-icon">
          <Icon name={icon} />
        </span>
      </div>
      <p className="metric-value">{value}</p>
      {description && <p className="metric-description">{description}</p>}
    </div>
  );
}
