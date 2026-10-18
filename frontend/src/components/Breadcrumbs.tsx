import { Fragment } from "react";
import { Link, useMatches } from "react-router";
import { Icon } from "./Icon";
export function Breadcrumbs() {
  const matches = useMatches().flatMap((match) => {
    const handle = match.handle as
      | { breadcrumb?: (params: Record<string, string | undefined>) => string }
      | undefined;
    return handle?.breadcrumb
      ? [{ label: handle.breadcrumb(match.params), href: match.pathname }]
      : [];
  });
  return (
    <nav
      aria-label="Breadcrumb"
      className="flex items-center gap-3 text-xs text-zinc-500"
    >
      <span>Workspace</span>
      {matches.map((crumb, i) => (
        <Fragment key={`${crumb.href}-${i}`}>
          <Icon name="chevron" size={12} />
          {i === matches.length - 1 ? (
            <span className="text-zinc-200" aria-current="page">
              {crumb.label}
            </span>
          ) : (
            <Link to={crumb.href}>{crumb.label}</Link>
          )}
        </Fragment>
      ))}
    </nav>
  );
}
