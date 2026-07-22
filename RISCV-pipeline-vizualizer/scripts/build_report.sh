#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
LATEX_DIR="$PROJECT_ROOT/docs/report/latex"
OUTPUT_DIR="$PROJECT_ROOT/docs/report"
OUTPUT_FILE="$OUTPUT_DIR/RISCV_PIPELINE_VISUALIZER_REPORT.pdf"
BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/riscv-report-build.XXXXXX")"
trap 'rm -rf -- "$BUILD_DIR"' EXIT

if command -v tectonic >/dev/null 2>&1; then
    TECTONIC_BIN="$(command -v tectonic)"
elif [[ -x "$PROJECT_ROOT/.tools/tectonic/tectonic" ]]; then
    TECTONIC_BIN="$PROJECT_ROOT/.tools/tectonic/tectonic"
fi

if [[ -n "${TECTONIC_BIN:-}" ]]; then
    (
        cd "$LATEX_DIR"
        "$TECTONIC_BIN" --outdir "$BUILD_DIR" main.tex
    )
elif command -v xelatex >/dev/null 2>&1 && command -v bibtex >/dev/null 2>&1; then
    (
        cd "$LATEX_DIR"
        xelatex -interaction=nonstopmode -halt-on-error -output-directory="$BUILD_DIR" main.tex
        (
            cd "$BUILD_DIR"
            BIBINPUTS="$LATEX_DIR:" bibtex main
        )
        xelatex -interaction=nonstopmode -halt-on-error -output-directory="$BUILD_DIR" main.tex
        xelatex -interaction=nonstopmode -halt-on-error -output-directory="$BUILD_DIR" main.tex
    )
else
    echo "Error: Tectonic or XeLaTeX with BibTeX is required to build the report." >&2
    echo "Install Tectonic from https://tectonic-typesetting.github.io/, or install a TeX distribution that provides xelatex and bibtex." >&2
    exit 1
fi

cp -f "$BUILD_DIR/main.pdf" "$OUTPUT_FILE"
echo "Report written to: $OUTPUT_FILE"
