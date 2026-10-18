import { useSearchParams } from "react-router";
import { useDeliverySummary } from "@/features/deliveries/hooks";
import { PageHeader, Panel, Notice } from "@/components/UI";
import { MetricCard } from "@/components/MetricCard";
import {
  HistoryRange,
  TrendChart,
  LifecycleChart,
} from "@/features/analytics/components/AnalyticsCharts";
import { duration, percent, number } from "@/lib/format";
export function AnalyticsPage() {
  const summary = useDeliverySummary();
  const s = summary.data;
  const [params, setParams] = useSearchParams();
  const selected = Number(params.get("minutes"));
  const minutes = [15, 60, 180].includes(selected) ? selected : 60;
  return (
    <div className="stack">
      <PageHeader
        eyebrow="PERFORMANCE & INSIGHTS"
        title="Analytics"
        description="Explore recent operating conditions alongside lifetime delivery performance."
        actions={
          <HistoryRange
            minutes={minutes}
            onChange={(value) => setParams({ minutes: String(value) })}
          />
        }
      />
      {summary.isError && (
        <Notice tone="warning">
          Summary metrics could not be refreshed. Charts load independently.
        </Notice>
      )}
      <div className="grid-four">
        <MetricCard
          label="Average assignment"
          value={duration(s?.averageAssignmentTimeSeconds)}
          description="Created → assigned"
          icon="dispatch"
          tone="blue"
        />
        <MetricCard
          label="Average delivery"
          value={duration(s?.averageDeliveryTimeSeconds)}
          description="Created → completed"
          icon="package"
        />
        <MetricCard
          label="Average transit"
          value={duration(s?.averageTransitTimeSeconds)}
          description="Started → completed"
          icon="truck"
          tone="purple"
        />
        <MetricCard
          label="Average reported delay"
          value={s && s.delayed ? `${s.averageDelayMinutes.toFixed(1)}m` : "—"}
          description="Recorded delay durations"
          icon="clock"
          tone="amber"
        />
      </div>
      <div className="grid-two">
        <TrendChart metric="throughput" minutes={minutes} />
        <TrendChart metric="delay-rate" minutes={minutes} />
      </div>
      <div className="overview-grid">
        <Panel
          title="Delivery outcomes"
          subtitle="Lifetime ratios · denominator is all recorded deliveries"
        >
          <div className="panel-body-topless">
            {[
              {
                name: "Completion rate",
                value: s?.completionRate,
                count: s?.completed,
                color: "var(--mint)",
              },
              {
                name: "Cancellation rate",
                value: s?.cancellationRate,
                count: s?.cancelled,
                color: "var(--red)",
              },
              {
                name: "Ever delayed",
                value: s?.delayRate,
                count: s?.delayed,
                color: "var(--amber)",
              },
            ].map((row) => (
              <div
                key={row.name}
                style={{
                  padding: "18px 0",
                  borderBottom: "1px solid var(--border)",
                }}
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs text-zinc-300">{row.name}</span>
                  <strong style={{ color: row.color, fontSize: 16 }}>
                    {row.value != null && s?.total ? percent(row.value) : "—"}
                  </strong>
                </div>
                <div className="capacity-track">
                  <span
                    style={{
                      width: `${Math.min(100, (row.value ?? 0) * 100)}%`,
                      background: row.color,
                    }}
                  />
                </div>
                <p className="text-xs muted mt-3">
                  {row.count == null
                    ? "No data"
                    : `${number(row.count)} of ${number(s?.total ?? 0)} deliveries`}
                </p>
              </div>
            ))}
            <p className="chart-footnote" style={{ padding: "17px 0 0" }}>
              A delivery can be both completed and delayed. These ratios are
              separate measures, not slices of one total.
            </p>
          </div>
        </Panel>
        <LifecycleChart summary={s} error={summary.isError} />
      </div>
    </div>
  );
}
