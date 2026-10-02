# AlgoCraft promotional media

The [repository README](../README.md) introduces AlgoCraft as **learning algorithms in Minecraft**. It follows a player's first session: craft a computer, open the IDE, write Java, Run, Submit, collect rewards and return tomorrow. The [Chinese README](README.zh-CN.md) uses the same English artwork.

## Finished trailer

[Watch or download the English trailer](media/algocraft-trailer-en-1080p.mp4) · [English subtitles](media/algocraft-trailer-en.srt) · [Poster](media/trailer-poster.jpg)

**2:07 · 1920 × 1080 · 30 FPS · H.264/AAC · English titles and captions · music and sound effects · no narration.** The MP4 uses a web-friendly fast-start layout and stereo audio normalized to approximately −16 LUFS. All finished media live in `docs/media/`, outside ignored build output.

The art direction uses a dark navy stage, emerald code accents and gold achievement accents. Space Grotesk carries the large titles, Inter the explanations, JetBrains Mono the code labels, and Press Start 2P the small voxel accents. Camera moves, pixel wipes, impact bursts, sound cues and 3D model turntables follow the music. The computer and trophies use the repository's actual Minecraft model geometry and textures.

| Time | What the viewer sees |
| --- | --- |
| 0:00–0:18 | Learning hook, craftable computer, Minecraft world and AlgoCraft identity |
| 0:18–0:37 | In-game IDE, then a real Java Two Sum solution being typed in the Web IDE |
| 0:37–0:43 | Run passes both examples; Submit returns Accepted, 7/7 |
| 0:43–0:59 | First-clear loot, submission history, 500 official problems, difficulty split and diagrams |
| 0:59–1:18 | Daily practice streak, weekly rewards and solve milestones up to a dragon egg |
| 1:18–1:34 | Five trophy tiers, engraving and 23 achievement variants |
| 1:34–1:45 | Shared server banks, server judging and private player imports |
| 1:45–2:07 | Feature recap and closing identity |

Supported builds: **Forge 1.20.1 / Java 17** and **NeoForge 1.21.1 / Java 21**, both at **0.1.0-beta**, with the same official bank and rewards.

## Footage provenance

The native footage shows **NeoForge 1.21.1**. Minecraft images come from the previously sealed hidden-client run at `build/review/server-banks-2026-10-01/stress/`. Preserved inputs are in [the video project](../promo/video/README.md). The video pans and crops these genuine screenshots; its game-world shots are still-image moves, not a continuous gameplay recording.

The Web IDE sequence was captured headlessly from the production HTML, Monaco editor, HTTP handlers and production Judge through the isolated `WebIdeReviewServer` JVM fixture. The capture records starter code, typing, two real Run PASS results, a real Submit Accepted **7/7**, and submission history. The original capture receipt is preserved at [capture-receipt.json](../promo/video/public/cap/capture-receipt.json). It is a local demonstration fixture; it does not show a connected Minecraft server delivering loot.

Calendar tiles, loot cards, milestone tracks, server diagrams and trophy engraving cards are editorial explanations. The mod rewards daily practice and streaks; it has no separate daily quest menu. Engraving metadata “Alex / 2026-10-01” is an example of the trophy fields. The dragon egg silhouette and enchanted apple glint are editorial icons based on the vanilla textures. Reward claims assume `enableRewards=true`. The brief English/Chinese wipe demonstrates the existing bilingual IDE; other title cards, diagrams and exported promotional artwork use English.

## CurseForge page copy

Prepared descriptions: [English Markdown](CURSEFORGE.en.md), [English HTML](CURSEFORGE.en.html), [Chinese Markdown](CURSEFORGE.zh-CN.md), [Chinese HTML](CURSEFORGE.zh-CN.html).

Suggested English short summary:

> Learn algorithms in Minecraft. Write Java at your in-game computer, solve 500 bundled problems, and earn survival rewards and collectible trophies.

The HTML files are description bodies ready to paste into an editor. Their image URLs point to `docs/media/` on the repository's GitHub `main` branch. Those URLs become usable after these files are published there. Local previews resolve them to the finished files for review. The poster currently links to the GitHub MP4 file page; replace that link with the chosen public video-host URL when publishing. No upload or publication was performed.

## Production workflow and research

