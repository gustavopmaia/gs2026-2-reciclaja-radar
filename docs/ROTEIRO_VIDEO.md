# Roteiro do vídeo: ReciclaJá Radar (2:58)

O vídeo mudo já está pronto em `video/video_mudo.mp4` (177,6 s). As imagens são **gravações reais do app no emulador**. Falta só a narração dos três integrantes.

## Como gravar e juntar

1. Abra o `video_mudo.mp4` em tela cheia e gravem **um único áudio** lendo o texto abaixo, no ritmo do vídeo. Os tempos estão na tabela; cada fala cabe com folga, num ritmo de aproximadamente 2,3 palavras por segundo.
   - Pode ser o gravador de voz do celular, o QuickTime (Arquivo → Nova Gravação de Áudio) ou uma chamada no Teams/Discord gravada.
   - Se gravarem separados, juntem os trechos na ordem e deixem pausas para casar com o tempo de cada bloco.
2. Salve como `video/audio.m4a` (também aceita .mp3 ou .wav, passando o nome: `video/montar.sh audio.mp3`).
3. Rode `video/montar.sh`. O script gera `video/ReciclaJa_Radar_GS2026.mp4`, normaliza o volume e **corta em 3:00 no máximo**.
4. Suba no YouTube como **Não listado** e coloque o link em `docs/VIDEO_URL.txt` e no PDF/entrega.

> Dica: se uma fala atrasar, não regravem tudo. Cortem uma frase do bloco seguinte. O que manda é a imagem bater com o que está sendo dito.

## Falas por bloco

| Tempo | Tela | Quem fala | Texto (falar exatamente) |
|---|---|---|---|
| **0:00 – 0:10** | Cartela: nome do app, integrantes e RMs | **Gustavo Maia** | "Olá! Somos o grupo ReciclaJá, da turma 2CCPY: Gabriel Fidalgo, Gustavo Maia e Gustavo Rossi. Este é o ReciclaJá Radar." |
| **0:10 – 0:24** | Cartela: problema e valor | **Gabriel Fidalgo** | "Hoje o lixo transborda antes de alguém olhar os dados. Ocupação dos contêineres, denúncias e chuva ficam em lugares diferentes. Nosso app junta tudo por bairro e avisa com até três dias de antecedência, explicando o porquê." |
| **0:24 – 0:40** | Cartela: arquitetura | **Gustavo Rossi** | "A arquitetura é em camadas com MVVM. A tela em Compose só descreve o estado, o ViewModel expõe um StateFlow, e o motor de previsão é Kotlin puro, sem Android. Por isso ele tem quinze testes rodando no CI." |
| **0:40 – 1:04** | Demo 1: painel do Jardim das Flores, indicadores, gráfico | **Gustavo Maia** | "No painel eu escolho o território, Jardim das Flores. Os alertas vêm ordenados por gravidade, cada indicador mostra valor, variação na semana e nível. No detalhe, o gráfico mostra trinta dias, a previsão tracejada e os limiares. Repare no selo: os dados são sintéticos e o app deixa isso claro." |
| **1:04 – 1:46** | Demo 2: alerta crítico, justificativa, decisão, histórico | **Gabriel Fidalgo** | "Abrindo o alerta crítico: a ocupação pode chegar a noventa e seis por cento. Aqui está o porquê: média móvel, tendência, previsão, quantas leituras temos, o ajuste da reta e até a chuva. Tem regra, período, fonte e confiança de oitenta e seis por cento. O app só sugere; quem decide é o gestor. Se eu tentar descartar sem responsável, ele bloqueia. Acato, informo o responsável e a observação, e a decisão vai para o histórico com data e hora." |
| **1:46 – 2:17** | Demo 3: desatualizado, insuficiente, sem dados | **Gustavo Rossi** | "E quando faltam dados? No Sítio Boa Vista o sensor parou há cinco dias: o app avisa que está desatualizado e não emite alerta novo. No Parque Industrial só existem cinco leituras, então os dados são insuficientes. Em Nova Aurora não há nada. Em nenhum desses casos o app finge que está tudo normal." |
| **2:17 – 2:50** | Demo 4: permissão negada, plano B, notificação | **Gustavo Maia** | "O recurso nativo é a notificação de alerta crítico. Sem permissão, o banner no painel é o plano B. Peço a permissão só no toque e nego. Nego de novo, e aí o botão vira um atalho para as configurações do sistema. Ativando, a notificação chega uma única vez, e tocar nela abre direto o alerta." |
| **2:50 – 2:58** | Cartela: evidências medidas | **Gustavo Rossi** | "Tudo medido antes e depois: contraste, tamanho do APK e memória. O código está no GitHub. Obrigado!" |

## Checklist antes de enviar

- [ ] Os três integrantes aparecem falando (a rubrica pede o grupo se apresentando)
- [ ] Duração final ≤ 3:00 (o `montar.sh` imprime a duração)
- [ ] Vídeo no YouTube como **Não listado** e link testado numa aba anônima
- [ ] Link colado em `docs/VIDEO_URL.txt` e na entrega do portal
