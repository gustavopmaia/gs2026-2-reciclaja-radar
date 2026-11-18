"""Monta o vídeo mudo (≤ 3 min) a partir dos clipes reais do emulador + cartelas.

Uso: python3 video/gerar_video.py <pasta_dos_clipes>
Clipes esperados (gravados com `adb shell screenrecord`):
  clip1_painel.mp4, clip2_alerta_decisao.mp4, clip3_degradacao.mp4, clip4_permissao.mp4
Saída: video/video_mudo.mp4 (1920x1080, 30 fps) e video/cartelas/*.png
"""
import subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CLIPS = Path(sys.argv[1])
OUT = ROOT / "cartelas"; OUT.mkdir(exist_ok=True)
TMP = ROOT / "_tmp"; TMP.mkdir(exist_ok=True)
CH = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
DIAG = (ROOT.parent / "docs/diagramas/camadas.png").as_uri()

CSS = """
*{box-sizing:border-box;margin:0}
body{width:1920px;height:1080px;background:#0F2A1B;color:#F2F7F3;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;overflow:hidden}
.k{font-size:26px;letter-spacing:.14em;text-transform:uppercase;color:#7FD8A0}
h1{font-size:92px;line-height:1.02;margin:18px 0 22px;font-weight:800}
h2{font-size:54px;line-height:1.1;margin:14px 0 26px;font-weight:700}
p,li{font-size:34px;line-height:1.38;color:#D6E4D9}
ul{padding-left:0;list-style:none} li{margin:0 0 18px;padding-left:44px;position:relative}
li:before{content:'';position:absolute;left:0;top:16px;width:20px;height:20px;border-radius:50%;background:#FFCA28}
.wrap{padding:110px 130px}
.t{display:grid;grid-template-columns:1fr auto;gap:10px 60px;font-size:40px;margin-top:40px}
.t span:nth-child(even){color:#7FD8A0;font-variant-numeric:tabular-nums}
.foot{position:absolute;left:130px;bottom:70px;font-size:26px;color:#8FB39A}
.panel{position:absolute;left:760px;top:0;width:1160px;height:1080px;padding:120px 110px 0 40px}
.num{font-size:28px;color:#FFCA28;font-weight:700;letter-spacing:.1em}
.badge{display:inline-block;border:3px solid #FFCA28;color:#FFCA28;border-radius:12px;padding:6px 18px;font-size:28px;font-weight:700;margin-bottom:10px}
"""

def page(name, body):
    h = TMP / f"{name}.html"
    h.write_text(f"<!doctype html><html><head><meta charset='utf-8'><style>{CSS}</style></head><body>{body}</body></html>", encoding="utf-8")
    png = OUT / f"{name}.png"
    subprocess.run([CH, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                    "--window-size=1920,1080", f"--screenshot={png}", h.as_uri()], capture_output=True, check=True)
    return png

# ---- cartelas cheias ----
abertura = page("s1_abertura", """<div class='wrap'><div class='k'>GS2026.2 · Cross-Platform Application Development · CitizenTech</div>
<h1>ReciclaJá Radar</h1><p>Painel de inteligência territorial que antecipa o transbordo<br>de pontos críticos de coleta e apoia a decisão humana.</p>
<div class='t'><span>Gabriel Fidalgo</span><span>RM563213</span><span>Gustavo Maia</span><span>RM562240</span><span>Gustavo Rossi</span><span>RM566075</span></div>
<div class='foot'>Turma 2CCPY · Grupo ReciclaJá · Android · Kotlin · Jetpack Compose</div></div>""")
problema = page("s2_problema", """<div class='wrap'><div class='k'>Problema e valor</div><h2>O lixo transborda antes de alguém olhar os dados</h2><ul>
<li>Ocupação dos contêineres, denúncias e chuva ficam em fontes diferentes</li>
<li>O transbordo só é notado quando o morador reclama, e a chuva vira alagamento</li>
<li>Para o gestor territorial em campo, muitas vezes sem internet</li>
<li>Valor: alerta com até 3 dias de antecedência, com justificativa, e a decisão registrada</li></ul></div>""")
arquitetura = page("s3_arquitetura", f"""<div class='wrap' style='display:grid;grid-template-columns:1fr 560px;gap:60px;padding-top:80px'>
<div><div class='k'>Arquitetura</div><h2>Camadas + MVVM, domínio em Kotlin puro</h2><ul>
<li>Interface Compose só descreve o estado</li><li>ViewModel + StateFlow com UiState selado</li>
<li>Motor de previsão explicável, sem Android (ADR-001)</li><li>Room offline-first e notificação atrás de adaptador</li>
<li>15 testes JVM e CI verde a cada push</li></ul></div>
<div style='background:#fff;border-radius:18px;padding:16px;height:920px;display:flex;align-items:center;justify-content:center'><img src='{DIAG}' style='max-height:890px;max-width:530px'></div></div>""")
fim = page("s8_fim", """<div class='wrap'><div class='k'>Evidências medidas, antes e depois</div><h2>Qualidade se prova rodando</h2>
<div class='t' style='font-size:38px'><span>Contraste WCAG ≥ 4,5:1</span><span>6/11 → 11/11</span><span>APK release (R8)</span><span>8,43 → 1,56 MB</span>
<span>Memória com painel aberto</span><span>47,8 → 32,8 MB</span><span>Testes de domínio no CI</span><span>15 verdes</span></div>
<div class='foot'>github.com/gustavopmaia/gs2026-2-reciclaja-radar · Gabriel Fidalgo · Gustavo Maia · Gustavo Rossi</div></div>""")

