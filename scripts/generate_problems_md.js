const fs = require('fs');
const path = require('path');

const dir = path.join(__dirname, '..', 'question_bank', 'official');
const problems = [];

// Read all problem files
for (let i = 1; i <= 500; i++) {
  const file = path.join(dir, `p${i}.json`);
  try {
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    problems.push({
      id: i,
      title: data.title,
      difficulty: data.difficulty,
      tags: data.tags || []
    });
  } catch (e) {
    problems.push({ id: i, title: `[ERROR: ${e.message}]`, difficulty: '?', tags: [] });
  }
}

// Group by category ranges
const sections = [
  { name: 'Array & Hashing', range: [1, 20] },
  { name: 'Two Pointers', range: [21, 30] },
  { name: 'Sliding Window', range: [31, 40] },
  { name: 'Stack', range: [41, 50] },
  { name: 'Binary Search', range: [51, 64] },
  { name: 'Linked List', range: [65, 80] },
  { name: 'Trees', range: [81, 100] },
  { name: 'Tries', range: [101, 105] },
  { name: 'Heap / Priority Queue', range: [106, 115] },
  { name: 'Backtracking', range: [116, 127] },
  { name: 'Graphs', range: [128, 146] },
  { name: 'Advanced Graphs', range: [147, 150] },
  { name: '1D Dynamic Programming', range: [151, 162] },
  { name: '2D Dynamic Programming', range: [163, 173] },
  { name: 'Greedy', range: [174, 181] },
  { name: 'Intervals', range: [182, 187] },
  { name: 'Math & Geometry', range: [188, 195] },
  { name: 'Bit Manipulation', range: [196, 202] },
  { name: 'Additional Classics (203-300)', range: [203, 300] },
  { name: 'Hard & Challenge Problems (301-400)', range: [301, 400] },
  { name: 'Miscellaneous & New Classics (401-500)', range: [401, 500] }
];

let md = '# Top 500 Algorithm Problems\n\n';
md += 'This list contains the most important algorithm problems for the AlgoCraft question bank.\n\n';

for (const section of sections) {
  md += `## ${section.name}\n\n`;
  for (let i = section.range[0]; i <= section.range[1]; i++) {
    const p = problems[i - 1];
    if (p) {
      md += `${p.id}. ${p.title}\n`;
    }
  }
  md += '\n';
}

const outFile = path.join(__dirname, '..', 'question_bank', 'top_500_problems.md');
fs.writeFileSync(outFile, md, 'utf8');
console.log('Generated top_500_problems.md successfully!');
console.log(`Total problems: ${problems.length}`);

// Check for remaining duplicates
const titleMap = new Map();
const dupes = [];
for (const p of problems) {
  if (titleMap.has(p.title)) {
    dupes.push(`P${titleMap.get(p.title)} & P${p.id}: "${p.title}"`);
  } else {
    titleMap.set(p.title, p.id);
  }
}
if (dupes.length > 0) {
  console.log(`\nWARNING: ${dupes.length} remaining duplicates:`);
  dupes.forEach(d => console.log(`  ${d}`));
} else {
  console.log('\nNo duplicates found!');
}
