from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path
from typing import Iterable, Sequence

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "question_bank" / "official" / "images"

W = 768
H = 432

BG = (23, 22, 20)
PANEL = (34, 33, 29)
PANEL_EDGE = (72, 67, 57)
GRID = (78, 73, 62)
GRID_LIGHT = (120, 113, 95)
TEXT = (236, 232, 214)
MUTED = (182, 174, 151)
STONE = (92, 88, 76)
STONE_DARK = (62, 58, 51)
COBBLE = (116, 109, 92)
DIRT = (94, 70, 49)
GRASS = (91, 126, 74)
WATER = (62, 103, 138)
LAPIS = (78, 102, 154)
REDSTONE = (151, 64, 58)
GOLD = (181, 139, 63)
AMETHYST = (121, 88, 148)
OBSIDIAN = (47, 43, 55)
EMPTY = (45, 43, 38)


def load_font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    names = [
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
    ]
    for name in names:
        try:
            return ImageFont.truetype(name, size=size)
        except OSError:
            pass
    return ImageFont.load_default()


FONT_TITLE = load_font(28, True)
FONT_SUB = load_font(17)
FONT_LABEL = load_font(19, True)
FONT_SMALL = load_font(15)
FONT_NODE = load_font(20, True)
FONT_TINY = load_font(13)

LAYOUT_MANIFEST = Path(__file__).with_name("minecraft_style_layout.json")
_ACTIVE_LAYOUT_CHECKS: list[dict[str, object]] = []
_LAYOUT_IMAGES: dict[str, dict[str, object]] = {}
REFERENCED_FILES = {
    diagram['file']
    for path in (ROOT / 'question_bank/official').glob('p*.json')
    for diagram in json.loads(path.read_text(encoding='utf-8')).get('diagrams', [])
}


def text_size(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont) -> tuple[int, int]:
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0], box[3] - box[1]


def _as_int_box(box: Sequence[float | int]) -> list[int]:
    return [int(round(value)) for value in box]


def _record_text_fit(
    text: str,
    container: tuple[int, int, int, int],
    text_box: tuple[float, float, float, float],
    kind: str,
) -> None:
    cx1, cy1, cx2, cy2 = container
    tx1, ty1, tx2, ty2 = text_box
    if tx1 < cx1 or ty1 < cy1 or tx2 > cx2 or ty2 > cy2:
        raise ValueError(
            f"{kind} text overflows its box: {text!r} text={_as_int_box(text_box)} "
            f"container={_as_int_box(container)}"
        )
    _ACTIVE_LAYOUT_CHECKS.append(
        {
            "kind": kind,
            "text": text,
            "container": _as_int_box(container),
            "textBounds": _as_int_box(text_box),
        }
    )


def centered_text(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    font: ImageFont.ImageFont,
    fill=TEXT,
) -> None:
    if text == "":
        return
    tw, th = text_size(draw, text, font)
    x1, y1, x2, y2 = box
    position = (x1 + (x2 - x1 - tw) / 2, y1 + (y2 - y1 - th) / 2 - 1)
    text_box = draw.textbbox(position, text, font=font)
    _record_text_fit(text, box, text_box, "centered_text")
    draw.text(position, text, font=font, fill=fill)


def canvas(title: str, subtitle: str = "") -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, W, H), fill=BG)
    draw.rectangle((22, 68, W - 22, H - 22), fill=PANEL, outline=PANEL_EDGE, width=4)
    draw.text((28, 18), title, font=FONT_TITLE, fill=TEXT)
    if subtitle:
        draw.text((30, 49), subtitle, font=FONT_SUB, fill=MUTED)
    return img, draw


def save(img: Image.Image, name: str) -> None:
    # Keep the bank closed over referenced assets; retired illustrations must not
    # reappear whenever this reproducible generator is run.
    if name not in REFERENCED_FILES:
        _ACTIVE_LAYOUT_CHECKS.clear()
        return
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / name
    img.save(path)
    image_entry = _LAYOUT_IMAGES.setdefault(name, {"file": name, "checks": []})
    image_entry["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
    image_entry["width"] = img.width
    image_entry["height"] = img.height
    image_entry["checks"].extend(_ACTIVE_LAYOUT_CHECKS)
    _ACTIVE_LAYOUT_CHECKS.clear()


def write_layout_manifest() -> None:
    manifest = {
        "version": 1,
        "source": Path(__file__).name,
        "images": [value for _, value in sorted(_LAYOUT_IMAGES.items())],
    }
    LAYOUT_MANIFEST.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def block_rect(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    fill=STONE,
    outline=STONE_DARK,
    width: int = 3,
) -> None:
    draw.rectangle(box, fill=fill, outline=outline, width=width)
    x1, y1, x2, y2 = box
    draw.line((x1 + 3, y1 + 3, x2 - 3, y1 + 3), fill=tuple(min(255, c + 25) for c in fill), width=2)
    draw.line((x1 + 3, y1 + 3, x1 + 3, y2 - 3), fill=tuple(min(255, c + 18) for c in fill), width=2)


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], fill=GRID_LIGHT, width: int = 4) -> None:
    x1, y1 = start
    x2, y2 = end
    draw.line((x1, y1, x2, y2), fill=fill, width=width)
    if abs(x2 - x1) >= abs(y2 - y1):
        direction = 1 if x2 >= x1 else -1
        pts = [(x2, y2), (x2 - 10 * direction, y2 - 7), (x2 - 10 * direction, y2 + 7)]
    else:
        direction = 1 if y2 >= y1 else -1
        pts = [(x2, y2), (x2 - 7, y2 - 10 * direction), (x2 + 7, y2 - 10 * direction)]
    draw.polygon(pts, fill=fill)


def node(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    label: str,
    fill=COBBLE,
    w: int = 48,
    h: int = 38,
) -> tuple[int, int, int, int]:
    cx, cy = center
    tw, th = text_size(draw, label, FONT_NODE)
    w = max(w, tw + 22)
    h = max(h, th + 18)
    box = (cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2)
    block_rect(draw, box, fill=fill)
    centered_text(draw, box, label, FONT_NODE)
    return box


def list_nodes(
    draw: ImageDraw.ImageDraw,
    values: Sequence[str],
    x: int,
    y: int,
    colors: Sequence[tuple[int, int, int]] | None = None,
    gap: int = 72,
    label: str = "",
) -> list[tuple[int, int, int, int]]:
    if label:
        draw.text((x, y - 38), label, font=FONT_LABEL, fill=MUTED)
    boxes = []
    for i, value in enumerate(values):
        fill = colors[i] if colors else COBBLE
        box = node(draw, (x + i * gap, y), str(value), fill=fill)
        boxes.append(box)
        if i + 1 < len(values):
            arrow(draw, (box[2] + 4, y), (x + (i + 1) * gap - 28, y))
    return boxes


def draw_array(
    draw: ImageDraw.ImageDraw,
    values: Sequence[str | int],
    x: int,
    y: int,
    cell: int = 46,
    highlights: Iterable[int] = (),
    label: str = "",
    height: int | None = None,
) -> None:
    highlight_set = set(highlights)
    if label:
        draw.text((x, y - 34), label, font=FONT_LABEL, fill=MUTED)
    for i, value in enumerate(values):
        fill = GOLD if i in highlight_set else STONE
        box = (x + i * cell, y, x + (i + 1) * cell, y + (cell if height is None else height))
        block_rect(draw, box, fill=fill)
        centered_text(draw, box, str(value), FONT_LABEL)
        centered_text(draw, (box[0], box[3] + 4, box[2], box[3] + 22), str(i), FONT_TINY, MUTED)


def draw_matrix(
    draw: ImageDraw.ImageDraw,
    matrix: Sequence[Sequence[str | int]],
    x: int,
    y: int,
    cell: int = 44,
    highlights: Iterable[tuple[int, int]] = (),
    thick_every: int | None = None,
) -> None:
    highlight_set = set(highlights)
    rows = len(matrix)
    cols = len(matrix[0]) if rows else 0
    for r, row in enumerate(matrix):
        for c, value in enumerate(row):
            fill = GOLD if (r, c) in highlight_set else STONE
            if value == ".":
                fill = EMPTY
            box = (x + c * cell, y + r * cell, x + (c + 1) * cell, y + (r + 1) * cell)
            block_rect(draw, box, fill=fill, outline=GRID, width=2)
            centered_text(draw, box, str(value), FONT_SMALL if len(str(value)) > 2 else FONT_LABEL)
    for r in range(rows + 1):
        line_w = 5 if thick_every and r % thick_every == 0 else 2
        draw.line((x, y + r * cell, x + cols * cell, y + r * cell), fill=GRID_LIGHT, width=line_w)
    for c in range(cols + 1):
        line_w = 5 if thick_every and c % thick_every == 0 else 2
        draw.line((x + c * cell, y, x + c * cell, y + rows * cell), fill=GRID_LIGHT, width=line_w)


def bar_chart(
    draw: ImageDraw.ImageDraw,
    heights: Sequence[int],
    x: int,
    y: int,
    width: int,
    height: int,
    water: Sequence[int] | None = None,
    label: str = "",
) -> None:
    if label:
        draw.text((x, y - 34), label, font=FONT_LABEL, fill=MUTED)
    max_h = max(heights) if heights else 1
    bar_gap = 6
    bar_w = max(18, (width - bar_gap * (len(heights) - 1)) // len(heights))
    base = y + height
    draw.line((x - 10, base, x + width + 10, base), fill=GRID_LIGHT, width=4)
    for i, h in enumerate(heights):
        bx1 = x + i * (bar_w + bar_gap)
        bh = int((h / max_h) * (height - 12))
        if water and water[i] > h:
            wh = int((water[i] / max_h) * (height - 12))
            draw.rectangle((bx1, base - wh, bx1 + bar_w, base - bh), fill=WATER, outline=(45, 76, 105), width=2)
        block_rect(draw, (bx1, base - bh, bx1 + bar_w, base), fill=DIRT if h > 0 else STONE_DARK)
        centered_text(draw, (bx1, base + 5, bx1 + bar_w, base + 25), str(i), FONT_TINY, MUTED)
        centered_text(draw, (bx1, base - bh - 24, bx1 + bar_w, base - bh - 5), str(h), FONT_TINY, TEXT)


def binary_tree(
    draw: ImageDraw.ImageDraw,
    nodes: dict[str, tuple[int, int, str]],
    edges: Sequence[tuple[str, str]],
    colors: dict[str, tuple[int, int, int]] | None = None,
) -> None:
    colors = colors or {}
    for a, b in edges:
        ax, ay, _ = nodes[a]
        bx, by, _ = nodes[b]
        draw.line((ax, ay + 18, bx, by - 18), fill=GRID_LIGHT, width=4)
    for key, (x, y, label) in nodes.items():
        node(draw, (x, y), label, fill=colors.get(key, COBBLE))


def title_chip(draw: ImageDraw.ImageDraw, text: str, x: int, y: int, fill=OBSIDIAN) -> None:
    tw, th = text_size(draw, text, FONT_LABEL)
    box = (x, y, x + tw + 28, y + max(42, th + 22))
    block_rect(draw, box, fill=fill)
    position = (x + 14, y + 8)
    _record_text_fit(text, box, draw.textbbox(position, text, font=FONT_LABEL), "title_chip")
    draw.text(position, text, font=FONT_LABEL, fill=TEXT)


def sudoku() -> None:
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, W, H), fill=BG)
    draw.rectangle((120, 42, W - 120, H - 42), fill=PANEL, outline=PANEL_EDGE, width=4)
    board = [
        ["5", "3", ".", ".", "7", ".", ".", ".", "."],
        ["6", ".", ".", "1", "9", "5", ".", ".", "."],
        [".", "9", "8", ".", ".", ".", ".", "6", "."],
        ["8", ".", ".", ".", "6", ".", ".", ".", "3"],
        ["4", ".", ".", "8", ".", "3", ".", ".", "1"],
        ["7", ".", ".", ".", "2", ".", ".", ".", "6"],
        [".", "6", ".", ".", ".", ".", "2", "8", "."],
        [".", ".", ".", "4", "1", "9", ".", ".", "5"],
        [".", ".", ".", ".", "8", ".", ".", "7", "9"],
    ]
    draw_matrix(draw, board, 222, 54, cell=36, thick_every=3)
    save(img, "p7_example1.png")


def pascal() -> None:
    img, draw = canvas("p19 Pascal's Triangle", "Each row is built from the two numbers above it.")
    rows = [[1], [1, 1], [1, 2, 1], [1, 3, 3, 1], [1, 4, 6, 4, 1]]
    y = 102
    cell = 46
    for r, row in enumerate(rows):
        start = W // 2 - len(row) * cell // 2
        for c, value in enumerate(row):
            box = (start + c * cell, y + r * 52, start + (c + 1) * cell - 6, y + r * 52 + 38)
            block_rect(draw, box, fill=GOLD if r == len(rows) - 1 else STONE)
            centered_text(draw, box, str(value), FONT_LABEL)
    save(img, "p19_example1.png")


def bars() -> None:
    img, draw = canvas("p24 Container With Most Water", "Input heights are vertical lines; choose any two lines.")
    bar_chart(draw, [1, 8, 6, 2, 5, 4, 8, 3, 7], 95, 126, 560, 210, label="height = [1,8,6,2,5,4,8,3,7]")
    save(img, "p24_example1.png")

    img, draw = canvas("p25 Trapping Rain Water", "Elevation map with water held between taller blocks.")
    height = [0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]
    water = [0, 1, 1, 2, 2, 2, 2, 3, 2, 2, 2, 1]
    bar_chart(draw, height, 72, 126, 624, 210, water=water, label="height = [0,1,0,2,1,0,1,3,2,1,2,1]")
    save(img, "p25_example1.png")

    img, draw = canvas("p47 Largest Rectangle in Histogram", "Bars have width 1; the task is to find the best rectangle area.")
    bar_chart(draw, [2, 1, 5, 6, 2, 3], 150, 126, 460, 210, label="heights = [2,1,5,6,2,3]")
    save(img, "p47_example1.png")


def stack_and_search() -> None:
    img, draw = canvas("p41 Valid Parentheses", "Opening brackets must be closed in the correct order.")
    tokens = ["(", "[", "{", "}", "]", ")"]
    draw_array(draw, tokens, 176, 162, cell=62, label="s = \"([{}])\"")
    title_chip(draw, "nested structure", 270, 270, fill=LAPIS)
    save(img, "p41_example1.png")

    img, draw = canvas("p49 Decode String", "A bracketed block repeats according to its leading count.")
    draw_array(draw, ["3", "[", "a", "2", "[", "c", "]", "]"], 104, 146, cell=58, label="s = \"3[a2[c]]\"")
    title_chip(draw, "counts and brackets are part of the input", 172, 258)
    save(img, "p49_example1.png")

    img, draw = canvas("p52 Search a 2D Matrix", "Rows are sorted and each row starts after the previous row.")
    matrix = [[1, 3, 5, 7], [10, 11, 16, 20], [23, 30, 34, 60]]
    draw_matrix(draw, matrix, 150, 128, cell=58)
    title_chip(draw, "target = 3", 460, 158, fill=LAPIS)
    save(img, "p52_example1.png")

    img, draw = canvas("p60 Search Range", "Input is sorted; return the first and last position of target.")
    draw_array(draw, [5, 7, 7, 8, 8, 10], 155, 162, cell=64, label="nums = [5,7,7,8,8,10]")
    title_chip(draw, "target = 8", 287, 258, fill=LAPIS)
    save(img, "p60_search_range.png")

    img, draw = canvas("p61 Peak Index in a Mountain Array", "Values strictly climb, then strictly fall.")
    bar_chart(draw, [0, 2, 5, 8, 6, 3, 1], 130, 130, 500, 205, label="arr = [0,2,5,8,6,3,1]")
    save(img, "p61_mountain_peak.png")


