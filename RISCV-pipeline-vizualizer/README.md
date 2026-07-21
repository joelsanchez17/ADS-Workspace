# RISC-V Pipeline Visualizer

This repository contains a browser-based debugging environment for five-stage RISC-V processors implemented in Chisel. The Python service manages uploads and simulation sessions; SBT/chiseltest executes the uploaded processor; the browser displays source, pipeline state, registers, logs, and VCD waveforms.

## Quick setup

Python 3.12 and JDK 17 are validated. Another current Python 3 or compatible JDK may work but is outside the recorded validation. SBT 1.9.7 and internet access for the first Scala dependency download and the frontend CDN assets are also required.

```bash
git clone <repository-url>
cd RISCV-pipeline-vizualizer

./scripts/setup.sh
./scripts/run.sh
```

Open <http://127.0.0.1:8080>. The default bind address is local-only. Use `./scripts/run.sh --host 0.0.0.0 --port 8080` only when remote access is intentionally required.

`scripts/setup.sh` creates or reuses `.venv`, installs the pinned runtime requirements, and verifies Java/SBT; `scripts/run.sh` launches the server. The scripts also recognize optional project-local installations at `.tools/jdk` and `.tools/sbt`.

Upload a course project `src` folder containing `src/main/scala/*.scala`. An optional `BinaryFile` is raw hexadecimal machine code, one 32-bit instruction word per line; the web application does not include an assembler.

Detailed architecture, validation results, and report assets are under `docs/report/`.
