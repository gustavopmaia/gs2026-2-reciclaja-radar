"""Gera a base sintética do ReciclaJá Radar (app/src/main/assets/seed.json).

Todos os dados são FICTÍCIOS (licença CC0). Nenhum dado pessoal.
As leituras usam `dayOffset` (0 = dia da carga, -1 = ontem...). O app reancora
os offsets na data em que a carga é feita, para a demo nunca "envelhecer".

Uso: python3 tools/gerar_dados_sinteticos.py
"""
import json
import random
from pathlib import Path

random.seed(42)  # determinístico: mesma base a cada execução
DIAS = 30

TERRITORIOS = [
    {"id": "jardim-das-flores", "name": "Jardim das Flores", "type": "BAIRRO", "lat": -23.5612, "lon": -46.6550},
    {"id": "vila-esperanca", "name": "Vila Esperança", "type": "BAIRRO", "lat": -23.5480, "lon": -46.6020},
    {"id": "centro", "name": "Centro", "type": "BAIRRO", "lat": -23.5505, "lon": -46.6333},
    {"id": "sitio-boa-vista", "name": "Sítio Boa Vista", "type": "AREA_RURAL", "lat": -23.7801, "lon": -46.7420},
    {"id": "nova-aurora", "name": "Nova Aurora", "type": "AREA_RURAL", "lat": -23.8120, "lon": -46.7011},
    {"id": "parque-industrial", "name": "Parque Industrial", "type": "BAIRRO", "lat": -23.6010, "lon": -46.5900},
]

INDICADORES = [
    {"id": "ocupacao", "theme": "RESIDUOS", "name": "Ocupação dos contêineres", "unit": "%",
     "attention": 75.0, "critical": 90.0,
     "description": "Média diária de ocupação dos contêineres dos pontos críticos de coleta."},
    {"id": "denuncias", "theme": "RESIDUOS", "name": "Descarte irregular reportado", "unit": "ocorr./dia",
     "attention": 6.0, "critical": 10.0,
     "description": "Ocorrências de descarte irregular registradas pela ouvidoria (agregadas por bairro)."},
    {"id": "coleta", "theme": "RESIDUOS", "name": "Resíduos coletados", "unit": "t/dia",
     "attention": 18.0, "critical": 22.0,
     "description": "Toneladas coletadas por dia; limiares = capacidade da rota."},
    {"id": "chuva", "theme": "CLIMA", "name": "Chuva acumulada", "unit": "mm/24h",
     "attention": 30.0, "critical": 50.0,
     "description": "Precipitação acumulada em 24 h (variável auxiliar: chuva atrasa a coleta)."},
]

# perfil por território: (base, inclinação por dia, ruído) para cada indicador
PERFIS = {
    "jardim-das-flores": {"ocupacao": (58, 0.95, 2.0), "denuncias": (3, 0.12, 0.8), "coleta": (14, 0.10, 0.7), "chuva": (8, 0.9, 3.0)},
    "vila-esperanca":    {"ocupacao": (66, 0.30, 2.0), "denuncias": (4, 0.05, 0.8), "coleta": (15, 0.02, 0.7), "chuva": (6, 0.3, 3.0)},
    "centro":            {"ocupacao": (48, 0.05, 2.5), "denuncias": (2, 0.00, 0.6), "coleta": (12, 0.00, 0.6), "chuva": (5, 0.0, 2.5)},
    "sitio-boa-vista":   {"ocupacao": (55, 0.20, 2.0), "denuncias": (1, 0.00, 0.4), "coleta": (4, 0.00, 0.4), "chuva": (7, 0.0, 3.0)},
    "parque-industrial": {"ocupacao": (62, 0.40, 2.0), "denuncias": (2, 0.00, 0.5), "coleta": (19, 0.00, 0.8), "chuva": (5, 0.0, 2.0)},
}


def gerar_leituras():
    leituras = []
    for terr, perfil in PERFIS.items():
        for ind, (base, slope, ruido) in perfil.items():
            for d in range(DIAS):
                offset = d - (DIAS - 1)  # -29..0
                if terr == "sitio-boa-vista" and offset > -5:
                    continue  # sensor parou há 5 dias -> dados desatualizados
                if terr == "parque-industrial" and offset < -4:
                    continue  # só 5 dias de histórico -> dados insuficientes
                if random.random() < 0.06:
                    continue  # falhas pontuais de coleta de dado
                valor = max(0.0, base + slope * d + random.gauss(0, ruido))
                if ind == "ocupacao":
                    valor = min(valor, 100.0)
                leituras.append({"territoryId": terr, "indicatorId": ind, "dayOffset": offset,
                                 "value": round(valor, 1), "origin": "SINTETICA"})
    return leituras


def main():
    seed = {
        "meta": {
            "fonte": "Base sintética ReciclaJá (tools/gerar_dados_sinteticos.py, random.seed=42)",
            "licenca": "CC0 1.0 — dados fictícios, livres para qualquer uso",
            "origem": "SINTETICA",
            "versao": "2026.11.1",
            "inspiracao": "Formato inspirado em dados abertos de coleta seletiva e de precipitação (INMET); nenhum valor real copiado.",
        },
        "territories": TERRITORIOS,
        "indicators": INDICADORES,
        "readings": gerar_leituras(),
    }
    out = Path(__file__).resolve().parent.parent / "app/src/main/assets/seed.json"
    out.write_text(json.dumps(seed, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(seed['readings'])} leituras -> {out}")


if __name__ == "__main__":
    main()
