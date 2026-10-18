import { TrendChart } from "@/features/analytics/components/AnalyticsCharts";
export function AnalyticsTrends() {
  return (
    <div className="grid-two">
      <TrendChart metric="throughput" />
      <TrendChart metric="delay-rate" />
    </div>
  );
}
