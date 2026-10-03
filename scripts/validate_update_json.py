"""Validate AlgoCraft's public loader update feed."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
feed = json.loads((ROOT / "update.json").read_text(encoding="utf-8"))
assert feed["homepage"] == "https://github.com/crabsatellite/AlgoCraft"
for mc in ("1.20.1", "1.21.1"):
    versions = feed[mc]
    assert versions and all(isinstance(version, str) and message for version, message in versions.items())
    assert feed["promos"][f"{mc}-latest"] in versions
    assert feed["promos"][f"{mc}-recommended"] in versions
    assert feed["promos"][f"{mc}-recommended"] == "0.1.0-beta"
assert not any(key.endswith("-recommended") and value != "0.1.0-beta" for key, value in feed["promos"].items())
print("Update JSON passed: both Minecraft versions have latest and recommended entries.")
