# RISC-V Pipeline Visualizer

## Project purpose

This repository contains a browser-based visualizer and debugging environment for five-stage RISC-V processors written in Chisel. It is intended for a trusted local teaching environment: an instructor runs the service, and students upload course processor sources for cycle-by-cycle inspection.

Python coordinates the web interface, upload filtering, session workspaces, and simulation control. The processor itself is executed by Chisel and chiseltest through SBT; Python does not emulate it.

## Main capabilities

- Automatic detection of four course levels, from a basic arithmetic pipeline to integrated memory and hazard handling.
- Browser editing and compilation of accepted Chisel source files.
- Cycle stepping with IF, ID, EX, MEM, and WB stage state.
- Register, forwarding, stall, flush, memory, and build-log inspection.
- VCD waveform viewing through the bundled Surfer interface.
- Browser-side export of edited source files as a ZIP archive.

## Repository structure

| Path | Purpose |
| --- | --- |
| `web_demo.py` | Local Uvicorn entry point |
| `web_visualizer/` | FastAPI/Socket.IO backend and browser interface |
| `infrastructure_template/` | Integrated Level 4 processor and controlled live testbench |
| `course_material/` | Level 1–3 course skeletons and solutions |
| `infrastructure_template/system_tests/` | System BinaryFiles for the four detected levels |
| `scripts/` | Setup, launch, and report-build helpers |
| `docs/report/` | Technical report, evidence, and publication assets |

## Prerequisites

- Python 3 with virtual-environment support; Python 3.12 is validated.
- JDK 17; another SBT-compatible JDK may work but has not been tested here.
- SBT 1.9.7, as declared in `project/build.properties`.
- Internet access during initial setup, the first SBT dependency resolution, and normal use of the current CDN-hosted editor libraries.
- A current JavaScript-capable browser.

On Ubuntu or Debian, Python virtual-environment support and JDK 17 can be installed with:

```bash
sudo apt update
sudo apt install python3-venv openjdk-17-jdk
```

Install SBT using its [official instructions](https://www.scala-sbt.org/download/). The setup script checks these prerequisites but does not download a JDK or SBT automatically.

## Quick installation

```bash
git clone git@github.com:RPTU-EIS/RISCV-pipeline-vizualizer.git
cd RISCV-pipeline-vizualizer
./scripts/setup.sh
./scripts/run.sh
```

Open <http://localhost:8080>. The default listener is restricted to the local computer.

The first processor compilation downloads the declared Scala, Chisel, compiler-plugin, and chiseltest dependencies. This may take several minutes. To resolve them in advance, run the following after setup:

```bash
sbt --batch update
```

## Starting the application manually

The helper scripts are recommended. The equivalent Python setup is:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
java -version
sbt --script-version
python web_demo.py
```

The server accepts `--host` and `--port`. Binding to a non-loopback address exposes a service that compiles submitted Scala and is outside the validated local-use scope.

## Using the application

1. Start the service and open <http://localhost:8080>.
2. Select the student's project `src` directory.
3. Review the detected level:
   - Level 1 — Basic arithmetic pipeline
   - Level 2 — Data forwarding
   - Level 3 — Branches and jumps
   - Level 4 — Integrated memory and hazard handling
4. Select the system BinaryFile or an uploaded custom BinaryFile.
5. Compile the design, then step through cycles or advance five cycles at a time.
6. Inspect the pipeline, registers, logs, forwarding/stall/flush indicators, and Surfer waveforms.
7. Export browser-side source edits when required.

## Expected student project format

Upload the project `src` directory containing hardware sources under `src/main/scala/`. Student tests, build output, uploaded build definitions, and unsupported files are ignored. Levels 1–3 require `PipelinedRISCV32I.scala`; the integrated Level 4 design is detected from its infrastructure modules.

An optional file whose name ends in `BinaryFile` is accepted as a custom instruction program.

## BinaryFile format

`BinaryFile` is not assembly-language source. It contains one 32-bit hexadecimal machine-code word per line:

```text
00500093
00108133
00000073
```

The final `00000073` word is used by the supplied processors as the test termination marker.

## Troubleshooting

| Symptom | Likely cause and action |
| --- | --- |
| `python3` or `venv` unavailable | Install Python 3 and its virtual-environment package, then rerun `./scripts/setup.sh`. |
| Java or SBT missing | Install JDK 17 and SBT 1.9.7 using the prerequisite instructions. Setup exits nonzero until both are available. |
| First compile is slow | Allow SBT to download dependencies, or run `sbt --batch update` beforehand. |
| Editor, socket, or ZIP export fails to load | Check access to the Socket.IO, Monaco, and JSZip CDNs used by the current interface. |
| Upload reports no accepted Scala files | Select the complete `src` directory containing `src/main/scala/*.scala`. |
| Compilation fails | Read the browser build log for the first Scala/Chisel error and confirm the required top-level debug interface exists. |
| Port 8080 is occupied | Run `./scripts/run.sh --port <free-port>`. |
| VCD is unavailable | Compile and simulate the project before opening the Waveforms tab. |

## Main implementation files

- `web_demo.py` selects the local host/port and starts Uvicorn.
- `web_visualizer/server.py` handles uploads, level detection, session staging, SBT processes, snapshots, and HTTP/Socket.IO communication.
- `web_visualizer/bridge.py` implements the local TCP protocol with the Chisel testbench.
- `web_visualizer/templates/index.html` contains the current browser workflow and visualization logic.
- `web_visualizer/static/pipeline.svg` is the semantic five-stage diagram updated by the browser.
- `infrastructure_template/src/test/scala/LivePipelineTest.scala` produces VCD data and serves interactive snapshots.

## Documentation and report

The supervisor-facing report is available at `docs/report/RISCV_PIPELINE_VISUALIZER_REPORT.pdf`. Its editable Markdown, LaTeX source, validation record, diagrams, screenshots, and data are retained below `docs/report/`.

## Current scope

The validated deployment is a trusted local teaching setup. It is not a hardened public compilation service: authentication, sandboxing, multi-user resource controls, complete offline packaging, and broader automated regression coverage remain future work. Processor instruction support also depends on the uploaded course-level implementation rather than on the visualizer alone.
