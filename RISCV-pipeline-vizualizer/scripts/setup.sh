#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENV_DIR="$PROJECT_ROOT/.venv"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Error: Python 3 is required but was not found on PATH." >&2
    exit 1
fi

if [ ! -x "$VENV_DIR/bin/python" ]; then
    python3 -m venv "$VENV_DIR"
fi

"$VENV_DIR/bin/python" -m pip install --upgrade pip
"$VENV_DIR/bin/python" -m pip install -r "$PROJECT_ROOT/requirements.txt"

if [ -x "$PROJECT_ROOT/.tools/jdk/bin/java" ]; then
    JAVA_CMD="$PROJECT_ROOT/.tools/jdk/bin/java"
    export JAVA_HOME="$PROJECT_ROOT/.tools/jdk"
    export PATH="$JAVA_HOME/bin:$PATH"
elif command -v java >/dev/null 2>&1; then
    JAVA_CMD="$(command -v java)"
else
    echo "Warning: Java is missing. Install a JDK compatible with SBT 1.9.7 (JDK 17 is validated)." >&2
    JAVA_CMD=""
fi

if [ -x "$PROJECT_ROOT/.tools/sbt/bin/sbt" ]; then
    SBT_CMD="$PROJECT_ROOT/.tools/sbt/bin/sbt"
    export PATH="$PROJECT_ROOT/.tools/sbt/bin:$PATH"
elif command -v sbt >/dev/null 2>&1; then
    SBT_CMD="$(command -v sbt)"
else
    echo "Warning: SBT is missing. Install SBT 1.9.7 before compiling a processor." >&2
    SBT_CMD=""
fi

if [ -n "$JAVA_CMD" ]; then "$JAVA_CMD" -version; fi
if [ -n "$SBT_CMD" ]; then "$SBT_CMD" --script-version; fi

echo
echo "Python environment ready. Start the application with:"
echo "  scripts/run.sh"
