"""Verify shared player content before delivering the two Minecraft builds."""
import argparse
import hashlib
import json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("other", type=Path, help="The Forge 1.20.1 checkout")
parser.add_argument("--output", type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
other = args.other.resolve()

def files(base, suffixes=None):
    return {p.relative_to(base).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in base.rglob("*") if p.is_file()
            and (suffixes is None or p.suffix.lower() in suffixes)}

counts = {}
for relative, suffixes in [("question_bank/official", {".json", ".png"}),
                           ("src/main/resources/assets/algocraft", None)]:
    left, right = files(root / relative, suffixes), files(other / relative, suffixes)
    differences = sorted(name for name in left.keys() | right.keys() if left.get(name) != right.get(name))
    if differences:
        raise SystemExit(f"Shared content differs in {relative}: {differences[:20]}")
    counts[relative] = len(left)

reward_path = "src/main/java/com/crabmods/algocraft/logic/RewardSystem.java"
def reward_rules(checkout):
    text = (checkout / reward_path).read_text(encoding="utf-8")
    return text[text.index("    private static void giveBaseRewards"):text.index("    private static void giveRandomBonus")]
if reward_rules(root) != reward_rules(other):
    raise SystemExit("First-clear, daily, weekly or milestone reward rules differ between versions")

receipt = {"passed": True, "checkouts": [str(root), str(other)], "identicalFileCounts": counts,
           "sharedContentSha256": hashlib.sha256(json.dumps(files(root / "question_bank/official", {".json", ".png"}), sort_keys=True).encode()).hexdigest(),
           "rewardRulesSha256": hashlib.sha256(reward_rules(root).encode()).hexdigest()}
if args.output:
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(receipt, indent=2), encoding="utf-8")
print(json.dumps(receipt, indent=2))
