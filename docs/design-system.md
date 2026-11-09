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

The "ghost" palette: neutrals tinted toward green and a pale ghost green
accent (deepened to sage in light mode so it reads on white), taken from the
ghost bus launcher icon. Amber stays reserved for mocking.

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| `bg` | `#EEF3EC` | `#101612` | Window |
| `surface` | `#FAFCF9` | `#18201A` | Cards, sheets, nav |
| `surface-2` | `#E1E9DE` | `#232D25` | Inactive / sunken controls |
| `ink` | `#17221A` | `#E4EFE1` | Text, icons, map marker |
| `ink-2` | `#56665A` | `#96A794` | Secondary text |
| `line` | `#D0DBCD` | `#2D3A2F` | Dividers, outlines, rails |
| `accent` | `#3E6B45` | `#CFE8C9` | Primary actions, selection, focus |
| `accent-soft` | `#D4E8CE` | `#2D3A2F` | Selected nav item, tonal buttons |
| `live` | `#F2A10C` | `#FFB52E` | Mocking only |
| `live-soft` | `#FDF0D2` | `#3A2A08` | Holding (paused but still mocking) |
| `route` | `#C0267E` | `#F05BAE` | Default route color |
| `ok` | `#1F8A4C` | `#7FD3A8` | Setup steps done, staged |
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
  the ghost bus (a bus front with a ghost's wavy hem instead of wheels,
  windshield and headlight "eyes" cut out) in pale ghost green `#D4E8D0` on dark sage `#2D3A2F`, with a monochrome layer for
  themed icons. The notification icon uses the same silhouette.
