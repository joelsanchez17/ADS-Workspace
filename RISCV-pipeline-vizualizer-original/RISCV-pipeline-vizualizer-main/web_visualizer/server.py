import socketio
import copy
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from .bridge import ChiselBridge
from live_debug.decoder import decode_rv32i
import os
import shutil
import uuid
import subprocess
import socket
import asyncio
import re
import signal
from pydantic import BaseModel
from typing import Dict, Optional


active_bridges = {}  # Stores bridges mapped by Session ID

debug_state = { "cursor": -1, "history": [] }

sio = socketio.AsyncServer(async_mode='asgi', cors_allowed_origins='*')
app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory="web_visualizer/static"), name="static")
app.mount("/branding", StaticFiles(directory="web_visualizer/templates/branding"), name="branding")

@app.get("/vcd/{session_id}")
async def get_vcd(session_id: str):
    """Serve the generated VCD file to the Surfer Waveform Viewer."""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(base_dir, ".."))
    if not is_safe_session_id(session_id):
        return {"error": "Invalid session id."}

    test_run_dir = os.path.join(project_root, "temp_sessions", session_id, "test_run_dir")
    vcd_path = None
    if os.path.isdir(test_run_dir):
        matches = []
        for root, _, files in os.walk(test_run_dir):
            if "PipelinedRV32I.vcd" in files:
                matches.append(os.path.join(root, "PipelinedRV32I.vcd"))
        if matches:
            matches.sort(key=lambda p: os.path.getmtime(p), reverse=True)
            vcd_path = matches[0]

    if vcd_path and os.path.exists(vcd_path):
        return FileResponse(vcd_path)
    return {"error": f"VCD file not found under {test_run_dir}. Compile and simulate first, then open Surfer."}

active_sessions = {}  # Maps session_id -> {"bridge": obj, "history": [], "cursor": 0}

LEVEL_MARKER_FILES = {
    "ForwardingUnit.scala",
    "HazardDetection.scala",
    "MemController.scala",
    "Branch.scala",
}

TASK5_BRANCH_MARKERS = {
    "isBEQ",
    "isBNE",
    "isBLT",
    "isBGE",
    "isBLTU",
    "isBGEU",
    "isJAL",
    "isJALR",
    "redirectPC",
    "redirect",
    "immB",
    "immJ",
    "branch target calculation",
    "branchTarget",
    "mispredicted branches",
}

IGNORED_UPLOAD_PREFIXES = ("target/", "project/", "generated-src/")
SESSION_ID_RE = re.compile(r"^sess_[A-Za-z0-9_-]{1,64}$")

def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]

async def connect_bridge_async(bridge: ChiselBridge, timeout: int = 90):
    """Connect to Chisel without depending on SBT stdout flushing."""
    print(f"🔌 Connecting to Chisel at {bridge.host}:{bridge.port}...")
    loop = asyncio.get_running_loop()
    deadline = loop.time() + timeout

    while loop.time() < deadline:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setblocking(False)
        try:
            await asyncio.wait_for(loop.sock_connect(sock, (bridge.host, bridge.port)), timeout=1)
            sock.setblocking(True)
            sock.settimeout(10)
            bridge.sock = sock
            bridge.f = sock.makefile("r", encoding="utf-8")
            print("✅ Connected to Hardware!")
            return
        except (ConnectionRefusedError, OSError, asyncio.TimeoutError):
            sock.close()
            await asyncio.sleep(1)

    raise TimeoutError(f"Timed out connecting to Chisel at {bridge.host}:{bridge.port}")

