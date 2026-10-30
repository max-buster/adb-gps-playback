# Design system

Shared look for both apps. Android implements it (`android/app/.../ui/theme`
and `ui/components`); the web app still uses its older tokens in
`web/app/globals.css` and should move to these names when it's restyled.

## Principles

1. **Mocking is never ambiguous.** Amber (`live`) means "this device's
   location is fake right now" and is used for nothing else: the live status
   pill on every tab, the halo on the map marker, the scrubber puck ring and
   the notification accent.
2. **The route is the hero.** Maps get the most space. The route line and its
   bullet use the feed's `route_color` / `route_text_color`, falling back to
   magenta (`route`), which stays visible over OSM's blue water and green parks.
3. **Numbers are instruments.** Distances, speeds, coordinates and σ use
   Overpass Mono with tabular figures so they don't jitter during playback.

## Color tokens

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| `bg` | `#F2F4F5` | `#0D1317` | Window |
| `surface` | `#FFFFFF` | `#151D23` | Cards, sheets, nav |
| `surface-2` | `#E8ECEE` | `#1E282F` | Inactive / sunken controls |
| `ink` | `#111A21` | `#E7EDF0` | Text, icons, map marker |
| `ink-2` | `#55636D` | `#93A1AA` | Secondary text |
| `line` | `#D3DADF` | `#2A363E` | Dividers, outlines, rails |
| `accent` | `#2B3BD1` | `#7C88FF` | Primary actions, selection, focus |
| `accent-soft` | `#E3E6FB` | `#232A5C` | Selected nav item, tonal buttons |
| `live` | `#F2A10C` | `#FFB52E` | Mocking only |
| `live-soft` | `#FDF0D2` | `#3A2A08` | Holding (paused but still mocking) |
| `route` | `#C0267E` | `#F05BAE` | Default route color |
| `ok` | `#1F8A4C` | `#4CC27E` | Setup steps done, staged |
| `err` | `#C8322B` | `#F0716A` | Errors, Stop mocking, Remove |

No dynamic (wallpaper) color on Android: the meanings above must look the
same on every device. Feed route colors are lightened on the dark theme until
they reach 3:1 contrast (`RouteColors.readableOn` in `:core`), and in dark mode
the OSM raster tiles are dimmed rather than swapped.

## Type

Overpass (400/600/700/800) for UI and Overpass Mono (400/600) for numbers,
both OFL. Android bundles Latin-subset static instances in `res/font`.

| Role | Size / line | Weight |
| --- | --- | --- |
| Screen title | 26 / 32 | 800 |
| Title | 20 / 26 | 700 |
| Body | 15 / 22 | 400 |
| Small | 12 / 16 | 400 |
| Label (uppercase, +0.08em) | 11 / 14 | 800 |
| Data large (mono) | 26 / 30 | 600 |
| Data (mono) | 13 / 18 | 400–600 |

## Spacing and shape

4 dp base: 4, 8, 12, 16, 24, 32. 16 is the screen gutter and card padding.
Touch targets stay at 44 dp or more. Radii: controls 8–10, cards 14, sheet 20,
pills full. Lists and cards are flat with a 1 dp `line` border; only things
floating over the map (chips, map buttons, the sheet) get a shadow.

## Components

- **Route bullet**: short name on the route color, max 4 characters.
- **Live status pill**: Real location / Mocking (amber, pulsing dot) /
  Holding (amber outline) / Error. In the top bar of every tab.
- **Strip scrubber**: the route as a line diagram, one tick per stop
  (quarter-distance ticks when there are no stops). Drag to scrub, tap near a
  stop to jump to it.
- **Transport bar**: previous stop, Play/Pause, next stop, then Stop mocking
  set apart in red.
- **Segmented choice**: the speed multiplier.
- **Icons**: hand-written vector drawables in `res/drawable`. Launcher icon is
  the amber play pin (a map pin with a play triangle cut out) on ink, with a
  monochrome layer for themed icons.
