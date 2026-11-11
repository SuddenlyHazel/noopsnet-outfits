#!/usr/bin/env python3
"""Draw the original 16px pride accessories using only Python's standard library."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
PALETTES = {
    "rainbow": ["E94B55", "FF9A42", "FFE36B", "57B97B", "5F94DB", "A878D0"],
    "trans": ["5BCEFA", "F5A9B8", "FFFFFF", "F5A9B8", "5BCEFA"],
    "nonbinary": ["FFF430", "FFFFFF", "9C59D1", "2D2934"],
    "bi": ["D60270", "D60270", "9B4F96", "0038A8", "0038A8"],
    "pan": ["FF218C", "FFD800", "21B1FF"],
    "lesbian": ["D52D00", "FF9A56", "FFFFFF", "D362A4", "A30262"],
    "ace": ["24212B", "A3A3A3", "FFFFFF", "800080"],
    "genderfluid": ["FF75A2", "FFFFFF", "BE18D6", "272230", "333EBD"],
}


def color(hex_value):
    return tuple(bytes.fromhex(hex_value)) + (255,)


def png(path, pixels):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    scanlines = b"".join(b"\0" + bytes(c for pixel in row for c in pixel) for row in pixels)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(scanlines, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def make_pin(palette):
    pixels = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    spans = {2: [(4, 6), (9, 11)], 3: [(3, 12)], 4: [(2, 13)], 5: [(2, 13)],
             6: [(2, 13)], 7: [(2, 13)], 8: [(3, 12)], 9: [(4, 11)],
             10: [(5, 10)], 11: [(6, 9)], 12: [(7, 8)]}
    mask = {(x, y) for y, row in spans.items() for a, b in row for x in range(a, b + 1)}
    for x, y in mask:
        for dx, dy in [(0, -1), (0, 1), (-1, 0), (1, 0)]:
            if (x + dx, y + dy) not in mask:
                pixels[y + dy][x + dx] = color("51405E")
    for x, y in mask:
        pixels[y][x] = color(palette[min(len(palette) - 1, (y - 2) * len(palette) // 11)])
    pixels[4][4] = color("FFFFFF")
    pixels[5][3] = color("FFF5FC")
    return pixels


def make_patch(palette):
    pixels = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(2, 14):
        for x in range(1, 15):
            if x in (1, 14) and y in (2, 13):
                continue
            pixels[y][x] = color("51405E")
            if 2 <= x <= 13 and 3 <= y <= 12:
                pixels[y][x] = color("D6BBDE")
            if 3 <= x <= 12 and 4 <= y <= 11:
                pixels[y][x] = color(palette[min(len(palette) - 1, (y - 4) * len(palette) // 8)])
    for x in range(3, 13, 2):
        pixels[3][x] = color("FFF5E7")
        pixels[12][x] = color("FFF5E7")
    for y in range(4, 12, 2):
        pixels[y][2] = color("FFF5E7")
        pixels[y][13] = color("FFF5E7")
    return pixels


if __name__ == "__main__":
    target = ROOT / "src/main/resources/assets/noopsnet_outfits/textures/accessory"
    for name, palette in PALETTES.items():
        png(target / "pin" / (name + ".png"), make_pin(palette))
        png(target / "patch" / (name + ".png"), make_patch(palette))
    print("Generated 16 original 16×16 RGBA textures.")
