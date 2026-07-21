#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PYTHON="$PROJECT_ROOT/.venv/bin/python"

if [ ! -x "$PYTHON" ]; then
    echo "Error: .venv is missing. Run scripts/setup.sh first." >&2
    exit 1
fi

if [ -x "$PROJECT_ROOT/.tools/jdk/bin/java" ]; then
    export JAVA_HOME="$PROJECT_ROOT/.tools/jdk"
    export PATH="$JAVA_HOME/bin:$PATH"
fi
if [ -x "$PROJECT_ROOT/.tools/sbt/bin/sbt" ]; then
    export PATH="$PROJECT_ROOT/.tools/sbt/bin:$PATH"
fi

cd "$PROJECT_ROOT"
exec "$PYTHON" web_demo.py "$@"
