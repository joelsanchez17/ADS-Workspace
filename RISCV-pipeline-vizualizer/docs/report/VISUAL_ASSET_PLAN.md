# Visual Asset Plan and Inventory

## Status

The planned publication assets have been generated from validated application runs. Editable sources, raw data, and capture metadata are retained so each figure can be reproduced or revised without relying on a machine-specific path.

| ID | Asset | Purpose and evidence | Output | Status |
| --- | --- | --- | --- | --- |
| D1 | System architecture | Browser, FastAPI/Socket.IO, session workspace, SBT/chiseltest, TCP bridge, history, VCD, and Surfer | `assets/diagrams/system_architecture.svg` and `.pdf`; source in `assets/source/system_architecture.mmd` | Generated and source-checked |
| D2 | Execution sequence | Upload/detection, compile, workspace staging, testbench connection, step, snapshot enrichment, and UI update | `assets/diagrams/execution_sequence.svg` and `.pdf`; source in `assets/source/execution_sequence.mmd` | Generated and source-checked |
| D3 | Five-stage pipeline | IF–ID–EX–MEM–WB plus forwarding, load-use stall, and branch flush paths | `assets/diagrams/five_stage_pipeline.svg` and `.pdf`; source in `assets/source/five_stage_pipeline.mmd` | Generated and cross-checked against the integrated core |
| F1 | Landing interface | Current upload page and four-step workflow at 1440×900 | `assets/screenshots/landing_interface.png` | Captured from the running application |
| F2 | Level 2 forwarding | Cycle 3: dependent `add x2,x1,x1` in EX with both forwarding selectors active | `assets/screenshots/level2_forwarding_cycle_03.png` | Browser and snapshot verified |
| F3 | Level 3 branch flush | Cycle 7: taken `beq x1,x2,+12` in EX, flush active, target `0x20` | `assets/screenshots/level3_branch_flush_cycle_07.png` | Browser and snapshot verified |
| F4 | Level 4 load-use stall | Cycle 41: load in EX, dependent add in ID, PC write disabled, IF/ID stalled | `assets/screenshots/level4_load_use_stall_cycle_41.png` | Browser and snapshot verified |
| F5 | Waveform viewer | Bundled Surfer loading the generated VCD and exposing the processor hierarchy | `assets/screenshots/waveform_view.png` | VCD download and browser rendering verified; optional in the main report |
| G1 | Hazard event counts | Per-run cycles with forwarding, stall, or flush active across the four representative programs | `assets/graphs/hazard_event_counts.svg` and `.pdf` | Generated from exported snapshots |

## Data and reproducibility

- `assets/source/validate_sessions.py` drives the HTTP/Socket.IO flow and exports cycle snapshots.
- `assets/source/plot_event_counts.py` derives the event-count table and renders the graph.
- `assets/data/level*_cycles.csv` and `level*_snapshots.json` retain the raw per-level evidence.
- `assets/data/validation_summary.json` and `event_counts.csv` retain concise summaries.
- `assets/data/screenshot_metadata.json` records the repository revision, viewport, level, program, and exact cycle for each execution capture.

Forwarding is counted once per cycle when either selector is nonzero; stall and flush use their respective debug flags. Counts extend from cycle 0 through the first `coreDone` snapshot. Because the programs and run lengths differ, the graph demonstrates mechanism coverage and must not be interpreted as a performance comparison between course levels.

## Capture and publication rules

- Keep repository-relative asset references and exclude usernames, local absolute paths, browser profiles, notifications, and student identifiers.
- Prefer PDF in LaTeX and retain SVG for generated diagrams and graphs; use PNG only for direct browser evidence.
- Preserve Mermaid, plotting, data, and metadata sources beside rendered outputs.
- Confirm permission for institutional branding or named attribution before external publication.
- Do not substitute the unrelated UVM testbench image or the stale checked-in landing screenshot for current application evidence.
