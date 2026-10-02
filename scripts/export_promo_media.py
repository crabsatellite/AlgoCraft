"""Export the rendered promo into docs/media (silent; ffmpeg runs without a window).

Inputs come from promo/video (Remotion). Usage: python scripts/export_promo_media.py
"""
import json, os, re, shutil, subprocess
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
VIDEO = ROOT / 'promo' / 'video'
OUT = VIDEO / 'out'
PUB = VIDEO / 'public'
MEDIA = ROOT / 'docs' / 'media'
NOWIN = 0x08000000 if os.name == 'nt' else 0
FEATURES = {  # trailer frame -> README image
    'feature-computer': 300, 'feature-accepted': 1262, 'feature-rewards': 1384, 'feature-bank': 1579,
    'feature-diagrams': 1690, 'feature-daily': 1975, 'feature-milestones': 2200, 'feature-trophies': 2566,
    'feature-achievements': 2800, 'feature-server': 3150,
}
OLD = ['algocraft-trailer-zh-1080p.mp4', 'algocraft-trailer-zh.srt', 'hero-en.png', 'hero-zh.png', 'computer.png',
       'ingame-ide.png', 'trophies.png', 'web-ide.png']

def run(*a):
    return subprocess.run(list(a), check=True, capture_output=True, text=True, encoding='utf-8', errors='replace', creationflags=NOWIN)

def jpg(src, dst, size=None, q=90):
    im = Image.open(src).convert('RGB')
    if size: im = im.resize(size, Image.LANCZOS)
    im.save(dst, quality=q, optimize=True, progressive=True)

def poster(src, dst):
    im = Image.open(src).convert('RGB')
    w, h = im.size
    layer = Image.new('RGBA', im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy, r = w // 2, int(h * 0.80), 64
    glow = Image.new('RGBA', im.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((cx - r - 30, cy - r - 30, cx + r + 30, cy + r + 30), fill=(52, 227, 160, 120))
    layer = Image.alpha_composite(glow.filter(ImageFilter.GaussianBlur(24)), layer)
    d = ImageDraw.Draw(layer)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(52, 227, 160, 255))
    d.polygon([(cx - 20, cy - 32), (cx - 20, cy + 32), (cx + 34, cy)], fill=(6, 30, 21, 255))
    Image.alpha_composite(im.convert('RGBA'), layer).convert('RGB').save(dst, quality=90, optimize=True, progressive=True)

def main():
    MEDIA.mkdir(parents=True, exist_ok=True)
    for name in OLD:
        p = MEDIA / name
        if p.exists(): p.unlink()
    raw = OUT / 'trailer-raw.mp4'
    # Two-pass EBU R128 loudness normalisation to -16 LUFS / -1.5 dBTP; video stream is copied untouched.
    probe = run('ffmpeg', '-hide_banner', '-nostats', '-threads', '2', '-i', str(raw), '-vn', '-af', 'loudnorm=I=-16:TP=-1.5:LRA=11:print_format=json', '-f', 'null', '-')
    m = json.loads(re.search(r'\{[^{}]*"input_i"[^{}]*\}', probe.stderr, re.S).group(0))
    af = (f"loudnorm=I=-16:TP=-1.5:LRA=11:measured_I={m['input_i']}:measured_TP={m['input_tp']}:measured_LRA={m['input_lra']}"
          f":measured_thresh={m['input_thresh']}:offset={m['target_offset']}:linear=true")
    final = MEDIA / 'algocraft-trailer-en-1080p.mp4'
    run('ffmpeg', '-hide_banner', '-y', '-threads', '2', '-i', str(raw), '-map', '0:v:0', '-map', '0:a:0', '-c:v', 'copy', '-af', af, '-ar', '48000',
        '-c:a', 'aac', '-b:a', '256k', '-movflags', '+faststart', '-metadata', 'title=AlgoCraft: learn algorithms in Minecraft',
        '-metadata', 'comment=Music: Voxel Revolution by Kevin MacLeod (incompetech.com), CC BY 4.0. SFX: Kenney (CC0).', str(final))
    shutil.copyfile(OUT / 'algocraft-trailer-en.srt', MEDIA / 'algocraft-trailer-en.srt')
    jpg(OUT / 'hero.png', MEDIA / 'hero.jpg')
    poster(OUT / 'poster.png', MEDIA / 'trailer-poster.jpg')
    for name in FEATURES:
        jpg(OUT / 'stills' / f'{name}.png', MEDIA / f'{name}.jpg', (1280, 720), 88)
    jpg(PUB / 'game' / 'ide-wide.png', MEDIA / 'ingame-ide.jpg', None, 92)
    jpg(PUB / 'game' / 'model-computer-world.png', MEDIA / 'ingame-world.jpg', None, 90)
    jpg(PUB / 'cap' / 'code.png', MEDIA / 'web-ide.jpg', (1600, 900), 90)
    print(json.dumps({'loudness_in': m['input_i'], 'tp_in': m['input_tp']}))

if __name__ == '__main__':
    main()
