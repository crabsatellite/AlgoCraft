// Headless English capture of the real production Web IDE for the promo video.
// Start `./mod-build.ps1 gradle runWebIdeReview` first and pass WEB_REVIEW_URL.
// No OS input, no clipboard, no visible browser; results come from the real Judge.
const { chromium } = require('../promo/video/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const out = path.resolve(__dirname, '../promo/video/public/cap');
fs.rmSync(out, {recursive: true, force: true});
for (const d of ['type', 'run', 'submit', 'search', 'browse']) fs.mkdirSync(path.join(out, d), {recursive: true});
const SOLUTION = [
  'class Solution {',
  '    public int[] twoSum(int[] nums, int target) {',
  '        Map<Integer, Integer> seen = new HashMap<>();',
  '        for (int i = 0; i < nums.length; i++) {',
  '            int need = target - nums[i];',
  '            if (seen.containsKey(need)) {',
  '                return new int[] { seen.get(need), i };',
  '            }',
  '            seen.put(nums[i], i);',
  '        }',
  '        return new int[0];',
  '    }',
  '}'].join('\n');
(async () => {
  const browser = await chromium.launch({headless: true, args: ['--mute-audio', '--disable-gpu', '--renderer-process-limit=2']});
  const context = await browser.newContext({viewport: {width: 1600, height: 900}, deviceScaleFactor: 1.2, locale: 'en-US'});
  const page = await context.newPage();
  const receipt = {editorDisplayPreferences: 'fontSize 18, no auto-close/indent, suggestion popups off (recording only)', headless: true, muted: true, systemClipboardUsed: false, viewport: '1600x900@1.2', fixture: 'WebIdeReviewServer + production Judge (fresh state)', frames: {}, errors: []};
  page.on('pageerror', e => receipt.errors.push(String(e)));
  page.on('framenavigated', f => { if (f === page.mainFrame()) console.log('NAV', f.url(), Date.now()); });
  page.on('console', m => { if (m.type() === 'error') console.log('CONSOLE', m.text().slice(0, 200)); });
  let n = {};
  const seq = async (dir, quality = 90) => {
    n[dir] = (n[dir] || 0);
    const p = path.join(out, dir, String(n[dir]++).padStart(4, '0') + '.jpg');
    await page.screenshot({path: p, type: 'jpeg', quality});
  };
  const shot = async name => page.screenshot({path: path.join(out, name + '.png')});
  try {
    await page.goto(process.argv[2], {waitUntil: 'networkidle', timeout: 60000});
    await page.waitForFunction(() => typeof editor !== 'undefined' && problems.length === 500);
    await page.locator('#lang-select').selectOption('en');
    await page.waitForFunction(() => i18n.currentLang === 'en');
    await page.locator('#problem-list > div').nth(0).click();
    await page.waitForFunction(() => currentProblem.id === '1');
    receipt.twoSumPassedBefore = await page.locator('#problem-passed').isVisible();
    await page.evaluate(() => { editor.setValue(currentProblem.initialCode); editor.updateOptions({autoClosingBrackets: 'never', autoClosingQuotes: 'never', autoIndent: 'none', formatOnType: false, fontSize: 18, cursorBlinking: 'solid', quickSuggestions: false, suggestOnTriggerCharacters: false, wordBasedSuggestions: 'off', parameterHints: {enabled: false}, hover: {enabled: false}, occurrencesHighlight: 'off'}); });
    await page.waitForTimeout(600);
    await shot('starter');
    await page.locator('#editor-container').click({position: {x: 190, y: 70}});
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Delete');
    await seq('type');
    for (let i = 0; i < SOLUTION.length; i += 2) {
      await page.keyboard.type(SOLUTION.slice(i, i + 2));
      await seq('type', 85);
    }
    assert.equal(await page.evaluate(() => editor.getValue()), SOLUTION);
    await page.waitForTimeout(300);
    await shot('code');
    // Run: visible examples only.
    await page.locator('#run-button').click();
    for (let i = 0; i < 40; i++) { await seq('run'); if (!(await page.evaluate(() => executionPending))) break; await page.waitForTimeout(120); }
    await page.waitForFunction(() => !executionPending, {timeout: 30000});
    for (let i = 0; i < 6; i++) { await page.waitForTimeout(100); await seq('run'); }
    receipt.runOutput = await page.locator('#output-container').innerText();
    assert.ok(receipt.runOutput.includes('PASS'));
    await shot('run-pass');
    // Submit: hidden tests, real verdict.
    await page.locator('#submit-button').click();
    for (let i = 0; i < 60; i++) { await seq('submit'); if (!(await page.evaluate(() => executionPending))) break; await page.waitForTimeout(120); }
    await page.waitForFunction(() => !executionPending, {timeout: 30000});
    for (let i = 0; i < 14; i++) { await page.waitForTimeout(80); await seq('submit'); }
    receipt.submitOutput = await page.locator('#output-container').innerText();
    receipt.modal = await page.locator('#success-modal').innerText();
    assert.ok(/Accepted/i.test(receipt.submitOutput + receipt.modal));
    await shot('accepted');
    await page.evaluate(() => closeModal());
    await page.waitForTimeout(700);
    await shot('passed-badge');
    console.log('history', Date.now()); await page.locator('button[onclick="showHistory()"]').click();
    await page.waitForTimeout(1200);
    await shot('history');
    await page.locator('#history-modal button[onclick="closeHistoryModal()"]').click();
    await page.waitForTimeout(400);
    // Search the bank.
    const search = page.locator('#sidebar-search');
    await search.click();
    for (const ch of 'graph') { await page.keyboard.type(ch); await page.waitForTimeout(60); await seq('search'); }
    await page.waitForTimeout(400); await seq('search');
    await search.fill(''); await page.waitForTimeout(300);
    // Browse problems with diagrams.
    const withVisuals = await page.evaluate(() => problems.map((p, i) => ({i, id: p.id, v: (p.visuals || []).length, d: p.difficulty})).filter(x => x.v > 0));
    receipt.problemsWithVisuals = withVisuals.length;
    receipt.visualIds = withVisuals.map(x => x.id);
    const step = Math.max(1, Math.floor(withVisuals.length / 12));
    const picks = withVisuals.filter((x, k) => k % step === 0).slice(0, 12).map(x => x.id);
    receipt.browse = [];
    for (const id of picks) {
      const idx = await page.evaluate(id => problems.findIndex(p => p.id === id), id);
      if (idx < 0) continue;
      await page.evaluate(i => loadProblem(i), idx);
      await page.waitForFunction(id => currentProblem.id === id, id);
      await page.waitForFunction(() => Array.from(document.querySelectorAll('#problem-visuals img')).every(i => i.complete && i.naturalWidth > 0));
      await page.waitForTimeout(500);
      await page.screenshot({path: path.join(out, 'browse', 'p' + id + '.png')});
      receipt.browse.push({id, title: await page.locator('#problem-title').innerText(), visuals: await page.locator('#problem-visuals img').count()});
    }
    // Bilingual beat.
    await page.locator('#lang-select').selectOption('zh');
    await page.waitForFunction(() => i18n.currentLang === 'zh');
    await page.waitForTimeout(600);
    await shot('zh');
    // Titles for the "500 problems" wall.
    await page.locator('#lang-select').selectOption('en');
    receipt.titles = await page.evaluate(() => problems.map(p => ({id: p.id, t: p.title, d: p.difficulty})));
    receipt.frames = n;
    assert.equal(receipt.errors.length, 0);
  } finally {
    await context.close(); await browser.close();
    fs.mkdirSync(path.join(root, 'build/promo/evidence'), { recursive: true });
    fs.writeFileSync(path.join(root, 'build/promo/evidence/capture-receipt.json'), JSON.stringify(receipt, null, 2));
  }
})().catch(e => { console.error(e); process.exitCode = 1; });



