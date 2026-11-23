"""Normalize Frankc Unicode tables and produce WOFF2.

Run from the repository root after installing fonttools and brotli:
    python -m pip install fonttools brotli
    python scripts/prepare_font.py
"""
from pathlib import Path
from fontTools.ttLib import TTFont

root = Path(__file__).resolve().parents[1]
source = root / '1/frankc.ttf'
font = TTFont(source)
# The Windows Unicode table contains Cyrillic; the original platform-0
# table only has legacy codepoints. Browsers can prefer the latter.
unicode_map = dict(font['cmap'].getcmap(3, 1).cmap)
for char, legacy in [('Ё', 0xA8), ('ё', 0xB8)]:
    unicode_map.setdefault(ord(char), unicode_map[legacy])
for table in font['cmap'].tables:
    if table.isUnicode():
        table.ensureDecompiled()
        table.cmap = dict(unicode_map)
font.save(source)
font.flavor = 'woff2'
font.save(root / '1/assets/fonts/frankc.woff2')
