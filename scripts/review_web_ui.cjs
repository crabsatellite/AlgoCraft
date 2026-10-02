// npm install --prefix build/review/browser playwright@1.58.2 --no-audit --no-fund
// Start: ./mod-build.ps1 gradle runWebIdeReview
// Run: node scripts/review_web_ui.cjs http://127.0.0.1:<printed port>
const { chromium } = require('../build/review/browser/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const output = path.join(root, 'build/review/web');
fs.mkdirSync(output, { recursive: true });

(async () => {
  const evidence = {
    startedAtUtc: new Date().toISOString(),
    headless: true,
    systemClipboardUsed: false,
    manifestSha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(root, 'question_bank/official/manifest.json'))).digest('hex'),
    scriptSha256: crypto.createHash('sha256').update(fs.readFileSync(__filename)).digest('hex'),
  };
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = [];
  const checks = [];
  const captures = [];
  page.on('pageerror', error => errors.push(error.stack || error.message));
  const check = (condition, label) => { assert.ok(condition, label); checks.push(label); };
  const code = () => page.evaluate(() => editor.getValue());
  const choose = async id => {
    await page.locator('#problem-list > div').nth(Number(id) - 1).click();
    await page.waitForFunction(id => currentProblem.id === id, id);
  };
  const paste = async text => {
    await page.locator('#editor-container').click({ position: { x: 160, y: 80 } });
    await page.keyboard.press('Control+A');
    // Use Monaco's paste action without accessing the shared desktop clipboard.
    await page.evaluate(text => editor.trigger('review', 'paste', { text }), text);
    await page.waitForFunction(text => editor.getValue() === text, text, { timeout: 5000 });
    assert.equal(await code(), text, 'Monaco paste action preserves Java code without the desktop clipboard');
    checks.push('Monaco paste action preserves Java code without the desktop clipboard');
  };
  try {
    await page.goto(process.argv[2], { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForFunction(() => typeof editor !== 'undefined' && problems.length === 500, { timeout: 60000 });
    check(await page.locator('#problem-list > div').count() === 500, 'Complete bank is accessible in the browser');
    const caretDraft = 'class Solution {\n    // caret test\n}\n';
    await paste(caretDraft);
    await page.keyboard.press('Control+Home');
    await page.keyboard.press('ArrowRight');
    await page.keyboard.press('ArrowRight');
    await page.keyboard.type('x');
    check(await code() === 'clx' + caretDraft.slice(2), 'Typing follows the middle caret instead of appending at end');
    await page.keyboard.press('Control+z');
    check(await code() === caretDraft, 'Undo restores a mid-line insertion');
    await page.keyboard.press('Control+y');
    await page.keyboard.press('Backspace');
    check(await code() === caretDraft, 'Redo and Backspace preserve the mid-line caret');
    await page.keyboard.press('Delete');
    check(await code() === caretDraft.slice(0, 2) + caretDraft.slice(3), 'Delete removes the character after the middle caret');
    await page.keyboard.press('Control+z');
    const clickedCaret = await page.evaluate(() => {
      const p = editor.getScrolledVisiblePosition({ lineNumber: 1, column: 7 });
      const rect = editor.getDomNode().getBoundingClientRect();
      return { x: rect.left + p.left, y: rect.top + p.top + p.height / 2 };
    });
    await page.mouse.click(clickedCaret.x, clickedCaret.y);
    await page.keyboard.type('x');
    check(await code() === caretDraft.slice(0, 6) + 'x' + caretDraft.slice(6), 'Clicking a rendered glyph boundary positions the typing caret');
    await page.keyboard.press('Backspace');
    check(await code() === caretDraft, 'Clicked-caret editing restores original text');
    const longLine = '// ' + 'abcdefghij'.repeat(160);
    await paste(longLine);
    await page.keyboard.press('End');
    await page.keyboard.press('ArrowLeft');
    await page.keyboard.type('x');
    check(await code() === longLine.slice(0, -1) + 'x' + longLine.slice(-1), 'Horizontal scrolling retains the caret insertion position');
    await page.waitForFunction(() => {
      const cursor = document.querySelector('.cursors-layer .cursor');
      const expected = editor.getScrolledVisiblePosition(editor.getPosition());
      return cursor && expected && Math.abs(cursor.getBoundingClientRect().left
        - editor.getDomNode().getBoundingClientRect().left - expected.left) < 2;
    });
    check(true, 'Rendered Monaco caret matches the logical caret after horizontal scrolling');
    const draft = 'class Solution {\n    // independent draft 中文\n}\n';
    await paste(draft);
    await choose('2');
    await choose('1');
    check(await code() === draft, 'Switching problems restores each independent draft');
    await page.locator('#lang-select').selectOption('zh');
    await page.waitForFunction(() => currentProblem.title === '两数之和');
    check(await code() === draft, 'Language change preserves selected problem and code');
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForFunction(() => typeof editor !== 'undefined' && currentProblem);
    check(await code() === draft, 'Browser reload restores saved draft');

    for (const id of ['1', '7', '73', '95', '500']) {
      await choose(id);
      const problem = JSON.parse(fs.readFileSync(path.join(root, `question_bank/official/p${id}.json`), 'utf8'));
      await paste(problem.solutions[0].code.replace(/\r\n?/g, '\n'));
      await page.locator('#run-button').click();
      await page.waitForFunction(() => !executionPending, { timeout: 30000 });
      check((await page.locator('#output-container').innerText()).includes('PASS'), `p${id} Run passes real examples`);
      await page.locator('#submit-button').click();
      await page.waitForFunction(() => !executionPending, { timeout: 30000 });
      check((await page.locator('#output-container').innerText()).includes('通过') || (await page.locator('#output-container').innerText()).includes('Accepted'), `p${id} Submit passes production Judge`);
      check(await code() === problem.solutions[0].code.replace(/\r\n?/g, '\n'), `p${id} Run and Submit preserve code`);
      await page.evaluate(() => closeModal());
    }
    await choose('1');
    await paste('class Solution { public int[] twoSum(int[] n, int t) { return new int[]{0,0}; } }');
    await page.locator('#submit-button').click();
    await page.waitForFunction(() => !executionPending, { timeout: 30000 });
    check((await page.locator('#output-container').innerText()).includes('Wrong Answer'), 'Wrong answers expose rejection and failed case details');
    await paste('class Solution { broken }');
    await page.locator('#submit-button').click();
    await page.waitForFunction(() => !executionPending, { timeout: 30000 });
    check((await page.locator('#output-container').innerText()).includes('Compilation Error'), 'Compilation errors remain readable without a page crash');

    // Hold a real HTTP response to observe pending clicks and problem-switch races.
    await choose('7');
    const valid = JSON.parse(fs.readFileSync(path.join(root, 'question_bank/official/p7.json'), 'utf8')).solutions[0].code;
    await paste(valid);
    let releaseResponse;
    let requestCount = 0;
    const held = new Promise(resolve => { releaseResponse = resolve; });
    await page.route('**/api/run', async route => {
      requestCount++;
      const response = await route.fetch();
      await held;
      await route.fulfill({ response });
    });
    await page.locator('#run-button').click();
    check(await page.locator('#submit-button').isDisabled(), 'Pending Run disables concurrent Submit');
    await page.evaluate(() => { runCode(); submitCode(); });
    await choose('2');
    releaseResponse();
    await page.waitForFunction(() => !executionPending, { timeout: 30000 });
    check(requestCount === 1, 'Repeated pending Run and Submit start only one request');
    check(!(await page.locator('#output-container').innerText()).includes('PASS'), 'Late Run response does not overwrite another problem console');
    await page.unroute('**/api/run');

    for (const width of [1440, 1024, 900, 640, 360, 320]) {
      await page.setViewportSize({ width, height: 800 });
      await choose('7');
      await page.waitForFunction(() => Array.from(document.querySelectorAll('#problem-visuals img')).every(img => img.complete && img.naturalWidth > 0));
      const overflow = await page.evaluate(() => ({
        viewport: innerWidth,
        document: document.documentElement.scrollWidth,
        controls: ['run-button', 'submit-button', 'lang-select', 'repo-select'].map(id => {
          const r = document.getElementById(id).getBoundingClientRect();
          return { id, x: r.x, right: r.right, width: r.width };
        })
      }));
      check(overflow.document <= width + 1, `${width}px viewport has no horizontal page overflow`);
      check(overflow.controls.every(c => c.x >= 0 && c.right <= width + 1 && c.width > 0), `${width}px action controls stay inside the viewport`);
      const file = path.join(output, `web-${width}.png`);
      await page.screenshot({ path: file, fullPage: true });
      captures.push(file);
    }
    // Use the live renderer for every statement, tag, example, and diagram at the narrowest viewport.
    for (const language of ['en', 'zh']) {
      await page.locator('#lang-select').selectOption(language);
      await page.waitForFunction(lang => i18n.currentLang === lang && problems.length === 500, language);
      await page.waitForTimeout(500);
      const failures = await page.evaluate(async () => {
        const failures = [];
        for (let i = 0; i < problems.length; i++) {
          loadProblem(i);
          await new Promise(requestAnimationFrame);
          await Promise.all(Array.from(document.querySelectorAll('#problem-visuals img')).map(img => img.decode().catch(() => failures.push(currentProblem.id + ': missing image'))));
          const panel = document.getElementById('statement-scroll');
          if (panel.scrollWidth > panel.clientWidth + 1) failures.push(currentProblem.id + ': statement overflow');
          if (document.documentElement.scrollWidth > innerWidth + 1) failures.push(currentProblem.id + ': page overflow');
        }
        return failures;
      });
      check(failures.length === 0, `${language} all 500 statements, examples, and images render without overflow: ${failures.slice(0, 8)}`);
    }
    // Review repaired pictures in the real statement layout as well as the
    // all-bank asset/overflow pass above. Local images are never OS windows.
    await page.locator('#lang-select').selectOption('zh');
    await page.waitForLoadState('networkidle');
    for (const width of [900, 360]) {
      await page.setViewportSize({ width, height: 900 });
      for (const id of ['87', '105', '159', '163', '331', '408', '412', '495', '496']) {
        await choose(id);
        await page.waitForFunction(() => Array.from(document.querySelectorAll('#problem-visuals img')).every(img => img.complete && img.naturalWidth > 0));
        const file = path.join(output, `editorial-p${id}-${width}.png`);
        await page.locator('#problem-visuals').screenshot({ path: file });
        captures.push(file);
      }
    }
    check(errors.length === 0, `No browser JavaScript errors: ${errors}`);
    fs.writeFileSync(path.join(output, 'result.json'), JSON.stringify({ status: 'passed', ...evidence, finishedAtUtc: new Date().toISOString(), renderedStatements: 1000, checks, captures, errors }, null, 2));
    console.log(JSON.stringify({ status: 'passed', checks: checks.length, captures: captures.length }));
  } catch (error) {
    await page.screenshot({ path: path.join(output, 'failure.png'), fullPage: true }).catch(() => {});
    fs.writeFileSync(path.join(output, 'result.json'), JSON.stringify({ status: 'failed', error: String(error), checks, errors }, null, 2));
    throw error;
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
