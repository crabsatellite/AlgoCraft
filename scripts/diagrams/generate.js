#!/usr/bin/env node

/**
 * AlgoCraft Mermaid Diagram Generator
 *
 * Converts Mermaid (.mmd) files to PNG images for algorithm problem visualization.
 *
 * Usage:
 *   node generate.js              # Generate all diagrams
 *   node generate.js --problem 94 # Generate diagrams for problem 94
 *   node generate.js --watch      # Watch mode
 *   node generate.js --clean      # Remove all generated images
 */

const { execSync, spawn } = require("child_process");
const fs = require("fs-extra");
const path = require("path");
const { glob } = require("glob");
const { program } = require("commander");

// Paths
const SCRIPT_DIR = __dirname;
const MERMAID_DIR = path.join(SCRIPT_DIR, "mermaid");
const OUTPUT_DIR = path.resolve(
  SCRIPT_DIR,
  "../../question_bank/official/images",
);
const PROBLEMS_DIR = path.resolve(SCRIPT_DIR, "../../question_bank/official");

// Mermaid CLI config
const MERMAID_CONFIG = {
  theme: "dark",
  backgroundColor: "transparent",
  width: 800,
  height: 600,
  scale: 2, // High DPI for Minecraft GUI
};

// Custom theme for algorithm diagrams
const MERMAID_THEME_CONFIG = `{
  "theme": "dark",
  "themeVariables": {
    "primaryColor": "#4a9eff",
    "primaryTextColor": "#ffffff",
    "primaryBorderColor": "#2a6ecf",
    "lineColor": "#888888",
    "secondaryColor": "#2d2d2d",
    "tertiaryColor": "#1a1a1a",
    "background": "transparent",
    "mainBkg": "#2d2d2d",
    "nodeBorder": "#4a9eff",
    "clusterBkg": "#1a1a1a",
    "clusterBorder": "#4a9eff",
    "titleColor": "#ffffff",
    "edgeLabelBackground": "#1a1a1a"
  }
}`;

program
  .option("-p, --problem <id>", "Generate diagrams for specific problem")
  .option("-a, --all", "Generate all diagrams")
  .option("-w, --watch", "Watch mode - regenerate on file change")
  .option("-c, --clean", "Remove all generated images")
  .option("-v, --verbose", "Verbose output")
  .parse();

const options = program.opts();

/**
 * Ensure output directory exists
 */
async function ensureOutputDir() {
  await fs.ensureDir(OUTPUT_DIR);
  console.log(`📁 Output directory: ${OUTPUT_DIR}`);
}

/**
 * Write Mermaid config file
 */
async function writeMermaidConfig() {
  const configPath = path.join(SCRIPT_DIR, "mermaid-config.json");
  await fs.writeFile(configPath, MERMAID_THEME_CONFIG);
  return configPath;
}

/**
 * Get all Mermaid files
 */
async function getMermaidFiles(problemId = null) {
  const pattern = problemId
    ? path.join(MERMAID_DIR, `p${problemId}_*.mmd`)
    : path.join(MERMAID_DIR, "*.mmd");

  const files = await glob(pattern.replace(/\\/g, "/"));
  return files;
}

/**
 * Generate PNG from Mermaid file
 */
async function generateDiagram(mmdFile, configPath) {
  const basename = path.basename(mmdFile, ".mmd");
  const outputFile = path.join(OUTPUT_DIR, `${basename}.png`);

  try {
    // Use mermaid-cli (mmdc)
    const mmdc = path.join(SCRIPT_DIR, "node_modules", ".bin", "mmdc");
    const cmd = `"${mmdc}" -i "${mmdFile}" -o "${outputFile}" -c "${configPath}" -b transparent -w ${MERMAID_CONFIG.width} -H ${MERMAID_CONFIG.height} -s ${MERMAID_CONFIG.scale}`;

    if (options.verbose) {
      console.log(`  Running: ${cmd}`);
    }

    execSync(cmd, {
      stdio: options.verbose ? "inherit" : "pipe",
      shell: true,
    });

    console.log(`  ✅ Generated: ${basename}.png`);
    return { success: true, file: outputFile };
  } catch (error) {
    console.error(`  ❌ Failed: ${basename} - ${error.message}`);
    return { success: false, error: error.message };
  }
}

