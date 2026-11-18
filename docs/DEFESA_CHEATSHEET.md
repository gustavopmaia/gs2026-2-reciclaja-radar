# Cola de defesa: ReciclaJá Radar (1 página)

**Em uma frase:** app Android offline que antecipa em 3 dias o transbordo de contêineres por bairro, explica cada alerta e registra a decisão humana.

## Decisão central (ADR-001): motor explicável, não ML
- **O que fizemos:** média móvel de 7 dias + regressão linear de 14 dias projetada 3 dias. O nível sai do maior valor entre previsão e média, comparado aos limiares. Confiança = R² × (leituras/14).
- **O que descartamos e por quê:**
  - **TFLite/ML:** não há histórico real para treinar, o modelo é opaco (quebra a justificativa obrigatória) e soma uns 2 MB ao APK.
  - **Só limiar no valor atual:** é reativo, avisa quando já transbordou.
  - **Servidor:** o app precisa funcionar offline.
- **Custo assumido:** não capta sazonalidade nem eventos súbitos, então pode dar **falso negativo**. A reta extrapola, por isso limitamos o horizonte a 3 dias.

## Perguntas prováveis e respostas curtas
| Pergunta | Resposta |
|---|---|
| Por que nativo e não Flutter/RN? | O trio domina Kotlin. Sem ponte JS, sem plugin de terceiro para Room e notificação, APK de 1,56 MB. iOS está fora do escopo. Se entrar, o domínio em Kotlin puro migra para KMP e só a UI é refeita. |
| Onde mora a regra? | `domain/engine/AlertEngine.kt`. Kotlin puro, zero `import android`. Os testes rodam na JVM. |
| E se o modelo errar? | A confiança aparece na tela. Abaixo de 40% o alerta cai para Informativo. Com dado velho (> 2 dias) ou poucas leituras (< 7), não há alerta. O humano sempre decide e registra. |
| Onde o dado vive quando a tela some? | Leituras e decisões no Room. Território e tema no `AppPreferences` (StateFlow). O ViewModel sobrevive à rotação, os IDs da rota ficam no `SavedStateHandle` e o formulário no `rememberSaveable`. |
| Permissão negada? | Três caminhos: concedida (notifica uma vez), negada (banner no painel) e negada para sempre (botão abre as configurações). Ao voltar, `ON_RESUME` recalcula e notifica o pendente. |
| Como trocar os dados sintéticos por reais? | Nova implementação de `RadarRepository`. Domínio e telas não mudam. |
| Estado derivado? | Alertas não são salvos, são recalculados a partir das leituras, então nunca divergem dos dados. |
| Por que sem Hilt? (ADR-002) | Grafo pequeno (5 telas). `AppContainer` tem 30 linhas. Migrar se passar de cerca de 10 telas. |
| Como provam qualidade? | Meta antes, medida antes e depois: contraste 6/11 → 11/11; APK 8,43 → 1,56 MB; PSS 47,8 → 32,8 MB; fonte a 200% corrigida. Abertura a frio 279 vs 334 ms: o R8 **não** melhorou no emulador, e assumimos isso. |

## Riscos declarados (três)
1. **Falso negativo** por sazonalidade (feriado, chuva extrema). Roadmap: componente semanal.
2. **Dados sintéticos.** Validar com dados reais da cooperativa antes do piloto.
3. **Limiares mal calibrados.** O administrador ajusta na tela. Medir falso positivo no piloto e revisar o ADR se passar de 20%.

**Fora do escopo:** iOS, login, sincronização com servidor, localização automática.

## Números para ter na ponta da língua
6 territórios · 4 indicadores · 453 leituras sintéticas (seed 42, CC0) · 5 telas · 9 componentes reutilizáveis · 15 testes · minSdk 26 / targetSdk 36 · APK 1,56 MB
