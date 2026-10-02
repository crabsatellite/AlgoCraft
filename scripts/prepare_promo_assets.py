"""Prepare deterministic inputs for the Remotion promo (promo/video/public).

Everything here is derived from repository assets, sealed headless captures,
the local Minecraft 1.21.1 client jar, CC0 Kenney sounds and a CC BY 4.0 track.
No game window, browser window or audio device is used.
"""
import json, os, shutil, subprocess, sys, zipfile, wave, re
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PUB = os.path.join(ROOT, 'promo', 'video', 'public')
MODELS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'algocraft', 'models')
LANG = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'algocraft', 'lang', 'en_us.json')
TEX = os.path.join(ROOT, 'build', 'review', 'visual-refresh-2026-10-01', 'tex')
SHOTS = os.path.join(ROOT, 'build', 'review', 'server-banks-2026-10-01', 'stress')
AUDIO = os.path.join(ROOT, 'build', 'promo', 'audio')
JAR = os.path.expanduser(r'~\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar')
NOWIN = 0x08000000 if os.name == 'nt' else 0

def mk(*p):
    d = os.path.join(PUB, *p); os.makedirs(d, exist_ok=True); return d

def load_model(rel):
    m = json.load(open(os.path.join(MODELS, rel + '.json'), encoding='utf-8'))
    parent = m.get('parent', '')
    if parent.startswith('algocraft:'):
        base = load_model(parent.split(':')[1])
        tex = dict(base.get('textures', {})); tex.update(m.get('textures', {}))
        out = dict(base); out.update(m); out['textures'] = tex
        if 'elements' not in m: out['elements'] = base.get('elements', [])
        return out
    return m

def resolve(textures, ref):
    for _ in range(10):
        if not ref.startswith('#'): break
        ref = textures.get(ref[1:], 'minecraft:block/missing')
    return ref.split('/')[-1]

used_tex = set()
def flatten(rel):
    m = load_model(rel)
    els = []
    for e in m.get('elements', []):
        faces = {}
        for k, f in e.get('faces', {}).items():
            t = resolve(m['textures'], f['texture']); used_tex.add(t)
            faces[k] = {'t': t, 'uv': f.get('uv'), 'r': f.get('rotation', 0)}
        els.append({'from': e['from'], 'to': e['to'], 'rot': e.get('rotation'), 'faces': faces})
    return els

