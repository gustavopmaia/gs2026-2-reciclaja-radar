#!/usr/bin/env bash
# Mede cold start (TotalTime de `am start -W`) N vezes e imprime cada valor e a mediana.
# Uso: tools/medir_cold_start.sh caminho.apk [N]
set -euo pipefail
APK="$1"; N="${2:-10}"; PKG=br.com.reciclaja.radar
ADB="${ADB:-adb}"
$ADB uninstall $PKG >/dev/null 2>&1 || true
$ADB install "$APK" >/dev/null
$ADB shell am start -W -n $PKG/.MainActivity >/dev/null; sleep 5   # 1ª abertura: carga do seed (descartada)
$ADB shell cmd package compile -m speed-profile -f $PKG >/dev/null   # mesmo estado de compilação AOT nas duas versões
vals=()
for i in $(seq 1 "$N"); do
  $ADB shell am force-stop $PKG; sleep 1
  t=$($ADB shell am start -W -n $PKG/.MainActivity | awk -F': ' '/TotalTime/{print $2}' | tr -d '\r')
  vals+=("$t"); echo "execução $i: ${t} ms"; sleep 2
done
printf '%s\n' "${vals[@]}" | sort -n | awk '{a[NR]=$1} END{m=(NR%2)?a[(NR+1)/2]:(a[NR/2]+a[NR/2+1])/2; print "mediana: " m " ms (n=" NR ")"}'
