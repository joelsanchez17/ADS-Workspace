import uvicorn
import argparse

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Start the RISC-V pipeline visualizer")
    parser.add_argument("--host", default="127.0.0.1", help="address to bind (default: 127.0.0.1)")
    parser.add_argument("--port", default=8080, type=int, help="TCP port (default: 8080)")
    args = parser.parse_args()

    browser_host = "localhost" if args.host in {"0.0.0.0", "127.0.0.1"} else args.host
    print(f"🚀 Starting Web Visualizer at http://{browser_host}:{args.port}")
    uvicorn.run("web_visualizer.server:socket_app", host=args.host, port=args.port, reload=False)
