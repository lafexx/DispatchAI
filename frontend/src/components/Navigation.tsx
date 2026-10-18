import { NavLink } from "react-router";
import { useDeliverySummary } from "@/features/deliveries/hooks";
import { Icon, type IconName } from "./Icon";
import { Avatar } from "./UI";
const items: { to: string; label: string; icon: IconName }[] = [
  { to: "/dashboard", label: "Overview", icon: "overview" },
  { to: "/deliveries", label: "Deliveries", icon: "package" },
  { to: "/dispatch", label: "Dispatch", icon: "dispatch" },
  { to: "/fleet", label: "Fleet", icon: "truck" },
  { to: "/analytics", label: "Analytics", icon: "chart" },
  { to: "/assistant", label: "AI assistant", icon: "spark" },
];
export function Navigation() {
  const summary = useDeliverySummary();
  return (
    <aside className="sidebar">
      <NavLink to="/dashboard" className="brand" aria-label="DispatchAI home">
        <span className="brand-mark">
          <Icon name="dispatch" size={21} />
        </span>
        <span>
          Dispatch<em>AI</em>
        </span>
      </NavLink>
      <p className="nav-label">WORKSPACE</p>
      <nav aria-label="Main navigation">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
          >
            <Icon name={item.icon} />
            {item.label}
            {item.to === "/dispatch" &&
              summary.data &&
              summary.data.pending > 0 && (
                <span className="nav-count">{summary.data.pending}</span>
              )}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-bottom">
        <div className="sidebar-note">
          <div className="flex items-center gap-2 text-xs text-mint">
            <Icon name="spark" size={15} />
            <strong>Decisions, with context.</strong>
          </div>
          <p>Compare available resources and get advice before you dispatch.</p>
          <NavLink to="/dispatch" className="link-button mt-3">
            Open dispatch <Icon name="arrow" size={13} />
          </NavLink>
        </div>
        <div className="workspace-person">
          <Avatar name="Operations" small />
          <div>
            <strong>Operations workspace</strong>
            <p>DispatchAI · Internal</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