async def terminate_owned_process(process, label: str, timeout: int = 5):
    """Terminate only the SBT process/process group started by this backend."""
    if not process or process.returncode is not None:
        return

    pid = process.pid
    try:
        pgid = os.getpgid(pid)
    except ProcessLookupError:
        return
    except Exception as e:
        print(f"⚠️  [CLEANUP] Could not inspect process group for {label} pid={pid}: {e}")
        pgid = None

    print(f"🧹 [CLEANUP] Terminating {label} pid={pid}")
    try:
        if pgid == pid:
            print(f"🧹 [CLEANUP] Sending SIGTERM to process group {pgid}")
            os.killpg(pgid, signal.SIGTERM)
        else:
            process.terminate()

        try:
            await asyncio.wait_for(process.wait(), timeout=timeout)
            print(f"✅ [CLEANUP] {label} stopped with return code {process.returncode}")
            return
        except asyncio.TimeoutError:
            print(f"⚠️  [CLEANUP] {label} did not exit after SIGTERM; sending SIGKILL")

        if pgid == pid:
            os.killpg(pgid, signal.SIGKILL)
        else:
            process.kill()
        await process.wait()
        print(f"✅ [CLEANUP] {label} killed")
    except ProcessLookupError:
        print(f"✅ [CLEANUP] {label} was already gone")
    except Exception as e:
        print(f"⚠️  [CLEANUP] Failed to terminate {label}: {e}")

async def cleanup_session(session_id: str):
    """Close bridge resources and stop the SBT process owned by one session."""
    sess = active_sessions.pop(session_id, None)
    if not sess:
        print(f"🧹 [CLEANUP] Session {session_id} is not active; nothing to clean")
        return

    print(f"🧹 [CLEANUP] Cleaning session {session_id}")
    bridge = sess.get("bridge")
    if bridge:
        try:
            bridge.close()
            print(f"✅ [CLEANUP] Closed bridge for {session_id}")
        except Exception as e:
            print(f"⚠️  [CLEANUP] Failed to close bridge for {session_id}: {e}")

    await terminate_owned_process(sess.get("process"), f"session {session_id}")
    print(f"✅ [CLEANUP] Session {session_id} cleanup complete")

async def cleanup_all_sessions():
    """Clean every active backend-owned simulation session."""
    session_ids = list(active_sessions.keys())
    print(f"🛑 [SHUTDOWN] Cleaning {len(session_ids)} active simulation session(s)")
    for session_id in session_ids:
        await cleanup_session(session_id)
    active_sessions.clear()
    print("✅ [SHUTDOWN] Active simulation sessions cleared")

@app.on_event("shutdown")
async def shutdown_event():
    print("🛑 [SHUTDOWN] FastAPI shutdown requested")
    await cleanup_all_sessions()

socket_app = socketio.ASGIApp(sio, app, on_shutdown=cleanup_all_sessions)

def is_safe_session_id(session_id: str) -> bool:
    return bool(SESSION_ID_RE.fullmatch(session_id or ""))

def normalized_upload_path(filepath: str) -> str:
    clean_path = filepath.replace("\\", "/")
    src_idx = clean_path.find("src/")
    if src_idx != -1:
        clean_path = clean_path[src_idx:]
    return clean_path.lstrip("/")

def has_unsafe_path_segments(filepath: str) -> bool:
    clean_path = filepath.replace("\\", "/")
    if clean_path.startswith("/") or clean_path.startswith("~") or re.match(r"^[A-Za-z]:/", clean_path):
        return True
    return any(part in ("", ".", "..") for part in clean_path.split("/"))

def is_student_scala_source(filepath: str) -> bool:
    clean_path = normalized_upload_path(filepath)
    path_parts = clean_path.split("/")
    return (
        clean_path.endswith(".scala")
        and not has_unsafe_path_segments(filepath)
        and not has_unsafe_path_segments(clean_path)
        and not clean_path.startswith(IGNORED_UPLOAD_PREFIXES)
        and "test" not in path_parts
        and (clean_path.startswith("src/main/scala/") or len(path_parts) == 1)
    )

