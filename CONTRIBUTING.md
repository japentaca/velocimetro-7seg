# Contribuir a Velocímetro 7 Seg

¡Gracias por tomarte el tiempo de contribuir! Este documento describe cómo
reportar problemas, proponer cambios y preparar un pull request.

## Código de conducta

Al participar aceptás cumplir el [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).

## ¿Cómo puedo contribuir?

### Reportar un bug

1. Revisá que no exista ya un issue abierto con el mismo problema.
2. Abrí un issue usando la plantilla **Bug report**.
3. Incluí siempre:
   - Modelo de teléfono y versión de Android.
   - Versión de la app (aparece en el APK o en `app/build.gradle.kts`).
   - Pasos exactos para reproducirlo.
   - Qué esperabas y qué pasó.
   - Captura de pantalla o video si es un problema visual.
   - Logs si hay un crash: `adb logcat -d -s Velocimetro:* AndroidRuntime:E`

> Los problemas de precisión del GPS **no** son bugs de la app salvo que haya
> un error de cálculo. La calidad de la señal depende del entorno y del
> hardware del teléfono.

### Proponer una funcionalidad

Abrí un issue con la plantilla **Feature request** explicando:

- Qué problema resuelve.
- Cómo la usarías en el auto.
- Si afecta la legibilidad del display o el consumo de batería.

### Enviar código

1. Hacé un fork y creá una rama descriptiva:
   `git checkout -b fix/suavizado-en-frenadas`
2. Mantené los cambios acotados: un PR = un tema.
3. Respetá el estilo existente:
   - Kotlin, 4 espacios, sin punto y coma.
   - **No agregues comentarios** salvo que expliquen una decisión no obvia.
   - Nombres de variables y funciones en inglés; textos de UI en español.
   - Los colores y dimensiones del tema viven en `ui/Theme.kt` y `ui/Seg7.kt`.
4. Verificá que compile antes de subir:

   ```powershell
   $p = Start-Process -FilePath ".\gradlew.bat" -ArgumentList ":app:assembleDebug","--console=plain" `
        -RedirectStandardOutput out.log -RedirectStandardError err.log -NoNewWindow -PassThru
   $p | Wait-Process -Timeout 900 -ErrorAction SilentlyContinue
   if (-not $p.HasExited) { $p.Kill(); "TIMEOUT" } else { "ExitCode=$($p.ExitCode)" }
   ```

5. Probá en un dispositivo físico. La app depende del GPS real, así que el
   emulador no alcanza para validar cambios de velocidad, odómetro o modo HUD.
6. Describí en el PR qué probaste y en qué dispositivo.

## Reglas de la consola en Windows

Este repo se desarrolla en Windows con PowerShell 5.1. `gradlew` y `adb` lanzan
daemons que heredan los handles de stdout/stderr: si los invocás con la salida
conectada a la consola, la sesión queda esperando indefinidamente. Antes de
ejecutar cualquier cosa, leé [`AGENTS.md`](AGENTS.md), que documenta el patrón
seguro (`Start-Process` + redirección a archivo + `Wait-Process` con timeout).

## Estilo de commits

Mensajes cortos, en imperativo y en español, con el alcance adelante:

```
gps: limitar dt del odómetro a 5 s
ui: agrandar el engranaje de ajustes
hud: persistir la combinación de espejo
```

## Licencia de las contribuciones

Al enviar un pull request aceptás que tu aporte se distribuya bajo la licencia
MIT de este proyecto.
