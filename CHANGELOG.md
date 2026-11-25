# Clima 0.3.0

- **El Modo MCP lo controla Arka**: se quita la pantalla «Modo MCP» (Activar MCP, Redireccionar a Hub, Crear token y la
  lista de tokens). Activarlo, permitirlo en local y crear tokens se hace desde **Arka › MCP**; Ajustes › Modo MCP solo
  muestra el estado y abre Arka. Si ya estaba conectada al hub, sigue conectada con el mismo enlace; los tokens LLM y Hub
  creados aquí dejan de funcionar (créalos en Arka).
- Lucía usa las herramientas en el teléfono aunque el enlace público esté apagado; lo decide «Permitir MCP local» en Arka.
- App más ligera: 1,86 MB → 1,82 MB.

# Clima 0.2.0

- **Iconos nuevos**: seis cielos, Luna, **Nubes** (el de fábrica), Tormenta, Lluvia, Sol y Atardecer, en Ajustes › Icono
  de la app (dos filas de tres). Se quitan los anteriores (Original, Claro, Dorado, Azul y Verde); si tenías uno elegido,
  pasa solo al nuevo que ocupa su lugar.

# Clima 0.1.1

- **Conectar todas** (Gateway Hub 0.0.21): con un toque en el Hub se activa aquí «Redireccionar a Hub» y la app queda
  vinculada, sin entrar en ella. Solo lo puede pedir una app firmada por el mismo desarrollador.
- **Lucía** (la voz del Hub) usa las herramientas de esta app en el teléfono, **sin internet**, con las mismas reglas que
  por MCP. Solo mientras «Redireccionar a Hub» esté activado.
- **Icono de la app**: 5 iconos para elegir (Original, Claro, Dorado, Azul y Verde) en Ajustes › Icono de la app. Tras esta
  actualización quizá tengas que volver a poner el icono en tu pantalla de inicio (una sola vez).

# Clima 0.1.0

Primera versión: el tiempo sobre un cielo que cambia con el clima y la noche.
- Ahora (temperatura que cuenta hasta su valor), máx/mín, sensación y consejos; por horas; 7 días; detalles.
- Lugares con búsqueda (Open‑Meteo), hasta 12, ordenar, quitar y deslizar entre ellos; «Mi ubicación» opcional.
- Esqueleto al cargar, pronóstico guardado al instante y sin internet; se actualiza si tiene más de 30 min.
- °C/°F y km/h · m/s · mph; Temas.
- Modo MCP (`cli_`, módulo `weather_`): `weather_places`, `weather_now`, `weather_forecast`, `weather_search`,
  `weather_place_add`, `weather_place_remove`.
- Resumen «El clima» para la pantalla «Hoy» de Inicio.
- 13 pruebas.
