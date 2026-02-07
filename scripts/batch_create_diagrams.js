#!/usr/bin/env node

/**
 * Batch create Mermaid diagram files for all remaining problems.
 * Run: node scripts/batch_create_diagrams.js
 */

const fs = require("fs");
const path = require("path");

const MERMAID_DIR = path.resolve(__dirname, "diagrams/mermaid");
const PROBLEMS_DIR = path.resolve(__dirname, "../question_bank/official");

const DARK_HEADER = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a9eff', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%`;
const TRIE_HEADER = `%%{init: {'theme': 'dark', 'themeVariables': { 'primaryColor': '#4a90d9', 'lineColor': '#888888', 'primaryTextColor': '#fff'}}}%%`;

// Helper: build binary tree mermaid from level-order array
function buildTree(values, opts = {}) {
  const { highlight = [], highlightColor = "#90EE90", highlightStroke = "#228B22" } = opts;
  let nodes = [], edges = [], styles = [];
  for (let i = 0; i < values.length; i++) {
    if (values[i] === null) continue;
    const v = values[i];
    const display = v < 0 ? `"${v}"` : `${v}`;
    nodes.push(`    N${i}((${display}))`);
    const li = 2 * i + 1, ri = 2 * i + 2;
    if (li < values.length && values[li] !== null) edges.push(`    N${i} --> N${li}`);
    if (ri < values.length && values[ri] !== null) edges.push(`    N${i} --> N${ri}`);
    const isH = highlight.includes(i) || highlight.includes(v);
    if (isH) {
      styles.push(`    style N${i} fill:${highlightColor},stroke:${highlightStroke},color:#000`);
    } else {
      styles.push(`    style N${i} fill:#4a9eff,stroke:#2a6ecf,color:#fff`);
    }
  }
  return `${DARK_HEADER}\ngraph TD\n${nodes.join("\n")}\n${edges.join("\n")}\n${styles.join("\n")}`;
}

// Helper: linked list
function buildList(values) {
  let parts = values.map((v, i) => `N${i}["${v}"]`);
  let chain = parts.join(" --> ");
  let styles = values.map((_, i) => `    style N${i} fill:#4a9eff,stroke:#2a6ecf,color:#fff`);
  return `${DARK_HEADER}\ngraph LR\n    ${chain}\n${styles.join("\n")}`;
}

// Helper: matrix grid
function buildMatrix(rows, opts = {}) {
  const { title = "Grid" } = opts;
  let rowStrs = rows.map((r, i) => `    R${i}["${r.join("  ")}"]`);
  let rowStyles = rows.map((_, i) => `    style R${i} fill:#2d2d44,stroke:#4a9eff,color:#fff`);
  return `${DARK_HEADER}\ngraph TB\n    subgraph G["${title}"]\n        direction TB\n${rowStrs.join("\n")}\n    end\n    style G fill:#1a1a2e,stroke:#4a9eff,color:#fff\n${rowStyles.join("\n")}`;
}

// Helper: interval timeline
function buildIntervals(intervals, opts = {}) {
  const { title = "Intervals", colors = [] } = opts;
  let items = intervals.map((iv, i) => {
    const c = colors[i] || "#4a9eff";
    return { start: iv[0], end: iv[1], idx: i, color: c };
  });
  let nodes = items.map(it => `    I${it.idx}["[${it.start}, ${it.end}]"]`);
  let styles = items.map(it => `    style I${it.idx} fill:${it.color},stroke:#2a6ecf,color:#fff`);
  return `${DARK_HEADER}\ngraph LR\n    subgraph TL["${title}"]\n        direction LR\n${nodes.join("\n")}\n    end\n    style TL fill:#1a1a2e,stroke:#4a9eff,color:#fff\n${styles.join("\n")}`;
}