def upload_ignore_reason(filepath: str) -> Optional[str]:
    if has_unsafe_path_segments(filepath):
        return "unsafe path"
    clean_path = normalized_upload_path(filepath)
    path_parts = clean_path.split("/")
    if has_unsafe_path_segments(clean_path):
        return "unsafe path"
    if clean_path.startswith(IGNORED_UPLOAD_PREFIXES):
        return "build/generated directory"
    if os.path.basename(clean_path) == "build.sbt":
        return "build.sbt is platform-controlled"
    if "test" in path_parts:
        return "student test files are ignored"
    if clean_path.endswith(".scala") and not clean_path.startswith("src/main/scala/") and len(path_parts) != 1:
        return "Scala file is outside src/main/scala"
    if not clean_path.endswith(".scala") and "BinaryFile" not in os.path.basename(clean_path):
        return "not a Scala source or BinaryFile"
    return None

def duplicate_basenames(scala_files: Dict[str, str]) -> Dict[str, list]:
    by_name = {}
    for path in scala_files:
        by_name.setdefault(os.path.basename(path), []).append(path)
    return {name: paths for name, paths in by_name.items() if len(paths) > 1}

def classify_compile_failure(log_output: str) -> str:
    lowered = log_output.lower()
    if "pipelinedrv32i" in lowered and ("not found" in lowered or "object pipelinedrv32i" in lowered):
        return "Missing PipelinedRISCV32I.scala or class PipelinedRV32I in package PipelinedRV32I."
    if "coreDone" in log_output or "gpRegVal" in log_output:
        return "Top-level IO is missing coreDone/gpRegVal required by LivePipelineTest."
    if ".dbg" in log_output or "value dbg" in lowered:
        return "Top-level IO is missing io.dbg.* required by the web visualizer."
    if "not found: object assignment02" in lowered or "not found: object uopc" in lowered:
        return "Task 3/4/5 TODO files are incomplete. Finish common.scala/ALU.scala and dependent modules."
    return "See SBT logs above for the first Scala/Chisel error."

def task5_branch_markers(scala_files: Dict[str, str]) -> set:
    haystack = "\n".join(scala_files.values())
    return {marker for marker in TASK5_BRANCH_MARKERS if marker in haystack}

def detect_level_from_scala_files(scala_files: Dict[str, str]) -> int:
    basenames = {os.path.basename(path) for path in scala_files.keys()}
    if "Branch.scala" in basenames:
        return 4
    if task5_branch_markers(scala_files):
        return 3
    if "HazardDetection.scala" in basenames or "MemController.scala" in basenames:
        return 4
    if "ForwardingUnit.scala" in basenames:
        return 2
    return 1

def level_detection_reason(scala_files: Dict[str, str], level: int) -> str:
    basenames = {os.path.basename(path) for path in scala_files.keys()}
    if level == 1:
        return "Detected Task 3 / Level 1 because no ForwardingUnit/HazardDetection/MemController/Branch file or Task 5 branch markers were found."
    if level == 2:
        return "Detected Task 4 / Level 2 because ForwardingUnit.scala was found and no Task 5 branch markers were present."
    if level == 3:
        found = sorted(task5_branch_markers(scala_files))
        preview = ", ".join(found[:6])
        if len(found) > 6:
            preview += ", ..."
        return f"Detected Task 5 / Level 3 because branch/jump markers were found: {preview}."
    if level == 4:
        found = sorted(basenames.intersection({"Branch.scala", "HazardDetection.scala", "MemController.scala"}))
        return f"Detected Level 4 because {', '.join(found)} was found."
    return f"Detected Level {level}."

@app.get("/")
async def read_index():
    return FileResponse('web_visualizer/templates/index.html')

# New from here
class CompileRequest(BaseModel):
    scala_files: Dict[str, str]
    asm_code: str
    session_id: Optional[str] = None
    level: int = 1



