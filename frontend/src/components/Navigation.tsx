import { NavLink } from "react-router";

const items = [
  {
    to: "/dashboard",
    label: "Dashboard",
  },
  {
    to: "/drivers",
    label: "Drivers",
  },
];

export function Navigation() {
  return (
    <nav className="border-b border-zinc-800 bg-zinc-950">
      <div className="mx-auto flex max-w-7xl items-center gap-8 px-4 sm:px-6 lg:px-8">
        <div className="py-4">
          <span className="text-sm font-semibold tracking-wide text-zinc-100">
            DispatchAI
          </span>
        </div>

        <div className="flex gap-1">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  "rounded-md px-3 py-2 text-sm font-medium transition",
                  isActive
                    ? "bg-zinc-800 text-zinc-100"
                    : "text-zinc-500 hover:bg-zinc-900 hover:text-zinc-300",
                ].join(" ")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  );
}
