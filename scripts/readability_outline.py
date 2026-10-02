"""Print editorial paragraphs with stable indices for explicit review decisions."""
import json
import re
import sys
from pathlib import Path

BANK = Path(__file__).resolve().parents[1] / 'question_bank/official'
for n in map(int, sys.argv[1].split(',')):
    print(f'\np{n}')
    for lang in ('en', 'zh'):
        path = BANK / (f'p{n}.json' if lang == 'en' else f'lang/zh_cn/p{n}.json')
        doc = json.loads(path.read_text(encoding='utf-8'))
        intro = re.split(r'(?m)^## ', doc['description'])[0]
        for i, paragraph in enumerate(re.split(r'\n\s*\n', intro.strip())):
            print(f'{lang}{i}: {paragraph}')