@app.get("/workspace")
async def get_workspace(level: int = 1):  # 🚨 NEW: Accept level from frontend
    base_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(base_dir, ".."))

    # 🚨 NEW: Point to the specific level folder!
    level_folder = f"level_{level}"
    template_dir = os.path.join(project_root, "infrastructure_template", level_folder)

    workspace_data = {"scala": {}, "asm": {}}

    editable_scala_files = [
        "Branch.scala", "common.scala", "ControlUnit.scala",
        "ForwardingUnit.scala", "ImmediateGen.scala",
        "PipelineRegisters.scala", "PipelineStages.scala", "RegFile.scala",
        "ALU.scala", "HazardDetection.scala"
    ]

    scala_src_dir = os.path.join(template_dir, "src", "main", "scala")
    # Read the allowed Scala files (os.path.exists will safely skip files you deleted for this level!)
    for f_name in editable_scala_files:
        f_path = os.path.join(scala_src_dir, f_name)
        if os.path.exists(f_path):
            with open(f_path, "r") as f:
                workspace_data["scala"][f_name] = f.read()

    # Read the Assembly/Binary file
    bin_path = os.path.join(template_dir, "src", "test", "programs", "BinaryFile")
    if os.path.exists(bin_path):
        with open(bin_path, "r") as f:
            workspace_data["asm"]["BinaryFile"] = f.read()
    else:
        workspace_data["asm"]["BinaryFile"] = "// No initial assembly found"

    return workspace_data


class UploadRequest(BaseModel):
    files: Dict[str, str] # Maps filename -> file content

@app.post("/upload_and_detect")
async def upload_and_detect(req: UploadRequest):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(base_dir, ".."))

    scala_files = {}
    student_asm = "// No custom assembly uploaded"
    ignored_files = []

    for filepath, content in req.files.items():
        reason = upload_ignore_reason(filepath)
        clean_path = normalized_upload_path(filepath)
        if clean_path.endswith(".scala"):
            if reason or not is_student_scala_source(clean_path):
                reason = reason or "not an accepted hardware source"
                ignored_files.append((clean_path, reason))
                print(f"🛡️ Upload Shield: Dropping {clean_path} ({reason})")
                continue
            scala_files[clean_path] = content  # Keep normalized filepath for the UI.

        elif "BinaryFile" in os.path.basename(clean_path) and not reason:
            student_asm = content
        else:
            reason = reason or "not an accepted upload file"
            ignored_files.append((clean_path, reason))
            print(f"🛡️ Upload Shield: Dropping {clean_path} ({reason})")

    if not scala_files:
        return {
            "status": "error",
            "message": "No accepted Scala files found. Upload the project src folder containing src/main/scala/*.scala, not only src/test or build output.",
            "ignored": ignored_files,
        }

    duplicates = duplicate_basenames(scala_files)
    if duplicates:
        detail = "; ".join(f"{name}: {', '.join(paths)}" for name, paths in sorted(duplicates.items()))
        return {
            "status": "error",
            "message": f"Duplicate Scala basenames would overwrite each other in the Chisel session: {detail}",
            "ignored": ignored_files,
        }

    # SMART AUTO-DETECTOR (Evaluate all accepted source files at once)
    level = detect_level_from_scala_files(scala_files)
    detection_reason = level_detection_reason(scala_files, level)
    basenames = {os.path.basename(path) for path in scala_files}
    if level in (1, 2, 3) and "PipelinedRISCV32I.scala" not in basenames:
        return {
            "status": "error",
            "message": f"Level {level} upload is missing PipelinedRISCV32I.scala. Upload the complete src folder.",
            "ignored": ignored_files,
        }

    # 🚨 NEW: Fetch from the new system_tests folder
    system_asm_path = os.path.join(project_root, "infrastructure_template", "system_tests", f"level_{level}", "BinaryFile")

    system_asm = "// System assembly not found"
    if os.path.exists(system_asm_path):
        with open(system_asm_path, "r") as f:
            system_asm = f.read()

    print("📦 Accepted Scala upload files:")
    for name in sorted(scala_files.keys()):
        print(f"   - {name}")
    if ignored_files:
        print("🧹 Ignored upload files:")
        for name, reason in ignored_files:
            print(f"   - {name}: {reason}")
    print(f"🎯 {detection_reason}")
    print(f"🧪 Selected system BinaryFile: {system_asm_path}")

    return {
        "status": "success",
        "level": level,
        "scala": scala_files,
        "asm_system": system_asm,
        "asm_student": student_asm,
        "message": detection_reason,
        "ignored": ignored_files,
    }

