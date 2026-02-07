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
    id: "68",
    diagrams: [
      { id: "example1", file: "p68_example1.png", caption: "Example 1: Linked List [1,2,3,4,5], remove 2nd from end" }
    ]
  },
  {
    id: "69",
    diagrams: [
      { id: "example1", file: "p69_example1.png", caption: "Example 1: Linked List with Random Pointers" }
    ]
  },
  {
    id: "73",
    diagrams: [
      { id: "lru", file: "p73_lru.png", caption: "LRU Cache: Doubly Linked List + HashMap Structure" }
    ]
  },
  {
    id: "78",
    diagrams: [
      { id: "example1", file: "p78_example1.png", caption: "Example 1: Unsorted List [4,2,1,3]" }
    ]
  },
  {
    id: "79",
    diagrams: [
      { id: "example1", file: "p79_example1.png", caption: "Example 1: List [1,4,3,2,5,2] with x=3 (green < x, red >= x)" }
    ]
  },
  {
    id: "80",
    diagrams: [
      { id: "example1", file: "p80_example1.png", caption: "Example 1: List [1,2,3,4,5], rotate by k=2" }
    ]
  },
  {
    id: "207",
    diagrams: [
      { id: "example1", file: "p207_example1.png", caption: "Example: Sorted List with Duplicates [1,1,2,3,3]" }
    ]
  },
  {
    id: "208",
    diagrams: [
      { id: "example1", file: "p208_example1.png", caption: "Example 1: List [1,2,3,3,4,4,5] (red = duplicate groups)" }
    ]
  },
  {
    id: "304",
    diagrams: [
      { id: "example1", file: "p304_example1.png", caption: "Example 1: List [1,2,3,4] (pairs to swap highlighted)" }
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
