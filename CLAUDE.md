# Clima (el tiempo MCP): reglas para agentes

- Lee `ARQUITECTURA.md` (y el contrato común `FAMILIA.md` del repo de Inicio) y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos. Las pruebas no usan internet: respuestas guardadas en
  `app/src/test/resources`.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Clima-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Reglas puras (leer Open‑Meteo, cielo, unidades, consejos, lugares, direcciones) en `data/`, sin Android y con pruebas.
  Las pantallas solo pintan y avisan.
- Internet (excepción aceptada por el dueño para esta app): solo `Net` y solo a `Api.hosts` (los dos de Open‑Meteo), por
  https. Lo único que sale son coordenadas y las palabras de búsqueda, armadas en `Api`. Nunca una dirección que mande la
  IA. El resumen para Inicio no usa internet.
- Privacidad: lugares, pronósticos y ajustes viven solo en el teléfono (fuera de copias). «Mi ubicación» es opcional y
  aproximada, sin servicios de Google. Fuera de Open‑Meteo, nada sale salvo por el Modo MCP que el usuario active.
- Cada herramienta MCP nueva lleva su prueba en `McpTest`: lista de herramientas, llamada correcta, argumentos malos y
  «no encontrado». Las que cambian algo exigen token de lectura y escritura (`READ_ONLY` si no).
- Prohibido: `eval`, motores de scripts, SQL concatenado.
- Firma: la de depuración del entorno, igual que el resto de la familia y Gateway Hub.