@app.post("/compile")
async def compile_code(req: CompileRequest):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(base_dir, ".."))

    if req.session_id and not is_safe_session_id(req.session_id):
        return {"status": "error", "message": "Invalid session id.", "logs": ""}

    invalid_paths = [path for path in req.scala_files if not is_student_scala_source(path)]
    if invalid_paths:
        detail = ", ".join(sorted(invalid_paths))
        return {"status": "error", "message": f"Unsafe or unsupported Scala source paths submitted: {detail}", "logs": ""}

    duplicates = duplicate_basenames(req.scala_files)
    if duplicates:
        detail = "; ".join(f"{name}: {', '.join(paths)}" for name, paths in sorted(duplicates.items()))
        return {"status": "error", "message": f"Duplicate Scala basenames would overwrite each other: {detail}", "logs": ""}

    if not req.scala_files:
        return {"status": "error", "message": "No Scala files submitted to compile. Upload src/main/scala first.", "logs": ""}

    detected_level = detect_level_from_scala_files(req.scala_files)
    effective_level = detected_level
    is_course_level = effective_level in (1, 2, 3)

    if effective_level == 1:
        template_dir = os.path.join(project_root, "course_material", "task3_level1", "solution")
    elif effective_level == 2:
        template_dir = os.path.join(project_root, "course_material", "task4_level2", "solution")
    elif effective_level == 3:
        template_dir = os.path.join(project_root, "course_material", "task5_level3_branch", "solution")
    else:
        template_dir = os.path.join(project_root, "infrastructure_template")

    detection_reason = level_detection_reason(req.scala_files, detected_level)

    is_new_session = True
    session_id = req.session_id if req.session_id else f"sess_{uuid.uuid4().hex[:8]}"

    # ---  ROBUSTNESS PATCH 1: AGGRESSIVE PROCESS CLEANUP ---
    if session_id in active_sessions:
        is_new_session = False
        print(f"♻️  Reusing existing session: {session_id}")
        await cleanup_session(session_id)
    else:
        print(f"🆕 Creating new session: {session_id}")

    session_dir = os.path.join(project_root, "temp_sessions", session_id)
    system_binary_path = os.path.join(project_root, "infrastructure_template", "system_tests", f"level_{effective_level}", "BinaryFile")

    print(f"🎯 {detection_reason}")
    print(f"📁 Session directory: {session_dir}")
    print(f"📚 Template directory: {template_dir}")
    print(f"🧪 Selected system BinaryFile path: {system_binary_path}")
    print("📦 Compile request Scala files:")
    for name in sorted(req.scala_files.keys()):
        print(f"   - {name}")

    try:
        # 1. Copy the Master Template (includes build.sbt and LivePipelineTest!)
        if is_course_level and os.path.exists(session_dir):
            shutil.rmtree(session_dir)

        if not os.path.exists(session_dir):
            shutil.copytree(template_dir, session_dir)
            if is_course_level:
                hardware_dir = os.path.join(session_dir, "src", "main", "scala")
                if os.path.exists(hardware_dir):
                    shutil.rmtree(hardware_dir)
        else:
            # 🚨 THE FIX: WIPE THE OLD HARDWARE DIRECTORY!
            # If the student reuses a session, we must delete the old hardware files
            # so they don't become "ghost files" and conflict with the new upload.
            old_hardware_dir = os.path.join(session_dir, "src", "main", "scala")
            if os.path.exists(old_hardware_dir):
                shutil.rmtree(old_hardware_dir)

        if is_course_level:
            test_scala_dir = os.path.join(session_dir, "src", "test", "scala")
            if os.path.exists(test_scala_dir):
                shutil.rmtree(test_scala_dir)
            os.makedirs(test_scala_dir, exist_ok=True)
            shutil.copy2(
                os.path.join(project_root, "infrastructure_template", "src", "test", "scala", "LivePipelineTest.scala"),
                os.path.join(test_scala_dir, "LivePipelineTest.scala")
            )

        # 2. 🚨 Write the Student's files and enforce the exact Chisel src structure!
        written_files = []
        for filepath, content in req.scala_files.items():
            if not is_student_scala_source(filepath):
                print(f"🛡️ Compile Shield: Dropping non-hardware Scala file -> {filepath}")
                continue
            filename = os.path.basename(filepath)
            safe_filepath = os.path.join("src", "main", "scala", filename)

            full_path = os.path.join(session_dir, safe_filepath)
            os.makedirs(os.path.dirname(full_path), exist_ok=True)
            with open(full_path, "w+") as f:
                f.write(content)
            written_files.append(safe_filepath)

        print("📝 Final Scala files written into session:")
        for name in sorted(written_files):
            print(f"   - {name}")

        # 3. Write the selected BinaryFile exactly where Chisel expects it
        bin_test_dir = os.path.join(session_dir, "src", "test", "programs")
        os.makedirs(bin_test_dir, exist_ok=True)
        with open(os.path.join(bin_test_dir, "BinaryFile"), "w+") as f:
            f.write(req.asm_code)



        student_port = find_free_port()
        env = os.environ.copy()
        env["CHISEL_PORT"] = str(student_port)

        await sio.emit('build_log', {'line': f"[info] {detection_reason}"}, room=session_id)
        await sio.emit('build_log', {'line': f"[info] Session directory: {session_dir}"}, room=session_id)
        await sio.emit('build_log', {'line': f"[info] System BinaryFile: {system_binary_path}"}, room=session_id)
        await sio.emit('build_log', {'line': f"[info] Scala files written: {', '.join(sorted(os.path.basename(f) for f in written_files))}"}, room=session_id)


        # 1. RUN SBT ASYNCHRONOUSLY
        process = await asyncio.create_subprocess_exec(
            "sbt", "--batch", "testOnly *LivePipelineTest",
            cwd=session_dir, env=env, stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
            start_new_session=True,
        )

        log_output = ""
        compilation_failed = False
        chisel_ready = asyncio.Event()

        # 2. BACKGROUND TASK TO STREAM LOGS LIVE
        async def read_logs():
            nonlocal compilation_failed, log_output
            while True:
                line_bytes = await process.stdout.readline()
                if not line_bytes: break

                line = line_bytes.decode('utf-8', errors='replace')
                log_output += line

                # Print to local terminal AND send to web socket instantly!
                print(line, end="", flush=True)
                # Stream directly to the specific user's room!
                asyncio.create_task(sio.emit(
                    'build_log',
                    {'line': line.replace(session_dir, "[WORKSPACE]")},
                    room=session_id  # 🚨 NEW: Target the specific room
                ))

                if "Failed tests:" in line or "Compilation failed" in line:
                    compilation_failed = True
                    chisel_ready.set()
                if "Waiting for Python on port" in line:
                    chisel_ready.set()

            if not chisel_ready.is_set():
                compilation_failed = True
                chisel_ready.set()

        asyncio.create_task(read_logs())
        bridge = ChiselBridge(port=student_port)
        bridge_connect_task = asyncio.create_task(connect_bridge_async(bridge))
        ready_task = asyncio.create_task(chisel_ready.wait())
        done, _ = await asyncio.wait(
            {bridge_connect_task, ready_task},
            return_when=asyncio.FIRST_COMPLETED,
        )

        if ready_task in done and compilation_failed:
            bridge_connect_task.cancel()
            await terminate_owned_process(process, f"failed compile session {session_id}")
            reason = classify_compile_failure(log_output)
            await sio.emit(
                'build_log',
                {'line': f"[error] SBT exited before the Chisel TCP bridge opened. {reason}"},
                room=session_id
            )
            return {"status": "error", "message": f"Chisel Compilation Failed before live bridge startup. {reason}", "logs": log_output.replace(session_dir, "[WORKSPACE]")}

        if bridge_connect_task not in done:
            try:
                await bridge_connect_task
            except Exception as e:
                await terminate_owned_process(process, f"failed bridge session {session_id}")
                return {"status": "error", "message": f"Chisel bridge did not open: {str(e)}", "logs": log_output.replace(session_dir, "[WORKSPACE]")}
        else:
            try:
                await bridge_connect_task
            except Exception as e:
                await terminate_owned_process(process, f"failed bridge session {session_id}")
                return {"status": "error", "message": f"Chisel bridge did not open: {str(e)}", "logs": log_output.replace(session_dir, "[WORKSPACE]")}

        # 3. CONNECT TO HARDWARE & FORCE CYCLE 0
        # 🚨 THE FIX: Consume the unsolicited Cycle 0 JSON that Chisel sends on boot!
        # This keeps the TCP buffer perfectly aligned with the user's clicks.
        bridge.receive_snapshot()

        bridge.reset() # Forces a fresh Cycle 0 Generation

        initial_raw = bridge.get_latest()
        initial_proc = process_snapshot(initial_raw) if initial_raw else None

        active_sessions[session_id] = {
            "process": process,
            "bridge": bridge,
            "history": [initial_proc] if initial_proc else [],
            "cursor": 0
        }

        return {
            "status": "success",
            "message": "Simulation Started!",
            "session_id": session_id,
            "logs": log_output.replace(session_dir, "[WORKSPACE]") # Send full log backup
        }

    except Exception as e:
        try:
            if 'process' in locals() and process.returncode is None:
                await terminate_owned_process(process, f"errored session {session_id}")
        except Exception:
            pass
        return {"status": "error", "message": f"Server Error: {str(e)}", "logs": ""}


