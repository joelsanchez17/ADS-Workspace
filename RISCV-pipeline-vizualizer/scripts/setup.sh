#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENV_DIR="$PROJECT_ROOT/.venv"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Error: Python 3 is required but was not found on PATH." >&2
    echo "Install Python 3 from https://www.python.org/downloads/ and run this script again." >&2
    exit 1
fi

if [ ! -x "$VENV_DIR/bin/python" ]; then
    if ! python3 -m venv "$VENV_DIR"; then
        echo "Error: Python virtual-environment support is unavailable." >&2
        echo "On Ubuntu/Debian, install it with:" >&2
        echo "  sudo apt update && sudo apt install python3-venv" >&2
        echo "Then run ./scripts/setup.sh again." >&2
        exit 1
    fi
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
    echo "Error: Java is missing. JDK 17 is the validated version." >&2
    echo "On Ubuntu/Debian, install it with:" >&2
    echo "  sudo apt update && sudo apt install openjdk-17-jdk" >&2
    echo "Other platforms: https://adoptium.net/temurin/releases/?version=17" >&2
    JAVA_CMD=""
fi

if [ -x "$PROJECT_ROOT/.tools/sbt/bin/sbt" ]; then
    SBT_CMD="$PROJECT_ROOT/.tools/sbt/bin/sbt"
    export PATH="$PROJECT_ROOT/.tools/sbt/bin:$PATH"
elif command -v sbt >/dev/null 2>&1; then
    SBT_CMD="$(command -v sbt)"
else
    echo "Error: SBT is missing. This project requires SBT 1.9.7." >&2
    echo "Follow the official installation instructions:" >&2
    echo "  https://www.scala-sbt.org/download/" >&2
    SBT_CMD=""
fi

if [ -z "$JAVA_CMD" ] || [ -z "$SBT_CMD" ]; then
    echo >&2
    echo "Setup incomplete: the Python environment is ready, but Chisel simulation prerequisites are missing." >&2
    exit 1
fi

"$JAVA_CMD" -version
"$SBT_CMD" --script-version

echo
echo "Complete simulation environment ready."
echo "The first SBT dependency download requires internet access and can take several minutes."
echo "To resolve it before the first student session, optionally run:"
echo "  sbt --batch update"
echo
echo "Start the application with:"
echo "  ./scripts/run.sh"
