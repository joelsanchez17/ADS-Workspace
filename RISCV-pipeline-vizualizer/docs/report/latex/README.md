# Building the report

The report uses A4 LaTeX sources, PDF exports of the editable Mermaid diagrams, and PNG browser screenshots. Run the build from any directory:

```bash
./scripts/build_report.sh
```

The script locates the repository dynamically and writes:

```text
docs/report/RISCV_PIPELINE_VISUALIZER_REPORT.pdf
```

It uses `tectonic` from `PATH` when available, or the project-local `.tools/tectonic/tectonic` binary used during final validation. Tectonic downloads its TeX bundle on the first build, so that build needs internet access. The validated version was Tectonic 0.16.9. The generated diagram PDFs are kept in `docs/report/assets/diagrams/`; their editable Mermaid sources are in `docs/report/assets/source/`.

After editing a Mermaid source, regenerate both publication formats with Mermaid CLI, for example:

```bash
PUPPETEER_EXECUTABLE_PATH=/path/to/chrome \
  node_modules/.bin/mmdc \
  -i docs/report/assets/source/system_architecture.mmd \
  -o docs/report/assets/diagrams/system_architecture.pdf \
  -b white
```

Replace the visible author, supervisor, chair/department, and submission-date placeholders in `main.tex` before formal submission. The PDF metadata uses the same author placeholder until it is supplied.