# --- HELPER FUNCTIONS ---
def extract_registers(instr_int):
    if not instr_int: return {"rs1":0, "rs2":0, "rd":0}
    return {"rs1": (instr_int >> 15) & 0x1F, "rs2": (instr_int >> 20) & 0x1F, "rd": (instr_int >> 7) & 0x1F}

def safe_int(val):
    if val is None: return 0
    if isinstance(val, int): return val
    if isinstance(val, str):
        val = val.strip()
        if val.startswith("0x"): return int(val, 16)
        if val.startswith("b"): return int(val, 2)
        try: return int(val)
        except: return 0
    return 0

def process_snapshot(raw_data):
    data = copy.deepcopy(raw_data)
    data['asm'] = {}
    data['pc_hex'] = {}
    for stage in ['if', 'id', 'ex', 'mem', 'wb']:
        raw_instr = data.get('instr', {}).get(stage, 0)
        data['asm'][stage] = decode_rv32i(safe_int(raw_instr))
        data['pc_hex'][stage] = f"0x{safe_int(data.get('pc', {}).get(stage, 0)):08x}"

    reg_map = data.get("regs", {})
    regs_list = [0] * 32
    if isinstance(reg_map, dict):
        for k, v in reg_map.items():
            idx = int(k.replace("x", ""))
            if 0 <= idx < 32: regs_list[idx] = safe_int(v)

    if 'ex' not in data: data['ex'] = {}
    data['ex']['val_a'] = safe_int(data['ex'].get('alu_op_a', 0))
    data['ex']['val_b'] = safe_int(data['ex'].get('alu_op_b', 0))
    data['id_info'] = extract_registers(data.get('instr', {}).get('id', 0))
    return {"raw": raw_data, "enriched": data, "registers": regs_list}

