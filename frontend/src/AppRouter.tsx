import { lazy, Suspense, type ReactNode } from "react";
import {
  createBrowserRouter,
  Navigate,
  RouterProvider,
  useLocation,
} from "react-router";
import { ProviderRoot } from "./ProviderRoot";
import { AppLayout } from "./components/AppLayout";
const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then((module) => ({
    default: module.DashboardPage,
  })),
);
const DeliveryPage = lazy(() =>
  import("./pages/DeliveryPage").then((module) => ({
    default: module.DeliveryPage,
  })),
);
const DeliveriesPage = lazy(() =>
  import("./pages/DeliveriesPage").then((module) => ({
    default: module.DeliveriesPage,
  })),
);
const DispatchPage = lazy(() =>
  import("./pages/DispatchPage").then((module) => ({
    default: module.DispatchPage,
  })),
);
const FleetPage = lazy(() =>
  import("./pages/FleetPage").then((module) => ({ default: module.FleetPage })),
);
const AnalyticsPage = lazy(() =>
  import("./pages/AnalyticsPage").then((module) => ({
    default: module.AnalyticsPage,
  })),
);
const AssistantPage = lazy(() =>
  import("./pages/AssistantPage").then((module) => ({
    default: module.AssistantPage,
  })),
);
import { PageHeader, EmptyState, Loading } from "./components/UI";
function LegacyDriversRedirect() {
  const location = useLocation();
  return <Navigate to={`/fleet${location.search}`} replace />;
}
const screen = (view: ReactNode) => (
  <Suspense fallback={<Loading label="Loading workspace…" />}>{view}</Suspense>
);
const router = createBrowserRouter([
  {
    element: (
      <ProviderRoot>
        <AppLayout />
      </ProviderRoot>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      {
        path: "dashboard",
        element: screen(<DashboardPage />),
        handle: { breadcrumb: () => "Overview" },
      },
      {
        path: "deliveries",
        handle: { breadcrumb: () => "Deliveries" },
        children: [
          { index: true, element: screen(<DeliveriesPage />) },
          {
            path: ":deliveryId",
            element: screen(<DeliveryPage />),
            handle: {
              breadcrumb: (params: Record<string, string | undefined>) =>
                params.deliveryId
                  ? `#${params.deliveryId.slice(0, 8).toUpperCase()}`
                  : "Delivery",
            },
          },
        ],
      },
      {
        path: "dispatch",
        element: screen(<DispatchPage />),
        handle: { breadcrumb: () => "Dispatch" },
      },
      {
        path: "fleet",
        element: screen(<FleetPage />),
        handle: { breadcrumb: () => "Fleet" },
      },
      {
        path: "drivers",
        element: <LegacyDriversRedirect />,
        handle: { breadcrumb: () => "Fleet" },
      },
      {
        path: "analytics",
        element: screen(<AnalyticsPage />),
        handle: { breadcrumb: () => "Analytics" },
      },
      {
        path: "assistant",
        element: screen(<AssistantPage />),
        handle: { breadcrumb: () => "AI assistant" },
      },
      {
        path: "*",
        element: (
          <>
            <PageHeader title="Page not found" />
            <EmptyState
              title="This page does not exist"
              description="Use the sidebar to return to your workspace."
            />
          </>
        ),
      },
    ],
  },
]);
export function AppRouter() {
  return <RouterProvider router={router} />;
}
