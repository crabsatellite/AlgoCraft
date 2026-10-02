"""Local checks for AlgoCraft's unattended test adapter; no remote services."""
from pathlib import Path
import hashlib
import json
import sys

ROOT = Path(__file__).resolve().parents[1]

def verify(root):
    required = ["scripts/runtime_process_guard.ps1", "gradle/algocraft-tests.gradle",
                "src/main/resources/algocraft.unattended.mixins.json",
                "src/main/java/com/crabmods/algocraft/client/test/UnattendedClientTestMode.java"]
    required += [f"src/main/java/com/crabmods/algocraft/mixin/Unattended{kind}Mixin.java"
                 for kind in ("Window", "Input", "Audio", "Clipboard")]
    for name in required:
        if not (root / name).is_file():
            raise ValueError(f"Missing unattended test guard: {name}")
    mixins = json.loads((root / required[2]).read_text(encoding="utf8"))
    for kind in ("Window", "Input", "Audio", "Clipboard"):
        if f"Unattended{kind}Mixin" not in mixins["client"]:
            raise ValueError(f"Unregistered unattended {kind} adapter")
    build = (root / "build.gradle").read_text(encoding="utf8")
    for marker in ("'algocraft.tests.unattended', 'true'", "earlyWindowControl = false",
                   "-XX:ActiveProcessorCount=2", "restore${suffix}Configuration"):
        if marker not in build:
            raise ValueError(f"Missing background launch configuration: {marker}")
    return {"passed": True, "selfContained": True,
            "guardHashes": {name: hashlib.sha256((root / name).read_bytes()).hexdigest() for name in required}}

if __name__ == "__main__":
    try:
        print(json.dumps(verify(ROOT)))
    except (ValueError, KeyError, OSError) as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1)
