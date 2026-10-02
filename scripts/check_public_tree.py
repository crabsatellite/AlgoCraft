"""Keep publishable source free of internal reports and local runtime files."""
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
REPORT = re.compile(r"(?:review|audit|handoff|receipt|verification|checklist|in_progress)", re.I)
REPORT_FORMATS = {".md", ".json", ".csv", ".html"}
TEMP_FORMATS = {".bak", ".tmp", ".log", ".pyc", ".orig", ".rej"}

def forbidden(name):
    path = PurePosixPath(name.replace("\\", "/"))
    if any(part == "node_modules" for part in path.parts):
        return True
    if path.parts[0] in {"build", "run", "runs", "saves", "deliverables"}:
        return True
    if any(part.startswith(".") for part in path.parts[:-1]) and path.parts[0] != ".github":
        return True
    if path.suffix.lower() in TEMP_FORMATS:
        return True
    return (path.suffix.lower() in REPORT_FORMATS and not name.startswith("src/test/")
            and bool(REPORT.search(name)))

if __name__ == "__main__":
    result = subprocess.run(["git", "ls-files", "--cached", "--others", "--exclude-standard"],
                            cwd=ROOT, capture_output=True, text=True, check=True)
    denied = sorted({name for name in result.stdout.splitlines()
                     if (ROOT / name).exists() and forbidden(name)})
    if denied:
        print("Source hygiene failed:\n" + "\n".join(denied), file=sys.stderr)
        raise SystemExit(1)
    print("Source hygiene passed: no internal reports or local runtime files.")
