#!/usr/bin/env python3
"""
Generate manifest.json for the AlgoCraft official question bank.
This script scans all problem JSON files and prompt image files and creates a manifest with:
- Version information
- File hashes for incremental updates
- Total problem count
- Schema validation for each problem

Usage:
    python generate_manifest.py
    python generate_manifest.py --validate-only  # Only validate, don't generate
    
The manifest.json will be created in the same directory as this script.
"""

import json
import hashlib
import sys
from datetime import datetime, timezone
from pathlib import Path

# Configuration
QUESTION_BANK_DIR = Path(__file__).parent
VERSION = "1.0.0"  # Update this when making releases

# JSON Schema for problem validation
PROBLEM_SCHEMA = {
    "required": ["id", "title", "description", "difficulty", "initialCode", "tests"],
    "properties": {
        "id": {"type": "string"},
        "title": {"type": "string", "minLength": 1},
        "description": {"type": "string", "minLength": 1},
        "difficulty": {"type": "string", "enum": ["EASY", "MEDIUM", "HARD"]},
        "initialCode": {"type": "string", "minLength": 1},
        "tags": {"type": "array", "items": {"type": "string"}},
        "examples": {
            "type": "array",
            "items": {
                "type": "object",
                "required": ["input", "output"],
                "properties": {
                    "input": {"type": "string"},
                    "output": {"type": "string"}
                }
            }
        },
        "tests": {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "required": ["input", "output"],
                "properties": {
                    "input": {"type": "string"},
                    "output": {"type": "string"}
                }
            }
        }
    }
}

