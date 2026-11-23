#!/usr/bin/env python3
"""Генератор демо-ассетов для игры «Вытри букву» в стиле комплекта пользователя.

Буква «А» берётся из готовых файлов пользователя (assets/letters/a_*.png) —
для неё скрипт только вычисляет якорь глаз. Остальные буквы рисуются заново:
чистая — ярко-зелёная с чёрным контуром, пена — полупрозрачная молочная
плёнка с пузырями без контура (как в референсе).

В конце скрипт перезаписывает js/letters.js с нормированными якорями глаз.
"""
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "assets")
LETTERS_DIR = os.path.join(ASSETS, "letters")
JS_DIR = os.path.join(ROOT, "js")
os.makedirs(LETTERS_DIR, exist_ok=True)
os.makedirs(JS_DIR, exist_ok=True)

W, H = 528, 596            # размер спрайта (как у ассетов пользователя)
TARGET_GLYPH_H = 493       # высота тела буквы, как у референсной «А»
GREEN = (0, 255, 12, 255)
INK = (0, 0, 0, 255)
FOAM_BASE = (229, 253, 254, 128)
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

# (символ, slug, рисовать ли заново)
CHARS = [("А", "a", False), ("Б", "b", True), ("В", "v", True)]


def glyph_mask(ch: str) -> Image.Image:
    """Маска тела буквы нужного размера, отцентрованная в кадре W×H."""
    font = ImageFont.truetype(FONT_PATH, 500)
    tmp = Image.new("L", (700, 700), 0)
    d = ImageDraw.Draw(tmp)
    d.text((60, 60), ch, font=font, fill=255)
    bbox = tmp.getbbox()
    crop = tmp.crop(bbox)
    scale = TARGET_GLYPH_H / crop.height
    crop = crop.resize((max(1, round(crop.width * scale)), TARGET_GLYPH_H),
                       Image.LANCZOS)
    mask = Image.new("L", (W, H), 0)
    mask.paste(crop, ((W - crop.width) // 2, (H - crop.height) // 2))
    return mask


def rounded_dilate(mask: Image.Image, r: int, thresh: int) -> Image.Image:
    return mask.filter(ImageFilter.GaussianBlur(r)).point(lambda v: 255 if v > thresh else 0)


def make_letter(ch: str, slug: str, redraw: bool):
    clean_path = os.path.join(LETTERS_DIR, f"{slug}_clean.png")
    foam_path = os.path.join(LETTERS_DIR, f"{slug}_foam.png")

    if redraw:
        body = glyph_mask(ch)
        # контур: скруглённое расширение маски минус тело
        grow = rounded_dilate(body, 16, 45)
        ring = ImageChops.subtract(grow, body)
        clean = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        clean.paste(Image.new("RGBA", (W, H), GREEN), (0, 0), body)
        clean.paste(Image.new("RGBA", (W, H), INK), (0, 0), ring)
        clean.save(clean_path)

        # пена: полупрозрачная плёнка (чуть внутри тела) + пузыри, без контура
        base_m = rounded_dilate(body, 10, 235)     # эрозия ~14 px, плёнка внутри контура
        foam = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        foam.paste(Image.new("RGBA", (W, H), FOAM_BASE), (0, 0), base_m)
        bubbles = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        db = ImageDraw.Draw(bubbles)
        import random
        rnd = random.Random(hash(ch) & 0xFFFF)
        for _ in range(34):
            r = rnd.randint(10, 46)
            x = rnd.randint(0, W)
            y = rnd.randint(0, H)
            db.ellipse([x - r, y - r, x + r, y + r], fill=(199, 236, 252, 210))
            db.ellipse([x - r, y - r, x + r, y + r], outline=(255, 255, 255, 230), width=3)
            db.ellipse([x - r * 0.55, y - r * 0.75, x - r * 0.05, y - r * 0.25],
                       fill=(255, 255, 255, 235))
        b_alpha = ImageChops.multiply(bubbles.getchannel("A"), base_m)
        bubbles.putalpha(b_alpha)
        foam = Image.alpha_composite(foam, bubbles)
        foam.save(foam_path)

    return {"char": ch, "slug": slug}


def main():
    cfg = [make_letter(ch, slug, redraw) for ch, slug, redraw in CHARS]
    lines = ["// Конфигурация букв. Можно править руками: добавьте свою пару",
             "// ассетов (foo_foam.png / foo_clean.png) строкой ниже.",
             "// Необязательное поле next: 'url/{char}' — переход после полной очистки буквы.",
             "window.LETTERS = ["]
    for c in cfg:
        lines.append(
            "  { char: '%s', foam: 'assets/letters/%s_foam.png', "
            "clean: 'assets/letters/%s_clean.png' },"
            % (c["char"], c["slug"], c["slug"]))
    lines.append("];")
    with open(os.path.join(JS_DIR, "letters.js"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print("done:", cfg)


if __name__ == "__main__":
    main()
