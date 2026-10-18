# SPDX-License-Identifier: GPL-2.0-only
#
# Copyright (C) the AnyPS5 contributors (https://github.com/boykopovar/AnyPS5)
#
# Derived from tools/progress.py of AnyPS5, which is licensed under the GNU
# General Public License version 2 only. This file is distributed under the
# same license: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
#
# Modified on 2026-10-18 for Luxium-decompiled: kept only the treemap
# rendering, with groups passed in by the caller (packages of the mod) and a
# separate hover name for each group. The badge, HTML report and comparison
# report were removed.
#
# The GPL applies to this file only. It is a build tool and is not part of the
# mod JAR; the rest of the repository keeps its own license.
"""Render decompilation progress as a squarified treemap SVG.

Each panel splits its area into one rectangle per package, sized by the
number of items it holds. Every item is drawn as a small cell, green when it
matches the original and grey when it does not.
"""

from html import escape

PANEL_WIDTH, GAP, MAP_HEIGHT, HEADER = 495, 10, 280, 30
DONE_COLOR, TODO_COLOR, BORDER, TEXT = "#2ea043", "#6e7681", "#0d1117", "#ffffff"


def worst_ratio(row, side):
    area = sum(row)
    return max(max(side * side * r / area ** 2, area ** 2 / (side * side * r)) for r in row)


def place(row, x, y, w, h, rects):
    thickness = sum(row) / min(w, h)
    offset = 0
    for area in row:
        length = area / thickness
        if w >= h:
            rects.append((x, y + offset, thickness, length))
        else:
            rects.append((x + offset, y, length, thickness))
        offset += length
    return (x + thickness, y, w - thickness, h) if w >= h else (x, y + thickness, w, h - thickness)


def squarify(values, x, y, w, h):
    total = sum(values)
    areas = [v * w * h / total for v in values]
    rects, row = [], []
    while areas:
        side = min(w, h)
        if not row or worst_ratio(row + [areas[0]], side) <= worst_ratio(row, side):
            row.append(areas.pop(0))
            continue
        x, y, w, h = place(row, x, y, w, h, rects)
        row = []
    if row:
        place(row, x, y, w, h, rects)
    return rects


def cells(count, x, y, w, h):
    if not count or w <= 0 or h <= 0:
        return []
    rows = max(1, min(count, round((count * h / w) ** 0.5)))
    result = []
    for r in range(rows):
        in_row = count // rows + (r < count % rows)
        cw = w / in_row
        result += [(x + c * cw, y + r * h / rows, cw, h / rows) for c in range(in_row)]
    return result


def rect(x, y, w, h, color, stroke=0.5):
    return (f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" '
            f'fill="{color}" stroke="{BORDER}" stroke-width="{stroke}"/>')


def text(x, y, value, size):
    return (f'<text x="{x:.2f}" y="{y:.2f}" font-family="sans-serif" font-size="{size}" fill="{TEXT}" '
            f'stroke="{BORDER}" stroke-width="3" paint-order="stroke">{escape(value)}</text>')


def treemap(title, groups, left):
    """groups: list of {"name", "label", "done", "todo"} dicts; name is shown on hover."""
    groups = sorted((g for g in groups if g["done"] + g["todo"]), key=lambda g: -(g["done"] + g["todo"]))
    done = sum(g["done"] for g in groups)
    total = done + sum(g["todo"] for g in groups)
    percent = f"{100 * done / total:.2f}" if total else "0.00"
    parts = [text(left + 4, 21, f"{title}: {percent}% ({done}/{total})", 16)]
    sizes = [g["done"] + g["todo"] for g in groups]
    for group, (x, y, w, h) in zip(groups, squarify(sizes, left, HEADER, PANEL_WIDTH, MAP_HEIGHT)):
        count = group["done"] + group["todo"]
        parts.append(f'<g><title>{escape(group["name"])}: {group["done"]}/{count} '
                     f'({100 * group["done"] / count:.0f}%)</title>')
        for i, cell in enumerate(cells(count, x + 1, y + 1, w - 2, h - 2)):
            parts.append(rect(*cell, DONE_COLOR if i < group["done"] else TODO_COLOR))
        parts.append(f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" '
                     f'fill="none" stroke="{BORDER}" stroke-width="2"/>')
        label_length = int((w - 10) / 7)
        label = group["label"][:label_length] if label_length > 0 else ""
        if (label == group["label"] or len(label) >= 3) and h > 22:
            parts.append(text(x + 5, y + 16, label, 12))
        parts.append("</g>")
    return parts


def render(left_title, left_groups, right_title, right_groups):
    width, height = 2 * PANEL_WIDTH + GAP, HEADER + MAP_HEIGHT
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" '
             f'width="{width}" height="{height}">',
             rect(0, 0, width, height, BORDER, 0)]
    parts += treemap(left_title, left_groups, 0)
    parts += treemap(right_title, right_groups, PANEL_WIDTH + GAP)
    parts.append("</svg>")
    return "\n".join(parts) + "\n"
