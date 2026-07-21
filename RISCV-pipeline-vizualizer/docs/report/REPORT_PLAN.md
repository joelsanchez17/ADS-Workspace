# Technical Report Plan

## Status and deliverable

The plan has been executed in `RISCV_PIPELINE_VISUALIZER_REPORT.md`. Its supporting environment record is `ENVIRONMENT_AND_VALIDATION.md`, and all publication assets are indexed in `VISUAL_ASSET_PLAN.md`. This file preserves the editorial rationale for later shortening or LaTeX conversion.

## Title, audience, and scope

**Title:** *Architecture and Operation of a Web-Based RISC-V Pipeline Debugging Environment*

The intended audience is university instructors, assessors, and students who know basic computer architecture and RISC-V but not this repository. The report explains the platform boundary, installation, user workflow, multi-process architecture, five-stage datapath, hazards, and representative validated runs. It does not claim that every decoded instruction is implemented by every course-level core, and it distinguishes the Python orchestration layer from the Chisel processor simulation.

## Final narrative order

| Order | Section | Purpose | Principal evidence |
| ---: | --- | --- | --- |
| 1 | Introduction and Scope | Establish the educational problem and define the system boundary. | Landing interface; application and course sources |
| 2 | Quick Setup | Give the shortest reproducible install/start path and tested versions. | `scripts/setup.sh`, `scripts/run.sh`, requirements files, environment record |
| 3 | Basic User Workflow | Explain upload, detection, compile, stepping, inspection, and waveform access. | Browser validation and execution sequence |
| 4 | High-Level Architecture | Explain browser, ASGI services, session workspace, SBT/chiseltest, TCP bridge, and Surfer. | Architecture diagram and source inspection |
| 5 | Five-Stage Pipeline Background | Introduce IF, ID, EX, MEM, WB and the course-level progression. | Pipeline diagram and Chisel sources |
| 6 | Detailed Execution and Data Flow | Trace upload through snapshot enrichment and SVG rendering. | Sequence diagram, backend, bridge, live testbench |
| 7 | Hazard Handling | Explain forwarding, load-use stalls, branch redirect, and flush behavior. | Level 2–4 captures and exported snapshot data |
| 8 | Demonstration and Evaluation | Present exact observed cycles plus the event-count graph. | Screenshots, CSV/JSON, plotting script |
| 9 | Limitations and Reproducibility | State CDN, session-resource, waveform, nomenclature, and support-boundary caveats. | Source review and validation observations |
| 10 | Conclusion | Summarize the educational value without overstating coverage. | Synthesis of verified results |

The completed Markdown draft is intended to compress to roughly 7–9 pages of main text after academic-template conversion, depending on figure sizing.

## Evidence and wording rules

- Use “execution-verified” only for behavior reproduced in `ENVIRONMENT_AND_VALIDATION.md`.
- Describe `BinaryFile` as newline-delimited hexadecimal machine words, not source assembly.
- Treat the Level 4 implementation as the integrated reference core; do not generalize its ISA or hazard behavior to every course level.
- Define a forwarding event as a cycle in which either forwarding selector is nonzero. Count stall and flush cycles from their corresponding debug flags.
- Do not compare event totals as performance figures because the four validated programs and run lengths differ.
- Use repository-relative paths and omit usernames, machine-specific directories, and student-identifying data.

## Selected visuals

The main report uses three reproducible Mermaid-to-SVG diagrams, four application screenshots, one waveform screenshot, and one Matplotlib event-count chart. The Level 2 forwarding, Level 3 branch flush, and Level 4 load-use stall views provide complementary evidence; none is a decorative duplicate. Source, metadata, exported data, and rendered formats remain together beneath `assets/`.

## Toolchain used

| Purpose | Tool and validated version | Output |
| --- | --- | --- |
| Architecture, sequence, pipeline diagrams | Mermaid CLI 11.16.0 | SVG |
| Browser capture and UI validation | Playwright 1.61.0 with Chrome 149.0.7827.55 | PNG |
| Quantitative graph | Matplotlib 3.11.1 | SVG and PDF |
| Waveform evidence | Bundled Surfer viewer | PNG |
| Raw validation/export | Python utility using HTTP and Socket.IO | CSV and JSON |

## Conversion note

For LaTeX, convert SVG diagrams to PDF only if the selected engine/template lacks direct SVG support; retain the SVG masters. Keep PNG screenshots at native resolution and preserve their captions’ level, program, cycle, viewport, and revision metadata. Code excerpts should remain selectable text rather than raster images.
