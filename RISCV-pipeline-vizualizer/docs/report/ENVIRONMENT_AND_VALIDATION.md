# Environment and Validation Record

## Scope and evidence convention

This record documents the environment used to validate the report assets on 21 July 2026. **Source-verified** means read from repository configuration or implementation. **Execution-verified** means observed by running the command or workflow. Project-local generated tools are stored under ignored `.venv/`, `.tools/`, and `node_modules/` directories and are not intended for Git.

Base Git revision: `02e87a0`. Validation also includes the uncommitted portability and documentation changes described in `RISCV_PIPELINE_VISUALIZER_REPORT.md`.

## Host environment

| Item | Value | Evidence |
| --- | --- | --- |
| Operating system | Ubuntu 24.04.4 LTS (Noble) under WSL2 | Execution-verified with `/etc/os-release` and `uname` |
| Architecture | x86-64 | Execution-verified |
| Kernel | Linux 6.6.87.2-microsoft-standard-WSL2 | Execution-verified |
| Python | CPython 3.12.3 | Execution-verified with `python3 --version` |
| Virtual environment | `.venv/` relative to repository root | Execution-verified |
| Node.js / npm | 20.20.2 / 10.8.2 | Execution-verified |

No hostname, account name, IP address, SSH data, token, or absolute home path is included in this record.

## Installed runtime and documentation tools

| Tool | Version | Location model | Purpose | Evidence |
| --- | --- | --- | --- | --- |
| Eclipse Temurin JDK | 17.0.19+10 | Project-local `.tools/jdk/` | Run SBT, Scala, Chisel, and chiseltest | Execution-verified with `java -version` |
| SBT | 1.9.7 | Project-local `.tools/sbt/` | Resolve dependencies and run `LivePipelineTest` | Source- and execution-verified |
| Scala | 2.12.13 | Resolved by SBT | Compile processor sources | Source-verified in `build.sbt`; compilation verified |
| Chisel | 3.5.0 | Resolved by SBT | Hardware construction language | Source-verified in `build.sbt`; elaboration verified |
| chiseltest | 0.5.0 | Resolved by SBT | Simulation, VCD, and live test | Source-verified in `build.sbt`; execution verified |
| Mermaid CLI | 11.16.0 | Local `node_modules/` | Export architecture and sequence diagrams | Execution-verified |
| Playwright | 1.61.0 | `.venv/` | Automated browser validation and screenshots | Execution-verified |
| Chrome for Testing | 149.0.7827.55 | `.tools/playwright-browsers/` | Headless browser | Execution-verified |
| Matplotlib | 3.11.1 | `.venv/` | Reproducible event-count graph | Execution-verified |

The host did not provide a system browser or non-interactive administrator access. Chromium runtime packages `libnspr4`, `libnss3`, `libasound2t64`, and `libasound2-data` were therefore downloaded as Ubuntu packages and extracted under ignored `.tools/browser-libs/`; the operating system was not modified.

## Python packages

Direct application requirements are pinned in `requirements.txt`:

```text
fastapi==0.139.2
pydantic==2.13.4
python-socketio==5.16.3
uvicorn==0.51.0
```

Documentation/validation additions are pinned in `requirements-docs.txt`: `httpx==0.28.1`, `matplotlib==3.11.1`, `playwright==1.61.0`, and `websocket-client==1.9.0`, in addition to the runtime file.

The complete installed Python environment, including transitive packages, was:

```text
annotated-doc==0.0.4
annotated-types==0.7.0
anyio==4.14.2
bidict==0.23.1
certifi==2026.6.17
click==8.4.2
contourpy==1.3.3
cycler==0.12.1
fastapi==0.139.2
fonttools==4.63.0
greenlet==3.5.3
h11==0.16.0
httpcore==1.0.9
httpx==0.28.1
idna==3.18
kiwisolver==1.5.0
matplotlib==3.11.1
numpy==2.5.1
packaging==26.2
pillow==12.3.0
playwright==1.61.0
pydantic==2.13.4
pydantic_core==2.46.4
pyee==13.0.1
pyparsing==3.3.2
python-dateutil==2.9.0.post0
python-engineio==4.13.3
python-socketio==5.16.3
simple-websocket==1.1.0
six==1.17.0
starlette==1.3.1
typing-inspection==0.4.2
typing_extensions==4.16.0
uvicorn==0.51.0
websocket-client==1.9.0
wsproto==1.3.2
```

## Installation commands used

### Python

```bash
python3 -m venv .venv
.venv/bin/python -m pip install --upgrade pip
.venv/bin/python -m pip install fastapi uvicorn python-socketio pydantic \
    matplotlib playwright httpx websocket-client
```

Versions successfully tested by these commands were then recorded in the two requirement files.

### Project-local JDK and SBT

The following method was used because system installation required unavailable interactive privileges. It is recorded for reproducibility, not required when compatible system tools already exist.

```bash
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
mkdir -p "$PROJECT_ROOT/.tools/jdk" "$PROJECT_ROOT/.tools/sbt"

curl -fL -o /tmp/riscv-viz-jdk17.tar.gz \
  'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse'
tar -xzf /tmp/riscv-viz-jdk17.tar.gz \
  -C "$PROJECT_ROOT/.tools/jdk" --strip-components=1

curl -fL -o /tmp/riscv-viz-sbt-1.9.7.tgz \
  'https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz'
tar -xzf /tmp/riscv-viz-sbt-1.9.7.tgz \
  -C "$PROJECT_ROOT/.tools/sbt" --strip-components=1

export JAVA_HOME="$PROJECT_ROOT/.tools/jdk"
export PATH="$JAVA_HOME/bin:$PROJECT_ROOT/.tools/sbt/bin:$PATH"
java -version
sbt --script-version
sbt --batch update
```