def linked_lists() -> None:
    img, draw = canvas("p65 Reverse Linked List", "Input list before any pointer changes.")
    list_nodes(draw, ["1", "2", "3", "4", "5"], 145, 200, label="head")
    save(img, "p65_example1.png")

    img, draw = canvas("p66 Merge Two Sorted Lists", "Two sorted input lists.")
    list_nodes(draw, ["1", "2", "4"], 175, 160, label="list1")
    list_nodes(draw, ["1", "3", "4"], 175, 270, label="list2")
    save(img, "p66_example1.png")

    img, draw = canvas("p67 Reorder List", "Input list to be rearranged in alternating order.")
    list_nodes(draw, ["1", "2", "3", "4", "5"], 145, 200, label="head")
    save(img, "p67_example1.png")

    img, draw = canvas("p68 Remove Nth From End", "Example input list with n counted from the tail.")
    list_nodes(draw, ["1", "2", "3", "4", "5"], 145, 190, label="head = [1,2,3,4,5]")
    title_chip(draw, "n = 2 from end", 282, 266, fill=LAPIS)
    save(img, "p68_example1.png")

    img, draw = canvas("p69 Copy List With Random Pointer", "Example 1: next links run left to right; random targets use node indices.")
    list_nodes(draw, ["7", "13", "11", "10", "1"], 190, 132, gap=86, label="next: last node -> null")
    for row, (label, values) in enumerate([
        ("index", ["0", "1", "2", "3", "4"]),
        ("random index", ["null", "0", "4", "2", "0"]),
    ]):
        y = 215 + row * 58
        centered_text(draw, (38, y, 163, y + 48), label, FONT_SMALL, MUTED)
        for i, value in enumerate(values):
            box = (163 + i * 86, y, 249 + i * 86, y + 48)
            block_rect(draw, box, fill=AMETHYST if row else STONE_DARK)
            centered_text(draw, box, value, FONT_LABEL)
    centered_text(draw, (60, 344, 710, 386), "Example: node 2 (value 11) has random -> node 4 (value 1)", FONT_LABEL)
    save(img, "p69_example1.png")

    img, draw = canvas("p70 Add Two Numbers", "Digits are stored in reverse order in linked lists.")
    list_nodes(draw, ["2", "4", "3"], 190, 150, label="l1 represents 342")
    list_nodes(draw, ["5", "6", "4"], 190, 272, label="l2 represents 465")
    save(img, "p70_example_prompt.png")

    img, draw = canvas("p71 Linked List Cycle", "A tail node may point back to an earlier node.")
    boxes = list_nodes(draw, ["3", "2", "0", "-4"], 160, 200, label="head")
    arrow(draw, ((boxes[3][0] + boxes[3][2]) // 2, boxes[3][3] + 4), ((boxes[1][0] + boxes[1][2]) // 2, boxes[1][3] + 4), fill=AMETHYST)
    save(img, "p71_example1.png")

    img, draw = canvas("p72 Find the Duplicate Number", "Array length is n + 1 and values are in the range 1..n.")
    draw_array(draw, [1, 3, 4, 2, 2], 215, 162, cell=62, label="nums = [1,3,4,2,2]")
    title_chip(draw, "do not modify nums", 258, 266, fill=LAPIS)
    save(img, "p72_duplicate.png")

    img, draw = canvas("p73 LRU Cache", "Example 1, after put(1,1), put(2,2), get(1); capacity = 2.")
    draw_array(draw, ["2", "1"], 270, 158, cell=110, label="keys in usage order")
    centered_text(draw, (236, 280, 380, 326), "least recent", FONT_LABEL, MUTED)
    centered_text(draw, (380, 280, 534, 326), "most recent", FONT_LABEL, MUTED)
    centered_text(draw, (100, 342, 670, 382), "get(1) makes key 1 the most recently used key", FONT_LABEL)
    save(img, "p73_lru.png")

    img, draw = canvas("p74 Merge k Sorted Lists", "Multiple sorted lists are given as input.")
    list_nodes(draw, ["1", "4", "5"], 135, 130, label="list 1")
    list_nodes(draw, ["1", "3", "4"], 135, 230, label="list 2")
    list_nodes(draw, ["2", "6"], 135, 330, label="list 3")
    save(img, "p74_example1.png")

    img, draw = canvas("p75 Reverse Nodes in k-Group", "Example 1: k = 2. Only complete groups are reversed.")
    boxes = list_nodes(draw, ["1", "2", "3", "4", "5"], 190, 175, gap=86, label="input nodes")
    for start, end, label, color in [(0, 1, "group 1", LAPIS), (2, 3, "group 2", GOLD), (4, 4, "leftover", STONE)]:
        x1, x2 = boxes[start][0] - 12, boxes[end][2] + 12
        draw.rectangle((x1, 135, x2, 217), outline=color, width=4)
        centered_text(draw, (x1 - 15, 252, x2 + 15, 292), label, FONT_LABEL, color)
    centered_text(draw, (90, 334, 685, 380), "The final group has only one node, so it stays in place", FONT_LABEL)
    save(img, "p75_example_prompt.png")

    img, draw = canvas("p76 Palindrome Linked List", "Check whether the values read the same forward and backward.")
    list_nodes(draw, ["1", "2", "2", "1"], 180, 200, label="head")
    save(img, "p76_example1.png")

    img, draw = canvas("p77 Intersection of Two Linked Lists", "Two list heads may share the same tail nodes.")
    a = list_nodes(draw, ["4", "1"], 95, 150, label="headA")
    b = list_nodes(draw, ["5", "6", "1"], 70, 292, label="headB")
    shared = list_nodes(draw, ["8", "4", "5"], 390, 220, colors=[GOLD, GOLD, GOLD], label="shared tail")
    arrow(draw, (a[-1][2] + 4, 150), (shared[0][0] - 8, 220))
    arrow(draw, (b[-1][2] + 4, 292), (shared[0][0] - 8, 220))
    save(img, "p77_example1.png")

    img, draw = canvas("p78 Sort List", "Sort the values in a singly linked list.")
    list_nodes(draw, ["4", "2", "1", "3"], 180, 200, label="head")
    save(img, "p78_example1.png")

    img, draw = canvas("p79 Partition List", "Partition around x while keeping relative order inside each side.")
    list_nodes(draw, ["1", "4", "3", "2", "5", "2"], 95, 190, label="head")
    title_chip(draw, "x = 3", 342, 266, fill=LAPIS)
    save(img, "p79_example1.png")

    img, draw = canvas("p80 Rotate List", "Rotate the list to the right by k positions.")
    list_nodes(draw, ["1", "2", "3", "4", "5"], 145, 190, label="head")
    title_chip(draw, "k = 2", 342, 266, fill=LAPIS)
    save(img, "p80_example1.png")


def trees_81_to_100() -> None:
    def base_tree(title: str, subtitle: str, filename: str, values: dict[str, str], edges: Sequence[tuple[str, str]], colors=None) -> None:
        img, draw = canvas(title, subtitle)
        active = set(values)
        for parent, child in edges:
            active.add(parent)
            active.add(child)
        all_pos = {
            "a": (384, 125, values.get("a", "3")),
            "b": (260, 215, values.get("b", "9")),
            "c": (508, 215, values.get("c", "20")),
            "d": (446, 305, values.get("d", "15")),
            "e": (570, 305, values.get("e", "7")),
            "f": (198, 305, values.get("f", "4")),
            "g": (322, 305, values.get("g", "5")),
        }
        pos = {key: all_pos[key] for key in active}
        binary_tree(draw, pos, edges, colors=colors)
        save(img, filename)

    base_tree("p81 Invert Binary Tree", "Input tree; return its mirrored structure.", "p81_example1.png",
              {"a": "4", "b": "2", "c": "7", "f": "1", "g": "3", "d": "6", "e": "9"},
              [("a", "b"), ("a", "c"), ("b", "f"), ("b", "g"), ("c", "d"), ("c", "e")])
    base_tree("p82 Maximum Depth", "Measure the longest root-to-leaf depth.", "p82_example1.png",
              {"a": "3", "b": "9", "c": "20", "d": "15", "e": "7"},
              [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])
    base_tree("p83 Diameter of Binary Tree", "The diameter is measured by edges between two nodes.", "p83_example1.png",
              {"a": "1", "b": "2", "c": "3", "f": "4", "g": "5"},
              [("a", "b"), ("a", "c"), ("b", "f"), ("b", "g")])
    base_tree("p84 Balanced Binary Tree", "Compare subtree heights at every node.", "p84_example1.png",
              {"a": "3", "b": "9", "c": "20", "d": "15", "e": "7"},
              [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])
    img, draw = canvas("p84 Balanced Binary Tree", "A deeper one-sided branch is part of the input.")
    binary_tree(
        draw,
        {
            "a": (384, 105, "1"),
            "b": (288, 178, "2"),
            "c": (480, 178, "2"),
            "d": (238, 252, "3"),
            "e": (338, 252, "3"),
            "f": (205, 326, "4"),
            "g": (271, 326, "4"),
        },
        [("a", "b"), ("a", "c"), ("b", "d"), ("b", "e"), ("d", "f"), ("d", "g")],
    )
    save(img, "p84_example2.png")
    img, draw = canvas("p85 Same Tree", "Compare tree p and tree q node by node.")
    draw.text((150, 102), "p", font=FONT_LABEL, fill=MUTED)
    binary_tree(
        draw,
        {"a": (220, 142, "1"), "b": (170, 222, "2"), "c": (270, 222, "3")},
        [("a", "b"), ("a", "c")],
    )
    draw.text((480, 102), "q", font=FONT_LABEL, fill=MUTED)
    binary_tree(
        draw,
        {"a": (550, 142, "1"), "b": (500, 222, "2"), "c": (600, 222, "3")},
        [("a", "b"), ("a", "c")],
    )
    save(img, "p85_example1.png")

    img, draw = canvas("p85 Same Tree", "Compare tree p and tree q node by node.")
    draw.text((150, 102), "p", font=FONT_LABEL, fill=MUTED)
    binary_tree(
        draw,
        {"a": (220, 142, "1"), "b": (170, 222, "2")},
        [("a", "b")],
    )
    draw.text((480, 102), "q", font=FONT_LABEL, fill=MUTED)
    binary_tree(
        draw,
        {"a": (550, 142, "1"), "c": (600, 222, "2")},
        [("a", "c")],
    )
    save(img, "p85_example2.png")
    base_tree("p86 Subtree of Another Tree", "Main tree from the example input.", "p86_example1.png",
              {"a": "3", "b": "4", "c": "5", "f": "1", "g": "2"},
              [("a", "b"), ("a", "c"), ("b", "f"), ("b", "g")])
    base_tree("p86 Subtree of Another Tree", "Candidate subtree from the example input.", "p86_example2.png",
              {"a": "4", "b": "1", "c": "2"},
              [("a", "b"), ("a", "c")])
    img, draw = canvas("p87 LCA in a BST", "Example 1: p = 2 and q = 8 are yellow; all input nodes are shown.")
    binary_tree(draw, {
        "a": (384, 106, "6"), "b": (230, 178, "2"), "c": (540, 178, "8"),
        "d": (130, 255, "0"), "e": (300, 255, "4"), "f": (477, 255, "7"), "g": (613, 255, "9"),
        "h": (247, 344, "3"), "i": (357, 344, "5")
    }, [("a","b"),("a","c"),("b","d"),("b","e"),("c","f"),("c","g"),("e","h"),("e","i")],
    colors={"b": GOLD, "c": GOLD})
    save(img, "p87_example1.png")
    base_tree("p88 Level Order Traversal", "Return values by tree level.", "p88_example1.png",
              {"a": "3", "b": "9", "c": "20", "d": "15", "e": "7"},
              [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])
    base_tree("p89 Right Side View", "View the tree from the right side.", "p89_example1.png",
              {"a": "1", "b": "2", "c": "3", "g": "5", "e": "4"},
              [("a", "b"), ("a", "c"), ("b", "g"), ("c", "e")])
    base_tree("p90 Count Good Nodes", "A node is compared with values on the root path.", "p90_example1.png",
              {"a": "3", "b": "1", "c": "4", "g": "3", "d": "1", "e": "5"},
              [("a", "b"), ("a", "c"), ("b", "g"), ("c", "d"), ("c", "e")])
    base_tree("p91 Validate BST", "Example input tree.", "p91_example1.png",
              {"a": "2", "b": "1", "c": "3"},
              [("a", "b"), ("a", "c")])
    base_tree("p91 Validate BST", "Another example input tree.", "p91_example2.png",
              {"a": "5", "b": "1", "c": "4", "d": "3", "e": "6"},
              [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])
    base_tree("p92 Kth Smallest in BST", "BST input with k shown separately.", "p92_example1.png",
              {"a": "3", "b": "1", "c": "4", "g": "2"},
              [("a", "b"), ("a", "c"), ("b", "g")])

    img, draw = canvas("p93 Build Tree From Traversals", "Preorder and inorder describe the same tree.")
    draw_array(draw, [3, 9, 20, 15, 7], 130, 130, cell=58, label="preorder")
    draw_array(draw, [9, 3, 15, 20, 7], 130, 250, cell=58, label="inorder")
    save(img, "p93_example1.png")

    base_tree("p94 Binary Tree Maximum Path Sum", "A path may start and end at any nodes; it cannot repeat a node.", "p94_example_prompt.png",
              {"a": "-10", "b": "9", "c": "20", "d": "15", "e": "7"},
              [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])
    base_tree("p95 Serialize and Deserialize", "Tree structure must survive encoding and decoding.", "p95_example1.png",
              {"a": "1", "b": "2", "c": "3", "d": "4", "e": "5"},
              [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])
    base_tree("p96 Symmetric Tree", "Left and right sides should mirror each other.", "p96_example1.png",
              {"a": "1", "b": "2", "c": "2", "f": "3", "g": "4", "d": "4", "e": "3"},
              [("a", "b"), ("a", "c"), ("b", "f"), ("b", "g"), ("c", "d"), ("c", "e")])
    img, draw = canvas("p97 Path Sum", "Check whether any root-to-leaf path reaches targetSum.")
    binary_tree(
        draw,
        {
            "a": (384, 104, "5"),
            "b": (250, 180, "4"),
            "c": (518, 180, "8"),
            "d": (178, 260, "11"),
            "e": (454, 260, "13"),
            "f": (590, 260, "4"),
            "g": (120, 340, "7"),
            "h": (236, 340, "2"),
            "i": (650, 340, "1"),
        },
        [
            ("a", "b"),
            ("a", "c"),
            ("b", "d"),
            ("c", "e"),
            ("c", "f"),
            ("d", "g"),
            ("d", "h"),
            ("f", "i"),
        ],
    )
    title_chip(draw, "targetSum = 22", 58, 88, fill=LAPIS)
    save(img, "p97_example1.png")
    img, draw = canvas("p98 Path Sum II", "Return all root-to-leaf paths that match targetSum.")
    binary_tree(
        draw,
        {
            "a": (384, 104, "5"),
            "b": (250, 180, "4"),
            "c": (518, 180, "8"),
            "d": (178, 260, "11"),
            "e": (454, 260, "13"),
            "f": (590, 260, "4"),
            "g": (120, 340, "7"),
            "h": (236, 340, "2"),
            "i": (552, 340, "5"),
            "j": (668, 340, "1"),
        },
        [
            ("a", "b"),
            ("a", "c"),
            ("b", "d"),
            ("c", "e"),
            ("c", "f"),
            ("d", "g"),
            ("d", "h"),
            ("f", "i"),
            ("f", "j"),
        ],
    )
    title_chip(draw, "targetSum = 22", 58, 88, fill=LAPIS)
    save(img, "p98_example1.png")
    img, draw = canvas("p99 Path Sum III", "Count downward parent-to-child paths that sum to targetSum.")
    binary_tree(
        draw,
        {
            "a": (384, 104, "10"),
            "b": (250, 178, "5"),
            "c": (518, 178, "-3"),
            "d": (178, 252, "3"),
            "e": (318, 252, "2"),
            "f": (590, 252, "11"),
            "g": (120, 330, "3"),
            "h": (236, 330, "-2"),
            "i": (376, 330, "1"),
        },
        [
            ("a", "b"),
            ("a", "c"),
            ("b", "d"),
            ("b", "e"),
            ("c", "f"),
            ("d", "g"),
            ("d", "h"),
            ("e", "i"),
        ],
    )
    title_chip(draw, "targetSum = 8", 58, 88, fill=LAPIS)
    save(img, "p99_example1.png")
    base_tree("p100 Flatten Binary Tree", "Input tree before flattening.", "p100_example1.png",
              {"a": "1", "b": "2", "c": "5", "f": "3", "g": "4", "e": "6"},
              [("a", "b"), ("a", "c"), ("b", "f"), ("b", "g"), ("c", "e")])


def graph_nodes(
    draw: ImageDraw.ImageDraw,
    points: dict[str, tuple[int, int, str]],
    edges: Sequence[tuple[str, str]],
    directed: bool = False,
    colors: dict[str, tuple[int, int, int]] | None = None,
) -> None:
    colors = colors or {}
    for a, b in edges:
        ax, ay, _ = points[a]
        bx, by, _ = points[b]
        if directed:
            arrow(draw, (ax, ay), (bx, by), fill=GRID_LIGHT, width=3)
        else:
            draw.line((ax, ay, bx, by), fill=GRID_LIGHT, width=4)
    for key, (x, y, label) in points.items():
        node(draw, (x, y), label, fill=colors.get(key, COBBLE), w=54, h=40)


def draw_intervals(
    draw: ImageDraw.ImageDraw,
    intervals: Sequence[tuple[int, int]],
    x: int,
    y: int,
    width: int,
    row_h: int = 44,
    label: str = "",
    accent: tuple[int, int, int] = LAPIS,
) -> None:
    if label:
        draw.text((x, y - 34), label, font=FONT_LABEL, fill=MUTED)
    min_v = min(a for a, _ in intervals)
    max_v = max(b for _, b in intervals)
    span = max(1, max_v - min_v)
    draw.line((x, y + len(intervals) * row_h + 10, x + width, y + len(intervals) * row_h + 10), fill=GRID, width=3)
    for i, (a, b) in enumerate(intervals):
        sx = x + int((a - min_v) / span * width)
        ex = x + int((b - min_v) / span * width)
        yy = y + i * row_h
        block_rect(draw, (sx, yy, max(sx + 42, ex), yy + 28), fill=accent if i % 2 else STONE)
        centered_text(draw, (sx, yy, max(sx + 42, ex), yy + 28), f"[{a},{b}]", FONT_SMALL)


def text_block(draw: ImageDraw.ImageDraw, text: str, x: int, y: int, fill=STONE) -> tuple[int, int, int, int]:
    tw, th = text_size(draw, text, FONT_LABEL)
    box = (x, y, x + tw + 26, y + max(42, th + 22))
    block_rect(draw, box, fill=fill)
    position = (x + 13, y + 8)
    _record_text_fit(text, box, draw.textbbox(position, text, font=FONT_LABEL), "text_block")
    draw.text(position, text, font=FONT_LABEL, fill=TEXT)
    return box


def diagrams_101_to_200() -> None:
    # Trie and heap/design prompts.
    img, draw = canvas("p101 Implement Trie", "Words share prefixes in a rooted character tree.")
    graph_nodes(draw, {"r": (180, 130, "root"), "a": (260, 190, "a"), "p1": (340, 250, "p"), "p2": (420, 250, "p"), "l": (500, 190, "l"), "e": (580, 130, "e")},
                [("r", "a"), ("a", "p1"), ("p1", "p2"), ("p2", "l"), ("l", "e")], directed=True,
                colors={"p2": GOLD, "e": GOLD})
    title_chip(draw, "words: app, apple", 268, 305, fill=LAPIS)
    centered_text(draw, (80, 361, 700, 399), "Yellow marks the end of a complete inserted word", FONT_LABEL, GOLD)
    save(img, "p101_trie.png")

    img, draw = canvas("p102 WordDictionary", "A dot branches to any child at that depth.")
    graph_nodes(draw, {"r": (188, 112, "root"), "b": (292, 172, "b"), "d": (292, 252, "d"), "m": (292, 332, "m"), "a1": (394, 172, "a"), "a2": (394, 252, "a"), "a3": (394, 332, "a"), "d1": (502, 172, "d"), "d2": (502, 252, "d"), "d3": (502, 332, "d")},
                [("r", "b"), ("r", "d"), ("r", "m"), ("b", "a1"), ("d", "a2"), ("m", "a3"), ("a1", "d1"), ("a2", "d2"), ("a3", "d3")], directed=True,
                colors={"d1": GOLD, "d2": GOLD, "d3": GOLD})
    centered_text(draw, (75, 367, 710, 402), "Yellow = word end; .ad matches bad, dad and mad", FONT_LABEL, GOLD)
    save(img, "p102_trie.png")

    img, draw = canvas("p103 Word Search II", "Find dictionary words on a character board.")
    board = [["o", "a", "a", "n"], ["e", "t", "a", "e"], ["i", "h", "k", "r"], ["i", "f", "l", "v"]]
    draw_matrix(draw, board, 120, 108, cell=56)
    text_block(draw, "words = [oath, pea, eat, rain]", 410, 154, fill=LAPIS)
    save(img, "p103_board.png")

    img, draw = canvas("p104 Replace Words", "Dictionary roots are prefixes for words in a sentence.")
    graph_nodes(draw, {"r": (170, 128, "root"), "c": (275, 190, "c"), "b": (275, 270, "b"), "r2": (275, 350, "r"), "ca": (390, 190, "a"), "ba": (390, 270, "a"), "ra": (390, 350, "a"), "cat": (505, 190, "t"), "bat": (505, 270, "t"), "rat": (505, 350, "t")},
                [("r", "c"), ("r", "b"), ("r", "r2"), ("c", "ca"), ("b", "ba"), ("r2", "ra"), ("ca", "cat"), ("ba", "bat"), ("ra", "rat")], directed=True,
                colors={"cat": GOLD, "bat": GOLD, "rat": GOLD})
    centered_text(draw, (76, 372, 708, 406), "Yellow marks a complete dictionary root", FONT_LABEL, GOLD)
    save(img, "p104_trie.png")

    img, draw = canvas("p105 Map Sum Pairs", "Example 1, after insert(apple, 3) and insert(app, 2).")
    graph_nodes(draw, {
        "r": (120, 145, "root"), "a": (220, 145, "a"), "p1": (320, 145, "p"),
        "p2": (420, 145, "p"), "l": (520, 145, "l"), "e": (620, 145, "e")
    }, [("r","a"),("a","p1"),("p1","p2"),("p2","l"),("l","e")], directed=True,
    colors={"p2": GOLD, "e": GOLD})
    centered_text(draw, (328, 211, 510, 263), "app: value 2", FONT_LABEL, GOLD)
    centered_text(draw, (520, 211, 722, 263), "apple: value 3", FONT_LABEL, GOLD)
    centered_text(draw, (70, 299, 700, 342), "Yellow marks a complete inserted key with its stored value", FONT_LABEL)
    centered_text(draw, (70, 349, 700, 390), "Both keys start with ap; sum(ap) = 2 + 3 = 5", FONT_LABEL)
    save(img, "p105_trie.png")

    img, draw = canvas("p106 Kth Largest Stream", "Numbers arrive one by one; k is fixed.")
    draw_array(draw, [4, 5, 8, 2], 172, 146, cell=72, label="initial stream")
    title_chip(draw, "k = 3", 326, 260, fill=LAPIS)
    save(img, "p106_kth_stream.png")

    img, draw = canvas("p111 Design Twitter", "Example 1, after user 1 follows user 2 and user 2 posts tweet 6.")
    text_block(draw, "User 1", 112, 118, fill=LAPIS)
    text_block(draw, "User 2", 498, 118, fill=AMETHYST)
    arrow(draw, (235, 142), (490, 142))
    centered_text(draw, (264, 92, 468, 129), "follows", FONT_LABEL, MUTED)
    text_block(draw, "tweet 5 (older)", 82, 208, fill=STONE)
    text_block(draw, "tweet 6 (newer)", 466, 208, fill=STONE)
    centered_text(draw, (70, 310, 708, 369), "getNewsFeed(1) -> [6, 5]   (newest first)", FONT_LABEL)
    save(img, "p111_design.png")

    img, draw = canvas("p112 Median From Data Stream", "Example 1: each query uses all numbers added so far.")
    draw_array(draw, [1, 2], 118, 130, cell=70, label="after addNum(1), addNum(2)")
    text_block(draw, "median = (1 + 2) / 2 = 1.5", 326, 140, fill=LAPIS)
    draw_array(draw, [1, 2, 3], 118, 282, cell=70, label="then addNum(3)")
    text_block(draw, "median = 2", 408, 290, fill=LAPIS)
    save(img, "p112_median_stream.png")

    img, draw = canvas("p116 Subsets", "Return every subset of the input set.")
    draw_array(draw, [1, 2, 3], 260, 128, cell=72, label="nums")
    for i, text in enumerate(["[]", "[1]", "[2]", "[1,2]", "..."]):
        text_block(draw, text, 140 + i * 104, 256, fill=GOLD if i == 0 else STONE)
    save(img, "p116_subsets.png")

    # Backtracking boards.
    img, draw = canvas("p124 N-Queens", "Rule illustration: a queen attacks its row, column and diagonals.")
    q = (1, 1)
    board = [["Q" if (r,c)==q else "x" if r==q[0] or c==q[1] or abs(r-q[0])==abs(c-q[1]) else "" for c in range(4)] for r in range(4)]
    draw_matrix(draw, board, 106, 116, cell=58, highlights={q})
    text_block(draw, "Q: one queen", 415, 130, fill=GOLD)
    text_block(draw, "x: attacked square", 415, 210, fill=STONE)
    centered_text(draw, (383, 300, 713, 361), "No second queen may\noccupy an x square", FONT_LABEL)
    save(img, "p124_example1.png")

    img, draw = canvas("p125 Sudoku Solver", "Fill empty cells while obeying Sudoku rules.")
    board = [
        ["5", "3", ".", ".", "7", ".", ".", ".", "."],
        ["6", ".", ".", "1", "9", "5", ".", ".", "."],
        [".", "9", "8", ".", ".", ".", ".", "6", "."],
        ["8", ".", ".", ".", "6", ".", ".", ".", "3"],
        ["4", ".", ".", "8", ".", "3", ".", ".", "1"],
        ["7", ".", ".", ".", "2", ".", ".", ".", "6"],
        [".", "6", ".", ".", ".", ".", "2", "8", "."],
        [".", ".", ".", "4", "1", "9", ".", ".", "5"],
        [".", ".", ".", ".", "8", ".", ".", "7", "9"],
    ]
    draw_matrix(draw, board, 236, 82, cell=31, thick_every=3)
    save(img, "p125_sudoku.png")

    # Graph and grid prompts.
    img, draw = canvas("p130 Clone Graph", "Adjacency list [[2,4],[1,3],[2,4],[1,3]] has no diagonal edge.")
    graph_nodes(draw, {"1": (260, 135, "1"), "2": (490, 135, "2"), "3": (490, 295, "3"), "4": (260, 295, "4")},
                [("1", "2"), ("2", "3"), ("3", "4"), ("4", "1")])
    save(img, "p130_example1.png")

    img, draw = canvas("p131 Walls and Gates", "INF rooms, gates, and walls are provided in a grid.")
    grid = [["INF", "-1", "0", "INF"], ["INF", "INF", "INF", "-1"], ["INF", "-1", "INF", "-1"], ["0", "-1", "INF", "INF"]]
    draw_matrix(draw, grid, 180, 96, cell=70, highlights={(0, 2), (3, 0)})
    save(img, "p131_walls_gates.png")

    img, draw = canvas("p132 Rotting Oranges", "Grid cells can be empty, fresh, or rotten oranges.")
    grid = [[2, 1, 1], [1, 1, 0], [0, 1, 1]]
    draw_matrix(draw, grid, 250, 106, cell=72, highlights={(0, 0)})
    title_chip(draw, "0 empty  1 fresh  2 rotten", 224, 344, fill=LAPIS)
    save(img, "p132_example1.png")

    img, draw = canvas("p133 Pacific Atlantic Water Flow", "Heights grid with ocean borders.")
    grid = [[1, 2, 2, 3, 5], [3, 2, 3, 4, 4], [2, 4, 5, 3, 1], [6, 7, 1, 4, 5], [5, 1, 1, 2, 4]]
    draw_matrix(draw, grid, 190, 86, cell=48)
    title_chip(draw, "Pacific", 80, 116, fill=WATER)
    title_chip(draw, "Atlantic", 548, 314, fill=WATER)
    save(img, "p133_example1.png")

    img, draw = canvas("p134 Surrounded Regions", "Board cells are X or O.")
    grid = [["X", "X", "X", "X"], ["X", "O", "O", "X"], ["X", "X", "O", "X"], ["X", "O", "X", "X"]]
    draw_matrix(draw, grid, 244, 104, cell=58, highlights={(1, 1), (1, 2), (2, 2), (3, 1)})
    save(img, "p134_example1.png")

    img, draw = canvas("p135 Course Schedule", "Prerequisite pairs form a directed graph.")
    graph_nodes(draw, {"0": (300, 140, "0"), "1": (470, 240, "1")}, [("0", "1")], directed=True)
    title_chip(draw, "numCourses = 2", 250, 314, fill=LAPIS)
    save(img, "p135_example1.png")

    img, draw = canvas("p136 Course Schedule II", "Return an ordering that satisfies prerequisites.")
    points = {"0": (384, 104, "0"), "1": (270, 220, "1"), "2": (500, 220, "2"), "3": (384, 330, "3")}
    for start, end in [((366, 124), (292, 198)), ((402, 124), (478, 198)),
                       ((288, 242), (362, 308)), ((482, 242), (406, 308))]:
        arrow(draw, start, end, fill=GRID_LIGHT, width=3)
    for key, (x, y, label) in points.items():
        node(draw, (x, y), label, w=54, h=40)
    save(img, "p136_example1.png")

    img, draw = canvas("p137 Graph Valid Tree", "Undirected edges over n labeled nodes.")
    graph_nodes(draw, {"0": (384, 112, "0"), "1": (250, 216, "1"), "2": (384, 216, "2"), "3": (518, 216, "3"), "4": (250, 326, "4")},
                [("0", "1"), ("0", "2"), ("0", "3"), ("1", "4")])
    save(img, "p137_example1.png")

    img, draw = canvas("p138 Connected Components", "Undirected graph may have multiple components.")
    graph_nodes(draw, {"0": (205, 140, "0"), "1": (205, 232, "1"), "2": (205, 324, "2"), "3": (520, 178, "3"), "4": (520, 286, "4")},
                [("0", "1"), ("1", "2"), ("3", "4")])
    save(img, "p138_example1.png")

    img, draw = canvas("p139 Redundant Connection", "Example 1: edges = [[1,2],[1,3],[2,3]].")
    graph_nodes(draw, {"1": (384, 130, "1"), "2": (244, 306, "2"), "3": (524, 306, "3")},
    [("1","2"),("1","3"),("2","3")])
    save(img, "p139_example1.png")

    img, draw = canvas("p140 Word Ladder", "Change one letter at a time using dictionary words.")
    title_chip(draw, "beginWord = hit", 74, 120, fill=LAPIS)
    title_chip(draw, "endWord = cog", 520, 120, fill=AMETHYST)
    arrow(draw, (270, 146), (498, 146), fill=GRID_LIGHT, width=3)
    title_chip(draw, "shortest sequence length?", 264, 178, fill=OBSIDIAN)
    draw_array(draw, ["hot", "dot", "dog", "lot", "log", "cog"], 116, 284, cell=90, label="wordList")
    save(img, "p140_word_ladder.png")

    img, draw = canvas("p141 Word Ladder II", "Return every shortest sequence, not just the length.")
    title_chip(draw, "beginWord = hit", 74, 120, fill=LAPIS)
    title_chip(draw, "endWord = cog", 520, 120, fill=AMETHYST)
    arrow(draw, (270, 146), (498, 146), fill=GRID_LIGHT, width=3)
    title_chip(draw, "all shortest sequences?", 254, 178, fill=OBSIDIAN)
    draw_array(draw, ["hot", "dot", "dog", "lot", "log", "cog"], 116, 284, cell=90, label="wordList")
    save(img, "p141_word_ladder_paths.png")

    img, draw = canvas("p142 Network Delay Time", "Directed weighted edges carry signal times.")
    p142_nodes = {"1": (250, 240, "1"), "2": (384, 120, "2"), "3": (520, 240, "3"), "4": (384, 334, "4")}
    arrow(draw, (366, 136), (270, 224), fill=GRID_LIGHT, width=3)
    node(draw, (324, 174), "1", fill=STONE_DARK, w=34, h=28)
    arrow(draw, (402, 136), (500, 224), fill=GRID_LIGHT, width=3)
    node(draw, (454, 174), "1", fill=STONE_DARK, w=34, h=28)
    arrow(draw, (502, 254), (404, 320), fill=GRID_LIGHT, width=3)
    node(draw, (456, 290), "1", fill=STONE_DARK, w=34, h=28)
    for key, (x, y, label) in p142_nodes.items():
        node(draw, (x, y), label, w=54, h=40, fill=LAPIS if key == "2" else COBBLE)
    title_chip(draw, "source k = 2", 524, 330, fill=AMETHYST)
    text_block(draw, "times = [[2,1,1],[2,3,1],[3,4,1]]", 192, 378, fill=LAPIS)
    save(img, "p142_example1.png")

    img, draw = canvas("p143 Cheapest Flights", "Directed prices; the route must respect the stop limit.")
    arrow(draw, (194, 242), (330, 150), fill=GRID_LIGHT, width=3)
    node(draw, (252, 184), "100", fill=STONE_DARK, w=48, h=30)
    arrow(draw, (358, 154), (358, 260), fill=GRID_LIGHT, width=3)
    node(draw, (390, 208), "100", fill=STONE_DARK, w=48, h=30)
    arrow(draw, (332, 274), (194, 256), fill=GRID_LIGHT, width=3)
    node(draw, (252, 286), "100", fill=STONE_DARK, w=48, h=30)
    arrow(draw, (386, 142), (562, 196), fill=GRID_LIGHT, width=3)
    node(draw, (482, 164), "600", fill=STONE_DARK, w=48, h=30)
    arrow(draw, (386, 276), (562, 224), fill=GRID_LIGHT, width=3)
    node(draw, (482, 270), "200", fill=STONE_DARK, w=48, h=30)
    for key, (x, y, label) in {
        "0": (166, 250, "0"),
        "1": (358, 130, "1"),
        "2": (358, 286, "2"),
        "3": (594, 210, "3"),
    }.items():
        fill = LAPIS if key == "0" else AMETHYST if key == "3" else COBBLE
        node(draw, (x, y), label, fill=fill, w=54, h=40)
    title_chip(draw, "src = 0", 92, 350, fill=LAPIS)
    title_chip(draw, "dst = 3", 544, 350, fill=AMETHYST)
    title_chip(draw, "k = 1 stop", 310, 350, fill=OBSIDIAN)
    save(img, "p143_example1.png")

    img, draw = canvas("p144 Reconstruct Itinerary", "Directed tickets; build one route that uses every edge.")
    arrow(draw, (188, 214), (282, 162), fill=GRID_LIGHT, width=3)
    arrow(draw, (338, 160), (432, 210), fill=GRID_LIGHT, width=3)
    arrow(draw, (488, 210), (582, 162), fill=GRID_LIGHT, width=3)
    arrow(draw, (610, 172), (610, 280), fill=GRID_LIGHT, width=3)
    for x, y, label, fill in [
        (160, 222, "JFK", LAPIS),
        (310, 150, "MUC", COBBLE),
        (460, 222, "LHR", COBBLE),
        (610, 150, "SFO", COBBLE),
        (610, 300, "SJC", COBBLE),
    ]:
        node(draw, (x, y), label, fill=fill, w=60, h=40)
    title_chip(draw, "start = JFK", 82, 344, fill=LAPIS)
    title_chip(draw, "use every ticket", 292, 344, fill=OBSIDIAN)
    title_chip(draw, "lexicographic min", 520, 344, fill=AMETHYST)
    save(img, "p144_example1.png")

    img, draw = canvas("p145 Min Cost to Connect Points", "Input coordinates; connection cost is Manhattan distance.")
    grid_box = (80, 92, 548, 334)
    block_rect(draw, grid_box, fill=EMPTY, outline=PANEL_EDGE, width=3)
    for x in range(grid_box[0] + 48, grid_box[2], 52):
        draw.line((x, grid_box[1] + 8, x, grid_box[3] - 8), fill=GRID, width=2)
    for y in range(grid_box[1] + 42, grid_box[3], 42):
        draw.line((grid_box[0] + 8, y, grid_box[2] - 8, y), fill=GRID, width=2)

    # Shows the distance rule only; this is not the MST result.
    draw.line((160, 286, 256, 286, 256, 232), fill=GOLD, width=5)
    node(draw, (160, 286), "(0,0)", fill=LAPIS, w=76, h=40)
    node(draw, (256, 232), "(2,2)", fill=COBBLE, w=76, h=40)
    node(draw, (316, 122), "(3,10)", fill=COBBLE, w=84, h=40)
    node(draw, (412, 232), "(5,2)", fill=COBBLE, w=76, h=40)
    node(draw, (508, 286), "(7,0)", fill=COBBLE, w=76, h=40)
    title_chip(draw, "|0-2| + |0-2| = 4", 82, 352, fill=GOLD)
    title_chip(draw, "choose n-1 edges", 322, 352, fill=OBSIDIAN)
    title_chip(draw, "min total cost?", 548, 352, fill=AMETHYST)
    save(img, "p145_points.png")

    img, draw = canvas("p146 Alien Dictionary", "These five words are already sorted using an unknown letter order.")
    draw_array(draw, ["wrt","wrf","er","ett","rftt"], 74, 128, cell=124, label="words (in the given order)")
    centered_text(draw, (80, 272, 695, 322), "Compare words using the first letters that differ", FONT_LABEL)
    centered_text(draw, (80, 328, 695, 378), "A complete prefix comes first: ab before abc", FONT_LABEL, MUTED)
    save(img, "p146_graph.png")

    img, draw = canvas("p147 Swim in Rising Water", "Grid values are elevation times.")
    draw_matrix(draw, [[0, 7, 8], [1, 6, 5], [2, 3, 4]], 266, 96, cell=68, highlights={(0, 0), (2, 2)})
    title_chip(draw, "start", 116, 136, fill=LAPIS)
    title_chip(draw, "target", 548, 274, fill=GRASS)
    title_chip(draw, "cells <= t are usable", 208, 340, fill=WATER)
    title_chip(draw, "minimize max elevation", 448, 340, fill=OBSIDIAN)
    save(img, "p147_example1.png")

    img, draw = canvas("p148 Shortest Path in Binary Matrix", "Open cells are 0; blocked cells are 1.")
    draw_matrix(draw, [[0, 0, 0], [1, 1, 0], [1, 1, 0]], 266, 96, cell=68, highlights={(0, 0), (2, 2)})
    title_chip(draw, "start", 118, 136, fill=LAPIS)
    title_chip(draw, "target", 548, 274, fill=GRASS)
    title_chip(draw, "8 directions allowed", 182, 340, fill=AMETHYST)
    title_chip(draw, "length counts cells", 438, 340, fill=OBSIDIAN)
    save(img, "p148_example1.png")

    img, draw = canvas("p149 Path With Minimum Effort", "Effort is the largest adjacent height difference.")
    draw_matrix(draw, [[1, 2, 2], [3, 8, 2], [5, 3, 5]], 266, 96, cell=68, highlights={(0, 0), (2, 2)})
    title_chip(draw, "start", 118, 136, fill=LAPIS)
    title_chip(draw, "target", 548, 274, fill=GRASS)
    title_chip(draw, "4 directions only", 182, 340, fill=AMETHYST)
    title_chip(draw, "minimize max step", 438, 340, fill=OBSIDIAN)
    save(img, "p149_example1.png")

    img, draw = canvas("p150 Is Graph Bipartite?", "Example 1: the drawing shows the input edges, without assigning groups.")
    graph_nodes(draw, {"0": (300, 130, "0"),"1": (170, 280, "1"),"2": (432, 280, "2"),"3": (578, 130, "3")},
    [("0","1"),("0","2"),("0","3"),("1","2"),("2","3")])
    centered_text(draw, (58, 343, 710, 390), "Can every edge connect nodes in two different groups?", FONT_LABEL)
    save(img, "p150_example1.png")

    # DP, greedy, and string prompts.
    img, draw = canvas("p151 Climbing Stairs", "n = 3: start at level 0 and finish at level 3 (the top).")
    for i in range(4):
        x, y = 92 + i * 166, 316 - i * 60
        box = (x, y, x + 100, y + 45)
        block_rect(draw, box, fill=LAPIS if i==0 else GRASS if i==3 else STONE)
        centered_text(draw, box, str(i), FONT_LABEL)
        centered_text(draw, (x, y+51, x+100, y+83), "start" if i==0 else "top" if i==3 else "level", FONT_LABEL, MUTED)
    arrow(draw, (192, 324), (252, 282), fill=GOLD)
    arrow(draw, (160, 304), (424, 223), fill=AMETHYST)
    arrow(draw, (358, 264), (418, 222), fill=GOLD)
    arrow(draw, (326, 244), (590, 163), fill=AMETHYST)
    arrow(draw, (524, 204), (584, 162), fill=GOLD)
    title_chip(draw, "1 step", 71, 94, fill=GOLD)
    title_chip(draw, "2 steps", 214, 94, fill=AMETHYST)
    save(img, "p151_climbing_stairs.png")

    img, draw = canvas("p152 Min Cost Climbing Stairs", "Choose start 0 or 1; pay that step's cost, then move 1 or 2 steps.")
    for i, cost in enumerate([10, 15, 20, 0]):
        x, y = 86 + i * 166, 305 - i * 52
        box = (x, y, x+110, y+50)
        block_rect(draw, box, fill=GRASS if i==3 else STONE)
        centered_text(draw, box, "top: 0" if i==3 else str(cost), FONT_LABEL)
        centered_text(draw, (x, y+53, x+110, y+87), f"index {i}", FONT_LABEL, MUTED)
    for i in range(3):
        x,y=86+i*166,305-i*52
        arrow(draw,(x+110,y+12),(x+160,y-30),fill=GOLD)
    for i in range(2):
        x,y=86+i*166,305-i*52
        arrow(draw,(x+70,y-10),(x+326,y-82),fill=AMETHYST)
    title_chip(draw, "start 0", 60, 92, fill=LAPIS)
    title_chip(draw, "or start 1", 190, 92, fill=LAPIS)
    title_chip(draw, "1 step", 393, 92, fill=GOLD)
    title_chip(draw, "2 steps", 540, 92, fill=AMETHYST)
    save(img, "p152_min_cost_climbing_stairs.png")

    img, draw = canvas("p153 House Robber", "Choose houses, but never choose adjacent neighbors.")
    values = [2, 7, 9, 3, 1]
    house_boxes = []
    for i, money in enumerate(values):
        x = 90 + i * 126
        base_y = 246
        body = (x, base_y, x + 80, base_y + 76)
        roof = [(x - 8, base_y), (x + 40, base_y - 40), (x + 88, base_y)]
        draw.polygon(roof, fill=DIRT, outline=STONE_DARK)
        block_rect(draw, body, fill=STONE)
        centered_text(draw, (x + 10, base_y + 18, x + 70, base_y + 54), str(money), FONT_NODE)
        centered_text(draw, (x, base_y + 86, x + 80, base_y + 106), f"i={i}", FONT_TINY, MUTED)
        house_boxes.append(body)
    for left, right in zip(house_boxes, house_boxes[1:]):
        y = 176
        arrow(draw, (left[2] - 8, y), (right[0] + 8, y), fill=REDSTONE, width=5)
    title_chip(draw, "nums = [2,7,9,3,1]", 212, 360, fill=STONE_DARK)
    title_chip(draw, "adjacent pair conflicts", 244, 114, fill=REDSTONE)
    title_chip(draw, "choose a non-adjacent set", 456, 62, fill=OBSIDIAN)
    save(img, "p153_house_robber.png")

    img, draw = canvas("p154 House Robber II", "The street is a circle: first and last houses touch.")
    circle_houses = [
        (384, 116, "0", "2"),
        (538, 196, "1", "3"),
        (486, 326, "2", "2"),
        (282, 326, "3", "5"),
        (230, 196, "4", "1"),
    ]
    centers = [(x, y) for x, y, _, _ in circle_houses]
    for a, b in zip(centers, centers[1:] + centers[:1]):
        draw.line((a[0], a[1], b[0], b[1]), fill=GRID_LIGHT, width=4)
    draw.line((230, 196, 384, 116), fill=REDSTONE, width=8)
    draw.line((384, 116, 538, 196), fill=REDSTONE, width=8)
    for x, y, index, money in circle_houses:
        body = (x - 34, y - 14, x + 34, y + 48)
        roof = [(x - 44, y - 14), (x, y - 48), (x + 44, y - 14)]
        draw.polygon(roof, fill=DIRT, outline=STONE_DARK)
        block_rect(draw, body, fill=STONE)
        centered_text(draw, (x - 22, y + 2, x + 22, y + 32), money, FONT_NODE)
        centered_text(draw, (x - 30, y + 52, x + 30, y + 74), f"i={index}", FONT_TINY, MUTED)
    title_chip(draw, "first-last are neighbors", 56, 104, fill=REDSTONE)
    title_chip(draw, "circular order", 298, 226, fill=STONE_DARK)
    save(img, "p154_house_robber_ii.png")

    img, draw = canvas("p155 Longest Palindromic Substring", "The answer is one contiguous block that mirrors around its center.")
    draw_array(draw, ["x", "a", "b", "a", "y"], 224, 150, cell=64, highlights=(1, 2, 3), label='s = "xabay"')
    arrow(draw, (310, 286), (414, 286), fill=GOLD, width=5)
    arrow(draw, (414, 306), (310, 306), fill=GOLD, width=5)
    title_chip(draw, "contiguous substring", 470, 88, fill=OBSIDIAN)
    title_chip(draw, "same forward/back", 254, 324, fill=GOLD)
    title_chip(draw, "not a subsequence", 454, 238, fill=STONE_DARK)
    save(img, "p155_longest_palindromic_substring.png")

    img, draw = canvas("p156 Palindromic Substrings", "Each mirrored range is a separate substring occurrence.")
    draw_array(draw, ["a", "b", "a", "b", "a"], 224, 140, cell=64, highlights=(0, 1, 2, 3, 4), label='s = "ababa"')
    draw.line((256, 248, 384, 248), fill=GOLD, width=5)
    draw.line((256, 238, 256, 258), fill=GOLD, width=5)
    draw.line((384, 238, 384, 258), fill=GOLD, width=5)
    centered_text(draw, (238, 262, 402, 292), "range 0..2", FONT_SMALL, MUTED)
    draw.line((320, 304, 448, 304), fill=REDSTONE, width=5)
    draw.line((320, 294, 320, 314), fill=REDSTONE, width=5)
    draw.line((448, 294, 448, 314), fill=REDSTONE, width=5)
    centered_text(draw, (302, 318, 466, 348), "range 1..3", FONT_SMALL, MUTED)
    title_chip(draw, "overlap is allowed", 454, 244, fill=OBSIDIAN)
    title_chip(draw, "same letters, new range", 210, 340, fill=STONE_DARK)
    save(img, "p156_palindromic_substrings.png")

    img, draw = canvas("p157 Decode Ways", "Groups must be 1..9 or 10..26; leading zero groups fail.")
    draw_array(draw, ["1", "1", "1", "0", "6"], 224, 138, cell=64, highlights=(0, 1, 2, 3, 4), label='s = "11106"')
    draw.line((224, 238, 352, 238), fill=GOLD, width=5)
    draw.line((224, 228, 224, 248), fill=GOLD, width=5)
    draw.line((352, 228, 352, 248), fill=GOLD, width=5)
    centered_text(draw, (214, 252, 362, 282), "1 | 1 | 10 | 6", FONT_SMALL, MUTED)
    draw.line((416, 292, 544, 292), fill=REDSTONE, width=5)
    draw.line((416, 282, 416, 302), fill=REDSTONE, width=5)
    draw.line((544, 282, 544, 302), fill=REDSTONE, width=5)
    centered_text(draw, (406, 306, 554, 336), "06 has leading zero", FONT_SMALL, MUTED)
    title_chip(draw, "single: 1..9", 78, 92, fill=OBSIDIAN)
    title_chip(draw, "pair: 10..26", 500, 92, fill=GOLD)
    title_chip(draw, "0 cannot stand alone", 412, 350, fill=REDSTONE)
    save(img, "p157_decode_ways.png")

    img, draw = canvas("p158 Coin Change", "Use any number of coins of each denomination; the total must equal amount.")
    draw_array(draw, [1,2,5], 142, 138, cell=108, label="denominations")
    title_chip(draw, "unlimited copies", 468, 144, fill=OBSIDIAN)
    title_chip(draw, "amount = 11", 292, 266, fill=LAPIS)
    centered_text(draw, (70, 342, 699, 388), "Return the smallest number of coins, or -1 if impossible", FONT_LABEL)
    save(img, "p158_coin_change.png")

    img, draw = canvas("p159 Maximum Product Subarray", "A candidate uses one nonempty contiguous range, including every interior value.")
    draw_array(draw, [-2,3,-4,0,7,-8], 146, 144, cell=78, highlights=(2,3,4), label="illustrative array")
    draw.rectangle((302,137,536,228), outline=GOLD, width=4)
    centered_text(draw, (70, 278, 706, 326), "[-4, 0, 7] is a legal candidate; its product is 0", FONT_LABEL)
    centered_text(draw, (70, 340, 706, 382), "Skipping the 0 would leave a gap and is not allowed", FONT_LABEL, MUTED)
    save(img, "p159_max_product_subarray.png")

    img, draw = canvas("p160 Word Break", "Split the entire string into dictionary words; keep every character in order.")
    draw_array(draw, ["leet","code"], 170, 144, cell=188, height=76, highlights=(0,1), label='s = "leetcode"')
    centered_text(draw, (80, 280, 700, 327), 'wordDict = ["leet", "code"]', FONT_LABEL)
    centered_text(draw, (80, 340, 700, 385), '"leet" + "code" uses the whole string', FONT_LABEL, MUTED)
    save(img, "p160_word_break.png")

    img, draw = canvas("p161 Longest Increasing Subsequence", "Choose in order; each chosen value must be strictly larger.")
    draw_array(draw, [10, 9, 2, 5, 3, 7, 101, 18], 72, 138, cell=70, highlights=(2, 4, 5, 6), label="nums")
    for start, end in ((2, 4), (4, 5), (5, 6)):
        arrow(draw, (72 + start * 70 + 35, 234), (72 + end * 70 + 35, 234), fill=GOLD, width=4)
    title_chip(draw, "selected: 2 < 3 < 7 < 101", 172, 278, fill=LAPIS)
    title_chip(draw, "skip allowed", 88, 318, fill=STONE_DARK)
    title_chip(draw, "equal values do not extend", 408, 318, fill=REDSTONE)
    save(img, "p161_lis.png")

    img, draw = canvas("p162 Partition Equal Subset Sum", "Use every input occurrence once, dividing them into two groups with equal sums.")
    draw_array(draw, [1,5,11,5], 214, 124, cell=84, label="nums")
    text_block(draw, "group A: 1 + 5 + 5 = 11", 85, 274, fill=GOLD)
    text_block(draw, "group B: 11", 467, 274, fill=LAPIS)
    centered_text(draw, (70, 346, 700, 386), "This is one valid partition of the example input", FONT_LABEL, MUTED)
    save(img, "p162_partition.png")

    img, draw = canvas("p163 Unique Paths", "Example 1: m = 3 rows, n = 7 columns. Move right or down by one cell.")
    board = [["" for _ in range(7)] for _ in range(3)]
    board[0][0], board[2][6] = "S", "E"
    draw_matrix(draw, board, 160, 112, cell=62, highlights={(0,0),(2,6)})
    arrow(draw,(100,331),(212,331),fill=GOLD)
    centered_text(draw,(75,351,236,390),"right",FONT_LABEL,GOLD)
    arrow(draw,(394,326),(394,382),fill=GOLD)
    centered_text(draw,(415,329,553,384),"down",FONT_LABEL,GOLD)
    save(img, "p163_unique_paths.png")

    img, draw = canvas("p164 Longest Common Subsequence", "Keep relative order while matching characters across two strings.")
    draw_array(draw, list("abcde"), 142, 118, cell=62, highlights=(0, 2, 4), label="text1")
    draw.text((154, 282), "text2", font=FONT_LABEL, fill=MUTED)
    draw_array(draw, list("ace"), 252, 264, cell=62, highlights=(0, 1, 2))
    arrow(draw, (173, 180), (283, 264), fill=GOLD, width=4)
    arrow(draw, (297, 180), (345, 264), fill=GOLD, width=4)
    arrow(draw, (421, 180), (407, 264), fill=GOLD, width=4)
    title_chip(draw, "matched: a -> c -> e", 226, 350, fill=LAPIS)
    title_chip(draw, "skip gaps, keep order", 448, 152, fill=STONE_DARK)
    save(img, "p164_lcs.png")

    img, draw = canvas("p165 Stock With Cooldown", "After selling, the next day is locked from buying.")
    bar_chart(draw, [1, 2, 3, 0, 2], 150, 132, 430, 170, label="prices by day")
    title_chip(draw, "sell", 470, 96, fill=GOLD)
    title_chip(draw, "cooldown: no buy", 358, 304, fill=STONE_DARK)
    arrow(draw, (488, 138), (376, 302), fill=GOLD, width=4)
    arrow(draw, (442, 304), (512, 232), fill=LAPIS, width=4)
    save(img, "p165_stock_cooldown.png")

    img, draw = canvas("p166 Coin Change II", "Count combinations that form the amount.")
    draw_array(draw, [1, 2, 5], 250, 138, cell=72, label="coins")
    title_chip(draw, "amount = 5", 352, 314, fill=LAPIS)
    title_chip(draw, "order ignored", 468, 146, fill=STONE_DARK)
    title_chip(draw, "coin types reusable", 112, 260, fill=GOLD)
    save(img, "p166_coin_change2.png")

    img, draw = canvas("p167 Target Sum", "Assign one sign to every index, then count matches.")
    draw_array(draw, [1, 1, 1, 1, 1], 178, 132, cell=72, label="nums")
    title_chip(draw, "choose + or -", 198, 260, fill=GOLD)
    title_chip(draw, "target = 3", 434, 260, fill=LAPIS)
    arrow(draw, (382, 206), (302, 258), fill=GOLD, width=4)
    arrow(draw, (430, 206), (492, 258), fill=LAPIS, width=4)
    save(img, "p167_target_sum.png")

    img, draw = canvas("p168 Interleaving String", "s3 must use all characters from s1 and s2 in order.")
    draw_array(draw, list("aabcc"), 112, 122, cell=52, label="s1")
    draw_array(draw, list("dbbca"), 112, 226, cell=52, label="s2")
    draw_array(draw, list("aadbbcbcac"), 408, 174, cell=31, label="s3")
    arrow(draw, (372, 148), (406, 188), fill=GOLD, width=4)
    arrow(draw, (372, 252), (406, 198), fill=LAPIS, width=4)
    title_chip(draw, "preserve order", 478, 274, fill=STONE_DARK)
    save(img, "p168_interleave.png")

    img, draw = canvas("p169 Longest Increasing Path", "Move only up, down, left, or right to a larger value.")
    draw_matrix(draw, [[9, 9, 4], [6, 6, 8], [2, 1, 1]], 192, 108, cell=66)
    title_chip(draw, "strictly larger", 470, 140, fill=GOLD)
    title_chip(draw, "no diagonals", 488, 236, fill=STONE_DARK)
    save(img, "p169_example1.png")

    img, draw = canvas("p170 Distinct Subsequences", "Choose source indices in order; equal letters at different indices count separately.")
    draw_array(draw, list("rabbbit"), 130, 116, cell=54, highlights=[0, 1, 2, 3, 5, 6], label="s")
    draw_array(draw, list("rabbit"), 160, 278, cell=54, highlights=range(6), label="t")
    arrow(draw, (157, 194), (187, 276), fill=GOLD, width=3)
    arrow(draw, (211, 194), (241, 276), fill=GOLD, width=3)
    arrow(draw, (265, 194), (295, 276), fill=GOLD, width=3)
    arrow(draw, (319, 194), (349, 276), fill=GOLD, width=3)
    arrow(draw, (427, 194), (403, 276), fill=GOLD, width=3)
    arrow(draw, (481, 194), (457, 276), fill=GOLD, width=3)
    title_chip(draw, "skip index 4", 438, 214, fill=STONE_DARK)
    title_chip(draw, "one valid index path", 258, 352, fill=LAPIS)
    save(img, "p170_distinct_subseq.png")

    img, draw = canvas("p171 Edit Distance", "Convert word1 to word2 using insert, delete, and replace.")
    draw_array(draw, list("horse"), 150, 116, cell=58, label="word1")
    draw_array(draw, list("ros"), 236, 258, cell=58, label="word2")
    arrow(draw, (292, 196), (292, 256), fill=GOLD, width=4)
    arrow(draw, (350, 196), (350, 256), fill=GOLD, width=4)
    arrow(draw, (408, 196), (408, 256), fill=GOLD, width=4)
    title_chip(draw, "insert", 482, 114, fill=LAPIS)
    title_chip(draw, "delete", 482, 184, fill=STONE_DARK)
    title_chip(draw, "replace", 482, 254, fill=AMETHYST)
    save(img, "p171_edit_distance.png")

    img, draw = canvas("p172 Burst Balloons", "Example 1: burst value 1 between values 3 and 5 to receive 3 * 1 * 5 coins.")
    draw_array(draw,[3,1,5,8],190,133,cell=90,highlights=(1,),label="current balloons (input)")
    centered_text(draw,(80,260,700,308),"Then the remaining row is [3, 5, 8]",FONT_LABEL)
    centered_text(draw,(70,330,710,387),"An absent outside neighbor has value 1; it is not a balloon",FONT_LABEL,MUTED)
    save(img,"p172_burst_balloons.png")

    img, draw = canvas("p173 Regular Expression Matching", "'*' can skip a token or repeat it.")
    draw_array(draw, list("aab"), 206, 118, cell=62, highlights=[0, 1, 2], label="s")
    draw_array(draw, ["c", "*", "a", "*", "b"], 144, 256, cell=62, highlights=[0, 1, 2, 3, 4], label="p")
    title_chip(draw, "skip c*", 114, 350, fill=REDSTONE)
    title_chip(draw, "repeat a*", 292, 350, fill=LAPIS)
    title_chip(draw, "then b", 498, 350, fill=GRASS)
    arrow(draw, (175, 318), (156, 350), fill=GOLD, width=4)
    arrow(draw, (330, 318), (342, 350), fill=GOLD, width=4)
    arrow(draw, (424, 318), (536, 350), fill=GOLD, width=4)
    arrow(draw, (330, 256), (238, 180), fill=GOLD, width=3)
    arrow(draw, (330, 256), (300, 180), fill=GOLD, width=3)
    arrow(draw, (424, 256), (362, 180), fill=GOLD, width=3)
    save(img, "p173_regex.png")

    img, draw = canvas("p174 Maximum Subarray", "Illustrative input: choose a nonempty contiguous range; no values inside it are skipped.")
    draw_array(draw, [3,-2,5,-1,2], 174, 144, cell=84, highlights=(0,1,2), label="illustrative nums")
    draw.rectangle((169,137,426,231),outline=GOLD,width=4)
    centered_text(draw,(70,288,706,336),"[3, -2, 5] is one legal candidate; its sum is 6",FONT_LABEL)
    centered_text(draw,(70,347,706,388),"This candidate is an illustration, not necessarily the maximum",FONT_SMALL,MUTED)
    save(img,"p174_max_subarray.png")

    img, draw = canvas("p175 Jump Game", "Each value gives a maximum forward jump from that index.")
    draw_array(draw, [2, 3, 1, 1, 4], 170, 148, cell=72, highlights=[0], label="nums")
    arrow(draw, (206, 226), (278, 226), fill=GOLD, width=4)
    arrow(draw, (206, 246), (350, 246), fill=GOLD, width=4)
    title_chip(draw, "from index 0", 176, 286, fill=LAPIS)
    title_chip(draw, "choose 1 or 2 steps", 390, 286, fill=GOLD)
    save(img, "p175_jump_game.png")

    img, draw = canvas("p176 Jump Game II", "Each value gives a maximum forward range.")
    draw_array(draw, [2, 3, 1, 1, 4], 170, 148, cell=72, highlights=[0, 1, 2], label="nums")
    arrow(draw, (206, 226), (278, 226), fill=GOLD, width=4)
    arrow(draw, (206, 246), (350, 246), fill=GOLD, width=4)
    title_chip(draw, "from index 0", 176, 286, fill=LAPIS)
    title_chip(draw, "forward range", 410, 286, fill=GOLD)
    save(img, "p176_jump_game2.png")

    img, draw = canvas("p177 Gas Station", "Choose one station and drive clockwise around the route.")
    route = [
        ("0", "g1 / c3", (384, 126), (336, 172)),
        ("1", "g2 / c4", (566, 206), (608, 192)),
        ("2", "g3 / c5", (496, 330), (492, 354)),
        ("3", "g4 / c1", (272, 330), (190, 354)),
        ("4", "g5 / c2", (202, 206), (82, 192)),
    ]
    arrows = [
        ((424, 142), (530, 190)),
        ((554, 242), (514, 294)),
        ((458, 330), (310, 330)),
        ((254, 294), (216, 242)),
        ((238, 190), (350, 142)),
    ]
    for start, end in arrows:
        arrow(draw, start, end, fill=GRID_LIGHT)
    for label, gas_cost, center, text_pos in route:
        node(draw, center, label, fill=GOLD)
        text_block(draw, gas_cost, text_pos[0], text_pos[1], fill=STONE)
    title_chip(draw, "gas / cost", 334, 244, fill=LAPIS)
    save(img, "p177_gas_station.png")

    img, draw = canvas("p178 Hand of Straights", "Cards must be rearranged into groups of consecutive values.")
    draw_array(draw, [1, 2, 3, 6, 2, 3, 4, 7, 8], 70, 144, cell=68)
    title_chip(draw, "groupSize = 3", 280, 260, fill=LAPIS)
    save(img, "p178_hand_straights.png")

    img, draw = canvas("p179 Merge Triplets", "Choose triplets to form the target triplet.")
    draw_array(draw, ["[2,5,3]", "[1,8,4]", "[1,7,5]"], 118, 138, cell=150, label="triplets")
    title_chip(draw, "target = [2,7,5]", 268, 270, fill=LAPIS)
    save(img, "p179_triplets.png")

    img, draw = canvas("p180 Partition Labels", "The full sample string is shown before any partition is chosen.")
    sample = list("ababcbacadefegdehijhklij")
    palette = [COBBLE, STONE, GOLD, LAPIS, GRASS, WATER, AMETHYST]
    colors = [palette[(ord(ch) - ord("a")) % len(palette)] for ch in sample]
    draw.text((54, 98), "s[0..11]", font=FONT_LABEL, fill=MUTED)
    for index, color in enumerate(colors[:12]):
        x1 = 54 + index * 54
        block_rect(draw, (x1, 128, x1 + 54, 182), fill=color)
        centered_text(draw, (x1, 128, x1 + 54, 182), sample[index], FONT_NODE)
        centered_text(draw, (x1, 194, x1 + 54, 214), str(index), FONT_TINY, fill=MUTED)
    draw.text((54, 238), "s[12..23]", font=FONT_LABEL, fill=MUTED)
    for offset, color in enumerate(colors[12:]):
        index = offset + 12
        x1 = 54 + offset * 54
        block_rect(draw, (x1, 268, x1 + 54, 322), fill=color)
        centered_text(draw, (x1, 268, x1 + 54, 322), sample[index], FONT_NODE)
        centered_text(draw, (x1, 334, x1 + 54, 354), str(index), FONT_TINY, fill=MUTED)
    save(img, "p180_partition_labels.png")

    img, draw = canvas("p181 Valid Parenthesis String", "The '*' character may stand for several choices.")
    draw_array(draw, list("(*))"), 246, 150, cell=72, label="s")
    title_chip(draw, "* can vary", 308, 260, fill=LAPIS)
    save(img, "p181_valid_paren.png")

    # Intervals and matrix/math prompts.
    img, draw = canvas("p182 Insert Interval", "Existing intervals and one new interval are the input.")
    draw_intervals(draw, [(1, 3), (6, 9)], 170, 130, 430, label="intervals")
    draw_intervals(draw, [(2, 5)], 170, 280, 430, label="newInterval", accent=LAPIS)
    save(img, "p182_example1.png")

    img, draw = canvas("p183 Merge Intervals", "Intervals may overlap on a number line.")
    draw_intervals(draw, [(1, 3), (2, 6), (8, 10), (15, 18)], 125, 116, 520, label="intervals")
    save(img, "p183_example1.png")

    img, draw = canvas("p184 Non-overlapping Intervals", "Return how many intervals must be removed.")
    draw_intervals(draw, [(1, 2), (2, 3), (3, 4), (1, 3)], 145, 116, 500, label="intervals")
    save(img, "p184_example1.png")

    img, draw = canvas("p185 Meeting Rooms", "Each meeting is an interval on a timeline.")
    draw_intervals(draw, [(0, 30), (5, 10), (15, 20)], 160, 126, 460, label="meetings")
    save(img, "p185_example1.png")

    img, draw = canvas("p186 Meeting Rooms II", "Find the minimum rooms for meeting intervals.")
    draw_intervals(draw, [(0, 30), (5, 10), (15, 20)], 160, 126, 460, label="meetings")
    save(img, "p186_example1.png")

    img, draw = canvas("p188 Rotate Image", "Input matrix before in-place rotation.")
    draw_matrix(draw, [[1, 2, 3], [4, 5, 6], [7, 8, 9]], 250, 102, cell=72)
    save(img, "p188_example1.png")

    img, draw = canvas("p189 Spiral Matrix", "Return all values from the matrix.")
    draw_matrix(draw, [[1, 2, 3], [4, 5, 6], [7, 8, 9]], 250, 102, cell=72)
    save(img, "p189_example1.png")

    img, draw = canvas("p191 Happy Number", "Repeatedly replace the number by the sum of squared digits.")
    draw_array(draw, [1, 9], 280, 142, cell=72, label="n = 19")
    title_chip(draw, "sum of digit squares", 246, 264, fill=LAPIS)
    save(img, "p191_example1.png")

    img, draw = canvas("p192 Plus One", "Digits represent a non-negative integer.")
    draw_array(draw, [1, 2, 3], 260, 150, cell=72, label="digits")
    save(img, "p192_example1.png")

    img, draw = canvas("p193 Pow(x, n)", "Compute x raised to integer exponent n.")
    text_block(draw, "x = 2.0", 260, 142, fill=LAPIS)
    text_block(draw, "n = 10", 260, 230, fill=AMETHYST)
    save(img, "p193_example1.png")

    img, draw = canvas("p194 Multiply Strings", "Numbers are provided as strings.")
    text_block(draw, "num1 = 123", 220, 142, fill=LAPIS)
    text_block(draw, "num2 = 456", 220, 230, fill=AMETHYST)
    save(img, "p194_example1.png")

    img, draw = canvas("p195 Detect Squares", "Example 1, before the first count([11,10]); three stored points, one copy each.")
    ox,oy,scale=160,330,23
    draw.line((ox,95,ox,oy,620,oy),fill=GRID_LIGHT,width=3)
    centered_text(draw,(625,317,675,354),"x",FONT_LABEL,MUTED)
    centered_text(draw,(109,79,160,115),"y",FONT_LABEL,MUTED)
    for px,py in [(3,10),(11,2),(3,2),(11,10)]:
        x,y=ox+px*scale,oy-py*scale
        query=(px,py)==(11,10)
        block_rect(draw,(x-12,y-12,x+12,y+12),fill=GOLD if query else LAPIS)
        centered_text(draw,(x-70,y+18,x+70,y+54),f"({px},{py})",FONT_LABEL,GOLD if query else TEXT)
    centered_text(draw,(65,367,714,402),"Yellow is the query point; it has not been added to the stored points",FONT_SMALL)
    save(img,"p195_example1.png")


def diagrams_201_to_300() -> None:
    img, draw = canvas("p205 Longest Common Prefix", "Words share a prefix in a trie.")
    graph_nodes(
        draw,
        {
            "r": (165, 130, "root"),
            "f": (265, 198, "f"),
            "l": (365, 198, "l"),
            "o": (465, 150, "o"),
            "w": (565, 110, "w"),
            "i": (465, 250, "i"),
            "g": (565, 250, "g"),
        },
        [("r", "f"), ("f", "l"), ("l", "o"), ("o", "w"), ("l", "i"), ("i", "g")],
        directed=True,
        colors={"w": GOLD, "g": GOLD},
    )
    title_chip(draw, "flower, flow, flight", 240, 322, fill=LAPIS)

    img, draw = canvas("p207 Remove Duplicates", "Sorted input list may contain repeated adjacent values.")
    list_nodes(draw, ["1", "1", "2", "3", "3"], 145, 200, label="head")
    save(img, "p207_example1.png")

    img, draw = canvas("p208 Remove Duplicates II", "Sorted input list may contain repeated adjacent groups.")
    list_nodes(draw, ["1", "2", "3", "3", "4", "4", "5"], 74, 200, label="head")
    save(img, "p208_example1.png")

    img, draw = canvas("p238 Range Sum of BST", "BST input with low and high values.")
    binary_tree(
        draw,
        {
            "a": (384, 112, "10"),
            "b": (270, 210, "5"),
            "c": (500, 210, "15"),
            "d": (210, 315, "3"),
            "e": (330, 315, "7"),
            "f": (560, 315, "18"),
        },
        [("a", "b"), ("a", "c"), ("b", "d"), ("b", "e"), ("c", "f")],
    )
    title_chip(draw, "low = 7, high = 15", 258, 358, fill=LAPIS)
    save(img, "p238_example1.png")

    img, draw = canvas("p294 Check X-Matrix", "Diagonal cells must be nonzero; all other cells must be zero.")
    grid = [[2, 0, 0, 1], [0, 3, 1, 0], [0, 5, 2, 0], [4, 0, 0, 2]]
    highlights = {(0, 0), (1, 1), (2, 2), (3, 3), (0, 3), (1, 2), (2, 1), (3, 0)}
    draw_matrix(draw, grid, 234, 104, cell=58, highlights=highlights)
    save(img, "p294_example1.png")


def diagrams_301_to_400() -> None:
    img, draw = canvas("p304 Swap Nodes in Pairs", "Input list before any node swaps.")
    list_nodes(draw, ["1", "2", "3", "4"], 190, 200, label="head")
    save(img, "p304_example1.png")

    img, draw = canvas("p306 Longest Valid Parentheses", "Input parentheses string.")
    draw_array(draw, list("(()"), 270, 150, cell=72, label="s")
    save(img, "p306_example1.png")

    img, draw = canvas("p319 Maximal Rectangle", "Binary matrix input.")
    grid = [["1", "0", "1", "0", "0"], ["1", "0", "1", "1", "1"], ["1", "1", "1", "1", "1"], ["1", "0", "0", "1", "0"]]
    draw_matrix(draw, grid, 194, 94, cell=54, highlights={(r, c) for r in range(4) for c in range(5) if grid[r][c] == "1"})
    save(img, "p319_example1.png")

    img, draw = canvas("p322 Recover Binary Search Tree", "Example input tree before recovery.")
    binary_tree(
        draw,
        {"a": (384, 122, "1"), "b": (285, 220, "3"), "c": (360, 318, "2")},
        [("a", "b"), ("b", "c")],
    )
    save(img, "p322_example1.png")

    img, draw = canvas("p331 Word Break II", "Example 1: return every split that uses dictionary words for the entire string.")
    centered_text(draw,(50,94,718,148),'wordDict = ["cat", "cats", "and", "sand", "dog"]',FONT_LABEL)
    draw_array(draw,["cats","and","dog"],116,188,cell=178,height=76,label='s = "catsanddog"')
    centered_text(draw,(80,314,704,369),'One valid result: "cats and dog"',FONT_LABEL)
    save(img,"p331_trie.png")

    img, draw = canvas("p335 Dungeon Game", "Dungeon grid input with health gains and losses.")
    draw_matrix(draw, [[-2, -3, 3], [-5, -10, 1], [10, 30, -5]], 250, 102, cell=72)
    save(img, "p335_example1.png")

    img, draw = canvas("p336 Skyline Problem", "Example 1: each rectangle is [left x, right x, height]; overlapping buildings remain solid.")
    buildings=[(2,9,10),(3,7,15),(5,12,12),(15,20,10),(19,24,8)]
    ox,oy,sx,sy=85,282,24,9
    draw.line((ox,90,ox,oy,690,oy),fill=GRID_LIGHT,width=3)
    for i,(left,right,height) in enumerate(buildings):
        color=[STONE,LAPIS,GOLD,AMETHYST,DIRT][i]
        box=(ox+left*sx,oy-height*sy,ox+right*sx,oy)
        block_rect(draw,box,fill=color)
        centered_text(draw,(box[0],box[1]-28,box[2],box[1]-3),str(height),FONT_LABEL,color)
    for v in [0,2,3,5,7,9,12,15,19,20,24]:
        x=ox+v*sx
        draw.line((x,oy,x,oy+7),fill=GRID_LIGHT,width=2)
        centered_text(draw,(x-13,oy+9,x+13,oy+38),str(v),FONT_SMALL,MUTED)
    centered_text(draw,(640,318,711,350),"x",FONT_LABEL,MUTED)
    centered_text(draw,(42,77,133,112),"height",FONT_LABEL,MUTED)
    centered_text(draw,(44,362,723,398),"[2,9,10]  [3,7,15]  [5,12,12]  [15,20,10]  [19,24,8]",FONT_LABEL)
    save(img,"p336_example1.png")

    img, draw = canvas("p347 Trapping Rain Water II", "Height map input.")
    draw_matrix(draw, [[1, 4, 3, 1, 3, 2], [3, 2, 1, 3, 2, 4], [2, 3, 3, 2, 3, 1]], 160, 116, cell=54)
    save(img, "p347_example1.png")

    img, draw = canvas("p371 Cut Off Trees", "Forest grid: 0 blocks movement, values above 1 are trees.")
    draw_matrix(draw, [[1, 2, 3], [0, 0, 4], [7, 6, 5]], 250, 102, cell=72)
    save(img, "p371_example1.png")

    img, draw = canvas("p374 Falling Squares", "Positive horizontal overlap stacks; edge touch does not.")
    ground = 342
    x0 = 92
    unit = 52
    draw.line((x0 + unit - 18, ground, x0 + 8 * unit + 18, ground), fill=GRID_LIGHT, width=4)
    for tick in range(1, 8):
        x = x0 + tick * unit
        draw.line((x, ground - 6, x, ground + 8), fill=GRID_LIGHT, width=3)
        centered_text(draw, (x - 18, ground + 10, x + 18, ground + 30), str(tick), FONT_TINY, MUTED)

    def falling_square(left: int, side: int, base_height: int, label: str, fill) -> tuple[int, int, int, int]:
        box = (
            x0 + left * unit,
            ground - (base_height + side) * unit,
            x0 + (left + side) * unit,
            ground - base_height * unit,
        )
        block_rect(draw, box, fill=fill, outline=STONE_DARK, width=4)
        centered_text(draw, box, label, FONT_LABEL)
        return box

    falling_square(1, 2, 0, "[1,2]", STONE)
    falling_square(2, 3, 2, "[2,3]", GOLD)
    falling_square(6, 1, 0, "[6,1]", LAPIS)
    title_chip(draw, "[2,3) overlap", 292, 246, fill=OBSIDIAN)
    title_chip(draw, "[5,6) gap", 476, 246, fill=LAPIS)
    save(img, "p374_example1.png")

    img, draw = canvas("p375 Reach a Number", "Example target = 2: flip the second move.")
    axis_y = 282
    start_x = 170
    unit = 72

    def number_x(value: int) -> int:
        return start_x + (value + 3) * unit

    draw.line((number_x(-3) - 34, axis_y, number_x(4) + 34, axis_y), fill=GRID_LIGHT, width=4)
    for value in range(-3, 5):
        x = number_x(value)
        draw.line((x, axis_y - 10, x, axis_y + 10), fill=GRID_LIGHT, width=3)
        centered_text(draw, (x - 24, axis_y + 16, x + 24, axis_y + 42), str(value), FONT_TINY, MUTED)

    for value, label, fill in [(0, "0", STONE), (2, "2", GOLD), (1, "1", LAPIS), (-1, "-1", AMETHYST)]:
        x = number_x(value)
        block_rect(draw, (x - 28, axis_y - 34, x + 28, axis_y + 8), fill=fill)
        centered_text(draw, (x - 28, axis_y - 34, x + 28, axis_y + 8), label, FONT_TINY, TEXT)

    arrow(draw, (number_x(0), 160), (number_x(1), 160), fill=GOLD, width=4)
    arrow(draw, (number_x(1), 205), (number_x(-1), 205), fill=REDSTONE, width=4)
    arrow(draw, (number_x(-1), 248), (number_x(2), 248), fill=GOLD, width=4)
    title_chip(draw, "+1", number_x(0) + 24, 128, fill=GOLD)
    title_chip(draw, "-2", number_x(0) - 48, 174, fill=REDSTONE)
    title_chip(draw, "+3", number_x(0) + 10, 218, fill=GOLD)
    save(img, "p375_example1.png")

    img, draw = canvas("p376 Cracking the Safe", "n = 2, k = 2: the safe checks the last two entered digits after each new digit.")
    draw_array(draw,list("01100"),174,133,cell=84,label='illustrative input sequence "01100"')
    for i,word in enumerate(["01","11","10","00"]):
        x=83+i*170
        title_chip(draw,word,x,270,fill=[LAPIS,GOLD,AMETHYST,GRASS][i])
        centered_text(draw,(x-18,320,x+130,362),f"positions {i} and {i+1}",FONT_SMALL,MUTED)
    centered_text(draw,(75,373,700,404),"The four consecutive windows cover every two-digit password",FONT_SMALL)
    save(img,"p376_debruijn_graph.png")

    img, draw = canvas("p377 Couples Holding Hands", "Seat row contains person IDs.")
    draw_array(draw, [0, 2, 1, 3], 220, 150, cell=72, label="row")
    save(img, "p377_example1.png")

    img, draw = canvas("p378 Max Chunks To Make Sorted II", "Sort each chunk, then concatenate.")
    chunk_colors = [GOLD, GOLD, LAPIS, GRASS, AMETHYST]
    for label, values, y in (("arr", [2, 1, 3, 4, 4], 124), ("after chunk sorts", [1, 2, 3, 4, 4], 260)):
        x = 164
        cell = 68
        draw.text((x, y - 34), label, font=FONT_LABEL, fill=MUTED)
        for i, value in enumerate(values):
            box = (x + i * cell, y, x + (i + 1) * cell, y + cell)
            block_rect(draw, box, fill=chunk_colors[i])
            centered_text(draw, box, str(value), FONT_LABEL)
        for split in (2, 3, 4):
            sx = x + split * cell
            draw.line((sx, y - 6, sx, y + cell + 6), fill=REDSTONE, width=4)
    arrow(draw, (520, 190), (584, 226), fill=GOLD, width=5)
    title_chip(draw, "[2,1] | [3] | [4] | [4]", 154, 350, fill=OBSIDIAN)
    title_chip(draw, "4 chunks", 520, 350, fill=GRASS)
    save(img, "p378_chunks.png")

    img, draw = canvas("p379 Basic Calculator IV", "Substitute, merge like terms, and sort.")
    text_block(draw, 'expr = "e + 8 - a + 5"', 96, 128, fill=LAPIS)
    title_chip(draw, "e = 1", 472, 128, fill=GOLD)
    arrow(draw, (368, 160), (452, 160), fill=GOLD, width=5)
    text_block(draw, "1 + 8 - a + 5", 168, 244, fill=AMETHYST)
    arrow(draw, (326, 216), (326, 238), fill=REDSTONE, width=5)
    title_chip(draw, "-1*a", 182, 306, fill=REDSTONE)
    title_chip(draw, "14", 382, 306, fill=GRASS)
    title_chip(draw, "degree desc, lex ties", 210, 358, fill=OBSIDIAN)
    save(img, "p379_polynomial.png")

    img, draw = canvas("p380 Race Car", "Instruction sequence moves on a number line.")
    draw.line((96, 280, 672, 280), fill=MUTED, width=4)
    ticks = [(0, 108), (1, 188), (3, 348), (6, 588), (7, 668)]
    for value, x in ticks:
        draw.line((x, 268, x, 292), fill=GRID_LIGHT, width=3)
        centered_text(draw, (x - 24, 296, x + 24, 326), str(value), FONT_SMALL, fill=TEXT)
    path = [(108, 246), (188, 246), (348, 246), (668, 246), (668, 204), (588, 204)]
    labels = ["A", "A", "A", "R", "A"]
    colors = [GOLD, GOLD, GOLD, REDSTONE, GRASS]
    for i in range(len(path) - 1):
        arrow(draw, path[i], path[i + 1], fill=colors[i], width=5)
        mid_x = (path[i][0] + path[i + 1][0]) // 2
        mid_y = (path[i][1] + path[i + 1][1]) // 2 - 34
        title_chip(draw, labels[i], mid_x - 24, mid_y, fill=colors[i])
    title_chip(draw, "target = 6", 98, 128, fill=LAPIS)
    title_chip(draw, "AAARA", 472, 128, fill=OBSIDIAN)
    title_chip(draw, "0 -> 1 -> 3 -> 7 -> 7 -> 6", 154, 374, fill=AMETHYST)
    save(img, "p380_race_car.png")

    img, draw = canvas("p381 Making A Large Island", "Flip one water cell and merge adjacent islands.")
    draw_matrix(draw, [[1, 0], [0, 1]], 112, 138, cell=72, highlights={(0, 0), (1, 1)})
    title_chip(draw, "before", 130, 318, fill=OBSIDIAN)
    arrow(draw, (300, 210), (408, 210), fill=GOLD, width=5)
    title_chip(draw, "flip 0 -> 1", 300, 246, fill=GOLD)
    draw_matrix(draw, [[1, 1], [0, 1]], 450, 138, cell=72, highlights={(0, 0), (0, 1), (1, 1)})
    title_chip(draw, "area = 3", 484, 318, fill=GRASS)
    save(img, "p381_example1.png")

    img, draw = canvas("p382 Unique Paths III", "Grid contains start, empty cells, end, and obstacles.")
    draw_matrix(draw, [[1, 0, 0, 0], [0, 0, 0, 0], [0, 0, 2, -1]], 190, 112, cell=60, highlights={(0, 0), (2, 2)})
    save(img, "p382_example1.png")

    img, draw = canvas("p383 Minimize Malware Spread", "Graph input with initially infected nodes.")
    graph_nodes(draw, {"0": (300, 140, "0"), "1": (300, 280, "1"), "2": (500, 210, "2")},
                [("0", "1")], colors={"0": REDSTONE, "1": REDSTONE})
    save(img, "p383_example1.png")

    img, draw = canvas("p385 Cat and Mouse", "Example 1 graph: hole=0, mouse starts at 1, cat starts at 2.")
    graph_nodes(draw, {"0": (160, 130, "0 hole"), "1": (365, 360, "1 mouse"), "2": (160, 300, "2 cat"), "3": (565, 130, "3"), "4": (565, 300, "4"), "5": (365, 210, "5")},
                [("0", "2"), ("0", "5"), ("1", "3"), ("2", "4"), ("2", "5"), ("3", "4"), ("3", "5")], colors={"0": LAPIS, "1": GOLD, "2": REDSTONE})
    save(img, "p385_example1.png")

    img, draw = canvas("p387 Malware Spread II", "Undirected adjacency matrix; red nodes are initially infected.")
    graph_nodes(draw, {"0": (300, 140, "0"), "1": (300, 280, "1"), "2": (500, 210, "2")},
                [("0", "1")], colors={"0": REDSTONE, "1": REDSTONE})
    save(img, "p387_example1.png")

    img, draw = canvas("p389 Binary Tree Cameras", "Input tree; camera placement is not shown.")
    binary_tree(draw, {"a": (384, 120, "0"), "b": (285, 220, "0"), "c": (225, 318, "0"), "d": (345, 318, "0")},
                [("a", "b"), ("b", "c"), ("b", "d")])
    save(img, "p389_example1.png")

    img, draw = canvas("p395 Recover Tree From Preorder", "Input traversal string with depth dashes.")
    text_block(draw, "1-2--3--4-5--6--7", 190, 176, fill=LAPIS)
    save(img, "p395_example1.png")


def diagrams_401_to_500() -> None:
    def bucket_table(draw: ImageDraw.ImageDraw, labels: Sequence[str], x: int, y: int, cell_w: int = 88) -> list[tuple[int, int, int, int]]:
        boxes = []
        for i, label in enumerate(labels):
            box = (x + i * cell_w, y, x + (i + 1) * cell_w - 8, y + 52)
            block_rect(draw, box, fill=STONE)
            centered_text(draw, box, label, FONT_SMALL)
            boxes.append(box)
        return boxes

    def draw_ring(draw: ImageDraw.ImageDraw, values: Sequence[str], center: tuple[int, int], radius: int,
                  colors: Sequence[tuple[int, int, int]] | None = None, label: str = "") -> list[tuple[int, int]]:
        if label:
            draw.text((center[0] - 98, center[1] - radius - 36), label, font=FONT_LABEL, fill=MUTED)
        points = []
        n = len(values)
        for i, value in enumerate(values):
            # Start at top, then move clockwise.
            angle = -3.14159265 / 2 + 2 * 3.14159265 * i / n
            x = int(center[0] + radius * __import__("math").cos(angle))
            y = int(center[1] + radius * __import__("math").sin(angle))
            points.append((x, y))
        for i in range(n):
            draw.line((points[i][0], points[i][1], points[(i + 1) % n][0], points[(i + 1) % n][1]), fill=GRID_LIGHT, width=4)
        for i, (x, y) in enumerate(points):
            node(draw, (x, y), values[i], fill=(colors[i] if colors else COBBLE), w=58, h=44)
        return points

    def tree_example(title: str, subtitle: str, filename: str,
                     nodes_: dict[str, tuple[int, int, str]],
                     edges_: Sequence[tuple[str, str]],
                     colors: dict[str, tuple[int, int, int]] | None = None) -> None:
        img, draw = canvas(title, subtitle)
        binary_tree(draw, nodes_, edges_, colors=colors)
        save(img, filename)

    img, draw = canvas("p401 Design HashMap", "Example 1: put assigns one value per key; another put replaces that value.")
    text_block(draw,"put(1,1), put(2,2)",73,112,fill=OBSIDIAN)
    text_block(draw,"key 1 -> value 1",91,196,fill=LAPIS)
    text_block(draw,"key 2 -> value 2",91,266,fill=LAPIS)
    text_block(draw,"then put(2,1)",437,112,fill=OBSIDIAN)
    text_block(draw,"key 1 -> value 1",438,196,fill=LAPIS)
    text_block(draw,"key 2 -> value 1",438,266,fill=GOLD)
    centered_text(draw,(60,346,710,387),"Only key 2's value changes; keys are unique",FONT_LABEL)
    save(img,"p401_design.png")

    img, draw = canvas("p402 Design HashSet", "Example 1: adding an existing key does not create a second copy.")
    text_block(draw,"add(1), add(2)",73,120,fill=OBSIDIAN)
    draw_array(draw,[1,2],98,207,cell=90,label="stored keys")
    text_block(draw,"then add(2)",437,120,fill=OBSIDIAN)
    draw_array(draw,[1,2],466,207,cell=90,label="stored keys")
    centered_text(draw,(70,341,700,387),"Both states contain exactly the keys 1 and 2",FONT_LABEL)
    save(img,"p402_design.png")

    img, draw = canvas("p403 Design Linked List", "Nodes can be read by index and updated at head, tail, or index.")
    boxes = list_nodes(draw, ["1", "2", "3"], 210, 200, label="0-indexed nodes")
    draw.text((116, 191), "head", font=FONT_LABEL, fill=MUTED)
    draw.text((600, 191), "tail", font=FONT_LABEL, fill=MUTED)
    for a, b in zip(boxes[1:], boxes[:-1]):
        arrow(draw, (a[0] - 4, 224), (b[2] + 8, 224), fill=AMETHYST, width=3)
    save(img, "p403_design.png")

    img, draw = canvas("p404 Design Skiplist", "Multiple linked levels provide shortcuts over the same sorted values.")
    levels = [
        (122, ["-inf", "1", "4", "+inf"], [130, 300, 470, 640]),
        (220, ["-inf", "1", "2", "3", "4", "+inf"], [90, 210, 330, 450, 570, 690]),
        (318, ["-inf", "1", "2", "3", "4", "+inf"], [90, 210, 330, 450, 570, 690]),
    ]
    for y, values, xs in levels:
        for i, value in enumerate(values):
            fill = OBSIDIAN if "inf" in value else COBBLE
            node(draw, (xs[i], y), value, fill=fill, w=64 if "inf" in value else 48, h=38)
            if i + 1 < len(values):
                arrow(draw, (xs[i] + 34, y), (xs[i + 1] - 34, y), width=3)
    for x in [90, 210, 570, 690]:
        draw.line((x, 140, x, 298), fill=GRID, width=3)
    save(img, "p404_design.png")

    img, draw = canvas("p406 Design Browser History", "Back and forward move around the current browser history.")
    pages = ["leet", "google", "facebook", "youtube"]
    boxes = list_nodes(draw, pages, 120, 190, gap=145, label="history order")
    for i, box in enumerate(boxes):
        if i == 2:
            block_rect(draw, (box[0] - 8, box[1] - 8, box[2] + 8, box[3] + 8), fill=GOLD, outline=PANEL_EDGE)
            node(draw, ((box[0] + box[2]) // 2, (box[1] + box[3]) // 2), pages[i], fill=GOLD, w=78, h=38)
    title_chip(draw, "current page", 312, 270, fill=LAPIS)
    save(img, "p406_design.png")

    img, draw = canvas("p407 Circular Queue", "Example 1, after the last successful insertion; capacity = 3.")
    draw_array(draw,[2, 3, 4],228,177,cell=104,label="logical order")
    centered_text(draw,(179,285,336,330),"front: 2",FONT_LABEL,LAPIS)
    centered_text(draw,(455,285,612,330),"rear: 4",FONT_LABEL,GOLD)
    centered_text(draw,(40,350,728,390),"enQueue(1), enQueue(2), enQueue(3), deQueue(), enQueue(4)",FONT_SMALL,MUTED)
    save(img,"p407_design.png")

    img, draw = canvas("p408 Circular Deque", "Example 1, after the last successful insertion; capacity = 3.")
    draw_array(draw,[4, 3, 1],228,177,cell=104,label="logical order")
    centered_text(draw,(179,285,336,330),"front: 4",FONT_LABEL,LAPIS)
    centered_text(draw,(455,285,612,330),"rear: 1",FONT_LABEL,GOLD)
    centered_text(draw,(40,350,728,390),"insertLast(1), insertLast(2), insertFront(3), deleteLast(), insertFront(4)",FONT_SMALL,MUTED)
    save(img,"p408_design.png")

    img, draw = canvas("p409 Front Middle Back Queue", "Operations target the front, middle, or back position.")
    draw_array(draw, ["1", "4", "3", "2"], 210, 160, cell=78, label="queue")
    title_chip(draw, "front", 174, 264, fill=LAPIS)
    title_chip(draw, "middle", 318, 264, fill=GOLD)
    title_chip(draw, "back", 486, 264, fill=REDSTONE)
    save(img, "p409_design.png")

    img, draw = canvas("p410 Design Ordered Stream", "The pointer releases only the next consecutive id chunk.")
    slot_labels = [("id 1", "aaaaa", LAPIS), ("id 2", "bbbbb", LAPIS), ("id 3", "ccccc", LAPIS),
                   ("id 4", "?", GOLD), ("id 5", "eeeee", AMETHYST)]
    slot_x, slot_y, slot_w = 82, 142, 118
    for i, (id_label, value, fill) in enumerate(slot_labels):
        box = (slot_x + i * slot_w, slot_y, slot_x + i * slot_w + 96, slot_y + 64)
        block_rect(draw, box, fill=fill)
        centered_text(draw, (box[0], box[1] + 8, box[2], box[1] + 34), id_label, FONT_TINY, MUTED)
        centered_text(draw, (box[0], box[1] + 26, box[2], box[3] - 6), value, FONT_SMALL)
    title_chip(draw, "ptr waits here", 400, 230, fill=GOLD)
    arrow(draw, (458, 228), (458, 208), fill=GOLD)
    title_chip(draw, "buffered", 552, 230, fill=AMETHYST)
    arrow(draw, (610, 228), (556, 208), fill=AMETHYST)
    title_chip(draw, "insert id 4 -> return [ddddd, eeeee]", 174, 318, fill=LAPIS)
    save(img, "p410_design.png")

    img, draw = canvas("p411 Authentication Manager", "A token is expired when expiry equals currentTime.")
    title_chip(draw, "timeToLive = 5", 78, 94, fill=OBSIDIAN)
    timeline_y = 220
    draw.line((100, timeline_y, 670, timeline_y), fill=GRID_LIGHT, width=5)
    for x, label in [(170, "2"), (310, "7"), (450, "10"), (590, "15")]:
        draw.line((x, timeline_y - 18, x, timeline_y + 18), fill=GRID_LIGHT, width=4)
        centered_text(draw, (x - 28, timeline_y + 24, x + 28, timeline_y + 46), label, FONT_SMALL, MUTED)
    block_rect(draw, (132, 142, 348, 186), fill=LAPIS)
    centered_text(draw, (132, 142, 348, 186), "aaa generated at 2; expires at 7", FONT_TINY)
    arrow(draw, (170, 188), (170, timeline_y - 22), fill=LAPIS, width=3)
    arrow(draw, (310, 188), (310, timeline_y - 22), fill=REDSTONE, width=3)
    title_chip(draw, "renew at 8 fails", 266, 268, fill=REDSTONE)
    block_rect(draw, (420, 142, 644, 186), fill=AMETHYST)
    centered_text(draw, (420, 142, 644, 186), "bbb renew at 10; expires at 15", FONT_TINY)
    arrow(draw, (450, 188), (450, timeline_y - 22), fill=AMETHYST, width=3)
    arrow(draw, (590, 188), (590, timeline_y - 22), fill=REDSTONE, width=3)
    title_chip(draw, "count at 15 returns 0", 438, 268, fill=GOLD)
    save(img, "p411_design.png")

    img, draw = canvas("p412 Movie Rental System", "Example 1: initial availability and the later report are shown at separate times.")
    centered_text(draw,(45,86,355,125),"Initial search(1)",FONT_LABEL,LAPIS)
    for i,(shop,price) in enumerate([(1,4),(0,5),(2,5)]):
        text_block(draw,f"shop {shop}: movie 1, price {price}",63,143+i*57,fill=LAPIS)
    centered_text(draw,(39,324,355,372),"returns shops [1, 0, 2]",FONT_LABEL)
    centered_text(draw,(390,86,715,125),"After the two rent calls",FONT_LABEL,GOLD)
    text_block(draw,"rent(0,1); rent(1,2)",403,143,fill=OBSIDIAN)
    text_block(draw,"shop 0: movie 1, price 5",389,219,fill=GOLD)
    text_block(draw,"shop 1: movie 2, price 7",389,277,fill=GOLD)
    centered_text(draw,(373,342,729,389),"report() -> [[0,1], [1,2]]",FONT_LABEL)
    save(img,"p412_design.png")

    img, draw = canvas("p413 Design Bitset", "Example 1: flip changes every 0 to 1 and every 1 to 0.")
    draw_array(draw,[0,1,0,1,0],138,131,cell=88,label="after fix(3), fix(1)")
    draw_array(draw,[1,0,1,0,1],138,287,cell=88,label="then flip()")
    centered_text(draw,(600,268,712,322),"count: 3",FONT_LABEL,GOLD)
    save(img,"p413_design.png")

    img, draw = canvas("p414 Video Sharing Platform", "Upload reuses the smallest freed ID and resets counters.")
    title_chip(draw, "reusable ids", 84, 98, fill=GOLD)
    block_rect(draw, (58, 130, 264, 260), fill=STONE)
    node(draw, (120, 194), "0", fill=GOLD, w=48, h=42)
    node(draw, (202, 194), "3", fill=GOLD, w=48, h=42)
    centered_text(draw, (78, 228, 244, 252), "min id first", FONT_SMALL)

    title_chip(draw, "active records", 340, 98, fill=LAPIS)
    block_rect(draw, (324, 130, 710, 308), fill=OBSIDIAN)
    rows = [
        ("id 1", "456", "views 2", "likes 1", "dislikes 1"),
        ("id 2", "999", "views 0", "likes 0", "dislikes 0"),
        ("id 4", "12", "views 1", "likes 0", "dislikes 0"),
    ]
    for r, row in enumerate(rows):
        y = 154 + r * 48
        x = 344
        for c, text in enumerate(row):
            w = 56 if c == 0 else 70
            box = (x, y, x + w, y + 34)
            block_rect(draw, box, fill=COBBLE if c < 2 else LAPIS, width=2)
            centered_text(draw, box, text, FONT_TINY)
            x += w + 8

    arrow(draw, (264, 194), (324, 194), fill=GOLD, width=4)
    centered_text(draw, (78, 315, 332, 346), "remove(id) puts the id back once", FONT_SMALL)
    centered_text(draw, (360, 336, 696, 366), "reused id gets a fresh zero-counter record", FONT_SMALL)
    save(img, "p414_design.png")

    img, draw = canvas("p415 Memory Allocator", "First-fit chooses the earliest gap that can hold the request.")
    title_chip(draw, "memory after free(mID=4)", 76, 94, fill=OBSIDIAN)
    title_chip(draw, "request size=3", 462, 94, fill=LAPIS)
    before = ["m1", "m1", "free", "free", "free", "m2", "m2", "free", "free", "free", "free", "free"]
    draw_array(draw, before, 92, 154, cell=46, highlights=[2, 3, 4])
    title_chip(draw, "first fitting gap", 302, 258, fill=GOLD)
    arrow(draw, (372, 256), (284, 218), fill=GOLD, width=4)
    title_chip(draw, "later gap waits", 510, 258, fill=OBSIDIAN)
    after = ["m1", "m1", "m7", "m7", "m7", "m2", "m2", "free", "free", "free", "free", "free"]
    draw_array(draw, after, 92, 318, cell=46, highlights=[2, 3, 4])
    save(img, "p415_memory_allocator.png")

    img, draw = canvas("p416 Design SQL", "Deleted row ids stay empty; inserts continue with the next id.")
    title_chip(draw, "table users", 70, 94, fill=LAPIS)
    title_chip(draw, "next rowId = 4", 492, 94, fill=GOLD)
    x0, y0 = 82, 150
    widths = [72, 116, 86, 86]
    headers = ["rowId", "name", "age", "city"]
    rows = [
        ("1", "alice", "30", "ny", LAPIS),
        ("2", "deleted", "-", "-", OBSIDIAN),
        ("3", "bob", "25", "sf", LAPIS),
    ]
    x = x0
    for c, header in enumerate(headers):
        box = (x, y0, x + widths[c], y0 + 38)
        block_rect(draw, box, fill=STONE, width=2)
        centered_text(draw, box, header, FONT_TINY)
        x += widths[c]
    for r, row in enumerate(rows):
        y = y0 + 38 + r * 50
        x = x0
        for c, text in enumerate(row[:4]):
            box = (x, y, x + widths[c], y + 42)
            block_rect(draw, box, fill=row[4], width=2)
            centered_text(draw, box, text, FONT_TINY)
            x += widths[c]
    arrow(draw, (424, 250), (508, 250), fill=GOLD, width=4)
    block_rect(draw, (528, 214, 684, 286), fill=GOLD)
    centered_text(draw, (528, 222, 684, 248), "insert carol", FONT_SMALL)
    centered_text(draw, (528, 252, 684, 278), "gets rowId 4", FONT_SMALL)
    centered_text(draw, (118, 344, 650, 372), "deleteRow(name, 2) must not shift rowId 3", FONT_SMALL)
    save(img, "p416_sql_rows.png")

    img, draw = canvas("p417 Graph Shortest Path", "Initial directed edges are gray; addEdge creates the gold edge.")
    title_chip(draw, "initial graph", 72, 92, fill=OBSIDIAN)
    title_chip(draw, "addEdge([1,3,4])", 456, 92, fill=GOLD)
    pts = {"0": (210, 160, "0"), "1": (390, 160, "1"), "2": (540, 270, "2"), "3": (230, 310, "3")}
    weighted_edges = [
        ("0", "2", "5", GRID_LIGHT),
        ("0", "1", "2", GRID_LIGHT),
        ("1", "2", "1", GRID_LIGHT),
        ("3", "0", "3", GRID_LIGHT),
        ("1", "3", "4", GOLD),
    ]
    for a, b, wt, color in weighted_edges:
        ax, ay, _ = pts[a]
        bx, by, _ = pts[b]
        dx = bx - ax
        dy = by - ay
        dist = max(1.0, math.hypot(dx, dy))
        start = (int(ax + dx / dist * 30), int(ay + dy / dist * 30))
        end = (int(bx - dx / dist * 34), int(by - dy / dist * 34))
        arrow(draw, start, end, fill=color, width=4)
        label_box = ((ax + bx) // 2 - 18, (ay + by) // 2 - 18, (ax + bx) // 2 + 18, (ay + by) // 2 + 18)
        centered_text(draw, label_box, wt, FONT_SMALL, GOLD if color != GOLD else TEXT)
    for key, point in pts.items():
        node(draw, (point[0], point[1]), point[2], fill=LAPIS if key in {"1", "3"} else COBBLE)
    centered_text(draw, (146, 356, 620, 384), "shortestPath follows arrow direction and current edge set", FONT_SMALL)
    save(img, "p417_example1.png")

    img, draw = canvas("p418 Range Sum Query", "The query asks for the inclusive interval [left, right].")
    values = [-2, 0, 3, -5, 2, -1]
    draw_array(draw, values, 126, 154, cell=74, highlights=[2, 3, 4, 5], label="nums")
    title_chip(draw, "left = 2", 256, 278, fill=LAPIS)
    title_chip(draw, "right = 5", 438, 278, fill=LAPIS)
    arrow(draw, (310, 266), (310, 226), fill=GOLD, width=4)
    arrow(draw, (500, 266), (500, 226), fill=GOLD, width=4)
    centered_text(draw, (188, 342, 580, 370), "sumRange includes both endpoint indices", FONT_SMALL)
    save(img, "p418_example1.png")

    img, draw = canvas("p419 Range Sum Query 2D", "Matrix input with a query rectangle.")
    grid = [[3, 0, 1, 4, 2], [5, 6, 3, 2, 1], [1, 2, 0, 1, 5], [4, 1, 0, 1, 7], [1, 0, 3, 0, 5]]
    query_cells = {(r, c) for r in range(2, 5) for c in range(1, 4)}
    draw_matrix(draw, grid, 182, 92, cell=46, highlights=query_cells)
    title_chip(draw, "row1=2 col1=1 row2=4 col2=3", 220, 342, fill=LAPIS)
    save(img, "p419_example1.png")

    img, draw = canvas("p420 Mutable Range Sum", "update assigns one index; later sumRange uses current values.")
    draw_array(draw, [1, 3, 5], 138, 126, cell=82, highlights=[1], label="before update")
    title_chip(draw, "update(index=1, val=2)", 414, 134, fill=GOLD)
    arrow(draw, (354, 168), (406, 168), fill=GOLD, width=4)
    draw_array(draw, [1, 2, 5], 138, 270, cell=82, highlights=[0, 1, 2], label="current nums")
    title_chip(draw, "sumRange(0, 2)", 430, 284, fill=LAPIS)
    arrow(draw, (352, 312), (422, 312), fill=LAPIS, width=4)
    centered_text(draw, (130, 366, 620, 394), "the query reads the updated array, not the original one", FONT_SMALL)
    save(img, "p420_example1.png")

    img, draw = canvas("p429 Design Leaderboard", "Scores can be added, reset, and queried by top K.")
    rows = [("player 1", "73"), ("player 2", "56"), ("player 3", "39"), ("player 4", "51"), ("player 5", "4")]
    for i, (player, score) in enumerate(rows):
        y = 104 + i * 48
        block_rect(draw, (210, y, 384, y + 36), fill=STONE)
        block_rect(draw, (394, y, 526, y + 36), fill=LAPIS if i < 2 else COBBLE)
        draw.text((226, y + 8), player, font=FONT_SMALL, fill=TEXT)
        centered_text(draw, (394, y, 526, y + 36), score, FONT_LABEL)
    title_chip(draw, "top(K) asks for the leading scores", 198, 356, fill=OBSIDIAN)
    save(img, "p429_leaderboard.png")

    img, draw = canvas("p438 LFU Cache", "Example 1, after put(1,1), put(2,2), get(1); capacity = 2.")
    for i,(key,value,freq,last) in enumerate([(1,1,2,"get(1)"),(2,2,1,"put(2,2)")]):
        y=156+i*87
        text_block(draw,f"key {key}: value {value}",88,y,fill=LAPIS)
        text_block(draw,f"frequency {freq}",340,y,fill=GOLD)
        text_block(draw,f"last: {last}",535,y,fill=STONE)
    centered_text(draw,(63,352,712,395),"Insertion counts as use; successful get increases the frequency",FONT_LABEL)
    save(img,"p438_design.png")

    img, draw = canvas("p441 All O(1) Data Structure", "Keys are organized by their current counts.")
    boxes = []
    for x, count, keys, color in [(170, "count 1", "leet", LAPIS), (390, "count 2", "hello", GOLD)]:
        box = (x, 160, x + 180, 250)
        block_rect(draw, box, fill=color)
        centered_text(draw, (box[0], box[1] + 8, box[2], box[1] + 42), count, FONT_LABEL)
        centered_text(draw, (box[0], box[1] + 48, box[2], box[3] - 10), "keys: " + keys, FONT_SMALL)
        boxes.append(box)
    arrow(draw, (boxes[0][2] + 6, 205), (boxes[1][0] - 8, 205), width=3)
    arrow(draw, (boxes[1][0] - 6, 225), (boxes[0][2] + 8, 225), fill=AMETHYST, width=3)
    save(img, "p441_design.png")

    img, draw = canvas("p448 Design Text Editor", "Text exists around a movable cursor.")
    text_block(draw, "left text: prac", 150, 170, fill=LAPIS)
    block_rect(draw, (356, 160, 396, 238), fill=REDSTONE)
    centered_text(draw, (336, 246, 416, 282), "cursor", FONT_LABEL, REDSTONE)
    text_block(draw, "right text: tice", 430, 170, fill=GOLD)
    save(img, "p448_design.png")

    img, draw = canvas("p460 Island Perimeter", "Input grid: land cells are 1 and water cells are 0.")
    grid = [[0, 1, 0, 0], [1, 1, 1, 0], [0, 1, 0, 0], [1, 1, 0, 0]]
    draw_matrix(draw, grid, 232, 92, cell=58, highlights={(r, c) for r, row in enumerate(grid) for c, v in enumerate(row) if v == 1})
    save(img, "p460_example1.png")

    img, draw = canvas("p465 N-ary Tree Depth", "Example input N-ary tree.")
    graph_nodes(
        draw,
        {"1": (384, 120, "1"), "3": (250, 218, "3"), "2": (384, 218, "2"), "4": (518, 218, "4"), "5": (205, 318, "5"), "6": (295, 318, "6")},
        [("1", "3"), ("1", "2"), ("1", "4"), ("3", "5"), ("3", "6")],
    )
    save(img, "p465_example1.png")

    tree_example("p467 Binary Tree Tilt", "Example input tree.", "p467_example1.png",
                 {"a": (384, 130, "1"), "b": (285, 240, "2"), "c": (485, 240, "3")},
                 [("a", "b"), ("a", "c")])

    img, draw = canvas("p468 Reshape the Matrix", "Input matrix and requested output shape.")
    draw_matrix(draw, [[1, 2], [3, 4]], 230, 120, cell=62)
    title_chip(draw, "r = 1, c = 4", 420, 176, fill=LAPIS)
    save(img, "p468_example1.png")

    tree_example("p475 Construct String From Tree", "Example input tree.", "p475_example1.png",
                 {"a": (384, 112, "1"), "b": (285, 215, "2"), "c": (485, 215, "3"), "d": (225, 318, "4")},
                 [("a", "b"), ("a", "c"), ("b", "d")])

    img, draw = canvas("p476 Merge Two Binary Trees", "Two input trees are merged from their roots.")
    draw.text((150, 92), "root1", font=FONT_LABEL, fill=MUTED)
    binary_tree(draw, {"a": (225, 136, "1"), "b": (165, 230, "3"), "c": (285, 230, "2"), "d": (120, 322, "5")},
                [("a", "b"), ("a", "c"), ("b", "d")])
    draw.text((460, 92), "root2", font=FONT_LABEL, fill=MUTED)
    binary_tree(draw, {"a": (535, 136, "2"), "b": (475, 230, "1"), "c": (595, 230, "3"), "d": (520, 322, "4"), "e": (640, 322, "7")},
                [("a", "b"), ("a", "c"), ("b", "d"), ("c", "e")])
    save(img, "p476_example1.png")

    tree_example("p479 Average of Levels", "Example input tree.", "p479_example1.png",
                 {"a": (384, 112, "3"), "b": (275, 220, "9"), "c": (495, 220, "20"), "d": (445, 322, "15"), "e": (545, 322, "7")},
                 [("a", "b"), ("a", "c"), ("c", "d"), ("c", "e")])

    tree_example("p487 Find Duplicate Subtrees", "Example input tree; return duplicate subtree roots.", "p487_example1.png",
                 {"a": (384, 98, "1"), "b": (260, 190, "2"), "c": (508, 190, "3"), "d": (210, 294, "4"), "e": (450, 294, "2"), "f": (570, 294, "4"), "g": (410, 374, "4")},
                 [("a", "b"), ("a", "c"), ("b", "d"), ("c", "e"), ("c", "f"), ("e", "g")])

    tree_example("p488 Two Sum IV", "BST input with target k shown separately.", "p488_example1.png",
                 {"a": (384, 102, "5"), "b": (270, 200, "3"), "c": (500, 200, "6"), "d": (210, 305, "2"), "e": (330, 305, "4"), "f": (560, 305, "7")},
                 [("a", "b"), ("a", "c"), ("b", "d"), ("b", "e"), ("c", "f")])
    img = Image.open(OUT_DIR / "p488_example1.png").convert("RGB")
    draw = ImageDraw.Draw(img)
    title_chip(draw, "k = 9", 330, 362, fill=LAPIS)
    save(img, "p488_example1.png")

    img, draw = canvas("p489 Maximum Binary Tree", "Input array used to build the tree.")
    draw_array(draw, [3, 2, 1, 6, 0, 5], 168, 162, cell=72, label="nums")
    save(img, "p489_example1.png")

    tree_example("p490 Print Binary Tree", "Example input tree before formatted placement.", "p490_example1.png",
                 {"a": (384, 128, "1"), "b": (295, 244, "2")},
                 [("a", "b")])

    img, draw = canvas("p495 Image Smoother", "Use the cell and its neighbors; ignore positions outside the image.")
    grid=[[1,1,1],[1,0,1],[1,1,1]]
    draw_matrix(draw,grid,105,123,cell=62,highlights={(r,c) for r in range(3) for c in range(3)})
    draw_matrix(draw,grid,463,123,cell=62,highlights={(r,c) for r in range(2) for c in range(2)})
    centered_text(draw,(65,326,340,374),"center: 9 cells",FONT_LABEL)
    centered_text(draw,(425,326,706,374),"top-left: 4 cells",FONT_LABEL)
    save(img,"p495_example1.png")

    img, draw = canvas("p496 Maximum Width of Binary Tree", "Example 1: include both end nodes and every position between them.")
    binary_tree(draw,{"a":(384,107,"1"),"b":(250,188,"3"),"c":(518,188,"2"),
    "d":(185,277,"5"),"e":(315,277,"3"),"f":(583,277,"9")},
    [("a","b"),("a","c"),("b","d"),("b","e"),("c","f")])
    block_rect(draw,(429,257,481,296),fill=EMPTY)
    centered_text(draw,(429,257,481,296),"null",FONT_SMALL,MUTED)
    draw.line((185,323,583,323),fill=GOLD,width=4)
    draw.line((185,310,185,336),fill=GOLD,width=4)
    draw.line((583,310,583,336),fill=GOLD,width=4)
    centered_text(draw,(105,353,660,396),"last level: [5, 3, null, 9] -> width 4",FONT_LABEL)
    save(img,"p496_example1.png")

    tree_example("p500 Trim BST", "Input BST with low and high bounds.", "p500_prompt.png",
                 {"a": (384, 120, "3"), "b": (260, 220, "0"), "c": (508, 220, "4"), "d": (330, 320, "2"), "e": (285, 382, "1")},
                 [("a", "b"), ("a", "c"), ("b", "d"), ("d", "e")])
    img = Image.open(OUT_DIR / "p500_prompt.png").convert("RGB")
    draw = ImageDraw.Draw(img)
    title_chip(draw, "low = 1, high = 3", 470, 318, fill=LAPIS)
    save(img, "p500_prompt.png")


def main() -> None:
    sudoku()
    pascal()
    bars()
    stack_and_search()
    linked_lists()
    trees_81_to_100()
    diagrams_101_to_200()
    diagrams_201_to_300()
    diagrams_301_to_400()
    diagrams_401_to_500()
    write_layout_manifest()


if __name__ == "__main__":
    main()
