"""Generate AlgoCraft trophy item models from readable cuboid parts.

Each tier is a classic cup trophy on a blackstone plinth. Higher tiers add parts
(handles, lid, gems, crown points) so the silhouette grows with the tier.
Achievement variants are the tier model plus the achievement marks stored in
scripts/trophy_variant_marks.json (front plaque, cup front and crown anchors).

Run: python scripts/generate_trophy_models.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'algocraft', 'models', 'item')
MARKS = os.path.join(ROOT, 'scripts', 'trophy_variant_marks.json')

MATERIALS = {
    'bronze': ('copper_block', 'orange_concrete', 'brown_concrete'),
    'silver': ('iron_block', 'light_gray_concrete', 'white_concrete'),
    'gold': ('gold_block', 'yellow_concrete', 'orange_concrete'),
    'diamond': ('diamond_block', 'cyan_concrete', 'light_blue_concrete'),
    'netherite': ('netherite_block', 'gray_concrete', 'purple_concrete'),
}
TIERS = ['bronze', 'silver', 'gold', 'diamond', 'netherite']


def textures(tier):
    metal, accent, gem = MATERIALS[tier]
    return {
        'base': 'minecraft:block/polished_blackstone',
        'shadow': 'minecraft:block/blackstone',
        'metal': 'minecraft:block/' + metal,
        'accent': 'minecraft:block/' + accent,
        'gem': 'minecraft:block/' + gem,
        'particle': 'minecraft:block/' + metal,
    }


def box(name, frm, to, tex):
    frm = [round(v, 3) for v in frm]
    to = [round(v, 3) for v in to]
    return {
        'name': name,
        'from': frm,
        'to': to,
        'faces': {face: {'texture': '#' + tex} for face in ('north', 'east', 'south', 'west', 'up', 'down')},
    }


def mirror(name, frm, to, tex):
    """Mirror a part from the west half (low x) to the east half."""
    return box(name, [16 - to[0], frm[1], frm[2]], [16 - frm[0], to[1], to[2]], tex)


def pair(stem, frm, to, tex):
    return [box('left_' + stem, frm, to, tex), mirror('right_' + stem, frm, to, tex)]


def cup():
    return [
        # Stepped plinth. Its front face (z = 2.55) carries the achievement plaque marks.
        box('plinth_foot', [2.5, 0, 2.2], [13.5, 1.2, 13.8], 'shadow'),
        box('plinth_block', [3, 1.2, 2.55], [13, 4, 13.45], 'base'),
        box('plinth_plaque', [4.8, 1.45, 2.3], [11.2, 3.75, 2.55], 'metal'),
        box('plinth_cap', [3.6, 4, 3.6], [12.4, 4.7, 12.4], 'shadow'),
        # Stem.
        box('stem_collar', [5.6, 4.7, 5.6], [10.4, 5.5, 10.4], 'metal'),
        box('stem', [7, 5.5, 7], [9, 8.2, 9], 'metal'),
        box('stem_knot', [6.4, 6.3, 6.4], [9.6, 7.3, 9.6], 'accent'),
        # Cup. Its front face (z = 5.25) carries the achievement emblem marks.
        box('bowl_base', [5.4, 8.2, 5.9], [10.6, 9.3, 10.1], 'metal'),
        box('bowl_lower', [4.6, 9.3, 5.25], [11.4, 11, 10.75], 'metal'),
        box('bowl_body', [3.9, 11, 5.25], [12.1, 16.4, 10.75], 'metal'),
        box('bowl_band_front', [4.6, 10.45, 5.05], [11.4, 10.95, 5.25], 'accent'),
        box('rim_front', [3.4, 16.4, 4.75], [12.6, 17.4, 5.75], 'accent'),
        box('rim_back', [3.4, 16.4, 10.25], [12.6, 17.4, 11.25], 'accent'),
        box('rim_left', [3.4, 16.4, 5.75], [4.4, 17.4, 10.25], 'accent'),
        box('rim_right', [11.6, 16.4, 5.75], [12.6, 17.4, 10.25], 'accent'),
        box('bowl_hollow', [4.4, 16.4, 5.75], [11.6, 16.9, 10.25], 'shadow'),
    ]


def handles():
    return (
        pair('handle_top', [2.1, 14.6, 7.3], [3.9, 15.4, 8.7], 'metal')
        + pair('handle_side', [2.1, 11, 7.3], [2.9, 14.6, 8.7], 'metal')
        + pair('handle_bottom', [2.1, 10.2, 7.3], [4.6, 11, 8.7], 'metal')
    )


def studs():
    return [
        box(f'plinth_stud_{i}', [x, 4.7, z], [x + 1, 5.25, z + 1], 'accent')
        for i, (x, z) in enumerate([(3.7, 3.7), (11.3, 3.7), (3.7, 11.3), (11.3, 11.3)], start=1)
    ] + [
        box('plinth_back_plaque', [5, 1.45, 13.45], [11, 3.75, 13.7], 'metal'),
        box('stem_gem_ring', [6.8, 7.3, 6.8], [9.2, 7.7, 9.2], 'gem'),
    ]


def lid():
    # The lid top (y = 19.2) is the anchor for crown marks.
    return [
        box('lid_dome', [5.2, 17.4, 6.2], [10.8, 18.5, 9.8], 'metal'),
        box('lid_top', [6, 18.5, 6.6], [10, 19.2, 9.4], 'accent'),
        box('lid_finial', [7.3, 19.2, 7.3], [8.7, 20.4, 8.7], 'gem'),
    ] + pair('bowl_side_gem', [3.6, 13, 7.4], [3.9, 14.2, 8.6], 'gem')


def jewels():
    return (
        pair('handle_gem', [1.8, 12.2, 7.6], [2.1, 13.4, 8.4], 'gem')
        + pair('plinth_side_gem', [2.75, 2, 7], [3, 3.2, 9], 'gem')
        + [box('rim_front_gem', [7.3, 16.6, 4.55], [8.7, 17.2, 4.75], 'gem')]
    )


def crown():
    corners = [(3.4, 4.75), (11.6, 4.75), (3.4, 10.25), (11.6, 10.25)]
    return [
        box(f'crown_point_{i}', [x, 17.4, z], [x + 1, 18.7, z + 1], 'metal')
        for i, (x, z) in enumerate(corners, start=1)
    ] + [
        box('plinth_trim_front', [3, 1.2, 2.4], [13, 1.45, 2.55], 'gem'),
        box('plinth_trim_back', [3, 1.2, 13.45], [13, 1.45, 13.6], 'gem'),
        box('plinth_trim_left', [2.85, 1.2, 2.55], [3, 1.45, 13.45], 'gem'),
        box('plinth_trim_right', [13, 1.2, 2.55], [13.15, 1.45, 13.45], 'gem'),
    ]


def elements(tier):
    level = TIERS.index(tier)
    parts = cup() + handles()
    if level >= 1:
        parts += studs()
    if level >= 2:
        parts += lid()
    if level >= 3:
        parts += jewels()
    if level >= 4:
        parts += crown()
    return parts

def display():
    hand = {'rotation': [60, 0, 0], 'translation': [0, 2.0, 1], 'scale': [0.46, 0.46, 0.46]}
    return {
        'thirdperson_righthand': hand,
        'thirdperson_lefthand': dict(hand),
        'firstperson_righthand': {'rotation': [0, -35, 0], 'translation': [1, 2.5, 0], 'scale': [0.48, 0.48, 0.48]},
        'firstperson_lefthand': {'rotation': [0, 35, 0], 'translation': [-1, 2.5, 0], 'scale': [0.48, 0.48, 0.48]},
        'gui': {'rotation': [25, 210, 0], 'translation': [0, -0.75, 0], 'scale': [0.5, 0.5, 0.5]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 0], 'scale': [0.4, 0.4, 0.4]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, -1.5, 0], 'scale': [0.75, 0.75, 0.75]},
    }


def model(tier, extra=()):
    return {
        'parent': 'minecraft:block/block',
        'textures': textures(tier),
        'display': display(),
        'elements': elements(tier) + list(extra),
    }


def write(path, data):
    with open(path, 'w', encoding='utf-8', newline='\n') as handle:
        json.dump(data, handle, indent=2)
        handle.write('\n')


def main():
    for tier in TIERS:
        write(os.path.join(ITEM, f'trophy_{tier}_shape.json'), model(tier))
    with open(MARKS, encoding='utf-8') as handle:
        marks = json.load(handle)
    for achievement, spec in marks.items():
        write(os.path.join(ITEM, 'trophy_variants', achievement + '.json'), model(spec['tier'], spec['marks']))
    for tier in TIERS:
        print(tier, len(elements(tier)))


if __name__ == '__main__':
    main()
