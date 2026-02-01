#!/usr/bin/env node

/**
 * AlgoCraft Auto Diagram Generator
 *
 * Automatically generates Mermaid diagrams from problem JSON data.
 * Parses example inputs like:
 *   - root = [1,2,3,null,4,5] -> Binary Tree
 *   - head = [1,2,3,4,5] -> Linked List
 *   - graph = [[1,2],[2,3]] -> Graph
 *   - grid = [[0,1],[1,0]] -> Matrix
 */

const fs = require("fs-extra");
const path = require("path");
const { glob } = require("glob");

const PROBLEMS_DIR = path.resolve(__dirname, "../../question_bank/official");
const MERMAID_DIR = path.join(__dirname, "mermaid");

// Color palette for diagrams
const COLORS = {
  primary: { fill: "#4a9eff", stroke: "#2a6ecf", text: "#fff" },
  success: { fill: "#90EE90", stroke: "#228B22", text: "#000" },
  warning: { fill: "#FFD700", stroke: "#B8860B", text: "#000" },
  danger: { fill: "#FF6B6B", stroke: "#CC5555", text: "#fff" },
  info: { fill: "#4ECDC4", stroke: "#3DAD9F", text: "#000" },
  purple: { fill: "#9370DB", stroke: "#7B68EE", text: "#fff" },
  orange: { fill: "#FFA500", stroke: "#CC8400", text: "#000" },
  dark: { fill: "#333333", stroke: "#555555", text: "#fff" },
};

/**
 * Parse binary tree array notation [1,2,3,null,4,5]
 */
function parseBinaryTree(input) {
  const match = input.match(/\[([^\]]*)\]/);
  if (!match) return null;

  const values = match[1].split(",").map((v) => {
    v = v.trim();
    if (v === "null" || v === "") return null;
    return isNaN(v) ? v : parseInt(v);
  });

  return values;
}

/**
 * Generate Mermaid code for binary tree
 */
function generateBinaryTreeMermaid(values, options = {}) {
  if (!values || values.length === 0) return null;

  const { highlight = [], highlightColor = "success" } = options;

  let mermaid = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a9eff', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%\ngraph TD\n`;

  const nodes = [];
  const edges = [];
  const styles = [];

  // Build tree structure
  for (let i = 0; i < values.length; i++) {
    if (values[i] === null) continue;

    const nodeId = `N${i}`;
    const value = values[i];
    nodes.push(`    ${nodeId}((${value}))`);

    // Left child
    const leftIdx = 2 * i + 1;
    if (leftIdx < values.length && values[leftIdx] !== null) {
      edges.push(`    ${nodeId} --> N${leftIdx}`);
    }

    // Right child
    const rightIdx = 2 * i + 2;
    if (rightIdx < values.length && values[rightIdx] !== null) {
      edges.push(`    ${nodeId} --> N${rightIdx}`);
    }

    // Style
    const isHighlighted = highlight.includes(i) || highlight.includes(value);
    const color = isHighlighted ? COLORS[highlightColor] : COLORS.primary;
    styles.push(
      `    style ${nodeId} fill:${color.fill},stroke:${color.stroke},color:${color.text}`,
    );
  }

  mermaid += nodes.join("\n") + "\n";
  mermaid += edges.join("\n") + "\n";
  mermaid += styles.join("\n");

  return mermaid;
}

/**
 * Generate Mermaid code for linked list
 */
function generateLinkedListMermaid(values, options = {}) {
  if (!values || values.length === 0) return null;

  const { cyclePos = -1 } = options;

  let mermaid = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a9eff', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%\ngraph LR\n`;

  const nodes = [];
  const edges = [];
  const styles = [];

  for (let i = 0; i < values.length; i++) {
    const nodeId = `N${i}`;
    nodes.push(`    ${nodeId}["${values[i]}"]`);

    if (i < values.length - 1) {
      edges.push(`    ${nodeId} --> N${i + 1}`);
    }

    const color = COLORS.primary;
    styles.push(
      `    style ${nodeId} fill:${color.fill},stroke:${color.stroke}`,
    );
  }

  // Add cycle edge if exists
  if (cyclePos >= 0 && cyclePos < values.length) {
    edges.push(`    N${values.length - 1} -.-> N${cyclePos}`);
    styles[cyclePos] =
      `    style N${cyclePos} fill:${COLORS.danger.fill},stroke:${COLORS.danger.stroke},color:${COLORS.danger.text}`;
  }

  mermaid += nodes.join("\n") + "\n";
  mermaid += edges.join("\n") + "\n";
  mermaid += styles.join("\n");

  return mermaid;
}

/**
 * Generate Mermaid code for graph (adjacency list)
 */