The first `sbt --batch update` downloaded SBT 1.9.7 and its Scala 2.12.18 launcher, then resolved the project configured for Scala 2.12.13, Chisel 3.5.0, and chiseltest 0.5.0. The only reported build-definition messages were deprecation warnings for `Resolver.sonatypeRepo`.

### Documentation tools

```bash
PUPPETEER_SKIP_DOWNLOAD=true npm install

PLAYWRIGHT_BROWSERS_PATH="$PWD/.tools/playwright-browsers" \
  .venv/bin/playwright install chromium
```

Mermaid CLI is pinned in `package.json`/`package-lock.json`. The browser is used both by Playwright and, through `PUPPETEER_EXECUTABLE_PATH`, Mermaid CLI. The Mermaid exports are SVG; the graph exports are SVG and PDF; browser screenshots are PNG.

## Validation commands and results

### Static and setup checks

| Check | Command or method | Result |
| --- | --- | --- |
| Shell syntax | `bash -n scripts/setup.sh scripts/run.sh` | Successful |
| Python syntax | In-memory `compile()` over project Python files | Successful |
| Setup independent of caller directory | Run `scripts/setup.sh` from a temporary directory | Successful after exporting local `JAVA_HOME`/`PATH` in the script |
| Launch independent of caller directory | Run `scripts/run.sh` from a temporary directory | Successful |
| First SBT dependency resolution | `sbt --batch update` | Successful |
| Mermaid exports | Run local `mmdc` for three `.mmd` files | Three valid SVG files generated |
| Matplotlib export | Run `plot_event_counts.py` | SVG and PDF generated |

### Web startup and browser checks

The application was started with `scripts/run.sh`. The following were execution-verified:

- Uvicorn listened on `127.0.0.1:8080`.
- `/` returned HTTP 200 with the current 69,677-byte HTML page.
- `/static/pipeline.svg` returned HTTP 200.
- the Socket.IO Engine.IO handshake returned HTTP 200 and advertised WebSocket upgrade;
- Playwright loaded the page without JavaScript errors;
- `socket.connected` evaluated to `true`;
- the landing page and upload control were visible;
- external Socket.IO, Monaco, and JSZip resources loaded in the online test environment.

### Processor-level validation

`docs/report/assets/source/validate_sessions.py` submitted controlled source trees through `/upload_and_detect` and `/compile`, joined each Socket.IO room, issued `init` and repeated `step`, and exported every processed snapshot until the first `coreDone`.

| Level | Submitted reference | Detection | Compilation/TCP | Last cycle | Forward / stall / flush cycles |
| --- | --- | --- | --- | ---: | ---: |
| 1 | Task 3 solution | Level 1 | Successful | 12 | 0 / 0 / 0 |
| 2 | Task 4 solution | Level 2 | Successful | 12 | 10 / 0 / 0 |
| 3 | Task 5 branch solution | Level 3 | Successful | 15 | 1 / 0 / 2 |
| 4 | Integrated infrastructure | Level 4 | Successful | 51 | 23 / 4 / 6 |

Browser-level upload, detection, compilation, Socket.IO updates, pipeline rendering, and register rendering were directly verified for Levels 2 and 3. Level 4 was validated through the same HTTP/Socket.IO APIs and its recorded history was rendered in a browser for the stall screenshot. Screenshots record Level 2 forwarding at cycle 3, a Level 3 taken branch at cycle 7, and a Level 4 load-use stall at cycle 41.

### VCD and Surfer

Level 4 generated `PipelinedRV32I.vcd`. `/vcd/sess_validation_l4` returned HTTP 200 and 312,820 bytes. The bundled Surfer WASM page loaded the URL, displayed the processor hierarchy, and rendered selected clock and `io.dbg` traces. This path is execution-verified in `assets/screenshots/waveform_view.png`.

## Failed checks and resolved environment issues

1. The initial `python3 web_demo.py` review failed because the host Python had no web packages. A project virtual environment and pinned requirements resolved it.
2. The first Playwright browser launch failed with missing `libnspr4.so`, followed by NSS/ALSA requirements. Project-local extraction of the four Ubuntu runtime packages resolved the launch without system modification.
3. The first setup-script validation found that local Java was checked directly but not exported for local SBT. `scripts/setup.sh` now exports `JAVA_HOME` and prepends both local tool directories before invoking SBT.
4. Running several live JVM sessions concurrently consumed substantial resources and delayed the first Level 4 compile. Server shutdown correctly terminated all owned process groups. Level 4 then compiled and completed alone in 8.3 seconds with warm dependency/build caches. This is an environment/resource constraint and also evidence that explicit per-session limits would be useful.

## Remaining unverified or unresolved points

- Python versions other than 3.12.3 and JDK versions other than Temurin 17.0.19 were not tested.
- Offline browser operation is not supported by the current CDN-based HTML.
- Multi-user load, malicious uploads, authentication, compiler sandboxing, and long-running session limits were not tested.
- Browser screenshots were automated in headless Chrome; visual differences in other browser engines were not evaluated.
- The Level 4 VCD headless test stops at 100 cycles if a program has not terminated. Longer waveform behavior was not tested.
- Processor ISA conformance beyond the supplied programs and existing Scala tests was not re-certified.

## Generated outputs

- Editable Mermaid: `assets/source/system_architecture.mmd`, `execution_sequence.mmd`, `five_stage_pipeline.mmd`.
- Diagram SVGs: `assets/diagrams/`.
- Screenshot PNGs and metadata: `assets/screenshots/`, `assets/data/screenshot_metadata.json`.
- Raw JSON/CSV validation data: `assets/data/`.
- Plot source: `assets/source/plot_event_counts.py`.
- Graph: `assets/graphs/hazard_event_counts.svg` and `.pdf`.