# ---- painéis laterais dos clipes ----
def lateral(name, n, titulo, itens):
    li = "".join(f"<li>{i}</li>" for i in itens)
    return page(name, f"<div class='panel'><div class='num'>DEMONSTRAÇÃO {n}/4 · APP REAL NO EMULADOR</div><h2>{titulo}</h2><ul>{li}</ul></div>")

p1 = lateral("c1", 1, "Painel do território", ["Jardim das Flores: alertas ordenados por gravidade", "Indicadores com variação semanal e nível", "Gráfico: 30 dias, previsão de 3 dias e limiares", "Selo de dados sintéticos sempre visível"])
p2 = lateral("c2", 2, "Alerta, justificativa e decisão", ["Crítico: ocupação pode chegar a 96,1%", "Por quê: média móvel, tendência, R², chuva, regra, período e fonte", "Confiança Alta (86%)", "Sem responsável a decisão é bloqueada", "Acatar → fica no histórico com data"])
p3 = lateral("c3", 3, "Quando faltam dados", ["Sítio Boa Vista: dados de 5 dias atrás → desatualizado, sem alerta novo", "Parque Industrial: 5 leituras → dados insuficientes", "Nova Aurora: sem dados", "O app nunca mostra 'Normal' sem base"])
p4 = lateral("c4", 4, "Permissão negada e plano B", ["Permissão pedida só no toque, com o motivo", "Negada → banner no painel (plano B)", "Negada para sempre → atalho para as configurações", "Concedida → notificação local, uma vez por alerta", "O toque na notificação abre o alerta"])

# ---- montagem ----
def still(png, secs, out):
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-loop", "1", "-t", str(secs), "-i", str(png),
                    "-vf", "fps=30,format=yuv420p", "-c:v", "libx264", "-preset", "medium", "-crf", "20", str(out)], check=True)

def clip(panel, src, speed, out):
    # celular à esquerda (altura 980) sobre o painel lateral
    vf = (f"[1:v]setpts=PTS/{speed},fps=30,scale=-2:980,pad=iw+12:ih+12:6:6:color=0x7FD8A0[ph];"
          f"[0:v][ph]overlay=150:44:shortest=1,format=yuv420p")
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-loop", "1", "-i", str(panel), "-i", str(src),
                    "-filter_complex", vf, "-an", "-c:v", "libx264", "-preset", "medium", "-crf", "20", str(out)], check=True)

SEG = [  # (tipo, arquivo, duração ou velocidade)
    ("still", abertura, 10), ("still", problema, 14), ("still", arquitetura, 16),
    ("clip", (p1, CLIPS / "clip1_painel.mp4"), 1.08),
    ("clip", (p2, CLIPS / "clip2_alerta_decisao.mp4"), 1.5),
    ("clip", (p3, CLIPS / "clip3_degradacao.mp4"), 1.52),
    ("clip", (p4, CLIPS / "clip4_permissao.mp4"), 1.52),
    ("still", fim, 8),
]
parts = []
for i, (kind, a, b) in enumerate(SEG):
    out = TMP / f"seg{i}.mp4"
    still(a, b, out) if kind == "still" else clip(a[0], a[1], b, out)
    parts.append(out)
lst = TMP / "lista.txt"
lst.write_text("".join(f"file '{p}'\n" for p in parts))
final = ROOT / "video_mudo.mp4"
subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", str(lst), "-c", "copy", str(final)], check=True)
for p in parts:
    d = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(p)], capture_output=True, text=True).stdout.strip()
    print(p.name, d)
print("total", subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(final)], capture_output=True, text=True).stdout.strip())
