import { Fragment } from "react";
import { Link, useMatches } from "react-router";

interface BreadcrumbHandle {
  breadcrumb?: (params: Record<string, string | undefined>) => string;
}

export function Breadcrumbs() {
  const matches = useMatches();

  const breadcrumbs = matches
    .map((match) => {
      const handle = match.handle as BreadcrumbHandle | undefined;

      if (!handle?.breadcrumb) {
        return null;
      }

      return {
        label: handle.breadcrumb(match.params),
        href: match.pathname,
      };
    })
    .filter(
      (
        breadcrumb,
      ): breadcrumb is {
        label: string;
        href: string;
      } => breadcrumb !== null,
    );

  if (breadcrumbs.length === 0) {
    return null;
  }

  return (
    <nav
      aria-label="Breadcrumb"
      className="mb-6 flex items-center gap-2 text-sm text-zinc-500"
    >
      {breadcrumbs.map((breadcrumb, index) => {
        const last = index === breadcrumbs.length - 1;

        return (
          <Fragment key={breadcrumb.href}>
            {index > 0 && (
              <span className="text-zinc-700">
                /
              </span>
            )}

            {last ? (
              <span className="text-zinc-300">
                {breadcrumb.label}
              </span>
            ) : (
              <Link
                to={breadcrumb.href}
                className="transition hover:text-zinc-300"
              >
                {breadcrumb.label}
              </Link>
            )}
          </Fragment>
        );
      })}
    </nav>
  );
}