def add_to_history(session_id, raw_snap):
    if not raw_snap: return
    processed = process_snapshot(raw_snap)
    sess = active_sessions[session_id]
    sess["history"].append(processed)
    sess["cursor"] = len(sess["history"]) - 1
    return processed
# --- MULTI-USER SOCKET.IO EVENTS (WITH AGGRESSIVE DEBUGGING) ---

@sio.event
async def connect(sid, environ):
    print(f"🟢 [SOCKET.IO] New browser connected with ID: {sid}")

@sio.event
async def join_session(sid, session_id):
    # sio.enter_room(sid, session_id)
    await sio.enter_room(sid, session_id)
    print(f"🚪 [ROOM] Browser {sid} explicitly joined private room: {session_id}")

@sio.event
async def command(sid, data):
    print(f"\n⚡ [COMMAND] Received from browser: {data}")

    session_id = data.get('session_id')
    if not session_id or session_id not in active_sessions:
        print(f"❌ [COMMAND] ERROR: Session {session_id} not found in active_sessions!")
        return

  
    await sio.enter_room(sid, session_id)

    sess = active_sessions[session_id]
    bridge = sess["bridge"]
    action = data.get('action')
    val = int(data.get('value', 1))
    response = None

    print(f"🔍 [COMMAND] Executing '{action}'. Current history length: {len(sess['history'])}")

    if action == 'init':
        if sess["history"]:
            response = sess["history"][0]
            print(f"✅ [COMMAND] Init successful. Grabbed Cycle 0 data.")
        else:
            print(f"❌ [COMMAND] ERROR: History is empty! Cycle 0 was never generated.")

    elif action == 'step':
        target = sess["cursor"] + 1
        if target < len(sess["history"]):
            sess["cursor"] = target
            response = sess["history"][sess["cursor"]]
            print(f"⏪ [COMMAND] Stepped using history to cycle {sess['cursor']}")
        else:
            print(f"⏩ [COMMAND] Advancing hardware 1 cycle...")
            response = add_to_history(session_id, bridge.step(1))

    elif action == 'run':
        for _ in range(val):
            if sess["cursor"] < len(sess["history"]) - 1:
                sess["cursor"] += 1
                response = sess["history"][sess["cursor"]]
            else:
                response = add_to_history(session_id, bridge.step(1))

    elif action == 'back':
        sess["cursor"] = max(0, sess["cursor"] - val)
        response = sess["history"][sess["cursor"]]
        print(f"⏪ [COMMAND] Went back to cycle {sess['cursor']}")

    elif action == 'reset':
        print(f"🔄 [COMMAND] Resetting hardware...")
        bridge.reset()
        sess["history"] = []
        response = add_to_history(session_id, bridge.step(0))

    if response:
        data = response.get('enriched', {})
        cycle = data.get('cycle', 0)

        # 1. FETCH STAGE: What is entering the pipeline?
        if_asm = data.get('asm', {}).get('if', 'nop')

        # 2. EXECUTE STAGE: What is the ALU doing right now?
        ex_asm = data.get('asm', {}).get('ex', 'nop')
        alu_res = data.get('ex', {}).get('alu_result', 0)

        # 3. MEMORY STAGE: Are we doing a STORE instruction?
        mem_we = data.get('mem', {}).get('we', 0)
        mem_addr = data.get('mem', {}).get('addr', 0)
        mem_wdata = data.get('mem', {}).get('wdata', 0)

        # 4. WRITEBACK STAGE: Are we updating a register?
        wb_we = data.get('wb', {}).get('we', 0)
        wb_rd = data.get('wb', {}).get('rd', 0)
        wb_data = data.get('wb', {}).get('wdata', 0)

        # 5. HAZARDS: Are we stalled?
        is_stalled = data.get('hazard', {}).get('id_stall', 0)

        # --- BUILD THE SMART EDUCATIONAL STRING ---
        # :<15 pads the string with spaces so the columns perfectly align!
        summary_msg = f"▶ Cycle {cycle:<2} | IF: {if_asm:<15} | EX: {ex_asm:<15} (ALU: 0x{alu_res:X})"

        if mem_we == 1:
            summary_msg += f" | MEM: [0x{mem_addr:X}] \u2190 0x{mem_wdata:X}"

        if wb_we == 1 and wb_rd != 0:
            summary_msg += f" | WB: x{wb_rd} \u2190 0x{wb_data:X}"

        if is_stalled == 1:
            summary_msg += " | ⚠️ STALL"

        await sio.emit('build_log', {'line': summary_msg}, room=session_id)
        await sio.emit('update', response, room=session_id)
    else:
        print(f"⚠️ [EMIT] Response was None. Nothing sent to browser.")
