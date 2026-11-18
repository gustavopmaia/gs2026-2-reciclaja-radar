#!/usr/bin/env bash
# Junta o vídeo mudo com o áudio gravado pelo grupo e garante no máximo 3:00.
# Uso: video/montar.sh [audio.m4a]   (aceita .m4a, .mp3, .wav)
# Saída: video/ReciclaJa_Radar_GS2026.mp4
set -euo pipefail
cd "$(dirname "$0")"
AUDIO="${1:-audio.m4a}"
[ -f "$AUDIO" ] || { echo "Coloque o áudio gravado em video/$AUDIO (veja docs/ROTEIRO_VIDEO.md)"; exit 1; }
# loudnorm: volume padronizado; -t 180: corta em 3:00; -shortest: termina junto com o menor dos dois
ffmpeg -y -i video_mudo.mp4 -i "$AUDIO" \
  -map 0:v:0 -map 1:a:0 -c:v copy \
  -af "loudnorm=I=-16:TP=-1.5:LRA=11,afade=t=out:st=176:d=2" -c:a aac -b:a 160k \
  -t 180 -shortest ReciclaJa_Radar_GS2026.mp4
ffprobe -v error -show_entries format=duration -of csv=p=0 ReciclaJa_Radar_GS2026.mp4 | awk '{printf "duração final: %.1f s (limite 180 s)\n", $1}'
