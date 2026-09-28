# -*- coding: utf-8 -*-
"""生成无限箭袋模组的贴图（纯标准库，不需要 Pillow）。

产物：
  src/main/resources/assets/endlessquiver/textures/item/quiver.png   16x16 物品图标
  src/main/resources/assets/endlessquiver/textures/gui/quiver.png    256x256 界面背景
      （内容只占左上 176x166，其余透明 —— 见下方 CANVAS_W 的说明）

用法： python tools/make_textures.py
"""
import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

TRANSPARENT = (0, 0, 0, 0)

# 物品图标的调色板（按合成材料取色：回响碎片青绿晶体 + 下界合金近黑金属 + 下界之星 + 末影之眼）
PALETTE = {
    '.': TRANSPARENT,
    'P': (0x3F, 0xB0, 0xB4, 0xFF),   # 回响碎片：晶体高光
    'p': (0x24, 0x78, 0x7F, 0xFF),   # 回响碎片：晶体中间调
    'o': (0x14, 0x50, 0x5A, 0xFF),   # 回响碎片：晶体暗部
    'X': (0x0C, 0x33, 0x3B, 0xFF),   # 回响碎片：晶体最暗 / 解理纹 / 箭羽描边
    'N': (0x7A, 0x70, 0x69, 0xFF),   # 下界合金：亮边
    'M': (0x55, 0x4A, 0x42, 0xFF),   # 下界合金：主体
    'I': (0x3A, 0x31, 0x2B, 0xFF),   # 下界合金：暗部
    'K': (0x1F, 0x1A, 0x16, 0xFF),   # 下界合金：近黑收边 / 开口最深处
    'S': (0xFD, 0xF6, 0xE3, 0xFF),   # 下界之星：星芯（暖白）
    's': (0xC9, 0xD2, 0xE2, 0xFF),   # 下界之星：次亮（蓝白，星光）
    'x': (0x87, 0x90, 0xA6, 0xFF),   # 下界之星：描边（冷灰蓝）
    'E': (0xA8, 0xFF, 0xE0, 0xFF),   # 末影之眼：虹膜亮部 / 青羽亮
    'e': (0x2F, 0xBF, 0x9F, 0xFF),   # 末影之眼：青色 / 青羽暗部
    'z': (0x1B, 0x7F, 0x69, 0xFF),   # 末影之眼：眼环暗部
    'q': (0x0E, 0x24, 0x20, 0xFF),   # 末影之眼：竖瞳
    'w': (0xC8, 0xB0, 0x82, 0xFF),   # 箭杆受光侧
    'V': (0xB0, 0x9A, 0x70, 0xFF),   # 箭杆中间调
    'W': (0x9A, 0x82, 0x58, 0xFF),   # 箭杆背光侧
    'f': (0xF0, 0xF0, 0xF0, 0xFF),   # 白色尾羽
    'F': (0xC4, 0xC4, 0xC4, 0xFF),   # 尾羽暗部
}

# 16 行 x 16 列：由「64x64 原图」做 4x4 盒式均值降采样（alpha 加权），再把每个像素
# 吸附回同一套调色板得到 —— 也就是 mat_16px.png 预览里的那张图。64x64 原图与生成逻辑在
# .tmp-probe/icons/make_icon64_mat.py；配色按合成材料来：
#   回响碎片 x4   → 袋身青绿晶体（P/p/o/X）
#   下界合金锭 x2 → 袋口金属后唇 + 前唇金属箍 + 下段腰带（N/M/I/K）
#   下界合金块 x1 → 袋底金属盖
#   下界之星 x1   → 腰带扣中央的四角星（S/s/x）
#   末影之眼 x1   → 袋身中段徽记（z/E/q + f 高光）
# 三支箭斜插出袋口：木质箭杆（w/V/W）+ 下界合金束环 + 叶片箭羽（中间那支青绿）。
ITEM_ART = [
    "..........o.o.oX",
    ".........xxpPxFX",
    "........XFNPPxFX",
    "........XN.p.NX.",
    "........WMWNWW..",
    "......NMNXNXNMM.",
    "......MMMMMIMKI.",
    ".....ooppooXXI..",
    ".....opzooooX...",
    "....ozoEXxoXX...",
    "....oppzXzoX....",
    "....opPPpoXX....",
    "...MMNFFNII.....",
    "...IIMNxIKK.....",
    "...MMMMMII......",
    "..IIIIIIII......",
]

