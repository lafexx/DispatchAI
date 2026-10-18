import type { CSSProperties } from "react";
const paths = {
  overview: "M3 3h7v7H3z M14 3h7v4h-7z M14 11h7v10h-7z M3 14h7v7H3z",
  package: "m3 7 9-5 9 5v10l-9 5-9-5V7Zm0 0 9 5 9-5M12 12v10M7.5 4.5l9 5",
  dispatch: "M4 5h9m-4-4 4 4-4 4M20 19H11m4-4-4 4 4 4M4 12h16",
  truck:
    "M3 5h11v12H3z M14 9h4l3 4v4h-7M7 21a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm11 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z",
  chart: "M4 3v17h17M8 15l4-5 4 2 5-7",
  spark:
    "m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Zm7-2v4m-2-2h4",
  arrow: "M5 12h14m-5-5 5 5-5 5",
  chevron: "m9 5 7 7-7 7",
  plus: "M12 5v14M5 12h14",
  search: "M10.5 18a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15ZM16 16l5 5",
  refresh: "M20 7A9 9 0 1 0 1 12M20 3v5h-5",
  check: "m5 12 4 4L19 6",
  clock: "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm0 4v5l3 2",
  alert: "m12 3 10 18H2L12 3Zm0 5v6m0 3v.1",
  users:
    "M9 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM2 21v-2a6 6 0 0 1 12 0v2M16 4a4 4 0 0 1 0 8m3 3a5 5 0 0 1 3 4v2",
  close: "m6 6 12 12M6 18 18 6",
  pin: "M12 22s8-7 8-13A8 8 0 0 0 4 9c0 6 8 13 8 13Zm0-10a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z",
  download: "M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5",
  info: "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm0 8v6m0-10v.1",
  send: "m22 2-7 20-4-9-9-4L22 2Zm0 0L11 13",
  back: "M19 12H5m5-5-5 5 5 5",
  weight: "M8 7a4 4 0 0 1 8 0M5 7h14l2 14H3L5 7Z",
} as const;
export type IconName = keyof typeof paths;
export function Icon({
  name,
  size = 18,
  className = "",
  style,
}: {
  name: IconName;
  size?: number;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.65"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={className}
      style={style}
    >
      <path d={paths[name]} />
    </svg>
  );
}
