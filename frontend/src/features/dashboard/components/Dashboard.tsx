import { Link } from "react-router";
import { useDeliverySummary, useDeliveries } from "@/features/deliveries/hooks";
import { useDrivers } from "@/features/drivers/hooks";
import { useCustomers, useVehicles } from "@/lib/queries";
import { PageHeader, Panel, Notice, Loading } from "@/components/UI";
import { MetricCard } from "@/components/MetricCard";
import { DeliveryTable } from "@/components/DeliveryTable";
import { Icon } from "@/components/Icon";
import {
  TrendChart,
  LifecycleChart,
} from "@/features/analytics/components/AnalyticsCharts";
import { number, percent, duration, activeStatus } from "@/lib/format";
export function Dashboard() {
  const summary = useDeliverySummary();
  const deliveries = useDeliveries();
  const drivers = useDrivers();
  const vehicles = useVehicles();
  const customers = useCustomers();
  const s = summary.data;
  const availableDrivers = drivers.data?.filter(
    (d) => d.availability === "AVAILABLE",
  ).length;
  const availableVehicles = vehicles.data?.filter((v) => v.available).length;
  const attention = (deliveries.data ?? []).filter(
    (d) => activeStatus(d.status) && d.delayed,
  ).length;
  const recent = [...(deliveries.data ?? [])]
    .sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""))
    .slice(0, 5);
  return (
    <div className="stack">
      <PageHeader
        eyebrow="YOUR OPERATION, AT A GLANCE"
        title="Operations overview"
        description="A clear view of demand, delivery performance, and the resources ready to move."
        actions={
          <Link className="button button-primary" to="/deliveries?create=1">
            <Icon name="plus" size={16} />
            New delivery
          </Link>
        }
      />
      {(summary.isError ||
        deliveries.isError ||
        drivers.isError ||
        vehicles.isError) && (
        <Notice tone="warning">
          Some operational data could not be refreshed. Available figures are
          shown below; unavailable figures appear as —.
        </Notice>
      )}
      <div className="grid-four">
        <MetricCard
          label="Total deliveries"
          value={s ? number(s.total) : "—"}
          description="All recorded deliveries"
          icon="package"
        />
        <MetricCard
          label="Active deliveries"
          value={s ? number(s.assigned + s.inTransit) : "—"}
          description={
            s ? `${s.pending} pending dispatch` : "Assigned or in transit"
          }
          icon="truck"
          tone="blue"
        />
        <MetricCard
          label="Completion rate"
          value={s && s.total ? percent(s.completionRate) : "—"}
          description={
            s
              ? `${number(s.completed)} deliveries completed`
              : "Share of all recorded deliveries"
          }
          icon="check"
          tone="purple"
        />
        <MetricCard
          label="Active delays"
          value={deliveries.data ? number(attention) : "—"}
          description="Open deliveries needing attention"
          icon="clock"
          tone="amber"
        />
      </div>
      <div className="overview-grid">
        <div className="stack">
          <TrendChart metric="throughput" />
          <Panel
            title="Needs attention"
            subtitle="Open work that may need an operator"
          >
            <div className="panel-body-topless">
              <Link to="/dispatch" className="attention-item">
                <span className="attention-bullet">
                  <Icon name="dispatch" size={16} />
                </span>
                <div>
                  <strong>
                    {s
                      ? `${s.pending} deliveries awaiting dispatch`
                      : "Dispatch queue"}
                  </strong>
                  <p>Review eligible resources or request AI advice</p>
                </div>
                <Icon name="arrow" size={15} />
              </Link>
              <Link to="/deliveries?status=delayed" className="attention-item">
                <span className="attention-bullet">
                  <Icon name="alert" size={16} />
                </span>
                <div>
                  <strong>
                    {deliveries.data
                      ? `${attention} active deliveries delayed`
                      : "Delayed deliveries"}
                  </strong>
                  <p>Review the reported delay and next action</p>
                </div>
                <Icon name="arrow" size={15} />
              </Link>
            </div>
          </Panel>
        </div>
        <div className="stack">
          <LifecycleChart summary={s} compact error={summary.isError} />
          <Panel
            title="Resource availability"
            action={
              <Link to="/fleet" className="link-button">
                View fleet <Icon name="arrow" size={13} />
              </Link>
            }
          >
            <div className="panel-body-topless">
              <div className="stat-row">
                <span>Drivers ready</span>
                <strong className="text-mint">
                  {availableDrivers ?? "—"}{" "}
                  <small className="muted">
                    / {drivers.data?.length ?? "—"}
                  </small>
                </strong>
              </div>
              <div className="stat-row">
                <span>Vehicles ready</span>
                <strong className="text-blue">
                  {availableVehicles ?? "—"}{" "}
                  <small className="muted">
                    / {vehicles.data?.length ?? "—"}
                  </small>
                </strong>
              </div>
              <div className="stat-row">
                <span>Avg assignment time</span>
                <strong>{duration(s?.averageAssignmentTimeSeconds)}</strong>
              </div>
            </div>
          </Panel>
        </div>
      </div>
      {deliveries.isPending ? (
        <Loading label="Loading recent deliveries…" />
      ) : (
        <DeliveryTable
          title="Recent deliveries"
          subtitle="Latest additions to the operation"
          deliveries={recent}
          drivers={drivers.data}
          customers={customers.data}
          action={
            <Link className="link-button" to="/deliveries">
              View all deliveries <Icon name="arrow" size={13} />
            </Link>
          }
        />
      )}
    </div>
  );
}
