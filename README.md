# Clima (el tiempo MCP)

El tiempo para Android, sobre un cielo que cambia con el clima y la noche, con acento naranja. Parte de la familia de
Stone, Luma, Calculadora, Notas, Filem, Brújula, Nivel, Reloj, Calendario, Inicio y QR: tu IA (Claude, ChatGPT…) puede
**consultar el clima de tus lugares** por MCP.

## Estado (0.1.0)

- **Ahora:** la temperatura (cuenta hasta su valor al abrir), el cielo, máxima y mínima, sensación térmica y consejos
  (paraguas, tormenta, protector solar, abrígate, calor, viento).
- **Por horas** (24 h, con probabilidad de lluvia) y **próximos 7 días** con barras de mínima a máxima.
- **Detalles:** humedad, viento (con dirección), índice UV, presión, amanecer y atardecer.
- **Lugares:** buscar ciudades (Open‑Meteo), hasta 12, ordenar y quitar; deslizar para cambiar de lugar.
  **Mi ubicación** opcional (aproximada, sin servicios de Google).
- **Carga:** esqueleto gris mientras llega el primer pronóstico; después abre al instante con el guardado y se actualiza
  si tiene más de 30 min. Sin internet muestra el guardado y lo dice.
- **Ajustes:** °C/°F, km/h · m/s · mph, Mi ubicación, **Modo MCP**, Temas, privacidad y versión.
- **Modo MCP:** `weather_places`, `weather_now`, `weather_forecast`, `weather_search` y, con token de lectura y
  escritura, `weather_place_add` y `weather_place_remove`. Tokens `cli_…`; módulo `weather_` para Gateway Hub.
- **Resumen para Inicio:** «El clima» (lugar, temperatura, máx/mín y consejos) en la pantalla «Hoy» de Inicio, desde el
  pronóstico guardado (Inicio nunca hace que Clima use internet).
- **Internet:** solo a las dos direcciones fijas de Open‑Meteo (pronóstico y búsqueda), con las coordenadas de tus lugares
  o lo que escribes al buscar; y al relay si activas el Modo MCP. Sin anuncios ni estadísticas.

Datos del tiempo: [Open‑Meteo.com](https://open-meteo.com/) (CC BY 4.0).

APK: [`android/Clima-0.1.0.apk`](android/Clima-0.1.0.apk) (`com.zirakusa.clima`, firmado como el resto de la familia y
Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md).
