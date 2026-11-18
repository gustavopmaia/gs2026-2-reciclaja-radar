"""Mede o contraste WCAG 2.1 dos pares texto/fundo usados no tema (lê ui/theme/Theme.kt).

Uso: python3 tools/contraste.py   -> imprime razão de cada par e se passa 4,5:1
"""
import re
import sys
from pathlib import Path

THEME = Path(__file__).resolve().parent.parent / "app/src/main/java/br/com/reciclaja/radar/ui/theme/Theme.kt"

# (texto, fundo, descrição) — nomes dos tokens em Palette; "White" = #FFFFFF
PARES = [
    ("White", "Green", "Texto do botão primário (claro)"),
    ("NormalLight", "CardLight", "Rótulo 'Normal' no card (claro)"),
    ("InfoLight", "CardLight", "Rótulo 'Informativo' no card (claro)"),
    ("AttentionLight", "CardLight", "Rótulo 'Atenção' no card (claro)"),
    ("CriticalLight", "CardLight", "Rótulo 'Crítico' no card (claro)"),
    ("MutedLight", "CardLight", "Texto secundário (claro)"),
    ("NormalDark", "CardDark", "Rótulo 'Normal' no card (escuro)"),
    ("InfoDark", "CardDark", "Rótulo 'Informativo' no card (escuro)"),
    ("AttentionDark", "CardDark", "Rótulo 'Atenção' no card (escuro)"),
    ("CriticalDark", "CardDark", "Rótulo 'Crítico' no card (escuro)"),
    ("MutedDark", "CardDark", "Texto secundário (escuro)"),
]


def lum(hex6):
    def ch(c):
        c = c / 255
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (int(hex6[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)


def ratio(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)


def main():
    src = THEME.read_text(encoding="utf-8")
    tokens = {m[0]: m[1][2:] for m in re.findall(r"val (\w+) = Color\(0x([0-9A-Fa-f]{8})\)", src)}
    tokens["White"] = "FFFFFF"
    falhas = 0
    for fg, bg, desc in PARES:
        r = ratio(tokens[fg], tokens[bg])
        ok = r >= 4.5
        falhas += not ok
        print(f"{'OK ' if ok else 'FALHA'}  {r:5.2f}:1  {desc}  (#{tokens[fg][-6:]} sobre #{tokens[bg][-6:]})")
    print(f"\n{len(PARES) - falhas}/{len(PARES)} pares atendem 4,5:1")
    sys.exit(1 if falhas else 0)


if __name__ == "__main__":
    main()