// Helper: graph from adjacency matrix or edge list
function buildGraph(nodeCount, edges, opts = {}) {
  const { directed = false, labels = [], highlight = [] } = opts;
  const arrow = directed ? "-->" : "---";
  let nodeLines = [];
  for (let i = 0; i < nodeCount; i++) {
    const label = labels[i] || i;
    nodeLines.push(`    ${i}((${label}))`);
  }
  let edgeLines = edges.map(([a, b, w]) => {
    if (w !== undefined) return `    ${a} ${arrow}|${w}| ${b}`;
    return `    ${a} ${arrow} ${b}`;
  });
  let styleLines = [];
  for (let i = 0; i < nodeCount; i++) {
    if (highlight.includes(i)) {
      styleLines.push(`    style ${i} fill:#FF6B6B,stroke:#CC5555,color:#fff`);
    } else {
      styleLines.push(`    style ${i} fill:#4a9eff,stroke:#2a6ecf,color:#fff`);
    }
  }
  return `${DARK_HEADER}\ngraph TD\n${nodeLines.join("\n")}\n${edgeLines.join("\n")}\n${styleLines.join("\n")}`;
}

// All diagram definitions
const diagrams = {
  // ===== TREE PROBLEMS BATCH 2 =====
  "p238_example1": buildTree([10, 5, 15, 3, 7, null, 18], { highlight: [5, 7, 10, 15], highlightColor: "#90EE90", highlightStroke: "#228B22" }),
  "p322_example1": buildTree([1, 3, null, null, 2]),
  "p326_example1": buildTree([1, 2, 3]),
  "p389_example1": buildTree([0, 0, null, 0, 0]),
  "p465_example1": `${DARK_HEADER}
graph TD
    N1((1))
    N3((3))
    N2((2))
    N4((4))
    N5((5))
    N6((6))
    N1 --> N3
    N1 --> N2
    N1 --> N4
    N3 --> N5
    N3 --> N6
    style N1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N3 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N4 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style N5 fill:#90EE90,stroke:#228B22,color:#000
    style N6 fill:#90EE90,stroke:#228B22,color:#000`,

  "p467_example1": buildTree([1, 2, 3]),
  "p469_example1": `${DARK_HEADER}
graph TD
    subgraph T1["root1"]
        A((1))
        B((2))
        C((3))
        D((4))
        E((5))
        F((6))
        A --> B
        A --> C
        B --> D
        B --> E
        C --> F
    end
    subgraph T2["root2"]
        A2((1))
        B2((3))
        C2((2))
        D2((6))
        E2((4))
        F2((5))
        A2 --> B2
        A2 --> C2
        B2 --> D2
        C2 --> E2
        C2 --> F2
    end
    style A fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style B fill:#90EE90,stroke:#228B22,color:#000
    style C fill:#FFA500,stroke:#CC8400,color:#000
    style D fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style E fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style F fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style A2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style B2 fill:#FFA500,stroke:#CC8400,color:#000
    style C2 fill:#90EE90,stroke:#228B22,color:#000
    style D2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style E2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style F2 fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p475_example1": buildTree([1, 2, 3, 4]),
  "p479_example1": buildTree([3, 9, 20, null, null, 15, 7]),
  "p487_example1": buildTree([1, 2, 3, 4, null, 2, 4, null, null, null, null, 4]),
  "p488_example1": buildTree([5, 3, 6, 2, 4, null, 7]),
  "p489_example1": buildTree([6, 3, 5, null, 2, 0, null, null, 1]),
  "p490_example1": buildTree([1, 2]),
  "p496_example1": buildTree([1, 3, 2, 5, 3, null, 9]),
  "p500_example1": buildTree([1, 0, 2], { highlight: [0], highlightColor: "#FF6B6B", highlightStroke: "#CC5555" }),

  // ===== GRAPH PROBLEMS =====
  "p146_graph": `${DARK_HEADER}
graph TD
    W((w)) --> E((e))
    E --> R((r))
    T((t)) --> F((f))
    R --> T
    style W fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style E fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style R fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style T fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style F fill:#4a9eff,stroke:#2a6ecf,color:#fff`,

  "p383_example1": buildGraph(3, [[0, 1]], { highlight: [0, 1] }),
  "p385_example1": buildGraph(6, [[0, 2], [0, 5], [1, 3], [2, 4], [2, 5], [3, 4], [3, 5]], { labels: ["Hole", "Mouse", "Cat", "3", "4", "5"] }),
  "p387_example1": buildGraph(3, [[0, 1]], { highlight: [0, 1] }),
  "p417_example1": buildGraph(4, [[0, 2, 5], [0, 1, 2], [1, 2, 1], [3, 0, 3]], { directed: true }),

  // ===== INTERVAL PROBLEMS =====
  "p182_example1": buildIntervals([[1, 3], [2, 5], [6, 9]], { title: "Insert Interval: [2,5] into [[1,3],[6,9]]", colors: ["#4a9eff", "#FF6B6B", "#4a9eff"] }),
  "p183_example1": buildIntervals([[1, 3], [2, 6], [8, 10], [15, 18]], { title: "Merge Intervals", colors: ["#FF6B6B", "#FF6B6B", "#4a9eff", "#4a9eff"] }),
  "p184_example1": buildIntervals([[1, 2], [1, 3], [2, 3], [3, 4]], { title: "Non-overlapping Intervals", colors: ["#4a9eff", "#FF6B6B", "#4a9eff", "#4a9eff"] }),
  "p185_example1": buildIntervals([[0, 30], [5, 10], [15, 20]], { title: "Meeting Rooms", colors: ["#4a9eff", "#FF6B6B", "#FF6B6B"] }),

  // ===== MATRIX PROBLEMS =====
  "p52_example1": buildMatrix([[1, 3, 5, 7], [10, 11, 16, 20], [23, 30, 34, 60]], { title: "Search 2D Matrix (target=3)" }),
  "p147_example1": buildMatrix([[0, 2], [1, 3]], { title: "Swim in Rising Water" }),
  "p149_example1": buildMatrix([[1, 2, 2], [3, 8, 2], [5, 3, 5]], { title: "Path With Minimum Effort" }),
  "p190_example1": buildMatrix([[1, 2, 3], [8, 9, 4], [7, 6, 5]], { title: "Spiral Matrix II (n=3)" }),
  "p307_example1": buildMatrix([[1, 2, 3], [4, 0, 5]], { title: "Sliding Puzzle" }),

  // ===== STACK PROBLEMS =====
  "p41_example1": `${DARK_HEADER}
graph LR
    subgraph Input["Input: s = '([)]'"]
        direction LR
        C1["("]
        C2["["]
        C3[")"]
        C4["]"]
    end
    subgraph Stack["Stack Operations"]
        direction TB
        S1["Push ("]
        S2["Push ["]
        S3["Pop: [ != ) -> Invalid"]
    end
    style C1 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style C2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style C3 fill:#FF6B6B,stroke:#CC5555,color:#fff
    style C4 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style S1 fill:#90EE90,stroke:#228B22,color:#000
    style S2 fill:#90EE90,stroke:#228B22,color:#000
    style S3 fill:#FF6B6B,stroke:#CC5555,color:#fff`,

  "p49_example1": `${DARK_HEADER}
graph LR
    subgraph Input["Input: 3[a]2[bc]"]
        direction LR
        I1["3"]
        I2["["]
        I3["a"]
        I4["]"]
        I5["2"]
        I6["["]
        I7["bc"]
        I8["]"]
    end
    subgraph Result["Output: aaabcbc"]
        R1["aaa"]
        R2["bcbc"]
    end
    style I1 fill:#FFA500,stroke:#CC8400,color:#000
    style I2 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style I3 fill:#90EE90,stroke:#228B22,color:#000
    style I4 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style I5 fill:#FFA500,stroke:#CC8400,color:#000
    style I6 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style I7 fill:#90EE90,stroke:#228B22,color:#000
    style I8 fill:#4a9eff,stroke:#2a6ecf,color:#fff
    style R1 fill:#9370DB,stroke:#7B68EE,color:#fff
    style R2 fill:#9370DB,stroke:#7B68EE,color:#fff`,
};

