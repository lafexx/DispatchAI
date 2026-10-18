import { Link } from "react-router";
import { AgentPanel } from "@/components/AgentPanel";
import { PageHeader, Panel } from "@/components/UI";
import { Icon } from "@/components/Icon";
export function AssistantPage() {
  return (
    <div className="stack">
      <PageHeader
        eyebrow="AI-ASSISTED OPERATIONS"
        title="Operations assistant"
        description="Turn live operational data into useful answers."
      />
      <div className="assistant-layout">
        <AgentPanel />
        <div className="stack">
          <Panel title="A useful second opinion">
            <div className="panel-body-topless text-xs muted leading-7">
              <p>
                Ask for a briefing, compare performance measures, or investigate
                delays.
              </p>
              <div className="stat-row">
                <span>Delivery state</span>
                <Icon name="check" size={14} />
              </div>
              <div className="stat-row">
                <span>Driver performance</span>
                <Icon name="check" size={14} />
              </div>
              <div className="stat-row">
                <span>Resource availability</span>
                <Icon name="check" size={14} />
              </div>
            </div>
          </Panel>
          <Panel title="Need to assign a delivery?">
            <div className="panel-body-topless">
              <p className="text-xs muted leading-7 mb-4">
                Dispatch advice compares eligible drivers and vehicles. You
                review the recommendation and confirm the assignment.
              </p>
              <Link to="/dispatch" className="button button-ghost">
                Go to dispatch <Icon name="arrow" size={14} />
              </Link>
            </div>
          </Panel>
        </div>
      </div>
    </div>
  );
}
