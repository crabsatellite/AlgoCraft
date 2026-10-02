import json
from pathlib import Path


BASE = Path(__file__).resolve().parents[1] / "question_bank" / "official"
IMAGES = BASE / "images"


CAPTION_OVERRIDES = {
    41: {"example1": "Parentheses input scan"},
    101: {"trie": "Trie containing apple and app"},
    106: {"kth_stream": "Kth largest stream state"},
    131: {"example1": "Rooms, gates, and walls grid"},
    448: {"design": "Text editor cursor state"},
}


REMOVE_OUTPUT_DIAGRAM_IDS = {
    65: {"example1_reversed"},
    66: {"example1_merged"},
    67: {"example1_reordered"},
    74: {"example1_merged"},
    75: {"example1_reversed"},
    81: {"example1_inverted"},
    100: {"example1_flattened"},
    20: {"example1"},
    70: {"example1"},
    75: {"example1"},
    94: {"example1", "example2"},
    121: {"example1"},
    128: {"example1"},
    129: {"example1"},
    139: {"example1"},
    140: {"example1"},
    145: {"example1"},
    187: {"example1"},
    132: {"example1_result"},
    364: {"timeline"},
    188: {"example1_rotated"},
    449: {"timeline"},
    450: {"timeline"},
    500: {"example1"},
    476: {"example1_merged"},
}


def title_for(number: int) -> str:
    path = BASE / f"p{number}.json"
    if not path.exists():
        return f"Problem {number}"
    return json.loads(path.read_text(encoding="utf-8-sig")).get("title", f"Problem {number}")


def normalize_entry(number: int, entry):
    if isinstance(entry, str):
        diagram_id = entry.removeprefix(f"p{number}_")
        file_name = f"{entry}.png"
        return {
            "id": diagram_id,
            "file": file_name,
            "caption": f"{title_for(number)} example",
        }

    if not isinstance(entry, dict):
        raise ValueError(f"Unsupported diagram entry for p{number}: {entry!r}")

    normalized = dict(entry)
    diagram_id = normalized.get("id", "")
    if not diagram_id and normalized.get("file"):
        stem = Path(normalized["file"]).stem
        diagram_id = stem.removeprefix(f"p{number}_")
        normalized["id"] = diagram_id

    override = CAPTION_OVERRIDES.get(number, {}).get(diagram_id)
    if override:
        normalized["caption"] = override
    return normalized


def main() -> None:
    changed = []
    for path in sorted(BASE.glob("p*.json"), key=lambda p: int(p.stem[1:])):
        number = int(path.stem[1:])
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        diagrams = data.get("diagrams")
        if not diagrams:
            continue

        next_diagrams = []
        for entry in diagrams:
            normalized = normalize_entry(number, entry)
            diagram_id = normalized.get("id", "")
            file_name = normalized.get("file", "")
            if diagram_id in REMOVE_OUTPUT_DIAGRAM_IDS.get(number, set()):
                continue
            if file_name and not (IMAGES / file_name).exists():
                continue
            next_diagrams.append(normalized)

        if next_diagrams:
            data["diagrams"] = next_diagrams
        else:
            data.pop("diagrams", None)

        if data.get("diagrams") != diagrams:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            changed.append(path.name)

    print(f"Updated diagram metadata in {len(changed)} problem files")
    for name in changed:
        print(f"  {name}")


if __name__ == "__main__":
    main()
