"""Extract chapter frames and contact sheets from the final MP4 without opening a player."""
from pathlib import Path
import json, os, subprocess
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/promo/final-frames'
OUT.mkdir(parents=True, exist_ok=True)
VIDEO = ROOT / 'docs/media/algocraft-trailer-en-1080p.mp4'
TIMES = [1.5, 4.7, 8.8, 11.8, 15.5, 18.8, 23, 29, 33, 36, 37.8, 39.8, 41.8, 45.3, 49.8, 52.5,
         55.8, 58, 61, 64, 69.8, 72.5, 76.8, 80.8, 84, 87, 90.8, 92.5, 95.8, 103.5, 116, 126]
flags = 0x08000000 if os.name == 'nt' else 0
for i, t in enumerate(TIMES):
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-threads', '2', '-ss', str(t), '-i', str(VIDEO),
                    '-frames:v', '1', '-threads', '2', str(OUT / f'{i:02d}.png')],
                   check=True, capture_output=True, creationflags=flags)
for offset, name in [(0, 'sheet-a'), (16, 'sheet-b')]:
    sheet = Image.new('RGB', (1920, 1160), '#05070d')
    for j in range(16):
        i = j + offset
        im = Image.open(OUT / f'{i:02d}.png').convert('RGB').resize((480, 270), Image.LANCZOS)
        x, y = j % 4 * 480, j // 4 * 290
        sheet.paste(im, (x, y))
        ImageDraw.Draw(sheet).text((x + 10, y + 272), f'{TIMES[i]:05.1f}s', fill='#b7c5da')
    sheet.save(OUT / f'{name}.jpg', quality=94)
(OUT / 'frames.json').write_text(json.dumps({'source': str(VIDEO), 'times': TIMES, 'headless': True}, indent=2), encoding='utf-8')
print('Extracted', len(TIMES), 'frames to', OUT)
