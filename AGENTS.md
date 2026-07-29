# Workspace Instructions

- Usar skills somente quando o usuario solicitar de forma explicita.
- Ao instalar ou reinstalar o app no device Android do usuario, sempre acompanhar os logs em seguida.
- Ao instalar ou reinstalar o app no device Android do usuario, sempre abrir o app em seguida.
- Depois de cada instalacao, iniciar monitoramento com `adb logcat` filtrado para `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.
- Quando fizer sentido, limpar o log antes da instalacao para facilitar a leitura da execucao atual.
- Quando faltar dado, campo, comportamento ou contrato do lado backend, nao improvisar workaround no app para mascarar o problema.
- Nesses casos, pedir explicitamente ajuste no backend e descrever objetivamente a demanda, incluindo endpoint afetado, campo faltante ou contrato esperado e impacto observado na UI ou no fluxo.
- Sequencia padrao preferencial para gerar e instalar uma nova build debug no device:
- `adb logcat -c`
- `.\gradlew.bat --offline assembleDebug`
- `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- `adb shell am start -n com.detrapay/.ui.splash.SplashActivity`
- `adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"`
- Quando o APK debug ja tiver sido gerado no build atual, nao rodar Gradle de novo so para instalar no device; usar diretamente `adb install -r app\build\outputs\apk\debug\app-debug.apk`, abrir o app e acompanhar os logs.

- Este ambiente de build e lento e instavel para rede. Ao rodar build, testes ou instalacao Gradle, preferir o cache local de dependencias para evitar downloads repetidos.
- Agrupar mudancas relacionadas em um unico pacote antes de rodar build ou testes Gradle. Evitar builds intermediarios a cada pequena alteracao, pois o build deste projeto e demorado.
- Executar o Gradle preferencialmente uma unica vez ao final do pacote de mudancas, combinando no mesmo comando os testes necessarios e o `assembleDebug` quando fizer sentido.
- Fazer build intermediario somente quando ele for indispensavel para diagnosticar uma falha de compilacao ou validar uma hipotese que nao possa ser verificada de forma mais barata.
- Quando as dependencias ja estiverem em cache, usar `--offline` nos comandos Gradle; se faltar alguma dependencia, repetir sem `--offline` somente para preencher o cache.
- Aguardar ate 10 minutos por comandos de build/teste/instalacao antes de considerar timeout neste ambiente.
