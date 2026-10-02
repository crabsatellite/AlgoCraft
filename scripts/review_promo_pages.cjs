// Headless review of README and CurseForge previews at desktop and phone widths.
// No visible browser, muted, no clipboard; fails on horizontal overflow or any broken image.
const {chromium} = require('../promo/video/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const assert = require('node:assert/strict');
const dir = path.resolve(__dirname, '../build/promo/pages');
const files = ['readme-en.html', 'readme-zh-CN.html', 'curseforge-en.html', 'curseforge-zh-CN.html'];
(async () => {
  const browser = await chromium.launch({headless: true, args: ['--mute-audio', '--disable-gpu', '--renderer-process-limit=2']});
  const page = await browser.newPage();
  const results = [], errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  try {
    for (const file of files) for (const width of [1280, 360]) {
      await page.setViewportSize({width, height: 900});
      await page.goto(pathToFileURL(path.join(dir, file)).href, {waitUntil: 'load'});
      await page.waitForFunction(() => Array.from(document.images).every(i => i.complete));
      const c = await page.evaluate(() => ({scroll: document.documentElement.scrollWidth, images: Array.from(document.images).map(i => ({src: decodeURI(i.src).split('/').pop(), ok: i.naturalWidth > 0, w: i.getBoundingClientRect().width}))}));
      assert.ok(c.scroll <= width + 1, `${file}@${width} overflows: ${c.scroll}`);
      assert.ok(c.images.length >= 14, `${file} has ${c.images.length} images`);
      for (const im of c.images) assert.ok(im.ok, `${file}@${width}: ${im.src} failed to load`);
      await page.screenshot({path: path.join(dir, file.replace('.html', `-${width}.png`)), fullPage: true});
      results.push({file, width, scrollWidth: c.scroll, images: c.images.length});
    }
    assert.equal(errors.length, 0);
  } finally {
    await browser.close();
    fs.writeFileSync(path.join(dir, 'review-receipt.json'), JSON.stringify({headless: true, muted: true, results, errors}, null, 2));
  }
  console.log(JSON.stringify(results));
})().catch(e => { console.error(e); process.exitCode = 1; });
