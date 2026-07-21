#!/usr/bin/env python3
"""Run controlled web/API simulations and export cycle snapshots for documentation."""

from __future__ import annotations

import argparse
import csv
import json
import queue
import time
from pathlib import Path

import httpx
import socketio


def find_project_root() -> Path:
    for candidate in Path(__file__).resolve().parents:
        if (candidate / "web_demo.py").is_file() and (candidate / "build.sbt").is_file():
            return candidate
    raise RuntimeError("Could not locate the repository root")


PROJECT_ROOT = find_project_root()
DATA_DIR = PROJECT_ROOT / "docs" / "report" / "assets" / "data"
LEVEL_SOURCES = {
    1: PROJECT_ROOT / "course_material" / "task3_level1" / "solution" / "src",
    2: PROJECT_ROOT / "course_material" / "task4_level2" / "solution" / "src",
    3: PROJECT_ROOT / "course_material" / "task5_level3_branch" / "solution" / "src",
    4: PROJECT_ROOT / "infrastructure_template" / "src",
}


def upload_payload(source_dir: Path) -> dict[str, str]:
    files: dict[str, str] = {}
    for path in sorted(source_dir.rglob("*")):
        if path.is_file() and (path.suffix == ".scala" or path.name.endswith("BinaryFile")):
            relative = path.relative_to(source_dir.parent).as_posix()
            files[relative] = path.read_text()
    return files


def run_level(base_url: str, level: int, max_cycles: int) -> tuple[list[dict], dict]:
    messages: queue.Queue[dict] = queue.Queue()
    logs: list[str] = []
    client = socketio.Client(reconnection=False, logger=False, engineio_logger=False)

    @client.on("update")
    def on_update(packet: dict) -> None:
        messages.put(packet)

    @client.on("build_log")
    def on_build_log(data: dict) -> None:
        logs.append(str(data.get("line", "")).strip())

    session_id = f"sess_validation_l{level}"
    client.connect(base_url, transports=["websocket", "polling"])
    client.emit("join_session", session_id)

    with httpx.Client(base_url=base_url, timeout=300.0) as http:
        detection = http.post(
            "/upload_and_detect", json={"files": upload_payload(LEVEL_SOURCES[level])}
        ).raise_for_status().json()
        if detection.get("status") != "success" or detection.get("level") != level:
            raise RuntimeError(f"Level {level} detection failed: {detection}")

        compilation = http.post(
            "/compile",
            json={
                "scala_files": detection["scala"],
                "asm_code": detection["asm_system"],
                "session_id": session_id,
            },
        ).raise_for_status().json()
        if compilation.get("status") != "success":
            raise RuntimeError(f"Level {level} compilation failed: {compilation.get('message')}")

    client.emit("command", {"action": "init", "session_id": session_id})
    snapshots: list[dict] = [messages.get(timeout=30)]

    while not snapshots[-1]["enriched"].get("coreDone") and len(snapshots) <= max_cycles:
        previous_cycle = snapshots[-1]["enriched"]["cycle"]
        client.emit("command", {"action": "step", "session_id": session_id})
        packet = messages.get(timeout=30)
        if packet["enriched"]["cycle"] <= previous_cycle:
            raise RuntimeError(f"Level {level} did not advance beyond cycle {previous_cycle}")
        snapshots.append(packet)

    client.disconnect()
    summary = {
        "level": level,
        "session_id": session_id,
        "detected_level": detection["level"],
        "detection_message": detection["message"],
        "cycles_observed": snapshots[-1]["enriched"]["cycle"],
        "termination_observed": bool(snapshots[-1]["enriched"].get("coreDone")),
        "forwarding_cycles": sum(
            bool(s["enriched"].get("fwd", {}).get("a_sel") or s["enriched"].get("fwd", {}).get("b_sel"))
            for s in snapshots
        ),
        "stall_cycles": sum(bool(s["enriched"].get("hazard", {}).get("id_stall")) for s in snapshots),
        "flush_cycles": sum(bool(s["enriched"].get("hazard", {}).get("flush")) for s in snapshots),
        "last_log_lines": logs[-12:],
    }
    return snapshots, summary


def write_outputs(level: int, snapshots: list[dict], summary: dict) -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    (DATA_DIR / f"level{level}_snapshots.json").write_text(
        json.dumps({"summary": summary, "snapshots": snapshots}, indent=2) + "\n"
    )

    fields = [
        "cycle", "core_done", "if_pc", "if_asm", "id_asm", "ex_asm", "mem_asm", "wb_asm",
        "fwd_a", "fwd_b", "if_stall", "id_stall", "flush", "wb_we", "wb_rd", "wb_wdata",
    ]
    with (DATA_DIR / f"level{level}_cycles.csv").open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for packet in snapshots:
            data = packet["enriched"]
            writer.writerow(
                {
                    "cycle": data.get("cycle", 0),
                    "core_done": data.get("coreDone", 0),
                    "if_pc": data.get("pc_hex", {}).get("if", ""),
                    "if_asm": data.get("asm", {}).get("if", ""),
                    "id_asm": data.get("asm", {}).get("id", ""),
                    "ex_asm": data.get("asm", {}).get("ex", ""),
                    "mem_asm": data.get("asm", {}).get("mem", ""),
                    "wb_asm": data.get("asm", {}).get("wb", ""),
                    "fwd_a": data.get("fwd", {}).get("a_sel", 0),
                    "fwd_b": data.get("fwd", {}).get("b_sel", 0),
                    "if_stall": data.get("hazard", {}).get("if_stall", 0),
                    "id_stall": data.get("hazard", {}).get("id_stall", 0),
                    "flush": data.get("hazard", {}).get("flush", 0),
                    "wb_we": data.get("wb", {}).get("we", 0),
                    "wb_rd": data.get("wb", {}).get("rd", 0),
                    "wb_wdata": data.get("wb", {}).get("wdata", 0),
                }
            )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--levels", nargs="+", type=int, default=[1, 2, 3, 4])
    parser.add_argument("--max-cycles", type=int, default=150)
    args = parser.parse_args()

    summaries = []
    started = time.monotonic()
    for level in args.levels:
        snapshots, summary = run_level(args.base_url, level, args.max_cycles)
        write_outputs(level, snapshots, summary)
        summaries.append(summary)
        print(json.dumps(summary, indent=2))
    (DATA_DIR / "validation_summary.json").write_text(json.dumps(summaries, indent=2) + "\n")
    print(f"Validated {len(summaries)} level(s) in {time.monotonic() - started:.1f} seconds")


if __name__ == "__main__":
    main()
