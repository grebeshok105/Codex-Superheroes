#!/usr/bin/env python3
"""Generates laser_scorch.png (32x32 RGBA): a dark radial scorch decal with a
faint ember rim, used by the Homelander eye-laser burn marks (Stage 12).

Stdlib only. Run:
    python3 art-source/homelander/emf/gen_laser_scorch.py
writes
    src/main/resources/assets/superheroes/textures/effect/homelander/laser_scorch.png
"""
import math
import random
import struct
import zlib
from pathlib import Path

SIZE = 32
OUT = (Path(__file__).resolve().parents[3]
       / "src/main/resources/assets/superheroes/textures/effect/homelander/laser_scorch.png")


def png_chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))


def write_png(path: Path, size: int, pixels: list[tuple[int, int, int, int]]) -> None:
    raw = b"".join(
        b"\x00" + bytes(c for px in pixels[y * size:(y + 1) * size] for c in px)
        for y in range(size)
    )
    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)  # RGBA8
    path.write_bytes(b"\x89PNG\r\n\x1a\n"
                     + png_chunk(b"IHDR", ihdr)
                     + png_chunk(b"IDAT", zlib.compress(raw, 9))
                     + png_chunk(b"IEND", b""))


def main() -> None:
    rng = random.Random(0x5C0C4)
    pixels = []
    c = (SIZE - 1) / 2.0
    for y in range(SIZE):
        for x in range(SIZE):
            # normalized distance from centre, plus a little organic jitter
            dx, dy = x - c, y - c
            d = math.sqrt(dx * dx + dy * dy) / (SIZE / 2.0)
            d += rng.uniform(-0.045, 0.045)
            # soft radial alpha falloff; burnt centre is opaque, edge dies out
            alpha = max(0.0, min(1.0, 1.18 - d * 1.55))
            alpha *= alpha
            # dark char grey, slightly lighter (ashy) toward the centre
            base = 14.0 + 22.0 * max(0.0, 1.0 - d * 1.9)
            r = g = b = base
            # faint ember rim in the annulus just inside the falloff
            rim = math.exp(-((d - 0.58) ** 2) / 0.012) * max(0.0, 1.0 - d)
            r += 158.0 * rim
            g += 52.0 * rim
            b += 8.0 * rim
            pixels.append((int(min(255, r)), int(min(255, g)), int(min(255, b)),
                           int(255 * alpha)))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    write_png(OUT, SIZE, pixels)
    print(f"wrote {OUT} ({SIZE}x{SIZE})")


if __name__ == "__main__":
    main()
