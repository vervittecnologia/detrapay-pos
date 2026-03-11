# Workspace Instructions

- Ao instalar ou reinstalar o app no device Android do usuario, sempre acompanhar os logs em seguida.
- Ao instalar ou reinstalar o app no device Android do usuario, sempre abrir o app em seguida.
- Depois de cada instalacao, iniciar monitoramento com `adb logcat` filtrado para `com.detrapay`, `AndroidRuntime` e `FATAL EXCEPTION`.
- Quando fizer sentido, limpar o log antes da instalacao para facilitar a leitura da execucao atual.
- Quando faltar dado, campo, comportamento ou contrato do lado backend, nao improvisar workaround no app para mascarar o problema.
- Nesses casos, pedir explicitamente ajuste no backend e descrever objetivamente a demanda, incluindo endpoint afetado, campo faltante ou contrato esperado e impacto observado na UI ou no fluxo.
- Sequencia padrao preferencial para deploy local no device:
- `adb logcat -c`
- `.\gradlew.bat installDebug`
- `adb shell am start -n com.detrapay/.ui.splash.SplashActivity`
- `adb logcat -d | Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"`
