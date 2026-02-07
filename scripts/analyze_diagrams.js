#!/usr/bin/env node

/**
 * AlgoCraft Diagram Needs Analysis
 *
 * Scans all 500 problems and determines which need diagrams based on:
 * - Data structure tags (Tree, Linked List, Graph, Matrix)
 * - Description keywords (root=, head=, grid=, graph=, intervals, etc.)
 * - Whether diagrams already exist
 */

const fs = require("fs");
const path = require("path");

// fs-extra compatibility helpers
const readJson = (p) => JSON.parse(fs.readFileSync(p, "utf-8"));
const pathExists = (p) => { try { fs.accessSync(p); return true; } catch { return false; } };
const writeJson = (p, data) => fs.writeFileSync(p, JSON.stringify(data, null, 2));

const PROBLEMS_DIR = path.resolve(__dirname, "../question_bank/official");
const IMAGES_DIR = path.join(PROBLEMS_DIR, "images");

// Tags that strongly suggest diagrams are needed
const DIAGRAM_TAGS = [
  "Tree", "Binary Tree", "Binary Search Tree", "Trie",
  "Linked List", "Graph", "Matrix", "Grid",
  "Heap", "Stack", "Queue", "Design"
];

// Keywords in description that suggest diagrams
const DIAGRAM_KEYWORDS = [
  /root\s*=\s*\[/i,
  /head\s*=\s*\[/i,
  /grid\s*=\s*\[/i,
  /graph\s*=\s*\[/i,
  /matrix\s*=\s*\[/i,
  /adjList\s*=\s*\[/i,
  /linked\s*list/i,
  /binary\s*tree/i,
  /trie/i,
  /interval/i,
  /node/i,
];

// Problem types where diagrams typically help understanding
const VISUAL_PROBLEM_TYPES = {
  TREE: "tree",
  LINKED_LIST: "linked_list",
  GRAPH: "graph",
  MATRIX: "matrix",
  INTERVAL: "interval",
  STACK_QUEUE: "stack_queue",
  DESIGN: "design",
  NONE: "none"
};

function classifyProblem(problem) {
  const tags = (problem.tags || []).map(t => t.toLowerCase());
  const desc = (problem.description || "").toLowerCase();
  const title = (problem.title || "").toLowerCase();

  // Tree
  if (tags.some(t => t.includes("tree") || t.includes("trie")) ||
      desc.match(/root\s*=\s*\[/) || title.includes("tree") || title.includes("trie")) {
    return VISUAL_PROBLEM_TYPES.TREE;
  }

  // Linked List
  if (tags.some(t => t.includes("linked list")) ||
      desc.match(/head\s*=\s*\[/) || title.includes("linked list")) {
    return VISUAL_PROBLEM_TYPES.LINKED_LIST;
  }

  // Graph
  if (tags.some(t => t === "graph") ||
      desc.match(/graph\s*=\s*\[/) || desc.match(/adjlist\s*=\s*\[/) ||
      title.includes("graph") || desc.includes("number of islands") ||
      desc.includes("course") && desc.includes("prerequisite")) {
    return VISUAL_PROBLEM_TYPES.GRAPH;
  }

  // Matrix/Grid
  if (tags.some(t => t === "matrix" || t === "grid") ||
      desc.match(/grid\s*=\s*\[/) || desc.match(/matrix\s*=\s*\[/) ||
      desc.match(/board\s*=\s*\[/)) {
    return VISUAL_PROBLEM_TYPES.MATRIX;
  }

  // Interval
  if (tags.some(t => t.includes("interval")) ||
      title.includes("interval") || title.includes("meeting room")) {
    return VISUAL_PROBLEM_TYPES.INTERVAL;
  }

  // Stack/Queue visual problems
  if ((tags.some(t => t === "stack" || t === "queue") &&
       (desc.includes("parenthes") || desc.includes("bracket")))) {
    return VISUAL_PROBLEM_TYPES.STACK_QUEUE;
  }

  return VISUAL_PROBLEM_TYPES.NONE;
}

function analyzeProblem(id) {
  const filePath = path.join(PROBLEMS_DIR, `p${id}.json`);

  if (!pathExists(filePath)) {
    return null;
  }

  const problem = readJson(filePath);
  const classification = classifyProblem(problem);
  const hasDiagrams = problem.diagrams && problem.diagrams.length > 0;
  const diagramCount = hasDiagrams ? problem.diagrams.length : 0;

  // Check if image files actually exist
  let missingImages = [];
  if (hasDiagrams) {
    for (const d of problem.diagrams) {
      if (!d.file) { missingImages.push("(no file field)"); continue; }
      const imgPath = path.join(IMAGES_DIR, d.file);
      if (!pathExists(imgPath)) {
        missingImages.push(d.file);
      }
    }
  }

  const needsDiagram = classification !== VISUAL_PROBLEM_TYPES.NONE;
  const status = needsDiagram
    ? (hasDiagrams ? (missingImages.length > 0 ? "MISSING_FILES" : "OK") : "NEEDS_DIAGRAM")
    : (hasDiagrams ? "OK" : "NO_DIAGRAM_NEEDED");

  return {
    id: problem.id,
    title: problem.title,
    tags: problem.tags,
    difficulty: problem.difficulty,
    classification,
    hasDiagrams,
    diagramCount,
    missingImages,
    needsDiagram,
    status
  };
}

function main() {
  console.log("\n📊 AlgoCraft Diagram Needs Analysis\n");
  console.log("=".repeat(80));

  const results = [];
  const needsDiagram = [];
  const missingFiles = [];
  const ok = [];
  const noDiagramNeeded = [];

  for (let i = 1; i <= 500; i++) {
    const result = analyzeProblem(i);
    if (result) {
      results.push(result);
      switch (result.status) {
        case "NEEDS_DIAGRAM": needsDiagram.push(result); break;
        case "MISSING_FILES": missingFiles.push(result); break;
        case "OK": ok.push(result); break;
        case "NO_DIAGRAM_NEEDED": noDiagramNeeded.push(result); break;
      }
    }
  }

  console.log(`\nTotal problems scanned: ${results.length}`);
  console.log(`\n✅ OK (has needed diagrams): ${ok.length}`);
  console.log(`⚠️  NEEDS DIAGRAM (visual problem, no diagram): ${needsDiagram.length}`);
  console.log(`❌ MISSING FILES (diagram ref but file missing): ${missingFiles.length}`);
  console.log(`⬜ NO DIAGRAM NEEDED: ${noDiagramNeeded.length}`);

  if (needsDiagram.length > 0) {
    console.log("\n" + "=".repeat(80));
    console.log("⚠️  Problems that NEED diagrams:\n");
    for (const p of needsDiagram) {
      console.log(`  p${p.id} - ${p.title} [${p.classification}] (${p.tags.join(", ")})`);
    }
  }

  if (missingFiles.length > 0) {
    console.log("\n" + "=".repeat(80));
    console.log("❌ Problems with MISSING image files:\n");
    for (const p of missingFiles) {
      console.log(`  p${p.id} - ${p.title}: ${p.missingImages.join(", ")}`);
    }
  }

  // Write detailed report as JSON
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      total: results.length,
      ok: ok.length,
      needsDiagram: needsDiagram.length,
      missingFiles: missingFiles.length,
      noDiagramNeeded: noDiagramNeeded.length
    },
    needsDiagram: needsDiagram.map(p => ({
      id: p.id, title: p.title, classification: p.classification, tags: p.tags
    })),
    missingFiles: missingFiles.map(p => ({
      id: p.id, title: p.title, missing: p.missingImages
    })),
    allResults: results
  };

  const reportPath = path.join(__dirname, "diagram_analysis_report.json");
  writeJson(reportPath, report);
  console.log(`\n📝 Full report saved to: ${reportPath}\n`);
}

main();
