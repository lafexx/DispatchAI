import { Outlet } from "react-router";

import { Breadcrumbs } from "./Breadcrumbs";
import { Navigation } from "./Navigation";

export function AppLayout() {
  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <Navigation />

      <main>
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <Breadcrumbs />

          <Outlet />
        </div>
      </main>
    </div>
  );
}
