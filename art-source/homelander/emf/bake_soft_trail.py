#!/usr/bin/env python3
"""Bake ``assets/superheroes/textures/effect/soft_trail.png``.

A 64x64 RGBA PNG used by the Homelander flight trail's soft profile
(``TrailPattern.soft``): white base colour (vertex colours tint it), alpha =
gaussian across the V axis — 1.0 on the centreline, ~0 at the edges —
constant across U so the texture maps onto each crossed quad without seam
artefacts. Stdlib only; deterministic output.
"""

import math
import struct
import zlib
from pathlib import Path

SIZE_U = 64
SIZE_V = 64
SIGMA = 0.20

OUT = (Path(__file__).resolve().parents[3]
       / "src/main/resources/assets/superheroes/textures/effect/soft_trail.png")


def alpha_at(v_index: int) -> int:
    v = (v_index + 0.5) / SIZE_V
    a = math.exp(-0.5 * ((v - 0.5) / SIGMA) ** 2)
    return min(255, round(a * 255))


def chunk(tag: bytes, data: bytes) -> bytes:
    return (struct.pack(">I", len(data)) + tag + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))


def main() -> None:
    rows = bytearray()
    for v in range(SIZE_V):
        rows.append(0)  # filter byte: none
        a = alpha_at(v)
        for _ in range(SIZE_U):
            rows += bytes((255, 255, 255, a))
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE_U, SIZE_V, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(rows), 9))
           + chunk(b"IEND", b""))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(png)
    print(f"wrote {OUT} ({len(png)} bytes, {SIZE_U}x{SIZE_V}, sigma={SIGMA})")


if __name__ == "__main__":
    main()
