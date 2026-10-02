"""Generate the algorithm computer block model (north = front).

A widescreen monitor with an IDE on screen, a thin stand, a keyboard and mouse
on a desk mat, and a tower case with a light strip. Parts never overlap so the
model renders cleanly without z-fighting. Collision boxes in
AlgorithmComputerBlock.MODEL_BOXES follow COLLISION below.

Run: python scripts/generate_computer_model.py [screen_texture]
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'algocraft', 'models', 'block', 'algorithm_computer.json')

TEXTURES = {
    'body': 'minecraft:block/gray_concrete',
    'trim': 'minecraft:block/light_gray_concrete',
    'dark': 'minecraft:block/black_concrete',
    'panel': 'minecraft:block/black_wool',
    'metal': 'minecraft:block/iron_block',
    'vent': 'minecraft:block/deepslate_tiles',
    'screen': 'minecraft:block/black_stained_glass',
    'screen_line': 'minecraft:block/lime_concrete',
    'screen_muted': 'minecraft:block/light_blue_concrete',
    'key': 'minecraft:block/light_gray_concrete',
    'key_label': 'minecraft:block/yellow_concrete',
    'accent': 'minecraft:block/lime_concrete',
    'particle': 'minecraft:block/gray_concrete',
}

COLLISION = [
    [0.6, 6.0, 7.25, 11.4, 13.1, 9.75],    # monitor
    [5.4, 0.6, 8.6, 6.6, 6.0, 9.6],        # stand neck
    [3.2, 0, 7.0, 8.8, 0.6, 10.2],         # stand foot
    [12.25, 0, 6.2, 15.6, 10.15, 15.4],    # tower
    [0.5, 0, 0.6, 12.8, 1.15, 6.2],        # desk mat, keyboard and mouse
]


def box(name, frm, to, tex, faces=('north', 'east', 'south', 'west', 'up', 'down')):
    return {
        'name': name,
        'from': [round(v, 3) for v in frm],
        'to': [round(v, 3) for v in to],
        'faces': {face: {'texture': '#' + tex} for face in faces},
    }


def monitor():
    parts = [
        box('monitor_stand_foot', [3.2, 0, 7.0], [8.8, 0.6, 10.2], 'trim'),
        box('monitor_stand_neck', [5.4, 0.6, 8.6], [6.6, 6.4, 9.6], 'metal'),
        box('monitor_shell', [0.6, 6, 7.6], [11.4, 12.8, 8.6], 'dark'),
        box('monitor_back_housing', [2.2, 6.4, 8.6], [9.8, 11.8, 9.6], 'body'),
        box('monitor_back_vent', [3.4, 9.2, 9.6], [8.6, 10.8, 9.75], 'vent'),
        box('monitor_webcam', [5.5, 12.8, 7.8], [6.5, 13.1, 8.4], 'body'),
        box('monitor_screen_panel', [1.1, 6.7, 7.4], [10.9, 12.3, 7.6], 'screen'),
        box('monitor_logo', [5.5, 6.25, 7.45], [6.5, 6.45, 7.6], 'trim'),
        box('monitor_power_led', [9.9, 6.25, 7.45], [10.3, 6.45, 7.6], 'accent'),
    ]
    # IDE on screen. Seen from the front, +x is the viewer's left, so the file
    # tree sits at high x and code lines grow toward low x.
    z0, z1 = 7.25, 7.4
    screen = [
        ('screen_file_tree', [9.3, 7], [10.6, 12], 'vent'),
        ('screen_tab_bar', [1.4, 11.55], [9, 11.95], 'key'),
        ('screen_code_line_1', [6, 10.8], [8.9, 11.1], 'screen_muted'),
        ('screen_code_line_2', [4.2, 10.15], [8.4, 10.45], 'screen_line'),
        ('screen_code_line_3', [5.4, 9.5], [8.4, 9.8], 'key_label'),
        ('screen_code_line_4', [3, 8.85], [7.9, 9.15], 'screen_muted'),
        ('screen_cursor', [2.5, 8.8], [2.75, 9.2], 'trim'),
        ('screen_code_line_5', [6.2, 8.2], [8.9, 8.5], 'screen_muted'),
        ('screen_status_accepted', [1.4, 7], [9, 7.45], 'screen_line'),
    ]
    parts += [box(name, [a[0], a[1], z0], [b[0], b[1], z1], tex) for name, a, b, tex in screen]
    return parts


def keyboard():
    y0, y1 = 0.85, 1.15
    parts = [
        box('desk_mat', [0.5, 0, 0.6], [12.8, 0.25, 6.2], 'panel'),
        box('keyboard_deck', [1.2, 0.25, 1.2], [10.2, 0.85, 5.0], 'dark'),
    ]
    rows = [
        ('keyboard_key_row_1', 4.1, 4.6, [(8.4, 9.8), (6.8, 8.2), (5.2, 6.6), (3.6, 5.0), (1.6, 3.4)]),
        ('keyboard_key_row_2', 3.3, 3.8, [(8.0, 9.8), (6.2, 7.8), (4.4, 6.0), (2.5, 4.2)]),
        ('keyboard_key_row_3', 2.5, 3.0, [(8.4, 9.8), (6.8, 8.2), (5.2, 6.6), (3.6, 5.0), (1.6, 3.4)]),
    ]
    for name, z0, z1, spans in rows:
        for i, (x0, x1) in enumerate(spans):
            suffix = '' if i == 0 else f'_part_{i + 1}'
            parts.append(box(name + suffix, [x0, y0, z0], [x1, y1, z1], 'key'))
    parts += [
        box('keyboard_enter_key', [1.6, y0, 3.3], [2.3, y1, 3.8], 'key_label'),
        box('keyboard_modifier_left', [7.9, y0, 1.6], [9.8, y1, 2.2], 'key'),
        box('keyboard_spacebar', [3.9, y0, 1.6], [7.6, y1, 2.2], 'key'),
        box('keyboard_modifier_right', [1.6, y0, 1.6], [3.6, y1, 2.2], 'key'),
        box('mouse_body', [10.9, 0.25, 2.3], [12.3, 0.95, 4.4], 'trim'),
        box('mouse_scroll_wheel', [11.45, 0.95, 3.6], [11.75, 1.1, 4.0], 'dark'),
    ]
    return parts


def tower():
    parts = [
        box('computer_case', [12.4, 0.4, 6.6], [15.6, 10.0, 15.4], 'body'),
        box('computer_front_panel', [12.6, 0.6, 6.4], [15.4, 9.8, 6.6], 'dark'),
        box('computer_light_strip', [14.55, 1.2, 6.25], [14.85, 9.2, 6.4], 'accent'),
        box('computer_power_button', [13.1, 8.4, 6.25], [13.8, 9.1, 6.4], 'trim'),
        box('computer_front_vent', [12.9, 1.2, 6.3], [14.2, 6.8, 6.4], 'vent'),
        box('computer_drive_slot', [12.9, 7.4, 6.3], [14.2, 7.7, 6.4], 'trim'),
        box('computer_vent_top', [12.9, 10.0, 9.0], [15.1, 10.15, 14.4], 'vent'),
        box('computer_side_window', [12.25, 2.0, 8.0], [12.4, 8.6, 14.6], 'screen'),
        box('computer_side_glow', [12.2, 2.4, 8.4], [12.25, 2.8, 14.2], 'accent'),
        box('computer_rear_io', [13.2, 5.5, 15.4], [14.8, 9.2, 15.55], 'dark'),
    ]
    feet = [(12.6, 6.8), (14.9, 6.8), (12.6, 14.5), (14.9, 14.5)]
    parts += [
        box(f'computer_foot_{i}', [x, 0, z], [x + 0.6, 0.4, z + 0.7], 'dark')
        for i, (x, z) in enumerate(feet, start=1)
    ]
    return parts


def display():
    return {
        'thirdperson_righthand': {'rotation': [75, 225, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'thirdperson_lefthand': {'rotation': [75, 225, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 225, 0], 'translation': [0, 1, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 1, 0], 'scale': [0.4, 0.4, 0.4]},
        'gui': {'rotation': [25, 205, 0], 'translation': [0, 0.6, 0], 'scale': [0.7, 0.7, 0.7]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, 1.5, 0], 'scale': [0.6, 0.6, 0.6]},
    }


def main():
    textures = dict(TEXTURES)
    if len(sys.argv) > 1:
        textures['screen'] = sys.argv[1]
    model = {
        'parent': 'minecraft:block/block',
        'textures': textures,
        'display': display(),
        'elements': monitor() + keyboard() + tower(),
    }
    with open(MODEL, 'w', encoding='utf-8', newline='\n') as handle:
        json.dump(model, handle, indent=2)
        handle.write('\n')
    print(len(model['elements']), 'elements')


if __name__ == '__main__':
    main()