function generateGraphMermaid(adjList, options = {}) {
  if (!adjList || adjList.length === 0) return null;

  const { directed = false } = options;
  const edgeOp = directed ? "-->" : "---";

  let mermaid = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a9eff', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%\ngraph TD\n`;

  const nodes = new Set();
  const edges = new Set();
  const styles = [];

  for (let i = 0; i < adjList.length; i++) {
    nodes.add(i);
    for (const neighbor of adjList[i]) {
      nodes.add(neighbor);
      const edgeKey = directed
        ? `${i}-${neighbor}`
        : [i, neighbor].sort().join("-");
      if (!edges.has(edgeKey)) {
        edges.add(edgeKey);
      }
    }
  }

  // Add nodes
  for (const node of nodes) {
    mermaid += `    ${node}((${node}))\n`;
  }

  // Add edges
  for (const edge of edges) {
    const [a, b] = edge.split("-");
    mermaid += `    ${a} ${edgeOp} ${b}\n`;
  }

  // Add styles
  for (const node of nodes) {
    const color = COLORS.primary;
    mermaid += `    style ${node} fill:${color.fill},stroke:${color.stroke}\n`;
  }

  return mermaid;
}

/**
 * Parse problem description to extract example data
 */
function parseExamples(description) {
  const examples = [];

  // Match root = [...] patterns (binary trees)
  const treeMatches = description.matchAll(/root\s*=\s*\[([^\]]*)\]/g);
  for (const match of treeMatches) {
    examples.push({
      type: "binary_tree",
      raw: match[0],
      data: parseBinaryTree(match[0]),
    });
  }

  // Match head = [...] patterns (linked lists)
  const listMatches = description.matchAll(/head\s*=\s*\[([^\]]*)\]/g);
  for (const match of listMatches) {
    const values = match[1].split(",").map((v) => {
      v = v.trim();
      return isNaN(v) ? v : parseInt(v);
    });
    examples.push({
      type: "linked_list",
      raw: match[0],
      data: values,
    });
  }

  // Match graph/adjList patterns
  const graphMatches = description.matchAll(
    /(?:adjList|graph)\s*=\s*\[(\[[^\]]*\](?:,\[[^\]]*\])*)\]/g,
  );
  for (const match of graphMatches) {
    try {
      const data = JSON.parse(`[${match[1]}]`);
      examples.push({
        type: "graph",
        raw: match[0],
        data,
      });
    } catch (e) {
      // Skip malformed data
    }
  }

  return examples;
}

/**
 * Generate diagrams for a problem
 */
function generateDiagramsForProblem(problem) {
  const diagrams = [];
  const examples = parseExamples(problem.description);

  let exampleIndex = 1;
  for (const example of examples) {
    let mermaidCode = null;

    switch (example.type) {
      case "binary_tree":
        mermaidCode = generateBinaryTreeMermaid(example.data);
        break;
      case "linked_list":
        mermaidCode = generateLinkedListMermaid(example.data);
        break;
      case "graph":
        mermaidCode = generateGraphMermaid(example.data);
        break;
    }

    if (mermaidCode) {
      diagrams.push({
        filename: `p${problem.id}_example${exampleIndex}.mmd`,
        type: example.type,
        code: mermaidCode,
      });
      exampleIndex++;
    }
  }

  return diagrams;
}

/**
 * Main function
 */
async function main() {
  console.log("\n🎨 AlgoCraft Auto Diagram Generator\n");

  await fs.ensureDir(MERMAID_DIR);

  // Find all problem files
  const problemFiles = await glob(
    path.join(PROBLEMS_DIR, "p*.json").replace(/\\/g, "/"),
  );

  console.log(`📊 Scanning ${problemFiles.length} problems...\n`);

  let totalGenerated = 0;
  const problemsWithDiagrams = [];

  for (const file of problemFiles) {
    try {
      const problem = await fs.readJson(file);
      const diagrams = generateDiagramsForProblem(problem);

      if (diagrams.length > 0) {
        problemsWithDiagrams.push({
          id: problem.id,
          title: problem.title,
          diagrams: diagrams.length,
        });

        for (const diagram of diagrams) {
          const outputPath = path.join(MERMAID_DIR, diagram.filename);

          // Skip if file already exists (preserve manual edits)
          if (await fs.pathExists(outputPath)) {
            continue;
          }

          await fs.writeFile(outputPath, diagram.code);
          console.log(`  ✅ Generated: ${diagram.filename}`);
          totalGenerated++;
        }
      }
    } catch (error) {
      // Skip problematic files
    }
  }

  console.log(`\n📈 Summary:`);
  console.log(`   Problems with diagrams: ${problemsWithDiagrams.length}`);
  console.log(`   New diagrams generated: ${totalGenerated}`);
  console.log(`   Output directory: ${MERMAID_DIR}\n`);

  // Write manifest
  const manifest = {
    generated: new Date().toISOString(),
    problems: problemsWithDiagrams,
  };
  await fs.writeJson(path.join(MERMAID_DIR, "manifest.json"), manifest, {
    spaces: 2,
  });
  console.log(`   📝 Manifest saved to manifest.json\n`);
}

main().catch(console.error);