def main():
    mk('models'); mk('tex'); mk('items'); mk('game'); mk('sfx'); mk('music'); mk('data')
    lang = json.load(open(LANG, encoding='utf-8'))
    out = {'computer': flatten('block/algorithm_computer')}
    tiers = ['bronze', 'silver', 'gold', 'diamond', 'netherite']
    variants = []
    for t in tiers:
        out[t] = flatten(f'item/{t}_trophy')
        for o in json.load(open(os.path.join(MODELS, 'item', f'{t}_trophy.json'), encoding='utf-8')).get('overrides', []):
            v = o['model'].split('/')[-1]
            out['v_' + v] = flatten('item/trophy_variants/' + v)
            variants.append({'id': v, 'tier': t, 'name': lang.get(f'algocraft.achievement.{v}.name', v), 'desc': lang.get(f'algocraft.achievement.{v}.desc', '')})
    json.dump(out, open(os.path.join(PUB, 'models', 'models.json'), 'w'))
    json.dump(variants, open(os.path.join(PUB, 'data', 'trophies.json'), 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    for t in sorted(used_tex):
        im = Image.open(os.path.join(TEX, t + '.png')).convert('RGBA')
        im.crop((0, 0, im.width, im.width)).save(os.path.join(PUB, 'tex', t + '.png'))
    with zipfile.ZipFile(JAR) as z:
        for name in ['item/iron_ingot', 'item/redstone', 'block/glass', 'item/experience_bottle', 'item/emerald',
                     'item/diamond', 'block/dragon_egg', 'item/item_frame', 'item/clock_00', 'item/writable_book',
                     'item/nether_star', 'item/golden_apple', 'item/name_tag', 'item/totem_of_undying', 'item/netherite_scrap', 'item/gold_ingot', 'item/netherite_ingot', 'block/emerald_block', 'block/iron_block']:
            data = z.read(f'assets/minecraft/textures/{name}.png')
            p = os.path.join(PUB, 'items', name.split('/')[-1] + '.png')
            open(p, 'wb').write(data)
            im = Image.open(p).convert('RGBA'); im = im.crop((0, 0, im.width, im.width))
            im.resize((im.width * 16, im.width * 16), Image.NEAREST).save(p)
    dragon_egg_icon()
    enchanted_apple_icon()
    for name in ['model-computer-world', 'ide-wide', 'ide-narrow', 'ide-scaled', 'model-items-gui',
                 'sample-p1', 'sample-p7', 'sample-p73', 'sample-p95', 'sample-p500']:
        im = Image.open(os.path.join(SHOTS, name + '.png')).convert('RGB')
        im.resize((im.width * 2, im.height * 2), Image.NEAREST).save(os.path.join(PUB, 'game', name + '.png'))
    # Problem bank facts from the real capture receipt.
    rc = json.load(open(os.path.join(PUB, 'cap', 'capture-receipt.json'), encoding='utf-8'))
    bank_dir = os.path.join(ROOT, 'question_bank', 'official')
    titles = []
    for i in range(1, 501):
        p = json.load(open(os.path.join(bank_dir, f'p{i}.json'), encoding='utf-8'))
        titles.append({'id': str(p['id']), 't': p['title'], 'd': p['difficulty']})
    assert len(titles) == 500 and all(re.match(r'^[\x20-\x7e]+$', x['t']) for x in titles)
    counts = {d: sum(1 for x in titles if x['d'].upper() == d) for d in ('EASY', 'MEDIUM', 'HARD')}
    assert counts == {'EASY': 172, 'MEDIUM': 205, 'HARD': 123}, counts
    json.dump({'titles': titles, 'counts': counts, 'browse': rc['browse'], 'typeFrames': rc['frames']},
              open(os.path.join(PUB, 'data', 'bank.json'), 'w', encoding='utf-8'), ensure_ascii=False)
    shutil.copy(os.path.join(AUDIO, 'voxel.mp3'), os.path.join(PUB, 'music', 'voxel.mp3'))
    sfx = {'click1': 'interface/click_002', 'click2': 'interface/click_003', 'key': 'interface/tick_002',
           'select': 'interface/select_006', 'confirm': 'interface/confirmation_002', 'glass': 'interface/glass_002',
           'maximize': 'interface/maximize_006', 'pluck': 'interface/pluck_001', 'drop': 'interface/drop_002',
           'impact': 'scifi/impactMetal_002', 'boom': 'scifi/lowFrequency_explosion_000',
           'power': 'digital/powerUp7', 'power2': 'digital/powerUp11', 'phaser': 'digital/phaserUp6',
           'three': 'digital/threeTone2', 'zap': 'digital/zapThreeToneUp', 'laser': 'scifi/laserRetro_002',
           'field': 'scifi/forceField_001', 'computer': 'scifi/computerNoise_001', 'switch': 'interface/switch_004', 'tick': 'interface/tick_004'}
    for k, rel in sfx.items():
        pack, n = rel.split('/')
        src = os.path.join(AUDIO, 'kenney', f'kenney_{dict(interface="interface", scifi="scifi", digital="digital")[pack]}', 'Audio', n + '.ogg')
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-threads', '1', '-i', src, '-ar', '48000', '-ac', '2', os.path.join(PUB, 'sfx', k + '.wav')],
                       check=True, creationflags=NOWIN)
    synth()
    print('models', len(out), 'textures', len(used_tex), 'variants', len(variants), counts)

def dragon_egg_icon():
    # Egg silhouette filled with the vanilla dragon_egg texture (the block texture alone reads as a dark square).
    tex = np.asarray(Image.open(os.path.join(PUB, 'items', 'dragon_egg.png')).convert('RGBA').resize((16, 16), Image.NEAREST))
    mask = ['......####......', '....########....', '...##########...', '..############..', '..############..', '.##############.', '.##############.', '################',
            '################', '################', '################', '.##############.', '.##############.', '..############..', '...##########...', '.....######.....']
    out = np.zeros((16, 16, 4), np.uint8)
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == '#':
                shade = 1.35 if (x < 7 and y < 8) else (0.8 if x > 10 else 1.0)
                out[y, x, :3] = np.clip(tex[y, x, :3].astype(int) * shade + 12, 0, 255); out[y, x, 3] = 255
    Image.fromarray(out).resize((256, 256), Image.NEAREST).save(os.path.join(PUB, 'items', 'dragon_egg_icon.png'))

def enchanted_apple_icon():
    # Static stand-in for the enchantment glint over the vanilla golden apple texture.
    im = np.asarray(Image.open(os.path.join(PUB, 'items', 'golden_apple.png')).convert('RGBA')).astype(float)
    h, w = im.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    band = (np.sin((xx + yy) / w * 9.0) * 0.5 + 0.5) ** 3
    glint = np.array([170, 90, 255], float)
    a = im[..., 3:4] / 255
    im[..., :3] = im[..., :3] * (1 - 0.45 * band[..., None] * a) + glint * 0.45 * band[..., None] * a
    Image.fromarray(np.clip(im, 0, 255).astype(np.uint8)).save(os.path.join(PUB, 'items', 'enchanted_golden_apple.png'))

def write(name, y, sr=48000):
    y = np.clip(y, -1, 1); st = np.stack([y, y], 1) if y.ndim == 1 else y
    with wave.open(os.path.join(PUB, 'sfx', name + '.wav'), 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes((st * 32767).astype('<i2').tobytes())

def synth(sr=48000):
    rng = np.random.default_rng(7)
    def bandnoise(n, lo, hi):
        X = np.fft.rfft(rng.standard_normal(n)); f = np.fft.rfftfreq(n, 1 / sr)
        X[(f < lo) | (f > hi)] = 0; y = np.fft.irfft(X, n); return y / np.abs(y).max()
    # Whoosh: swept band noise with stereo pan.
    n = int(0.7 * sr); t = np.arange(n) / sr
    y = np.zeros(n)
    for i, c in enumerate(np.geomspace(300, 4200, 14)):
        seg = bandnoise(n, c * 0.7, c * 1.4); w = np.exp(-((t - 0.25 - i * 0.02) / 0.13) ** 2); y += seg * w
    y = y / np.abs(y).max() * 0.55
    pan = np.clip(t / 0.7, 0, 1)
    write('whoosh', np.stack([y * (1 - pan * 0.6), y * (0.4 + pan * 0.6)], 1))
    # Riser: 2 bars of rising filtered noise + tone.
    n = int(3.9 * sr); t = np.arange(n) / sr
    env = (t / 3.9) ** 2.2
    tone = np.sin(2 * np.pi * np.cumsum(np.geomspace(110, 880, n)) / sr) * 0.25
    y = (bandnoise(n, 800, 9000) * 0.5 + tone) * env
    write('riser', y * 0.8)
    # Sub boom with soft click.
    n = int(1.6 * sr); t = np.arange(n) / sr
    f = 32 + 90 * np.exp(-t * 18)
    y = np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t * 2.4)
    y[:200] += bandnoise(200, 2000, 9000) * np.linspace(1, 0, 200) * 0.6
    write('sub', y * 0.9)
    # Shimmer: stacked detuned sines for trophies.
    n = int(2.2 * sr); t = np.arange(n) / sr
    y = sum(np.sin(2 * np.pi * fr * t + k) * np.exp(-t * (1.6 + k * 0.3)) for k, fr in enumerate([1568, 2093, 2637, 3136, 4186]))
    write('shimmer', y / 5 * 0.7 * np.minimum(1, t * 60))

if __name__ == '__main__':
    main()







