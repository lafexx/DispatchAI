import { useState } from "react";
import { Outlet, useNavigate } from "react-router";
import { useIsFetching, useQueryClient } from "@tanstack/react-query";
import { useDeliverySummary } from "@/features/deliveries/hooks";
import { Navigation } from "./Navigation";
import { Breadcrumbs } from "./Breadcrumbs";
import { Icon } from "./Icon";
export function AppLayout() {
  const [search, setSearch] = useState("");
  const navigate = useNavigate();
  const client = useQueryClient();
  const fetching = useIsFetching();
  const summary = useDeliverySummary();
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <Navigation />
      <div className="app-main">
        <header className="topbar">
          <Breadcrumbs />
          <div className="topbar-right">
            <form
              className="search-field"
              style={{ width: 220, minWidth: 180, padding: "7px 10px" }}
              onSubmit={(e) => {
                e.preventDefault();
                navigate(`/deliveries?q=${encodeURIComponent(search)}`);
              }}
            >
              <Icon name="search" size={14} />
              <input
                aria-label="Search deliveries"
                placeholder="Search deliveries…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </form>
            <span
              className={`sync-indicator ${summary.isError ? "issue" : ""}`}
            >
              <i />
              {summary.isError
                ? "Summary unavailable"
                : fetching
                  ? "Refreshing…"
                  : "Auto refresh · 5s"}
            </span>
            <button
              type="button"
              className="icon-button"
              aria-label="Refresh operational data"
              title="Refresh operational data"
              onClick={() => void client.invalidateQueries()}
            >
              <Icon name="refresh" size={15} />
            </button>
          </div>
        </header>
        <main id="main-content" className="page-content">
          <Outlet />
          <footer className="app-footer">
            <span>
              DispatchAI <span className="mx-2">/</span> Operations workspace
            </span>
            <span>
              Times shown in {Intl.DateTimeFormat().resolvedOptions().timeZone}
            </span>
          </footer>
        </main>
      </div>
    </div>
  );
}
