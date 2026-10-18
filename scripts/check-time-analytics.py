#!/usr/bin/env python3
"""Read-only running-stack API checks."""

import json
import sys
import urllib.request
from datetime import datetime

base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8085").rstrip("/")


def get(path):
    with urllib.request.urlopen(
        base + "/api/analytics/deliveries" + path, timeout=15
    ) as response:
        return json.load(response)


for metric, seconds in [("throughput", 60), ("delay-rate", 300)]:
    rows = get("/" + metric + "?minutes=10")
    assert len(rows) == 10
    previous = None
    for row in rows:
        start = datetime.fromisoformat(row["start"].replace("Z", "+00:00"))
        end = datetime.fromisoformat(row["end"].replace("Z", "+00:00"))
        assert (end - start).total_seconds() == seconds
        assert previous is None or (start - previous).total_seconds() == 60
        previous = start
        for key in ["created", "completed", "activeDeliveries", "delayedDeliveries"]:
            assert row[key] >= 0
        assert row["delayedDeliveries"] <= row["activeDeliveries"]
        if row["activeDeliveries"] == 0:
            assert row["delayRate"] is None
        else:
            assert (
                abs(
                    row["delayRate"]
                    - row["delayedDeliveries"] / row["activeDeliveries"]
                )
                < 1e-9
            )
    print(metric + ": chronological windows and rates valid")
s = get("/summary")
assert s["total"] == sum(
    s[key] for key in ["pending", "assigned", "inTransit", "completed", "cancelled"]
)
print("summary: status counts match total")
