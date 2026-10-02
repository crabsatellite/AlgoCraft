# AlgoCraft English trailer project

Editable Remotion/React/Three.js source for the 127-second, 1080p30 AlgoCraft trailer. Titles and promotional artwork are English. There is no spoken narration. Source scenes, timeline and sound cues live in `src/`; captures, models, textures, fonts via npm, music and effects are preserved as inputs.

## Render silently

Requires Node.js, FFmpeg/ffprobe on PATH and a Chromium **headless shell**. On this Windows workspace the driver uses the existing Playwright headless-shell installation. Set `PROMO_CHROME_PATH` to another headless-shell executable when moving the project. A missing executable causes a failure; there is no visible browser fallback.

```powershell
npm ci
npx playwright install chromium --only-shell
python -m pip install -r requirements.txt
node render.mjs hero
node render.mjs srt
node render.mjs video 3
node render.mjs stills "feature-computer:300,feature-accepted:1262,feature-rewards:1384,feature-bank:1579,feature-diagrams:1690,feature-daily:1975,feature-milestones:2200,feature-trophies:2566,feature-achievements:2800,feature-server:3150"
```

The driver writes intermediates into ignored `out/`. It uses three frame workers when called with `video 3`. It does not open a window or play sound. Generate the `ComputerIcon` only after rebuilding the computer model: `node render.mjs icon`.

From the repository root, run `python -X utf8 scripts/export_promo_media.py` to export the finished MP4, SRT, poster, hero and feature images to `docs/media/`. Then generate the store copy, review it, package it and verify the final delivery as described in [PROMO_MEDIA.md](../../docs/PROMO_MEDIA.md).

The included inputs render without starting Minecraft or a JVM fixture. Optional input refresh scripts are `scripts/prepare_promo_assets.py` and `scripts/capture_promo_web.cjs`. Refreshing them needs the sealed native screenshots, vanilla client JAR, original sound-pack downloads and, for Web capture, the guarded isolated JVM fixture. Preserve the pre-existing fixture state before recapturing it.

The visual style, fonts and beat timing are in `src/theme.ts`; the 24-cue English subtitle track is in `src/captions.ts`. Model geometry is flattened from the mod's actual JSON models. `src/Model3D.tsx` rebuilds the cuboids and UVs, then waits for materials before capturing a frame.

See [asset credits](CREDITS.md) and [footage provenance](../../docs/PROMO_MEDIA.md#footage-provenance) before republishing the project or video.