The trailer follows the player loop before introducing breadth and long-term goals. This ordering draws on [Derek Lieu's guide to choosing and ordering game-trailer content](https://www.derek-lieu.com/blog/2023/7/16/how-to-decide-what-to-show-in-a-game-trailer-and-in-what-order-to-put-it). We used [Remotion's programmatic rendering API](https://www.remotion.dev/docs/renderer/render-media) for frame-timed typography and effects, Three.js for actual voxel models, and FFmpeg for audio normalization, encoding and delivery checks.

Rebuild with the preserved inputs:

```powershell
cd promo/video
npm ci
npx playwright install chromium --only-shell
python -m pip install -r requirements.txt
node render.mjs hero
node render.mjs srt
node render.mjs video 3
node render.mjs stills "feature-computer:300,feature-accepted:1262,feature-rewards:1384,feature-bank:1579,feature-diagrams:1690,feature-daily:1975,feature-milestones:2200,feature-trophies:2566,feature-achievements:2800,feature-server:3150"
cd ../..
python -X utf8 scripts/export_promo_media.py
python -X utf8 scripts/prepare_promo_pages.py
node scripts/review_promo_pages.cjs
python -X utf8 scripts/verify_promo_delivery.py --media-only
python -X utf8 scripts/package_promo.py
python -X utf8 scripts/verify_promo_delivery.py
```

The video project preserves source, dependency lockfile, captures, music and sound-effect inputs. `node_modules/` and intermediate `out/` are ignored. Chromium runs headlessly; FFmpeg subprocesses create no windows. Rendering never opens a media player or plays audio on the computer. No OS mouse or clipboard is used.

Recapturing the Web sequence is optional. Use the repository's guarded `./mod-build.ps1 gradle runWebIdeReview --no-daemon --console=plain`, then `node scripts/capture_promo_web.cjs <printed-local-URL>`. Back up and restore `build/review/web-state` because the fixture stores its submission history there. Stop only the exact fixture process you started. The delivered capture was taken from fresh temporary fixture state, and the pre-existing fixture state was restored afterward.

## Credits

“Voxel Revolution” by Kevin MacLeod ([incompetech.com](https://incompetech.com/music/royalty-free/index.html?Search=Search&isrc=USUAN2000025)), licensed under [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/). The track is edited to fit the trailer, faded and mixed with sound effects.

Sound effects from Kenney's [Interface Sounds](https://kenney.nl/assets/interface-sounds), [Sci-Fi Sounds](https://kenney.nl/assets/sci-fi-sounds) and [Digital Audio](https://kenney.nl/assets/digital-audio), licensed CC0. Additional whoosh, riser, sub and shimmer effects were generated for this video. Full asset credits and the retained license notices are in [CREDITS.md](../promo/video/CREDITS.md).

Minecraft textures and native game images are from Minecraft 1.21.1, Mojang/Microsoft. They are not covered by the Kenney CC0 license. AlgoCraft model geometry, UI, title graphics and animation are from this repository.

## Delivery evidence

The delivery scripts review the English/Chinese READMEs and both store descriptions at desktop and phone widths, check every local image and link, verify the repository's 500-problem difficulty counts, fully decode the MP4, measure loudness and true peak, validate subtitle timing, test the ZIP and record SHA-256 hashes. Media and page receipts are sealed inside the upload pack. The generated upload pack and receipt are written to ignored `build/distributions/`. The portable render binding is retained in [trailer-render.json](verification/trailer-render.json).

These are documentation and promotional-media checks. The independent gameplay acceptance for both supported versions is documented in [the Beta verification handoff](BETA_0.1.0_HANDOFF.md).

## Public video URL

The README contains the poster, the local MP4 link and this full external URL:

https://github.com/crabsatellite/AlgoCraft/raw/refs/heads/main/docs/media/algocraft-trailer-en-1080p.mp4

The CurseForge copy uses a clickable poster and labeled video link. Public
access requires a public repository containing the final media. A private
repository link is not a public video host. For an inline CurseForge player,
upload the same English MP4 to a supported video host and replace the link with
that host's embed through the description editor.

[CurseForge's supported description tags](https://support.curseforge.com/support/solutions/articles/9000284625-supported-html-tags-css-for-project-descriptions)
document the available embeds and domain whitelist.
