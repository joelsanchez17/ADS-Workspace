#!/usr/bin/env python3
"""Plot signal-active cycle counts from validated simulation snapshot exports."""

from __future__ import annotations

import csv
import json
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np


def find_project_root() -> Path:
    for candidate in Path(__file__).resolve().parents:
        if (candidate / "web_demo.py").is_file() and (candidate / "build.sbt").is_file():
            return candidate
    raise RuntimeError("Could not locate the repository root")


ROOT = find_project_root()
DATA = ROOT / "docs" / "report" / "assets" / "data"
GRAPHS = ROOT / "docs" / "report" / "assets" / "graphs"


summaries = []
for level in range(1, 5):
    payload = json.loads((DATA / f"level{level}_snapshots.json").read_text())
    summaries.append(payload["summary"])

(DATA / "validation_summary.json").write_text(json.dumps(summaries, indent=2) + "\n")
with (DATA / "event_counts.csv").open("w", newline="") as handle:
    writer = csv.DictWriter(
        handle,
        fieldnames=["level", "cycles_observed", "forwarding_cycles", "stall_cycles", "flush_cycles"],
    )
    writer.writeheader()
    for summary in summaries:
        writer.writerow({key: summary[key] for key in writer.fieldnames})

labels = [f"Level {item['level']}\n({item['cycles_observed']} cycles)" for item in summaries]
forwarding = [item["forwarding_cycles"] for item in summaries]
stalls = [item["stall_cycles"] for item in summaries]
flushes = [item["flush_cycles"] for item in summaries]

x = np.arange(len(labels))
width = 0.24
fig, ax = plt.subplots(figsize=(8.2, 4.4), constrained_layout=True)
ax.bar(x - width, forwarding, width, label="Forwarding active", color="#4A90E2")
ax.bar(x, stalls, width, label="ID stall active", color="#E5A93D")
ax.bar(x + width, flushes, width, label="Flush active", color="#C95A64")
ax.set_ylabel("Signal-active cycles")
ax.set_xlabel("Auto-detected course level and observed run length")
ax.set_xticks(x, labels)
ax.set_title("Hazard-resolution activity in the validated system programs")
ax.grid(axis="y", color="#D8DDE5", linewidth=0.7)
ax.set_axisbelow(True)
ax.legend(frameon=False, ncols=3, loc="upper left")
ax.spines[["top", "right"]].set_visible(False)

GRAPHS.mkdir(parents=True, exist_ok=True)
fig.savefig(GRAPHS / "hazard_event_counts.svg", metadata={"Creator": "Matplotlib"})
fig.savefig(GRAPHS / "hazard_event_counts.pdf")
print(DATA / "event_counts.csv")
print(GRAPHS / "hazard_event_counts.svg")
print(GRAPHS / "hazard_event_counts.pdf")
