# AGENTS.md

Reglas para trabajar en este proyecto (Windows + PowerShell 5.1).

## Ejecución de procesos (Gradle, adb, emulador, etc.)

**Nunca** lanzar un proceso largo de forma que parezca colgado. Reglas obligatorias:

1. **Siempre usar timeout explícito** en cada comando (parámetro `timeout`).
2. **Siempre redirigir la salida a un archivo de log** y luego leerlo, en lugar de
   consumir el stdout en vivo. Ejemplo:

   ```powershell
   & .\gradlew.bat :app:assembleDebug --console=plain --no-daemon *> build.log
   ```

3. **No usar** `Select-Object -First/-Last` ni pipes que bufferean la salida: hacen
   que el comando parezca colgado hasta que termina. Si hace falta acotar, leer el log.
4. **Validar el fin del proceso** antes de continuar:

   ```powershell
   $p = Start-Process -FilePath ".\gradlew.bat" -ArgumentList ":app:assembleDebug","--console=plain" `
        -RedirectStandardOutput out.log -RedirectStandardError err.log -NoNewWindow -PassThru
   $p | Wait-Process -Timeout 600 -ErrorAction SilentlyContinue
   if (-not $p.HasExited) { $p.Kill(); "TIMEOUT - proceso colgado" } else { "ExitCode=$($p.ExitCode)" }
   ```

5. **Monitorear procesos en background**: si algo tarda, consultar estado y log en
   pasos separados (`Get-Process`, `Get-Content -Tail`), nunca bloquear la sesión.
6. **Evitar prompts interactivos**: agregar `--console=plain`, `--no-daemon` cuando
   corresponda, y aceptar licencias de SDK con `--licenses` / `yes`.
7. Si un proceso no termina dentro del timeout: matarlo, reportar el error y no
   reintentar a ciegas.
8. **No lanzar comandos en paralelo** que compitan por el mismo lock de Gradle.

## CAUSA RAÍZ DEL "COLGADO" EN WINDOWS (leer siempre)

En Windows, los **daemons en background heredan los handles de stdout/stderr** del
proceso que los lanza. El shell no ve EOF hasta que *todos* los hijos cierran el
pipe, así que la sesión queda esperando para siempre aunque el comando ya terminó.

Aplica a:
- `adb ...` -> el `adb server` se desprende y mantiene el pipe abierto.
- `gradlew.bat` -> el `Gradle Daemon` hace lo mismo.

**Regla absoluta: nunca invocar `adb` ni `gradlew` con la salida conectada a la
consola.** Siempre con `Start-Process` + redirección a archivo + `Wait-Process`
con timeout + chequeo de `ExitCode`. Después, leer el log con `Get-Content`.

Patrón seguro para adb:

```powershell
$p = Start-Process -FilePath "adb" -ArgumentList "devices" `
     -RedirectStandardOutput "adb-out.log" -RedirectStandardError "adb-err.log" `
     -NoNewWindow -PassThru
$p | Wait-Process -Timeout 30 -ErrorAction SilentlyContinue
if (-not $p.HasExited) { $p.Kill(); "TIMEOUT" } else { "ExitCode=$($p.ExitCode)" }
Get-Content adb-out.log
```

Si hace falta, arrancar el server una sola vez y luego matarlo:
`adb kill-server` (con el mismo patrón de redirección).

## Build / verificación

- Build debug (patrón seguro):
  `Start-Process .\gradlew.bat -ArgumentList ":app:assembleDebug","--console=plain" -RedirectStandardOutput out.log -RedirectStandardError err.log -NoNewWindow -PassThru`
- APK de salida: `app\build\outputs\apk\debug\app-debug.apk`
- Instalar: `adb install -r app\build\outputs\apk\debug\app-debug.apk` (con redirección)
- Logs de la app: `adb logcat -d -s Velocimetro:*` (con redirección)
