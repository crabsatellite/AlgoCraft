"""Prepare source-backed reading packets and image sheets; never auto-approve prose."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / 'question_bank/official'
OUTPUT = ROOT / 'build/review/readability-2026-09-30'


def packet(start, end):
    for number in range(start, end + 1):
        problem = json.loads((BANK / f'p{number}.json').read_text(encoding='utf-8'))
        translation = json.loads((BANK / f'lang/zh_cn/p{number}.json').read_text(encoding='utf-8'))
        description = translation['description']
        parts = re.split(r'(?m)^## ', description)
        introduction = parts[0]
        constraints = next((part for part in parts[1:] if part.startswith('约束')), '')
        extras = [part for part in parts[1:] if not part.startswith(('示例', '约束'))]
        print(f'\n[{number}] {translation["title"]} / {problem["title"]}')
        print(introduction)
        print('CONSTRAINTS: ' + constraints)
        for part in extras:
            print('EXTRA: ' + part)
        examples = problem.get('examples', [])
        print('EXAMPLES: ' + json.dumps(examples[:2], ensure_ascii=False))
        print('PICTURES: ' + json.dumps(problem.get('diagrams', []), ensure_ascii=False))


def sheets():
    from PIL import Image, ImageDraw, ImageFont
    entries = []
    for number in range(1, 501):
        problem = json.loads((BANK / f'p{number}.json').read_text(encoding='utf-8'))
        for diagram in problem.get('diagrams', []):
            entries.append((number, diagram['file'], problem['title']))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 18)
    index = []
    for offset in range(0, len(entries), 12):
        group = entries[offset:offset + 12]
        # Thumbnails keep the same 384px width available in a narrow IDE.
        sheet = Image.new('RGB', (1200, 1040), '#eeeeee')
        draw = ImageDraw.Draw(sheet)
        for position, (number, filename, title) in enumerate(group):
            x, y = (position % 3) * 400, (position // 3) * 260
            with Image.open(BANK / 'images' / filename) as source:
                thumbnail = source.convert('RGB')
                thumbnail.thumbnail((384, 216))
            sheet.paste(thumbnail, (x + 8, y + 34))
            draw.text((x + 8, y + 8), f'p{number}: {filename}', fill='black', font=font)
            index.append({'problem': number, 'file': filename, 'sheet': offset // 12 + 1})
        sheet.save(OUTPUT / f'sheet-{offset // 12 + 1:02d}.png')
    (OUTPUT / 'image-index.json').write_text(json.dumps(index, indent=2), encoding='utf-8')
    print(f'{len(entries)} diagrams in {(len(entries) + 11) // 12} sheets')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--start', type=int, default=1)
    parser.add_argument('--end', type=int, default=25)
    parser.add_argument('--sheets', action='store_true')
    arguments = parser.parse_args()
    if arguments.sheets:
        sheets()
    else:
        packet(arguments.start, arguments.end)
