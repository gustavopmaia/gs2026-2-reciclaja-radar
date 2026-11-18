#!/usr/bin/env bash
# Gera docs/GS2026.2_ReciclaJa_Radar.pdf a partir do Markdown (pandoc + Chrome headless).
# Diagramas Mermaid viram as imagens de docs/diagramas/*.png.
set -euo pipefail
cd "$(dirname "$0")/../docs"
CH="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
python3 - <<'PY'
import re
s = open("GS2026.2_ReciclaJa_Radar.md", encoding="utf-8").read()
names = iter(["contexto", "camadas", "sequencia"])
s = re.sub(r"```mermaid\n.*?```", lambda m: f'<img class="diag" src="diagramas/{next(names)}.png">', s, flags=re.S)
open("_pdf.md", "w", encoding="utf-8").write(s)
PY
pandoc _pdf.md -f markdown-yaml_metadata_block -t html5 -s --metadata title="ReciclaJá Radar — GS2026.2" --css pdf.css -o _pdf.html
"$CH" --headless=new --disable-gpu --no-pdf-header-footer --virtual-time-budget=5000 --print-to-pdf=GS2026.2_ReciclaJa_Radar.pdf "file://$PWD/_pdf.html" 2>/dev/null
rm -f _pdf.md _pdf.html
echo "ok: docs/GS2026.2_ReciclaJa_Radar.pdf"
