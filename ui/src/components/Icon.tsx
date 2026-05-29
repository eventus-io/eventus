interface IconProps {
  d: string;
  size?: number;
  stroke?: string;
}

export function Icon({ d, size = 14, stroke = 'currentColor' }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 20 20"
      fill="none"
      stroke={stroke}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ flexShrink: 0 }}
    >
      <path d={d} />
    </svg>
  );
}

export const ICON_SEARCH    = 'M4 9a5 5 0 1 1 10 0a5 5 0 0 1-10 0 M13 13l4 4';
export const ICON_FILTER    = 'M3 5h14 M5.5 10h9 M8.5 15h3';
export const ICON_PUBLISH   = 'M10 3v10 M6 7l4-4 4 4 M4 17h12';
export const ICON_SUBSCRIBE = 'M4 7h12 M4 10h12 M4 13h7 M11.8 13a2.2 2.2 0 1 1 4.4 0a2.2 2.2 0 0 1-4.4 0';
export const ICON_REFRESH   = 'M16 4v4h-4 M4 16v-4h4 M16 8a6 6 0 0 0-11.3-2 M4 12a6 6 0 0 0 11.3 2';
export const ICON_EXTERNAL  = 'M11 4h5v5 M16 4l-8 8 M9 4H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-4';
