import { useId } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  getAnalyticsHistory,
  type DeliverySummaryResponse,
  type TimeBucketResponse,
} from "@/api";
import { Panel, Loading, Notice, EmptyState } from "@/components/UI";
import { timeLabel, percent, number } from "@/lib/format";
const statusColors = {
  PENDING: "#edbb6a",
  ASSIGNED: "#74a8ff",
  IN_TRANSIT: "#b39bfa",
  COMPLETED: "#69e3b5",
  CANCELLED: "#f18b95",
};
export function HistoryRange({
  minutes,
  onChange,
}: {
  minutes: number;
  onChange: (n: number) => void;
}) {
  return (
    <div className="segment-control" aria-label="Chart time range">
      {[
        [15, "15 min"],
        [60, "1 hour"],
        [180, "3 hours"],
      ].map(([value, label]) => (
        <button
          type="button"
          key={value}
          aria-pressed={minutes === value}
          className={minutes === value ? "active" : ""}
          onClick={() => onChange(Number(value))}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
interface ChartPoint extends TimeBucketResponse {
  tick: string;
  delayPercent: number | null;
}
function TrendTooltip({
  active,
  payload,
  delay = false,
}: {
  active?: boolean;
  payload?: { payload?: ChartPoint }[];
  delay?: boolean;
}) {
  const row = payload?.[0]?.payload;
  if (!active || !row) return null;
  return (
    <div className="chart-tooltip">
      <p>
        {timeLabel(row.start)} – {timeLabel(row.end)}
      </p>
      {delay ? (
        <>
          <div className="tooltip-line">
            <span>Delay rate</span>
            <strong className="text-amber">
              {row.delayRate == null ? "No activity" : percent(row.delayRate)}
            </strong>
          </div>
          <div className="tooltip-line">
            <span>Delayed / active</span>
            <strong>
              {row.delayedDeliveries} / {row.activeDeliveries}
            </strong>
          </div>
          <small>Distinct deliveries in this five-minute window</small>
        </>
      ) : (
        <>
          <div className="tooltip-line">
            <span>Created</span>
            <strong className="text-blue">{number(row.created)}</strong>
          </div>
          <div className="tooltip-line">
            <span>Completed</span>
            <strong className="text-mint">{number(row.completed)}</strong>
          </div>
        </>
      )}
    </div>
  );
}
export function TrendChart({
  metric,
  minutes = 60,
}: {
  metric: "throughput" | "delay-rate";
  minutes?: number;
}) {
  const query = useQuery({
    queryKey: ["analytics", metric, minutes],
    queryFn: () => getAnalyticsHistory(metric, minutes),
    refetchInterval: 5000,
  });
  const id = useId().replaceAll(":", "");
  const delay = metric === "delay-rate";
  const rows: ChartPoint[] = (query.data ?? []).map((p) => ({
    ...p,
    tick: timeLabel(delay ? p.end : p.start),
    delayPercent: p.delayRate == null ? null : p.delayRate * 100,
  }));
  const hasActivity = rows.some((r) =>
    delay ? r.activeDeliveries > 0 : r.created > 0 || r.completed > 0,
  );
  return (
    <Panel
      title={delay ? "Delay rate" : "Demand & throughput"}
      subtitle={
        delay
          ? "Rolling five-minute windows, updated each minute"
          : "Deliveries created and completed per minute"
      }
      action={
        <div className="chart-legend">
          {delay ? (
            <span>
              <i className="legend-dot" style={{ background: "#edbb6a" }} />
              Delay rate
            </span>
          ) : (
            <>
              <span>
                <i className="legend-dot" style={{ background: "#74a8ff" }} />
                Created
              </span>
              <span>
                <i className="legend-dot" style={{ background: "#69e3b5" }} />
                Completed
              </span>
            </>
          )}
        </div>
      }
    >
      {query.isError && (
        <div className="mx-5 mb-3">
          <Notice tone="warning">
            {query.data
              ? "Showing the last fetched history. Refresh is temporarily unavailable."
              : "Trend history is unavailable. Retrying automatically."}
          </Notice>
        </div>
      )}
      {query.isPending ? (
        <Loading label="Loading history…" />
      ) : rows.length === 0 ? (
        <EmptyState
          title={query.isError ? "History unavailable" : "No history available"}
          description="History will appear as deliveries move through the system."
          icon="chart"
        />
      ) : (
        <>
          <div
            className="chart-host"
            aria-label={
              delay
                ? "Five-minute delay rate chart"
                : "Created and completed deliveries chart"
            }
          >
            <ResponsiveContainer width="100%" height="100%" minWidth={0}>
              <AreaChart
                data={rows}
                margin={{ top: 16, right: 18, bottom: 4, left: -6 }}
                accessibilityLayer
              >
                <defs>
                  <linearGradient id={`mint-${id}`} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#69e3b5" stopOpacity={0.24} />
                    <stop offset="100%" stopColor="#69e3b5" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id={`blue-${id}`} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#74a8ff" stopOpacity={0.1} />
                    <stop offset="100%" stopColor="#74a8ff" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient
                    id={`amber-${id}`}
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop offset="0%" stopColor="#edbb6a" stopOpacity={0.22} />
                    <stop offset="100%" stopColor="#edbb6a" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  stroke="#263347"
                  strokeDasharray="3 5"
                  vertical={false}
                />
                <XAxis
                  dataKey="tick"
                  tick={{ fill: "#8294ac", fontSize: 10 }}
                  axisLine={false}
                  tickLine={false}
                  minTickGap={38}
                  interval="preserveStartEnd"
                  dy={7}
                />
                <YAxis
                  tick={{ fill: "#8294ac", fontSize: 10 }}
                  tickLine={false}
                  axisLine={false}
                  allowDecimals={delay}
                  domain={delay ? [0, 100] : [0, "auto"]}
                  tickFormatter={(v) => (delay ? `${v}%` : String(v))}
                  width={48}
                />
                <Tooltip
                  content={<TrendTooltip delay={delay} />}
                  cursor={{ stroke: "#536981", strokeDasharray: "3 4" }}
                />
                {delay ? (
                  <Area
                    type="linear"
                    dataKey="delayPercent"
                    name="Delay rate"
                    stroke="#edbb6a"
                    fill={`url(#amber-${id})`}
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 4, strokeWidth: 3, stroke: "#111822" }}
                    connectNulls={false}
                    isAnimationActive={false}
                  />
                ) : (
                  <>
                    <Area
                      type="monotone"
                      dataKey="created"
                      name="Created"
                      stroke="#74a8ff"
                      fill={`url(#blue-${id})`}
                      strokeWidth={2}
                      dot={false}
                      isAnimationActive={false}
                    />
                    <Area
                      type="monotone"
                      dataKey="completed"
                      name="Completed"
                      stroke="#69e3b5"
                      fill={`url(#mint-${id})`}
                      strokeWidth={2.3}
                      dot={false}
                      isAnimationActive={false}
                    />
                  </>
                )}
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-footnote">
            {!hasActivity ? "No activity in this period. " : ""}
            {delay
              ? "Delayed deliveries ÷ deliveries with activity. Empty windows have no rate; they are not 0%."
              : "The current minute is still in progress. Late events can revise recent values."}
            <details className="mt-2">
              <summary className="cursor-pointer text-zinc-400">
                View chart data
              </summary>
              <div className="table-scroll mt-3" style={{ maxHeight: 180 }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Window</th>
                      <th>{delay ? "Delayed / active" : "Created"}</th>
                      <th>{delay ? "Delay rate" : "Completed"}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((r) => (
                      <tr key={r.start}>
                        <td>{r.tick}</td>
                        <td>
                          {delay
                            ? `${r.delayedDeliveries} / ${r.activeDeliveries}`
                            : r.created}
                        </td>
                        <td>
                          {delay
                            ? r.delayRate == null
                              ? "No activity"
                              : percent(r.delayRate)
                            : r.completed}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </details>
          </div>
        </>
      )}
    </Panel>
  );
}
export function LifecycleChart({
  summary,
  compact = false,
  error = false,
}: {
  summary?: DeliverySummaryResponse;
  compact?: boolean;
  error?: boolean;
}) {
  const rows = summary
    ? [
        {
          name: "Pending",
          value: summary.pending,
          color: statusColors.PENDING,
        },
        {
          name: "Assigned",
          value: summary.assigned,
          color: statusColors.ASSIGNED,
        },
        {
          name: "In transit",
          value: summary.inTransit,
          color: statusColors.IN_TRANSIT,
        },
        {
          name: "Completed",
          value: summary.completed,
          color: statusColors.COMPLETED,
        },
        {
          name: "Cancelled",
          value: summary.cancelled,
          color: statusColors.CANCELLED,
        },
      ]
    : [];
  return (
    <Panel
      title="Delivery lifecycle"
      subtitle="All recorded deliveries"
      className={compact ? "compact-lifecycle" : ""}
    >
      {!summary ? (
        error ? (
          <EmptyState
            title="Lifecycle data unavailable"
            description="Try refreshing in a moment."
            icon="chart"
          />
        ) : (
          <Loading />
        )
      ) : summary.total === 0 ? (
        <EmptyState
          title="Your first delivery starts here"
          description="Create a delivery to populate lifecycle statistics."
        />
      ) : (
        <>
          <div className="ring-content">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={rows.filter((r) => r.value > 0)}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={65}
                  outerRadius={83}
                  paddingAngle={3}
                  stroke="none"
                  isAnimationActive={false}
                >
                  {rows
                    .filter((r) => r.value > 0)
                    .map((r) => (
                      <Cell key={r.name} fill={r.color} />
                    ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    background: "#1a2635",
                    border: "1px solid #3d5068",
                    borderRadius: 8,
                    color: "#e1ebf7",
                    fontSize: 12,
                  }}
                  itemStyle={{ color: "#e1ebf7" }}
                />
              </PieChart>
            </ResponsiveContainer>
            <div className="ring-center">
              <strong>{number(summary.total)}</strong>
              <span>total deliveries</span>
            </div>
          </div>
          <div className="panel-body-topless lifecycle-legend">
            {rows.map((r) => (
              <div className="stat-row" key={r.name}>
                <span>
                  <i className="legend-dot" style={{ background: r.color }} />
                  {r.name}
                </span>
                <strong>
                  {number(r.value)}{" "}
                  <small className="muted ml-3">
                    {percent(summary.total ? r.value / summary.total : 0)}
                  </small>
                </strong>
              </div>
            ))}
          </div>
        </>
      )}
    </Panel>
  );
}
