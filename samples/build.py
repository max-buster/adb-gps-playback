#!/usr/bin/env python3
"""Maintain the example GTFS feeds in samples/.

    python3 samples/build.py           # refresh sizeBytes/sha256 in index.json
    python3 samples/build.py --demo    # also regenerate the synthetic demo zips

index.json is hand-edited (id, name, description, url, ...). This script only
fills in sizeBytes and sha256 for entries whose url is a relative path to a
file in this folder. Entries with an absolute url (a GitHub Release asset or
an agency's own download link) are left alone: set their size by hand if you
know it, and leave sha256 out unless the file at that url never changes.

The demo feeds are made up (fictional agency, routes and timetable laid over
Manhattan's street grid) and released under CC0, so they can live in the repo.
Zips are written with fixed timestamps so rebuilding them is byte-for-byte
reproducible and doesn't churn git.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import math
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
INDEX = HERE / "index.json"
FEEDS = HERE / "feeds"
ZIP_TIME = (2026, 1, 1, 0, 0, 0)

# ---------------------------------------------------------------- demo feeds

# Straight runs along real Manhattan streets (approximate, good to ~20 m).
FIFTH_AVE = [  # Washington Square → 110th St, northbound
    ("Washington Sq", 40.73155, -73.99706),
    ("14 St", 40.73600, -73.99383),
    ("23 St", 40.74154, -73.98973),
    ("34 St", 40.74846, -73.98469),
    ("42 St", 40.75367, -73.98095),
    ("50 St", 40.75870, -73.97728),
    ("59 St", 40.76442, -73.97311),
    ("72 St", 40.77262, -73.96714),
    ("86 St", 40.78148, -73.96067),
    ("96 St", 40.78776, -73.95609),
    ("110 St", 40.79661, -73.94965),
]
CROSSTOWN_42 = [  # 42nd St, 1st Ave → 12th Ave, westbound
    ("1 Av", 40.74887, -73.96932),
    ("Lexington Av", 40.75160, -73.97585),
    ("Madison Av", 40.75304, -73.97933),
    ("6 Av", 40.75452, -73.98400),
    ("Times Sq", 40.75604, -73.98680),
    ("8 Av", 40.75747, -73.98965),
    ("9 Av", 40.75876, -73.99272),
    ("10 Av", 40.76000, -73.99566),
    ("11 Av", 40.76130, -73.99872),
    ("12 Av", 40.76250, -74.00160),
]


def _dist_m(a, b):
    lat = math.radians((a[0] + b[0]) / 2)
    dy = (b[0] - a[0]) * 111_320
    dx = (b[1] - a[1]) * 111_320 * math.cos(lat)
    return math.hypot(dx, dy)


def _densify(points, step_m=40.0):
    """Shape points every ~step_m metres between consecutive stops."""
    out = [points[0]]
    for a, b in zip(points, points[1:]):
        n = max(1, round(_dist_m(a, b) / step_m))
        for i in range(1, n + 1):
            t = i / n
            out.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t))
    return out


def _csv(rows):
    buf = io.StringIO()
    w = csv.writer(buf, lineterminator="\n")
    for r in rows:
        w.writerow(r)
    return buf.getvalue()


def _hms(sec):
    return f"{sec // 3600:02d}:{sec % 3600 // 60:02d}:{sec % 60:02d}"


def _demo_tables(with_shapes: bool) -> dict[str, str]:
    lines = [
        # route_id, short, long, color, text, stops (in the "0" direction)
        ("AV", "5", "Fifth Avenue Demo", "1F6FD1", "FFFFFF", FIFTH_AVE),
        ("XT", "42", "42nd Street Crosstown Demo", "D9472B", "FFFFFF", CROSSTOWN_42),
    ]
    stops, routes, trips, stop_times, shapes = {}, [], [], [], []
    for rid, short, long_, color, text, line in lines:
        routes.append((rid, "DEMO", short, long_, "3", color, text))
        for direction, seq in ((0, line), (1, list(reversed(line)))):
            shape_id = f"{rid}_{direction}"
            headsign = seq[-1][0]
            for k in range(3):  # three departures per direction, 20 min apart
                trip_id = f"{rid}_{direction}_{k + 1}"
                trips.append((rid, "DAILY", trip_id, headsign, direction, shape_id if with_shapes else ""))
                t = 7 * 3600 + k * 1200
                for i, (name, lat, lon) in enumerate(seq):
                    sid = f"{rid}_{name.replace(' ', '_').upper()}"
                    stops[sid] = (sid, name, f"{lat:.6f}", f"{lon:.6f}")
                    if i:
                        # ~18 km/h average including the 20 s dwell
                        t += round(_dist_m(seq[i - 1][1:], (lat, lon)) / 5.0)
                    stop_times.append((trip_id, _hms(t), _hms(t + 20), sid, i + 1))
                    t += 20
            if with_shapes:
                pts = _densify([(lat, lon) for _, lat, lon in seq])
                for i, (lat, lon) in enumerate(pts):
                    shapes.append((shape_id, f"{lat:.6f}", f"{lon:.6f}", i + 1))

    tables = {
        "agency.txt": _csv([
            ("agency_id", "agency_name", "agency_url", "agency_timezone"),
            ("DEMO", "GTFS Playback Demo Transit", "https://example.com/", "America/New_York"),
        ]),
        "calendar.txt": _csv([
            ("service_id", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "start_date", "end_date"),
            ("DAILY", 1, 1, 1, 1, 1, 1, 1, "20260101", "20991231"),
        ]),
        "routes.txt": _csv([("route_id", "agency_id", "route_short_name", "route_long_name", "route_type", "route_color", "route_text_color"), *routes]),
        "stops.txt": _csv([("stop_id", "stop_name", "stop_lat", "stop_lon"), *sorted(stops.values())]),
        "trips.txt": _csv([("route_id", "service_id", "trip_id", "trip_headsign", "direction_id", "shape_id"), *trips]),
        "stop_times.txt": _csv([("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence"), *stop_times]),
    }
    if with_shapes:
        tables["shapes.txt"] = _csv([("shape_id", "shape_pt_lat", "shape_pt_lon", "shape_pt_sequence"), *shapes])
    return tables


def _write_zip(path: Path, tables: dict[str, str]):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        for name in sorted(tables):
            info = zipfile.ZipInfo(name, ZIP_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, tables[name])


def build_demo():
    _write_zip(FEEDS / "demo-manhattan.zip", _demo_tables(with_shapes=True))
    _write_zip(FEEDS / "demo-manhattan-no-shapes.zip", _demo_tables(with_shapes=False))


# --------------------------------------------------------------------- index

def refresh_index() -> int:
    index = json.loads(INDEX.read_text())
    problems = 0
    seen = set()
    for feed in index["feeds"]:
        fid = feed.get("id", "?")
        for key in ("id", "name", "url"):
            if not feed.get(key):
                print(f"{fid}: missing {key}", file=sys.stderr)
                problems += 1
        if fid in seen:
            print(f"{fid}: duplicate id", file=sys.stderr)
            problems += 1
        seen.add(fid)
        url = feed.get("url", "")
        if "://" in url:
            continue
        path = (HERE / url).resolve()
        if not path.is_file() or HERE not in path.parents:
            print(f"{fid}: {url} is not a file under samples/", file=sys.stderr)
            problems += 1
            continue
        data = path.read_bytes()
        feed["sizeBytes"] = len(data)
        feed["sha256"] = hashlib.sha256(data).hexdigest()
    INDEX.write_text(json.dumps(index, indent=2, ensure_ascii=False) + "\n")
    return problems


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--demo", action="store_true", help="regenerate the synthetic demo zips first")
    args = ap.parse_args()
    if args.demo:
        build_demo()
    sys.exit(1 if refresh_index() else 0)


if __name__ == "__main__":
    main()
