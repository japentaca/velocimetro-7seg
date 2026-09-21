# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y este proyecto sigue [Versionado Semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Por hacer

- Alerta de exceso de velocidad configurable.
- Brillo automático al máximo en modo HUD.

## [1.0.0] - 2026-09-20

### Agregado

- Display de 7 segmentos dibujado a medida, con glow verde y segmentos apagados
  visibles.
- Velocidad por GPS con `FusedLocationProviderClient` en alta precisión,
  suavizado exponencial y deadband para el auto detenido.
- Tres dígitos de 0 a 999 km/h, sin decimales y con apagado de ceros a la
  izquierda.
- Velocidad máxima de la sesión con reset.
- Odómetro de viaje por integración de velocidad y tiempo de viaje.
- Indicador de señal GPS con precisión en metros y código de color.
- Modo HUD con espejo horizontal y rotación 180° configurables por separado.
- Panel de ajustes con persistencia en `SharedPreferences`.
- Pantalla siempre encendida y orientación horizontal forzada.
- Ícono adaptativo con un dígito de 7 segmentos.
- Documentación: `README.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`,
  `SECURITY.md` y `AGENTS.md`.

[Sin publicar]: https://github.com/japentaca/velocimetro-7seg/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/japentaca/velocimetro-7seg/releases/tag/v1.0.0
