"""Bundle the finished promo deliverables for upload. Does not upload or publish anything."""
from pathlib import Path
import zipfile
ROOT = Path(__file__).resolve().parents[1]
MEDIA = ROOT / 'docs' / 'media'
target = ROOT / 'build/distributions/algocraft-promo-0.1.0-beta.zip'
target.parent.mkdir(parents=True, exist_ok=True)
CREDITS = ('AlgoCraft promo pack\n\nTrailer: algocraft-trailer-en-1080p.mp4 (1920x1080, 30 fps, H.264/AAC, English on-screen text, no narration).\n'
           'Subtitles: algocraft-trailer-en.srt.\n\nMusic: "Voxel Revolution" by Kevin MacLeod (incompetech.com)\n'
           'Licensed under Creative Commons: By Attribution 4.0 License, http://creativecommons.org/licenses/by/4.0/\n'
           'Sound effects: Kenney (kenney.nl), CC0.\nMinecraft item textures shown in the trailer are from Minecraft 1.21.1 (Mojang).\n')
with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(MEDIA.iterdir()):
        if p.is_file(): z.write(p, f'docs/media/{p.name}')
    for name in ['CURSEFORGE.en.md', 'CURSEFORGE.en.html', 'CURSEFORGE.zh-CN.md', 'CURSEFORGE.zh-CN.html', 'PROMO_MEDIA.md']:
        z.write(ROOT / 'docs' / name, f'docs/{name}')
    for name in ['README.zh-CN.md', 'PLAYER_GUIDE.md', 'PROBLEM_FORMAT.md']:
        z.write(ROOT / 'docs' / name, f'docs/{name}')
    z.write(ROOT / 'README.md', 'README.md')
    z.write(ROOT / 'promo/video/CREDITS.md', 'CREDITS.md')
    z.writestr('CREDITS.txt', CREDITS)
print(target, target.stat().st_size)
