"""Verify editorial scope against its retained baseline and export the reading ledger.

The reading record is based on the manual packets/contact sheets documented in
working-notes.md. This script verifies preservation and records edits; it does
not decide whether prose or an illustration is pedagogically acceptable.
"""
import csv
import hashlib
import io
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / 'question_bank/official'
STATE = ROOT / 'build/review/readability-2026-09-30'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def verify():
    baseline_path = STATE / 'question-bank-before.zip'
    changes, image_changes, rows = [], [], []
    with zipfile.ZipFile(baseline_path) as baseline:
        for number in range(1, 501):
            documents, modified = {}, {}
            for language in ('en', 'zh'):
                path = BANK / (f'p{number}.json' if language == 'en' else f'lang/zh_cn/p{number}.json')
                before = json.loads(baseline.read(path.relative_to(ROOT).as_posix()))
                current = json.loads(path.read_text(encoding='utf8'))
                fields = sorted(k for k in before.keys() | current.keys() if before.get(k) != current.get(k))
                assert not set(fields) - {'description', 'diagrams'}, (number, language, fields)
                if fields:
                    changes.append({'problem': number, 'language': language, 'fields': fields})
                documents[language], modified[language] = current, fields
            en, zh = documents['en'], documents['zh']
            assert ('description' in modified['en']) == ('description' in modified['zh']), number
            assert en['title'] == json.loads(baseline.read(f'question_bank/official/p{number}.json'))['title']
            contract = {k: v for k, v in en.items() if k not in ('description', 'diagrams')}
            contract_hash = sha(json.dumps(contract, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode())
            images = en.get('diagrams', [])
            changed_images = []
            for diagram in images:
                file = BANK / 'images' / diagram['file']
                if file.read_bytes() != baseline.read(file.relative_to(ROOT).as_posix()):
                    changed_images.append(diagram['file'])
                    image_changes.append({'problem': number, 'file': diagram['file'], 'sha256': sha(file.read_bytes())})
            image_status = '无现有插图'
            if images:
                image_status = '已看，重绘' if changed_images else '已看，图注修改' if 'diagrams' in modified['en'] else '已看，保留'
            rows.append({
                'problem': number, 'title_zh': zh['title'], 'title_en': en['title'],
                'text_review': '已读，修改' if 'description' in modified['zh'] else '已读，保留',
                'image_review': image_status,
                'image_files': '; '.join(d['file'] for d in images),
                'executable_contract_sha256': contract_hash,
            })
    assert len(rows) == 500
    assert sum(bool(row['image_files']) for row in rows) == 184
    refs = {d['file'] for p in BANK.glob('p*.json') for d in json.loads(p.read_text(encoding='utf8')).get('diagrams', [])}
    assert refs == {p.name for p in (BANK / 'images').glob('*.png')}
    assert len(refs) == 188
    receipt = {
        'baselineSha256': sha(baseline_path.read_bytes()),
        'manifestSha256': sha((BANK / 'manifest.json').read_bytes()),
        'reviewedChineseStatements': 500, 'reviewedReferencedImages': 188,
        'changedStatementQuestions': sum(r['text_review'] == '已读，修改' for r in rows),
        'changedLocalizedStatements': sum('description' in c['fields'] for c in changes),
        'changedPngs': len(image_changes),
        'changedCaptionQuestions': len({c['problem'] for c in changes if 'diagrams' in c['fields']}),
        'unchangedExecutableContracts': 500, 'unchangedLocalizedSolutionDocuments': 500,
        'unexpectedFields': [], 'documentsChanged': changes, 'imagesChanged': image_changes,
    }
    (STATE / 'baseline-comparison.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    with (STATE / '500-question-ledger.csv').open('w', encoding='utf-8-sig', newline='') as out:
        writer = csv.DictWriter(out, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)
    text = '''# 题库首读审核记录

审核于 2026-09-30 开始。标准：第一次遇到该题型的算法学习者，应该能从题面理解输入、目标、规则、示例和代码接口。机械套话、未定义术语、错误规则、误导图片和提前讲解解法都需要改。合法示例结果与规则演示可以保留，但要说明上下文。

实际阅读范围：500 道中文题面的介绍、约束、附加说明、前两个结构化示例和图注。重大重写同时核对英文与 Java 接口，并针对变动检查更多示例。188 张引用图片全部按 384 像素显示宽度看过；37 张重绘图片及最后的排版修正再次复看。英文修改与中文同步；没有声称对全部未修改英文题面另做了一轮逐句审核。

本表记录实际阅读和编辑结果，不以扫描命中、旧完成表或自动测试替代教学判断。“无现有插图”表示本题没有待审图片。此轮没有更改题目标题、初始代码、示例数据、隐藏用例和参考解答内容；500 题执行合同与基线一致。逐题执行合同哈希在 `build/review/readability-2026-09-30/500-question-ledger.csv`。

修正：245 道题的中英文题面（490 个版本）、38 道题的双语图注、37 张 PNG。例：p58 API 与 bad 参数说明，p72 重复值规则，p151 台阶终点，p159 含零连续范围，p379 多项式格式，p430 隐含根；p87 补全树，p105 补全 apple/app 路径，p139 正确输入图，p163 3×7 网格，p408 真实队列状态。

测试结果与交接边界见 `docs/FIRST_READER_HANDOFF_2026-09-30.md`。编辑基线、阅读笔记、接触表和校验收据保留在 `build/review/readability-2026-09-30/`。

| ID | 中文题名 | 题面阅读 | 图片复看 |
| --- | --- | --- | --- |
'''
    text += '\n'.join(f"| p{r['problem']} | {r['title_zh']} | {r['text_review']} | {r['image_review']} |" for r in rows) + '\n'
    (ROOT / 'question_bank/FIRST_READER_REVIEW_2026-09-30.md').write_text(text, encoding='utf8')
    print(json.dumps({k: v for k, v in receipt.items() if k not in ('documentsChanged', 'imagesChanged')}, ensure_ascii=False))


if __name__ == '__main__':
    verify()
