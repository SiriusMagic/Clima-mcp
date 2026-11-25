# Arquitectura de Clima

Carpeta = package (`android/app/src/main/java/com/zirakusa/clima/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Weather` (`Sky` desde el código WMO, `Now`, `Hour`, `Day`, `Forecast.parse` de Open‑Meteo, fresco 30 min; `City` y la búsqueda); `Units` (°C/°F, viento, rumbo en 8 puntos, nivel UV), `Advice` (`Tip`s de las próximas 12 h) y `Api` (las dos direcciones fijas, coordenadas y búsqueda revisadas y codificadas); `Service` (`Fetch`, `WeatherService`: guardado si está fresco, si no uno nuevo; sin internet, el guardado); `Settings` (`Settings`, `Places`: lugares, el mostrado, «Mi ubicación» y el pronóstico guardado de cada uno). |
| `ui/` | `ClimaState` (único dueño del estado: lugares, el mostrado, pronósticos, cargando/sin conexión, búsqueda y ajustes; el trabajo de internet va por `launch`) y `ClimaApp` (`AppActions`). |
| `ui/screens/` | `WeatherScreen` (un lugar por página, arriba lugar y botones, esqueleto, sin conexión, bienvenida), `WeatherParts` (ahora con cuenta hasta el valor, consejos, por horas, 7 días, detalles), `SkyLook` (iconos, nombres, degradados del cielo, `countUp`, `Bone`), `PlacesScreen`, `SettingsScreen` (igual que en las apps hermanas), `Parts`, `Chrome` y `SettingsParts`. |
| `ui/theme/` | `Theme`: `ThemeMode` y `Accent` (naranja de fábrica) para las páginas; el tiempo siempre va sobre su cielo con texto blanco. |
| `mcp/` | Modo MCP, controlado por Arka (`HubLink`: `link`, `connect`, `disconnect`, `status`, `tool`): `Tokens` (`cli_…`, cifrados con `SecretBox`), `WeatherTools` (`weather_places`, `weather_now`, `weather_forecast`, `weather_search`; con escritura `weather_place_add`, `weather_place_remove`; a través de `WeatherData`), `McpHandler`, `RelayLink`, `McpMode` (`LiveData`) y `HubLink` (módulo `weather_`). |
| `Net.kt` | El único acceso a internet para el tiempo: OkHttp, https, solo `Api.hosts`, 20 s y 2 MiB como mucho, sin redirecciones. |
| `Location.kt` | `Here`: la posición aproximada con `LocationManager` (sin Google). |
| `Summary.kt` | `SummaryLines` (pura, con prueba) y `SummaryProvider`: «El clima» para Inicio desde el pronóstico guardado, solo con la misma firma. |
| `MainActivity.kt` | Crea el estado, pide la ubicación cuando el usuario quiere «Mi ubicación», actualiza al volver, copia e «Ir al Hub». |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Las pantallas de páginas usan `ClimaColors`; la del tiempo, blanco sobre `skyBrush`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
