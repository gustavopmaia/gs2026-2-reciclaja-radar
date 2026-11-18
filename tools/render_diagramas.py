"""Renderiza os blocos Mermaid do documento em docs/diagramas/*.png (Chrome headless). Uso: python3 tools/render_diagramas.py docs/GS2026.2_ReciclaJa_Radar.md docs/diagramas"""
import re, subprocess, sys
from PIL import Image, ImageChops
S = __import__("tempfile").mkdtemp()
CH = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
md, outdir = sys.argv[1], sys.argv[2]
s = open(md).read()
for n, b in zip(["contexto", "camadas", "sequencia"], re.findall(r"```mermaid\n(.*?)```", s, re.S)):
    open(f"{outdir}/{n}.mmd", "w").write(b)
    html = f'''<!doctype html><html><head><meta charset="utf-8"><style>body{{margin:0;background:#fff}}</style>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script></head><body><div class="mermaid">{b}</div>
<script>mermaid.initialize({{startOnLoad:true,theme:'base',flowchart:{{useMaxWidth:false}},sequence:{{useMaxWidth:false}},
themeVariables:{{fontFamily:'Helvetica, Arial',fontSize:'20px',primaryColor:'#E3F2E7',primaryBorderColor:'#1B6B3A',primaryTextColor:'#17201A',lineColor:'#1B6B3A',clusterBkg:'#F4F8F4',clusterBorder:'#9CB8A3',actorBkg:'#E3F2E7',actorBorder:'#1B6B3A',noteBkgColor:'#FFF4D6'}}}});</script></body></html>'''
    h = f"{S}/{n}.html"; open(h, "w").write(html)
    png = f"{outdir}/{n}.png"
    subprocess.run([CH, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
                    "--virtual-time-budget=8000", "--window-size=2400,2200", f"--screenshot={png}", f"file://{h}"], capture_output=True)
    im = Image.open(png).convert("RGB")
    bbox = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255))).getbbox()
    im.crop((max(0, bbox[0]-30), max(0, bbox[1]-30), bbox[2]+30, bbox[3]+30)).save(png)
    print(n, Image.open(png).size)
