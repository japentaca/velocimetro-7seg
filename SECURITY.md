# Política de seguridad

## Versiones soportadas

Este proyecto es una app personal sin versiones de soporte extendido. Las
correcciones se aplican siempre sobre la última versión publicada.

| Versión | Soportada |
|---|---|
| 1.0.x | Sí |
| < 1.0 | No |

## Reportar una vulnerabilidad

Si encontrás un problema de seguridad, **no abras un issue público**. Escribí a
**japedev@gmail.com** con:

- Descripción del problema y su impacto.
- Pasos para reproducirlo.
- Versión de la app y versión de Android donde lo probaste.
- Prueba de concepto, si la tenés.

Voy a responder dentro de los 7 días hábiles con la evaluación y un plan de
corrección. Si el reporte es válido, se te dará crédito en el `CHANGELOG.md` y
en la nota de la versión, salvo que prefieras lo contrario.

## Alcance

La app **no declara el permiso `INTERNET`** y no envía datos a ningún servidor,
así que la superficie de ataque remota es nula. Los problemas que interesan son,
principalmente:

- Fugas de datos de ubicación hacia otras apps o hacia el almacenamiento
  persistente.
- Uso indebido de componentes exportados (`android:exported`).
- Dependencias con vulnerabilidades conocidas.

## Fuera de alcance

- La precisión del GPS. Es una limitación del hardware y del entorno, no un
  problema de seguridad.
- Vulnerabilidades del sistema operativo Android o del firmware del teléfono.
- Problemas que requieran un dispositivo rooteado o acceso físico con
  depuración habilitada.
