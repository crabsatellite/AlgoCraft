#!/usr/bin/env node

/**
 * AlgoCraft Problem JSON Updater
 *
 * Updates problem JSON files to include diagram references.
 * Run this after generating PNG images.
 *
 * Usage:
 *   node update_problems.js         # Update all problems with diagrams
 *   node update_problems.js --dry   # Preview changes without writing
 */

const fs = require("fs-extra");
const path = require("path");
const { glob } = require("glob");
const { program } = require("commander");

// Paths
const SCRIPT_DIR = __dirname;
const MERMAID_DIR = path.join(SCRIPT_DIR, "mermaid");
const IMAGES_DIR = path.resolve(
  SCRIPT_DIR,
  "../../question_bank/official/images",
);
const PROBLEMS_DIR = path.resolve(SCRIPT_DIR, "../../question_bank/official");

// Diagram captions based on filename patterns
const CAPTION_PATTERNS = {
  example1: "Example 1",
  example2: "Example 2",
  example3: "Example 3",
  reversed: "After Reversal",
  inverted: "After Inversion",
  merged: "After Merge",
  reordered: "After Reorder",
  result: "Result",
  rotated: "After Rotation",
  flattened: "After Flatten",
};

program
  .option("-d, --dry", "Dry run - preview changes without writing")
  .option("-v, --verbose", "Verbose output")
  .parse();

const options = program.opts();

/**
 * Parse problem ID from filename
 */
function parseProblemId(filename) {
  const match = filename.match(/^p(\d+)_/);
  return match ? match[1] : null;
}

/**
 * Generate caption from filename
 */
function generateCaption(filename) {
  const basename = path.basename(filename, path.extname(filename));

  // Extract parts after problem ID
  const match = basename.match(/^p\d+_(.+)$/);
  if (!match) return "Diagram";

  const parts = match[1];

  // Check for known patterns
  for (const [pattern, caption] of Object.entries(CAPTION_PATTERNS)) {
    if (parts.includes(pattern)) {
      // If it's just the pattern, return the caption
      if (parts === pattern) return caption;

      // If pattern is at start (e.g., example1_reversed)
      if (parts.startsWith("example")) {
        const exampleMatch = parts.match(/example(\d+)/);
        if (exampleMatch) {
          const exNum = exampleMatch[1];
          const suffix = parts.replace(`example${exNum}`, "").replace(/^_/, "");
          if (suffix && CAPTION_PATTERNS[suffix]) {
            return `Example ${exNum} - ${CAPTION_PATTERNS[suffix]}`;
          }
          return `Example ${exNum}`;
        }
      }
      return caption;
    }
  }

  // Default: convert underscores to spaces and capitalize
  return parts.replace(/_/g, " ").replace(/\b\w/g, (l) => l.toUpperCase());
}

/**
 * Get all diagrams grouped by problem ID
 */
async function getDiagramsByProblem() {
  const mmdFiles = await glob(
    path.join(MERMAID_DIR, "*.mmd").replace(/\\/g, "/"),
  );
  const diagramsByProblem = new Map();

  for (const file of mmdFiles) {
    const filename = path.basename(file);
    const problemId = parseProblemId(filename);

    if (!problemId) continue;

    if (!diagramsByProblem.has(problemId)) {
      diagramsByProblem.set(problemId, []);
    }

    const basename = path.basename(file, ".mmd");
    const pngFile = `${basename}.png`;

    diagramsByProblem.get(problemId).push({
      id: basename.replace(`p${problemId}_`, ""),
      file: pngFile,
      caption: generateCaption(filename),
    });
  }

  // Sort diagrams within each problem
  for (const [problemId, diagrams] of diagramsByProblem) {
    diagrams.sort((a, b) => a.id.localeCompare(b.id));
  }

  return diagramsByProblem;
}

/**
 * Update a single problem JSON
 */
async function updateProblemJson(problemId, diagrams, dryRun = false) {
  const jsonPath = path.join(PROBLEMS_DIR, `p${problemId}.json`);

  if (!(await fs.pathExists(jsonPath))) {
    console.log(`  ⚠️  Problem file not found: p${problemId}.json`);
    return false;
  }

  try {
    const problem = await fs.readJson(jsonPath);

    // Check if diagrams already exist and are the same
    const existingDiagrams = JSON.stringify(problem.diagrams || []);
    const newDiagrams = JSON.stringify(diagrams);

    if (existingDiagrams === newDiagrams) {
      if (options.verbose) {
        console.log(`  ⏭️  No changes: p${problemId}.json`);
      }
      return false;
    }

    problem.diagrams = diagrams;

    if (dryRun) {
      console.log(`  📝 Would update: p${problemId}.json`);
      console.log(`     Diagrams: ${diagrams.map((d) => d.file).join(", ")}`);
    } else {
      await fs.writeJson(jsonPath, problem, { spaces: 2 });
      console.log(
        `  ✅ Updated: p${problemId}.json (${diagrams.length} diagram(s))`,
      );
    }

    return true;
  } catch (error) {
    console.error(`  ❌ Error updating p${problemId}.json: ${error.message}`);
    return false;
  }
}

/**
 * Main function
 */
async function main() {
  console.log("\n📝 AlgoCraft Problem JSON Updater\n");

  if (options.dry) {
    console.log("🔍 DRY RUN - No files will be modified\n");
  }

  const diagramsByProblem = await getDiagramsByProblem();

  console.log(`📊 Found diagrams for ${diagramsByProblem.size} problem(s):\n`);

  let updated = 0;
  let skipped = 0;

  for (const [problemId, diagrams] of diagramsByProblem) {
    const wasUpdated = await updateProblemJson(
      problemId,
      diagrams,
      options.dry,
    );
    if (wasUpdated) updated++;
    else skipped++;
  }

  console.log(`\n📈 Summary:`);
  console.log(`   Updated: ${updated}`);
  console.log(`   Skipped: ${skipped}`);
  console.log(`   Total: ${diagramsByProblem.size}\n`);
}

main().catch(console.error);
