#!/usr/bin/env python3
"""Recolor Yamato textures from blue to Raiden purple via HSV hue shift.
Only pixels with hue in the blue range (≈195°–270°) are shifted; gold tsuba and other
non-blue pixels stay intact."""

import colorsys
import os
import shutil
import sys
from PIL import Image

SRC = "/tmp/raiden_assets/assets/minecraft/textures/item"
DST = "/home/ubuntu/repos/grebeshok105/src/main/resources/assets/superheroes/textures/item"

# Hue ranges in [0..1] (hue/360)
BLUE_LO = 165.0 / 360.0   # cyan and up
BLUE_HI = 270.0 / 360.0   # before magenta

def recolor_pixel(r, g, b):
    rf, gf, bf = r/255.0, g/255.0, b/255.0
    h, s, v = colorsys.rgb_to_hsv(rf, gf, bf)
    # Treat near-blue pixels (even desaturated, since the texture has many
    # bluish grays). Bluish dominance check is more inclusive than just hue.
    is_blueish_hue = BLUE_LO <= h <= BLUE_HI
    is_blue_dom = b > r and b >= g and (b - r) > 8
    if (is_blueish_hue or is_blue_dom) and s > 0.05:
        # Map source blue range into a tight Raiden-purple range 260..298 deg.
        if is_blueish_hue:
            t = (h - BLUE_LO) / (BLUE_HI - BLUE_LO)
        else:
            t = 0.5
        nh = (260.0 + t * 38.0) / 360.0
        ns = min(1.0, s * 1.40)
        nv = min(1.0, v * 1.05)
        nr, ng, nb = colorsys.hsv_to_rgb(nh, ns, nv)
        return int(nr*255), int(ng*255), int(nb*255)
    return r, g, b

def process(src_path, dst_path):
    img = Image.open(src_path).convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            nr, ng, nb = recolor_pixel(r, g, b)
            px[x, y] = (nr, ng, nb, a)
    os.makedirs(os.path.dirname(dst_path), exist_ok=True)
    img.save(dst_path, optimize=True)
    print(f"  -> {dst_path} ({w}x{h})")

def copy_mcmeta(name):
    src_meta = f"{SRC}/{name}.mcmeta"
    dst_meta = f"{DST}/{name}.mcmeta"
    if os.path.exists(src_meta):
        shutil.copy(src_meta, dst_meta)
        print(f"  -> {dst_meta}")

if __name__ == "__main__":
    for name in ("yamato.png", "yamato3.png", "yamato5.png", "yamato_sheathe.png"):
        src = f"{SRC}/{name}"
        dst = f"{DST}/{name}"
        if not os.path.exists(src):
            print(f"missing: {src}", file=sys.stderr)
            continue
        print(f"recolor {name}")
        process(src, dst)
        copy_mcmeta(name)
    print("done")
