// Headless render driver: node render.mjs stills <name:frame,...> | icon | hero | video
import {bundle} from '@remotion/bundler';
import {renderStill, renderMedia, selectComposition} from '@remotion/renderer';
import path from 'node:path';
import fs from 'node:fs';
import os from 'node:os';
import {fileURLToPath} from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const browserExecutable = process.env.PROMO_CHROME_PATH || path.join(os.homedir(), 'AppData/Local/ms-playwright/chromium_headless_shell-1208/chrome-headless-shell-win64/chrome-headless-shell.exe');
const chromiumOptions = {gl: 'angle', headless: true};
const [, , mode, arg] = process.argv;
fs.mkdirSync(path.join(here, 'out'), {recursive: true});
if (mode === 'srt') {
  const {build} = await import('esbuild');
  const res = await build({entryPoints: [path.join(here, 'src/captions.ts')], bundle: true, platform: 'node', format: 'esm', write: false, external: ['react', 'react-dom']});
  const tmp = path.join(here, 'out/captions.mjs'); fs.writeFileSync(tmp, res.outputFiles[0].text);
  const {CAPTIONS} = await import('file:///' + tmp.replace(/\\/g, '/'));
  const ts = fr => { const ms = Math.round(fr / 30 * 1000); const p = (n, w = 2) => String(n).padStart(w, '0'); return `${p(Math.floor(ms / 3600000))}:${p(Math.floor(ms / 60000) % 60)}:${p(Math.floor(ms / 1000) % 60)},${p(ms % 1000, 3)}`; };
  fs.writeFileSync(path.join(here, 'out/algocraft-trailer-en.srt'), CAPTIONS.map(([a, b, s], i) => `${i + 1}\n${ts(a)} --> ${ts(b - 1)}\n${s}\n`).join('\n'), 'utf8');
  console.log('srt', CAPTIONS.length); process.exit(0);
}
if (!fs.existsSync(browserExecutable)) throw new Error('Chromium headless shell is missing. Set PROMO_CHROME_PATH; a visible browser is never used.');
const serveUrl = await bundle({entryPoint: path.join(here, 'src/index.ts'), publicDir: path.join(here, 'public'), onProgress: () => {}});
const comp = async id => selectComposition({serveUrl, id, browserExecutable, chromiumOptions});
const still = async (id, frame, output, imageFormat = 'png') => {
  const composition = await comp(id);
  fs.mkdirSync(path.dirname(output), {recursive: true});
  await renderStill({composition, serveUrl, output, frame, browserExecutable, chromiumOptions, imageFormat, ...(imageFormat === 'jpeg' ? {jpegQuality: 92} : {})});
  console.log('still', id, frame, output);
};
if (mode === 'icon') await still('ComputerIcon', 0, path.join(here, 'public/fx/computer-icon.png'));
if (mode === 'hero') { await still('Hero', 0, path.join(here, 'out/hero.png')); await still('Poster', 0, path.join(here, 'out/poster.png')); }
if (mode === 'stills') for (const item of arg.split(',')) { const [name, fr] = item.split(':'); await still('TrailerSilent', Number(fr), path.join(here, 'out/stills', `${name}.png`)); }
if (mode === 'video') {
  const composition = await comp('Trailer');
  const t = Date.now();
  await renderMedia({composition, serveUrl, codec: 'h264', outputLocation: path.join(here, 'out/trailer-raw.mp4'), browserExecutable, chromiumOptions,
    concurrency: Number(arg || 3), crf: 20, pixelFormat: 'yuv420p', colorSpace: 'bt709', audioCodec: 'aac', audioBitrate: '320k', x264Preset: 'slow',
    onProgress: ({progress}) => { const p = Math.round(progress * 100); if (p % 5 === 0 && p !== globalThis.__last) { globalThis.__last = p; console.log('progress', p, Math.round((Date.now() - t) / 1000) + 's'); } }});
  console.log('video done', (Date.now() - t) / 1000);
}