def validate_problem(problem: dict, filename: str) -> list[str]:
    """Validate a problem against the schema. Returns list of errors."""
    errors = []
    
    # Check required fields
    for field in PROBLEM_SCHEMA["required"]:
        if field not in problem:
            errors.append(f"Missing required field: {field}")
    
    # Validate difficulty
    if "difficulty" in problem:
        valid_difficulties = PROBLEM_SCHEMA["properties"]["difficulty"]["enum"]
        if problem["difficulty"] not in valid_difficulties:
            errors.append(f"Invalid difficulty '{problem['difficulty']}'. Must be one of: {valid_difficulties}")
    
    # Validate tests array
    if "tests" in problem:
        if not isinstance(problem["tests"], list):
            errors.append("'tests' must be an array")
        elif len(problem["tests"]) == 0:
            errors.append("'tests' must have at least one test case")
        else:
            for i, test in enumerate(problem["tests"]):
                if not isinstance(test, dict):
                    errors.append(f"tests[{i}] must be an object")
                elif "input" not in test or "output" not in test:
                    errors.append(f"tests[{i}] missing 'input' or 'output'")
    
    # Validate examples array
    if "examples" in problem:
        if not isinstance(problem["examples"], list):
            errors.append("'examples' must be an array")
        else:
            for i, ex in enumerate(problem["examples"]):
                if not isinstance(ex, dict):
                    errors.append(f"examples[{i}] must be an object")
                elif "input" not in ex or "output" not in ex:
                    errors.append(f"examples[{i}] missing 'input' or 'output'")
    
    # Validate tags
    if "tags" in problem:
        if not isinstance(problem["tags"], list):
            errors.append("'tags' must be an array")
        elif not all(isinstance(t, str) for t in problem["tags"]):
            errors.append("All tags must be strings")

    if "images" in problem:
        errors.append("Legacy 'images' field is no longer supported; use 'diagrams' only")

    if "diagram" in problem:
        errors.append("Legacy 'diagram' field is no longer supported; use 'diagrams' only")

    if "diagrams" in problem:
        if not isinstance(problem["diagrams"], list):
            errors.append("'diagrams' must be an array")
        elif len(problem["diagrams"]) == 0:
            errors.append("'diagrams' should be omitted instead of empty")
        else:
            for i, diagram in enumerate(problem["diagrams"]):
                if not isinstance(diagram, dict):
                    errors.append(f"diagrams[{i}] must be an object")
                    continue
                for field in ("id", "file", "caption"):
                    if not isinstance(diagram.get(field), str) or not diagram[field].strip():
                        errors.append(f"diagrams[{i}] missing non-empty '{field}'")
                file = diagram.get("file")
                if isinstance(file, str) and file.strip():
                    image_path = QUESTION_BANK_DIR / "images" / file
                    if "/" in file or "\\" in file or not file.endswith(".png"):
                        errors.append(f"diagrams[{i}] file should be a PNG filename, not a path: {file}")
                    elif not image_path.exists():
                        errors.append(f"diagrams[{i}] image file is missing: images/{file}")
    
    # Validate initialCode contains a valid class definition
    if "initialCode" in problem and "description" in problem:
        code = problem["initialCode"]
        description = problem["description"]
        tags = problem.get("tags", [])
        
        import re
        
        # First check: does code contain 'class Solution'?
        # This is the standard case for most problems
        has_solution_class = "class Solution" in code
        
        if has_solution_class:
            # Standard problem with Solution class - OK
            pass
        else:
            # No Solution class - must be a design problem with custom class
            # Extract the main class name (skip commented class definitions like ListNode, TreeNode)
            
            # Remove multi-line comments /* ... */
            code_no_comments = re.sub(r'/\*.*?\*/', '', code, flags=re.DOTALL)
            # Remove single-line comments // ...
            code_no_comments = re.sub(r'//.*$', '', code_no_comments, flags=re.MULTILINE)
            
            # Find actual class definitions (not in comments)
            class_match = re.search(r'\bclass\s+(\w+)', code_no_comments)
            
            if not class_match:
                errors.append("initialCode should contain a class definition")
            else:
                class_name = class_match.group(1)
                
                # Validate custom class name is documented in description or has Design tag
                # Common patterns: "Implement the `ClassName`", "the ClassName class"
                class_in_description = (
                    f"`{class_name}`" in description or
                    f"**{class_name}**" in description or
                    f"Implement the {class_name}" in description or
                    f"the {class_name} class" in description.lower() or
                    re.search(rf'\b{class_name}\b.*class\b', description, re.IGNORECASE) is not None or
                    re.search(rf'\bclass\b.*\b{class_name}\b', description, re.IGNORECASE) is not None
                )
                
                has_design_tag = "Design" in tags
                
                if not class_in_description and not has_design_tag:
                    errors.append(
                        f"Custom class '{class_name}' not documented in description and no 'Design' tag. "
                        f"Either use 'class Solution' or ensure the class is properly documented."
                    )
    
    return errors


def normalized_file_bytes(filepath: Path) -> bytes:
    """Read a UTF-8 text file with stable LF line endings."""
    content = filepath.read_text(encoding='utf-8')
    return content.replace('\r\n', '\n').replace('\r', '\n').encode('utf-8')


def manifest_file_bytes(filepath: Path) -> bytes:
    """Read a file exactly as the manifest downloader will verify it."""
    if filepath.suffix.lower() == ".json":
        return normalized_file_bytes(filepath)
    return filepath.read_bytes()


def sha256_file(filepath: Path) -> str:
    """Calculate SHA-256 hash of a manifest file."""
    return hashlib.sha256(manifest_file_bytes(filepath)).hexdigest()


def sha256_string(s: str) -> str:
    """Calculate SHA-256 hash of a string."""
    return hashlib.sha256(s.encode('utf-8')).hexdigest()