# 界面配色
GUI_BG = (0xC6, 0xC6, 0xC6, 0xFF)
GUI_LIGHT = (0xFF, 0xFF, 0xFF, 0xFF)
GUI_DARK = (0x55, 0x55, 0x55, 0xFF)
SLOT_BG = (0x8B, 0x8B, 0x8B, 0xFF)
SLOT_DARK = (0x37, 0x37, 0x37, 0xFF)
SLOT_LIGHT = (0xFF, 0xFF, 0xFF, 0xFF)

GUI_W, GUI_H = 176, 166
# 画布必须是 256x256：
# GuiGraphics.blit(贴图, x, y, u, v, w, h) 这个 7 参重载按 256x256 解释 UV。
# 若 PNG 只有 176x166，游戏会把「贴图左上 176x166 像素」拉伸着画（x 放大 256/176≈1.45 倍、
# y 放大 256/166≈1.54 倍），整个界面就会错位。原版容器贴图都是 256x256 就是这个原因。
CANVAS_W, CANVAS_H = 256, 256
ARROW_SLOT_XY = (80, 35)          # 与 QuiverMenu 里 addSlot 的坐标一致
PLAYER_INV_START = (8, 84)        # 3x9 背包
HOTBAR_START = (8, 142)           # 1x9 快捷栏


def write_png(path, width, height, rows):
    """rows: 每行是 (r,g,b,a) 元组的列表，长度 = width。"""
    raw = bytearray()
    for row in rows:
        raw.append(0)  # filter type 0
        for px in row:
            raw.extend(px)

    def chunk(tag, data):
        out = struct.pack('>I', len(data)) + tag + data
        out += struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)
        return out

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as fp:
        fp.write(png)
    return len(png)


def build_item():
    """物品图标。边长取 ITEM_ART 的实际尺寸（当前 64x64）。

    游戏内物品仍然只占 16 逻辑像素，64x64 的好处是同样面积里能塞下
    箭羽描边、缝线、铆钉、皮革渐变这些高分辨率才放得下的细节。
    """
    side = len(ITEM_ART)
    assert side == len(ITEM_ART[0]), '物品图标必须是正方形'
    rows = []
    for line in ITEM_ART:
        assert len(line) == side, '每行必须是 %d 列: %r' % (side, line)
        rows.append([PALETTE[ch] for ch in line])
    path = os.path.join(ROOT, 'src', 'main', 'resources', 'assets',
                        'endlessquiver', 'textures', 'item', 'quiver.png')
    return path, write_png(path, side, side, rows)


def draw_slot(rows, x, y):
    """在 (x,y) 画一个 18x18 的凹陷格子（x,y 是格子框左上角）。"""
    for dy in range(18):
        for dx in range(18):
            px, py = x + dx, y + dy
            if not (0 <= px < GUI_W and 0 <= py < GUI_H):
                continue
            if dx == 0 or dy == 0:
                rows[py][px] = SLOT_DARK
            elif dx == 17 or dy == 17:
                rows[py][px] = SLOT_LIGHT
            else:
                rows[py][px] = SLOT_BG


def build_gui():
    rows = [[GUI_BG for _ in range(GUI_W)] for _ in range(GUI_H)]

    # 外框：左上亮、右下暗（原版容器界面的观感）
    for x in range(GUI_W):
        rows[0][x] = GUI_LIGHT
        rows[GUI_H - 1][x] = GUI_DARK
    for y in range(GUI_H):
        rows[y][0] = GUI_LIGHT
        rows[y][GUI_W - 1] = GUI_DARK

    # 箭袋格子（Slot 的 x/y 是物品位置，格子框要往左上各挪 1 像素）
    draw_slot(rows, ARROW_SLOT_XY[0] - 1, ARROW_SLOT_XY[1] - 1)

    # 玩家背包 3x9 + 快捷栏 1x9
    for row in range(3):
        for col in range(9):
            draw_slot(rows, PLAYER_INV_START[0] - 1 + col * 18,
                      PLAYER_INV_START[1] - 1 + row * 18)
    for col in range(9):
        draw_slot(rows, HOTBAR_START[0] - 1 + col * 18, HOTBAR_START[1] - 1)

    path = os.path.join(ROOT, 'src', 'main', 'resources', 'assets',
                        'endlessquiver', 'textures', 'gui', 'quiver.png')
    # 补成 256x256 画布（内容在左上角）
    canvas = [row + [TRANSPARENT] * (CANVAS_W - GUI_W) for row in rows]
    canvas += [[TRANSPARENT] * CANVAS_W for _ in range(CANVAS_H - GUI_H)]
    return path, write_png(path, CANVAS_W, CANVAS_H, canvas)


def main():
    for path, size in (build_item(), build_gui()):
        print('%s  (%d bytes)' % (path, size))


if __name__ == '__main__':
    main()
