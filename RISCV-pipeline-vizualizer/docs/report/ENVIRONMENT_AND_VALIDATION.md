# Environment and Validation Record

## Scope

This internal record describes the final implementation and report validation performed on 21 July 2026. The working tree was based on Git revision `ff31871`; the approved application and documentation changes remained uncommitted, as requested. Paths below are repository-relative, and no account, host, or home-directory information is retained.

## Host and toolchain

| Item | Validated value | Use |
| --- | --- | --- |
| Operating system | Ubuntu 24.04.4 LTS under WSL2, x86-64 | Application and report validation |
| Python | CPython 3.12.3 in `.venv/` | FastAPI/Socket.IO service and validation utilities |
| JDK | Eclipse Temurin 17.0.19 | Scala, Chisel, and chiseltest |
| SBT | 1.9.7 | Dependency resolution and test launch |
| Scala / Chisel / chiseltest | 2.12.13 / 3.5.0 / 0.5.0 | Processor construction and simulation |
| Playwright / Chrome | 1.61.0 / Chrome for Testing 149.0.7827.55 | Browser regression and screenshots |
| Mermaid CLI | 11.16.0 | Editable diagram export to SVG and PDF |
| Tectonic | 0.16.9 | Academic PDF build |
| Ghostscript | 10.02.1 | PDF page rendering and independent page-count check |
| pypdf | 6.14.2 | PDF metadata, media-box, and page-count inspection |

Application requirements remain pinned in `requirements.txt`. Report and validation dependencies are pinned in `requirements-docs.txt`. Large generated tools and browser runtimes are kept under ignored `.tools/`, `.venv/`, and `node_modules/` directories.

## Installation and startup validation

The final scripts passed `bash -n scripts/setup.sh scripts/run.sh scripts/build_report.sh`. Python compilation checks passed for the application entry point, server, bridge, and validation script.

`scripts/setup.sh` and `scripts/run.sh` were invoked from a temporary directory outside the repository. Setup dynamically found the project root, reused `.venv`, installed the pinned packages, detected JDK 17 and SBT 1.9.7, and reported that the complete simulation environment was ready. Startup likewise changed to the correct root, used the local environment, and served the application on `http://127.0.0.1:8080`. The setup output explains that the first SBT dependency resolution requires internet access and recommends `sbt --batch update`.

The missing-tool branches were inspected to confirm that Python, virtual-environment support, Java, and SBT failures return a nonzero status and display an installation command or official link. The script does not download a JDK or SBT and does not modify shell configuration.

## Web and interface regression

The application was started through `scripts/run.sh` and exercised with HTTP, Socket.IO, and a headless Chrome browser. The following checks passed:

- `/` and `/static/pipeline.svg` returned HTTP 200;
- Socket.IO connected and joined the current session room;
- `/workspace` returned HTTP 404 and no browser request used that route;
- `/static/client.js` returned HTTP 404 and no browser request referenced it;
- the compile request succeeded without a client-supplied `level` field;
- the toolbar retained the Show Hazards control and had no Datapath checkbox or layout gap;
- the visualizer continued to render forwarding, branch-flush, and load-use-stall overlays;
- the selector labels were `System BinaryFile` and `Custom BinaryFile`;
- Level 3 and Level 4 used the approved course names;
- successful and intentionally failed compile logs contained useful explanations but no absolute user path;
- a missing-VCD response used a logical session description rather than a filesystem path;
- the generated Level 4 VCD loaded in the bundled Surfer viewer.

The intentionally invalid Scala source failed as expected. Its browser-facing SBT output substituted `[WORKSPACE]` for the session directory, showing that sanitization did not remove the diagnostic context.

## Course-level simulation validation

`docs/report/assets/source/validate_sessions.py` submitted representative sources through the same upload, compile, Socket.IO initialization, and cycle-stepping interfaces used by the browser. It did not send a client level. Each run continued through the first `coreDone` snapshot.

| Detected level | Course description | Last cycle | Forward / stall / flush cycles | Result |
| ---: | --- | ---: | ---: | --- |
| 1 | Basic arithmetic pipeline | 12 | 0 / 0 / 0 | Completed |
| 2 | Data forwarding | 12 | 10 / 0 / 0 | Completed |
| 3 | Branches and jumps | 15 | 1 / 0 / 2 | Completed |
| 4 | Integrated memory and hazard handling | 51 | 23 / 4 / 6 | Completed |

The browser was also used to upload, compile, and step Levels 2, 3, and 4. The retained screenshots show Level 2 forwarding at cycle 3, a Level 3 taken branch at cycle 7, and a Level 4 load-use stall at cycle 41. The Level 1 API run confirms upload, detection, compilation, and completion for the basic core.

The event totals demonstrate that the expected mechanisms were exercised. They are not a performance comparison because the BinaryFiles and run lengths differ.

## Figures and screenshot privacy

Affected screenshots were recaptured at a 1440 by 900 CSS-pixel viewport. Metadata is stored in `assets/data/screenshot_metadata.json` with base revision, level, system BinaryFile, cycle, and capture environment. Browser profiles, usernames, hostnames, notifications, and absolute paths are absent. The editable Mermaid sources in `assets/source/` were exported to both SVG and tightly cropped PDF publication formats in `assets/diagrams/`.

## Report build and inspection

`scripts/build_report.sh` dynamically locates the repository and invokes Tectonic from `PATH` or the ignored project-local tool directory. It produces `docs/report/RISCV_PIPELINE_VISUALIZER_REPORT.pdf`. The final PDF properties are:

| Property | Value |
| --- | --- |
| Total pages | 11 |
| Main text | 8 pages, excluding title, contents, references, and appendix material |
| Media box | 595.28 × 841.89 points (A4) |
| Title metadata | `Architecture and Operation of a Web-Based RISC-V Pipeline Debugging Environment` |
| Author metadata | `[Author name]` |

All 11 pages were rendered to 992 by 1403 pixel PNG files at 120 dpi and inspected. The review covered the title page, contents, commands, troubleshooting and maintenance tables, three generated diagrams, landing screenshot, combined hazard figure, bibliography, and appendix. No clipped text, table overflow, broken figure, unresolved reference, or machine path was observed. The main document contains no repository URL placeholder or internal Markdown-to-LaTeX production section. Tectonic reported only non-fatal underfull-box warnings caused by breakable repository paths and bibliography URLs; no overfull box remained.

## Validation boundary and future checks

The final phase did not change processor semantics, session architecture, CORS, VCD limits, decoder coverage, concurrency, authentication, sandboxing, frontend vendoring, or CI. Python versions other than 3.12.3, browser engines other than Chrome, hostile uploads, multi-user load, and public network deployment were not tested. Processor ISA conformance beyond the supplied programs and existing Scala tests was not re-certified. These boundaries are reflected as proportionate future work in the report.
