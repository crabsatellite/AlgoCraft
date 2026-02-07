#!/usr/bin/env node

/**
 * Batch update problem JSON files with diagram references.
 * Usage: node update_diagram_refs.js
 *
 * Reads the updates array below and applies them to problem files.
 */

const fs = require("fs");
const path = require("path");

const PROBLEMS_DIR = path.resolve(__dirname, "../question_bank/official");

// Define updates: { id, diagrams: [{ id, file, caption }] }
const updates = [
  {
    id: "84",
    diagrams: [
      { id: "example1", file: "p84_example1.png", caption: "Example 1: Balanced Tree [3,9,20,null,null,15,7]" },
      { id: "example2", file: "p84_example2.png", caption: "Example 2: Unbalanced Tree [1,2,2,3,3,null,null,4,4]" }
    ]
  },
  {
    id: "85",
    diagrams: [
      { id: "example1", file: "p85_example1.png", caption: "Example 1: Same Trees p=[1,2,3], q=[1,2,3]" },
      { id: "example2", file: "p85_example2.png", caption: "Example 2: Different Structure p=[1,2], q=[1,null,2]" }
    ]
  },
  {
    id: "86",
    diagrams: [
      { id: "example1", file: "p86_example1.png", caption: "Example 1: Subtree Match" },
      { id: "example2", file: "p86_example2.png", caption: "Example 2: Subtree Mismatch (extra node 0)" }
    ]
  },
  {
    id: "93",
    diagrams: [
      { id: "example1", file: "p93_example1.png", caption: "Example 1: Constructed Tree [3,9,20,null,null,15,7]" }
    ]
  },
  {
    id: "98",
    diagrams: [
      { id: "example1", file: "p98_example1.png", caption: "Example 1: Tree [5,4,8,11,null,13,4,7,2,5,1]" }
    ]
  },
  {
    id: "99",
    diagrams: [
      { id: "example1", file: "p99_example1.png", caption: "Example 1: Tree [10,5,-3,3,2,null,11,3,-2,null,1]" }
    ]
  },
  {
    id: "101",
    diagrams: [
      { id: "trie", file: "p101_trie.png", caption: "Trie Structure after inserting 'apple' and 'app'" }
    ]
  },
  {
    id: "102",
    diagrams: [
      { id: "trie", file: "p102_trie.png", caption: "Trie Structure for WordDictionary with 'bad' and 'mad'" }
    ]
  },
  {
    id: "103",
    diagrams: [
      { id: "board", file: "p103_board.png", caption: "Example 1: 4x4 Character Board" }
    ]
  },
  {
    id: "104",
    diagrams: [
      { id: "trie", file: "p104_trie.png", caption: "Trie of dictionary roots: cat, bat, rat" }
    ]
  },
  {
    id: "105",
    diagrams: [
      { id: "trie", file: "p105_trie.png", caption: "Trie with cumulative sum values" }
    ]
  },
  {
    id: "106",
    diagrams: [
      { id: "kth_stream", file: "p106_kth_stream.png", caption: "Min-Heap for Kth Largest Element Stream" }
    ]
  }
];

let updated = 0;
let errors = 0;

for (const update of updates) {
  const filePath = path.join(PROBLEMS_DIR, `p${update.id}.json`);
  try {
    const content = fs.readFileSync(filePath, "utf-8");
    const problem = JSON.parse(content);
    problem.diagrams = update.diagrams;
    fs.writeFileSync(filePath, JSON.stringify(problem, null, 2) + "\n");
    console.log(`  ✅ Updated p${update.id}.json with ${update.diagrams.length} diagram(s)`);
    updated++;
  } catch (err) {
    console.error(`  ❌ Failed p${update.id}.json: ${err.message}`);
    errors++;
  }
}

console.log(`\nDone: ${updated} updated, ${errors} errors`);
