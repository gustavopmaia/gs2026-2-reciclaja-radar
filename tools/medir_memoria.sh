#!/usr/bin/env bash
# Mede memória (TOTAL PSS, KB) com o Painel aberto, N vezes, e imprime a mediana.
# Uso: tools/medir_memoria.sh caminho.apk [N]
set -euo pipefail
APK="$1"; N="${2:-5}"; PKG=br.com.reciclaja.radar
ADB="${ADB:-adb}"
$ADB uninstall $PKG >/dev/null 2>&1 || true
$ADB install "$APK" >/dev/null
$ADB shell am start -W -n $PKG/.MainActivity >/dev/null; sleep 5
vals=()
for i in $(seq 1 "$N"); do
  $ADB shell am force-stop $PKG; sleep 1
  $ADB shell am start -W -n $PKG/.MainActivity >/dev/null; sleep 6
  k=$($ADB shell dumpsys meminfo $PKG | awk '/TOTAL PSS:/{print $3}' | tr -d '\r')
  vals+=("$k"); echo "execução $i: ${k} KB"
done
printf '%s\n' "${vals[@]}" | sort -n | awk '{a[NR]=$1} END{m=(NR%2)?a[(NR+1)/2]:(a[NR/2]+a[NR/2+1])/2; print "mediana: " m " KB (n=" NR ")"}'