// Diagram-to-problem mapping for JSON updates
const problemUpdates = {
  "238": [{ id: "example1", file: "p238_example1.png", caption: "Example 1: BST [10,5,15,3,7,null,18] with range [7,15] highlighted" }],
  "322": [{ id: "example1", file: "p322_example1.png", caption: "Example 1: BST [1,3,null,null,2] with swapped nodes" }],
  "326": [{ id: "example1", file: "p326_example1.png", caption: "Example 1: Tree [1,2,3]" }],
  "389": [{ id: "example1", file: "p389_example1.png", caption: "Example 1: Tree [0,0,null,0,0]" }],
  "465": [{ id: "example1", file: "p465_example1.png", caption: "Example 1: N-ary Tree (depth=3)" }],
  "467": [{ id: "example1", file: "p467_example1.png", caption: "Example 1: Tree [1,2,3]" }],
  "469": [{ id: "example1", file: "p469_example1.png", caption: "Example 1: Flip Equivalent Trees (children swapped)" }],
  "475": [{ id: "example1", file: "p475_example1.png", caption: "Example 1: Tree [1,2,3,4]" }],
  "479": [{ id: "example1", file: "p479_example1.png", caption: "Example 1: Tree [3,9,20,null,null,15,7]" }],
  "487": [{ id: "example1", file: "p487_example1.png", caption: "Example 1: Tree with duplicate subtrees" }],
  "488": [{ id: "example1", file: "p488_example1.png", caption: "Example 1: BST [5,3,6,2,4,null,7], k=9" }],
  "489": [{ id: "example1", file: "p489_example1.png", caption: "Example 1: Maximum Binary Tree [6,3,5,null,2,0,null,null,1]" }],
  "490": [{ id: "example1", file: "p490_example1.png", caption: "Example 1: Tree [1,2]" }],
  "496": [{ id: "example1", file: "p496_example1.png", caption: "Example 1: Tree [1,3,2,5,3,null,9]" }],
  "500": [{ id: "example1", file: "p500_example1.png", caption: "Example 1: BST [1,0,2] trim to [1,2] (red=removed)" }],
  "146": [{ id: "graph", file: "p146_graph.png", caption: "Character ordering graph from words" }],
  "383": [{ id: "example1", file: "p383_example1.png", caption: "Example 1: Graph with infected nodes (red)" }],
  "385": [{ id: "example1", file: "p385_example1.png", caption: "Example 1: Cat and Mouse game graph" }],
  "387": [{ id: "example1", file: "p387_example1.png", caption: "Example 1: Graph with infected nodes (red)" }],
  "417": [{ id: "example1", file: "p417_example1.png", caption: "Example 1: Directed weighted graph (4 nodes)" }],
  "182": [{ id: "example1", file: "p182_example1.png", caption: "Insert Interval: [2,5] merges with [1,3]" }],
  "183": [{ id: "example1", file: "p183_example1.png", caption: "Merge Intervals: [1,3] and [2,6] overlap" }],
  "184": [{ id: "example1", file: "p184_example1.png", caption: "Non-overlapping: remove [1,3] (red)" }],
  "185": [{ id: "example1", file: "p185_example1.png", caption: "Meeting Rooms: [0,30] conflicts (red)" }],
  "52": [{ id: "example1", file: "p52_example1.png", caption: "Example 1: 3x4 Sorted Matrix" }],
  "147": [{ id: "example1", file: "p147_example1.png", caption: "Example 1: 2x2 Elevation Grid" }],
  "149": [{ id: "example1", file: "p149_example1.png", caption: "Example 1: 3x3 Heights Grid" }],
  "190": [{ id: "example1", file: "p190_example1.png", caption: "Spiral Matrix II: n=3 result" }],
  "307": [{ id: "example1", file: "p307_example1.png", caption: "Sliding Puzzle: Initial Board" }],
  "41": [{ id: "example1", file: "p41_example1.png", caption: "Parentheses matching with stack" }],
  "49": [{ id: "example1", file: "p49_example1.png", caption: "Decode String: 3[a]2[bc] -> aaabcbc" }],
};

// Create .mmd files
let created = 0;
for (const [name, content] of Object.entries(diagrams)) {
  const filePath = path.join(MERMAID_DIR, `${name}.mmd`);
  if (fs.existsSync(filePath)) {
    console.log(`  ⏭️  Skipping (exists): ${name}.mmd`);
    continue;
  }
  fs.writeFileSync(filePath, content);
  console.log(`  ✅ Created: ${name}.mmd`);
  created++;
}
console.log(`\nCreated ${created} .mmd files\n`);

// Update problem JSONs
let updated = 0;
for (const [id, diags] of Object.entries(problemUpdates)) {
  const filePath = path.join(PROBLEMS_DIR, `p${id}.json`);
  try {
    const problem = JSON.parse(fs.readFileSync(filePath, "utf-8"));
    problem.diagrams = diags;
    fs.writeFileSync(filePath, JSON.stringify(problem, null, 2) + "\n");
    console.log(`  ✅ Updated p${id}.json`);
    updated++;
  } catch (err) {
    console.error(`  ❌ Failed p${id}.json: ${err.message}`);
  }
}
console.log(`\nUpdated ${updated} problem JSON files`);
