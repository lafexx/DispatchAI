import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import {
  createBrowserRouter,
  Navigate,
  RouterProvider,
} from "react-router";

import "./index.css";

import { ProviderRoot } from "./ProviderRoot";
import { AppLayout } from "./components/AppLayout";

import { DashboardPage } from "./pages/DashboardPage";
import { DeliveryPage } from "./pages/DeliveryPage";
import { DriversPage } from "./pages/DriversPage";

const router = createBrowserRouter([
  {
    element: (
      <ProviderRoot>
        <AppLayout />
      </ProviderRoot>
    ),

    children: [
      {
        index: true,
        element: (
          <Navigate
            to="/dashboard"
            replace
          />
        ),
      },

      {
        path: "dashboard",
        element: <DashboardPage />,
        handle: {
          breadcrumb: () => "Dashboard",
        },
      },

      {
        path: "deliveries",
        handle: {
          breadcrumb: () => "Deliveries",
        },

        children: [
          {
            path: ":deliveryId",
            element: <DeliveryPage />,

            handle: {
              breadcrumb: (
                params: Record<
                  string,
                  string | undefined
                >,
              ) => {
                const id = params.deliveryId;

                return id
                  ? `${id.slice(0, 8)}…`
                  : "Delivery";
              },
            },
          },
        ],
      },

      {
        path: "drivers",
        element: <DriversPage />,

        handle: {
          breadcrumb: () => "Drivers",
        },
      },
    ],
  },
]);

createRoot(
  document.getElementById("root")!,
).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
