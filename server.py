from flask import Flask, request, Response, send_from_directory
import os, tempfile, subprocess, shutil, re

app = Flask(__name__)

# Read environment variables
TESTER_FILE = os.environ.get("TESTER_FILE", "EmotionAnalyzerTester.java")
RUN_TIMEOUT = int(os.environ.get("RUN_TIMEOUT", "30"))

@app.after_request
def add_cors_headers(resp):
    resp.headers["Access-Control-Allow-Origin"] = "*"
    resp.headers["Access-Control-Allow-Headers"] = "Content-Type"
    resp.headers["Access-Control-Allow-Methods"] = "POST, OPTIONS, GET"
    return resp

@app.route("/run", methods=["POST", "OPTIONS"])
def run_tests():
    if request.method == "OPTIONS":
        return Response(status=204)

    data = request.get_json(silent=True) or {}
    code = data.get("code", "")

    # must have correct class name
    if "class EmotionAnalyzer" not in code:
        return Response("Error: Must contain 'class EmotionAnalyzer'.", 400)

    # remove package statements if present
    code = re.sub(r'^\s*package\s+.*?;\s*', '', code, flags=re.MULTILINE)

    tmp = tempfile.mkdtemp(prefix="p3_")
    try:
        # write student file
        student_path = os.path.join(tmp, "EmotionAnalyzer.java")
        with open(student_path, "w", encoding="utf-8") as f:
            f.write(code)

        # copy tester
        shutil.copy(TESTER_FILE, os.path.join(tmp, os.path.basename(TESTER_FILE)))

        # compile
        comp = subprocess.run(
            ["javac", "-encoding", "UTF-8", "EmotionAnalyzerTester.java", "EmotionAnalyzer.java"],
            cwd=tmp, capture_output=True, text=True, timeout=RUN_TIMEOUT
        )
        if comp.returncode != 0:
            msg = "Compilation failed.\n\n"
            if comp.stderr: msg += "STDERR:\n" + comp.stderr + "\n\n"
            if comp.stdout: msg += "STDOUT:\n" + comp.stdout + "\n"
            return Response(msg, 200, mimetype="text/plain")

        # run tester
        run = subprocess.run(
            ["java", "EmotionAnalyzerTester"],
            cwd=tmp, capture_output=True, text=True, timeout=RUN_TIMEOUT
        )
        return Response((run.stdout or "") + (("\n" + run.stderr) if run.stderr else ""),
                        200, mimetype="text/plain")

    except subprocess.TimeoutExpired:
        return Response("⏱️ Execution timed out.", 200, mimetype="text/plain")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

@app.get("/healthz")
def healthz():
    return "ok", 200

@app.get("/")
def index():
    return send_from_directory(".", "index.html")

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    app.run(host="0.0.0.0", port=port)
