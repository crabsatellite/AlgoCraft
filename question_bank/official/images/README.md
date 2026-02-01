# Algorithm Problem Diagram Images

This directory contains **55 PNG diagram images** for algorithm problem visualization.

## Current Coverage

| Category           | Problems                                           | Count |
| ------------------ | -------------------------------------------------- | ----- |
| **Binary Trees**   | p81-p97, p100, p476                                | ~25   |
| **Linked Lists**   | p65-p77                                            | ~15   |
| **Graphs**         | p130, p135-p150                                    | ~10   |
| **Matrices/Grids** | p121, p124, p129, p132-p134, p148, p188-p189, p460 | ~10   |

## Generation

Images are auto-generated from Mermaid source files:

```bash
cd scripts/diagrams
npm install
npm run build   # Generate PNGs + Update problem JSONs
```

## Naming Convention

- `p{problem_id}_example{n}.png` - Example diagram for problem
- `p{problem_id}_example{n}_{variant}.png` - Variant (e.g., `_reversed`, `_result`)

## Usage in Game

Problem JSON files reference these images via the `diagrams` field:

```json
{
  "diagrams": [
    {
      "id": "example1",
      "file": "p94_example1.png",
      "caption": "Example 1"
    }
  ]
}
```

The game client loads these images when displaying problem descriptions.