/**
 * Generate all diagrams
 */
async function generateAll(problemId = null) {
  console.log("\n🎨 AlgoCraft Diagram Generator\n");

  await ensureOutputDir();
  const configPath = await writeMermaidConfig();

  const files = await getMermaidFiles(problemId);

  if (files.length === 0) {
    console.log("📭 No Mermaid files found.");
    if (problemId) {
      console.log(`   Looking for: p${problemId}_*.mmd`);
    }
    console.log(`   Directory: ${MERMAID_DIR}\n`);
    return;
  }

  console.log(`📊 Found ${files.length} diagram(s) to generate:\n`);

  let success = 0;
  let failed = 0;

  for (const file of files) {
    const result = await generateDiagram(file, configPath);
    if (result.success) success++;
    else failed++;
  }

  console.log(`\n📈 Results: ${success} success, ${failed} failed\n`);

  // Cleanup config
  await fs.remove(configPath);
}

/**
 * Watch mode
 */
async function watchMode() {
  const chokidar = require("chokidar");

  console.log("\n👀 Watch mode started. Waiting for changes...\n");
  console.log(`   Watching: ${MERMAID_DIR}\n`);

  const watcher = chokidar.watch(path.join(MERMAID_DIR, "*.mmd"), {
    persistent: true,
    ignoreInitial: true,
  });

  const configPath = await writeMermaidConfig();
  await ensureOutputDir();

  watcher.on("add", async (filePath) => {
    console.log(`\n📝 New file: ${path.basename(filePath)}`);
    await generateDiagram(filePath, configPath);
  });

  watcher.on("change", async (filePath) => {
    console.log(`\n✏️  Changed: ${path.basename(filePath)}`);
    await generateDiagram(filePath, configPath);
  });

  watcher.on("unlink", (filePath) => {
    const basename = path.basename(filePath, ".mmd");
    const pngFile = path.join(OUTPUT_DIR, `${basename}.png`);
    if (fs.existsSync(pngFile)) {
      fs.removeSync(pngFile);
      console.log(`\n🗑️  Removed: ${basename}.png`);
    }
  });

  // Keep process running
  process.on("SIGINT", () => {
    console.log("\n\n👋 Watch mode stopped.\n");
    watcher.close();
    process.exit(0);
  });
}

/**
 * Clean all generated images
 */
async function cleanImages() {
  console.log("\n🧹 Cleaning generated images...\n");

  if (await fs.pathExists(OUTPUT_DIR)) {
    const files = await glob(
      path.join(OUTPUT_DIR, "*.png").replace(/\\/g, "/"),
    );
    for (const file of files) {
      await fs.remove(file);
      console.log(`  🗑️  Removed: ${path.basename(file)}`);
    }
    console.log(`\n✅ Cleaned ${files.length} file(s)\n`);
  } else {
    console.log("📭 No images directory found.\n");
  }
}

/**
 * Update problem JSON with diagram references
 */
async function updateProblemJson(problemId, diagrams) {
  const jsonPath = path.join(PROBLEMS_DIR, `p${problemId}.json`);

  if (!(await fs.pathExists(jsonPath))) {
    console.log(`  ⚠️  Problem file not found: p${problemId}.json`);
    return;
  }

  const problem = await fs.readJson(jsonPath);
  problem.diagrams = diagrams;
  await fs.writeJson(jsonPath, problem, { spaces: 2 });
  console.log(`  📝 Updated: p${problemId}.json`);
}

// Main execution
async function main() {
  try {
    if (options.clean) {
      await cleanImages();
    } else if (options.watch) {
      await watchMode();
    } else if (options.problem) {
      await generateAll(options.problem);
    } else {
      await generateAll();
    }
  } catch (error) {
    console.error("❌ Error:", error.message);
    if (options.verbose) {
      console.error(error.stack);
    }
    process.exit(1);
  }
}

main();