def load_and_validate_problem(filepath: Path) -> tuple[dict | None, list[str]]:
    """Load a problem file and validate it. Returns (problem, errors)."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            problem = json.load(f)
        errors = validate_problem(problem, filepath.name)
        return problem, errors
    except json.JSONDecodeError as e:
        return None, [f"Invalid JSON: {e}"]
    except Exception as e:
        return None, [f"Failed to read file: {e}"]


def referenced_image_files(problem_files: list[Path]) -> list[Path]:
    """Return PNG files referenced by structured diagrams."""
    referenced = set()
    for filepath in problem_files:
        problem = json.loads(filepath.read_text(encoding='utf-8'))
        for diagram in problem.get("diagrams", []):
            if not isinstance(diagram, dict):
                continue
            file = diagram.get("file")
            if isinstance(file, str) and file.strip():
                referenced.add(QUESTION_BANK_DIR / "images" / file)
    return sorted(referenced, key=lambda path: path.relative_to(QUESTION_BANK_DIR).as_posix())


def translation_files() -> list[Path]:
    """Return translation files that should be synced with the official bank."""
    lang_dir = QUESTION_BANK_DIR / "lang"
    if not lang_dir.exists():
        return []
    return sorted(
        [
            path
            for path in lang_dir.glob("*/*.json")
            if path.parent.name and path.stem.startswith("p") and path.stem[1:].isdigit()
        ],
        key=lambda path: path.relative_to(QUESTION_BANK_DIR).as_posix()
    )


def generate_manifest(validate_only: bool = False):
    """Generate the manifest.json file."""
    
    # Find all problem JSON files and repository metadata files.
    problem_files = sorted([
        f for f in QUESTION_BANK_DIR.glob("p*.json")
        if f.name != "manifest.json"
    ], key=lambda x: int(x.stem[1:]) if x.stem[1:].isdigit() else 0)
    metadata_files = []
    catalog_path = QUESTION_BANK_DIR / "catalog.json"
    if catalog_path.exists():
        metadata_files.append(catalog_path)
    image_files = referenced_image_files(problem_files)
    localized_files = translation_files()
    sync_files = metadata_files + problem_files + image_files + localized_files
    
    print(f"Found {len(problem_files)} problem files")
    print(f"Found {len(image_files)} prompt image files")
    print(f"Found {len(localized_files)} translation files")
    
    # Validate all problems first
    total_errors = 0
    validated_count = 0
    
    for filepath in problem_files:
        problem, errors = load_and_validate_problem(filepath)
        if errors:
            print(f"\n[ERROR] {filepath.name} has {len(errors)} error(s):")
            for error in errors:
                print(f"   - {error}")
            total_errors += len(errors)
        else:
            validated_count += 1
    
    if total_errors > 0:
        print(f"\n[WARN] Validation completed with {total_errors} error(s) in {len(problem_files) - validated_count} file(s)")
        if validate_only:
            sys.exit(1)
        print("   Continuing with manifest generation anyway...")
    else:
        print(f"\n[OK] All {validated_count} problems validated successfully")
    
    if validate_only:
        return None
    
    # Build file entries
    files = []
    all_hashes = []
    
    for filepath in sync_files:
        file_bytes = manifest_file_bytes(filepath)
        file_hash = hashlib.sha256(file_bytes).hexdigest()
        file_size = len(file_bytes)
        file_name = filepath.relative_to(QUESTION_BANK_DIR).as_posix()
        
        files.append({
            "name": file_name,
            "hash": file_hash,
            "size": file_size
        })
        all_hashes.append(file_hash)
        
        print(f"  {file_name}: {file_hash[:16]}... ({file_size} bytes)")
    
    # Calculate combined signature
    combined = "".join(all_hashes)
    signature = sha256_string(combined)
    
    # Build manifest
    manifest = {
        "version": VERSION,
        "lastUpdated": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "signature": signature,
        "totalProblems": len(problem_files),
        "files": files
    }
    
    # Write manifest
    manifest_path = QUESTION_BANK_DIR / "manifest.json"
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
    
    print(f"\nGenerated manifest.json:")
    print(f"  Version: {VERSION}")
    print(f"  Signature: {signature[:32]}...")
    print(f"  Total problems: {len(problem_files)}")
    print(f"  Output: {manifest_path}")
    
    return manifest


if __name__ == "__main__":
    validate_only = "--validate-only" in sys.argv or "-v" in sys.argv
    generate_manifest(validate_only=validate_only)
