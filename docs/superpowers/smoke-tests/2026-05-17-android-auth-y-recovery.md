# Smoke test — Android auth expiry + MIUI service recovery

**Plan:** [../plans/2026-05-17-android-auth-y-recovery.md](../plans/2026-05-17-android-auth-y-recovery.md)

Marca cada item al verificarlo en dispositivo real. **Tareas 1.9, 2.5 y 4.1 del plan se cubren con esta checklist.**

## 0. Pre-flight

- [ ] Build release: `cd apps/android-client && powershell.exe -Command ".\gradlew.bat :app:assembleRelease"`. Genera APK sin errores.
- [ ] Instalar APK en el dispositivo de prueba.
- [ ] `adb logcat -d | grep "AuthSessionManager\|RetrofitClient\|AUTH "` muestra al menos un `[AUTH] AuthSessionManager initialized` al arrancar.
- [ ] El `service_log.txt` del dispositivo contiene `[AUTH]` entries tras login.

---

## 1. Flujo normal (sin errores) — regresión

- [ ] Login con credenciales válidas. Banner verde "✓ Capturando OK".
- [ ] Capturar 1 Yape. Card "ÚLTIMO EVENTO" se actualiza. Card "Enviadas Hoy" suma 1.
- [ ] `service_log.txt` muestra `[AUTH] LOGIN_OK` + `[SEND] Batch complete: all 1 notifications sent successfully`.
- [ ] No aparecen notificaciones de "Sesión expirada" ni de "Servicio detenido".

---

## 2. Flujo Problema A — token expirado (Fase 1)

### 2.1 Preparación
- [ ] Login en dispositivo de prueba (debería suceder en < 30 s tras tap).
- [ ] Capturar al menos 1 Yape correctamente para validar baseline (subida exitosa).

### 2.2 Forzar el 401

Opción A (recomendada — más realista):
- [ ] Desde backend, ejecutar `php artisan tinker` y revocar el token del usuario:
  ```php
  \App\Models\User::find(USER_ID)->tokens()->delete();
  ```

Opción B:
- [ ] Editar temporalmente `apps/api/config/sanctum.php` con `'expiration' => 1` (1 minuto), `php artisan config:clear`, esperar 1 min.

### 2.3 Disparar un nuevo SEND
- [ ] Generar otro Yape (alguien envía un pago real, o usar el botón "Enviar Notificación de Prueba" del dashboard).
- [ ] El worker intenta enviar y recibe 401.

### 2.4 Verificar comportamiento esperado
- [ ] Aparece **system notification persistente** con título "NotiCentral · Sesión expirada".
- [ ] La notificación es ongoing (no se puede swipe-dismiss).
- [ ] En la app, el banner del status card cambia a **rojo**: "⚠️ Sesión expirada — toca para iniciar sesión".
- [ ] El contador "Pendientes" sube (era 0, ahora 1+).
- [ ] El contador "Enviadas Hoy" NO sube.
- [ ] `service_log.txt` muestra:
  - `[AUTH] 401 on /api/notifications (token present) — dispatched to AuthSessionManager`
  - `[AUTH] TOKEN_EXPIRED on /api/notifications - clearing local state`
  - `[SEND] AUTH_ERROR: http=401 ... dispatched to AuthSessionManager`

### 2.5 Confirmar circuit breaker (nuevos Yapes no se intentan)
- [ ] Generar 2-3 Yapes adicionales mientras `awaitingLogin=true`.
- [ ] El contador "Pendientes" sube (2, 3, 4...) pero **no se llama a la API**: `service_log.txt` muestra `[SEND] Skipped batch: awaitingLogin=true, tokenPresent=false` por cada intento.
- [ ] **Cero entries de `[SEND] AUTH_ERROR` adicionales** — el cache ya está limpio, no hay nuevos 401s en el log.

### 2.6 Re-login y drain
- [ ] Tap en la notificación persistente. Lleva a la pantalla Splash → Login.
- [ ] Re-loguearse con credenciales válidas.
- [ ] Tras login exitoso:
  - System notification "Sesión expirada" desaparece.
  - Banner vuelve a verde "✓ Capturando OK".
  - El worker se dispara automáticamente (drain), las pendientes (2-3-4) se envían.
  - `service_log.txt` muestra `[AUTH] LOGIN_OK - draining pending notifications` + `[SEND] Batch complete: all N notifications sent successfully`.
- [ ] Contador "Pendientes" baja a 0, "Enviadas" suma N.

### 2.7 Caso negativo — POST_NOTIFICATIONS denegado
- [ ] Settings → NotiCentral → Notificaciones → **revocar** POST_NOTIFICATIONS.
- [ ] Forzar otro 401.
- [ ] La system notification NO aparece (porque el permiso está revocado), pero:
  - El banner rojo de "Sesión expirada" en la UI **sí aparece**.
  - El contador "Pendientes" sube igual.
  - Tap en el banner redirige a login.
- [ ] **Defensa en profundidad funciona**: el banner UI compensa la ausencia de notif.

---

## 3. Flujo Problema B — MIUI / OEM agresivo (Fase 2)

**Requiere dispositivo Xiaomi/Redmi/POCO. Si solo tienes Pixel/Samsung, el caso 3.x se puede simular forzando watchdog state pero el comportamiento real es distinto.**

### 3.1 Preparación
- [ ] Instalar en dispositivo MIUI/Xiaomi.
- [ ] Login + verificar que captura funciona.

### 3.2 Forzar caída del listener
- [ ] Cerrar la app desde "Recientes" (swipe up).
- [ ] Activar battery saver agresivo (Configuración → Batería → Ahorrador).
- [ ] Esperar 30+ minutos (o ejecutar `adb shell cmd jobscheduler run com.yapenotifier.android <jobId>` con el id del ServiceWatchdogWorker).

### 3.3 Verificar estado terminal
- [ ] Tras 3 intentos de rebind fallidos + `lastConnectedAge >= 30min` + `hasRealDisconnect=true`:
  - `service_log.txt` muestra `[SERVICE] ListenerRecoveryMode=MANUAL_ACTION_REQUIRED`.
  - System notification "NotiCentral · Servicio detenido" aparece, con 5 pasos numerados en texto plano.
  - Si el OEM es Xiaomi, el `extraHint()` "En MIUI también activa Autoinicio y Sin restricción de batería..." aparece al final del bigText.

### 3.4 Verificar card en la app
- [ ] Abrir la app.
- [ ] El status card muestra "⚠️ Servicio detenido" (compacto).
- [ ] La card `cardRecovery` aparece destacada (amber stroke).
- [ ] Muestra el título "Servicio detenido — necesita atención" + 5 pasos numerados.
- [ ] Botón "ABRIR AJUSTES DEL PERMISO" funciona y abre `ACTION_NOTIFICATION_LISTENER_SETTINGS`.

### 3.5 Recovery manual
- [ ] En la pantalla de Settings, buscar NotiCentral.
- [ ] Apagar el switch del permiso.
- [ ] Esperar 2 s.
- [ ] Volver a prender.
- [ ] Volver a la app. En < 5 s:
  - Status card vuelve a "✓ Capturando OK".
  - `cardRecovery` se oculta.
  - `service_log.txt` muestra `[SERVICE] LISTENER_CONNECTED` + `[SERVICE] ListenerRecoveryMode=NORMAL`.

### 3.6 Falsos positivos
- [ ] Dejar la app sin Yapes por 30+ min en horario normal (sin matar la app).
- [ ] Verificar que **NO** entra en `MANUAL_ACTION_REQUIRED` solo por idle. La card de recovery NO aparece.
- [ ] El status card puede mostrar "Reconectando..." brevemente pero vuelve a "Capturando OK" cuando llega cualquier notificación.

---

## 4. Permisos del Sistema (bug Phone 1)

### 4.1 Verificar render inmediato (Task 2.4 fix)
- [ ] Abrir la app. La card "Permisos del Sistema" muestra los 2 items inmediatamente (Permiso de Notificación + Ahorro de Batería), sin pasar por un estado vacío.
- [ ] Hacer back → reabrir varias veces. La card nunca se ve vacía.

### 4.2 Caso MIUI específico
- [ ] En dispositivo Xiaomi, abrir la app sin haber concedido el permiso de notificaciones.
- [ ] La card muestra "Permiso de Notificación: Desactivado" en rojo, NO vacío.

---

## 5. Build verification (Task 4.1)

- [ ] `cd apps/android-client && powershell.exe -Command ".\gradlew.bat :app:compileDebugKotlin"` → BUILD SUCCESSFUL.
- [ ] `git log --oneline 69a8d31..HEAD` muestra ≥ 10 commits de tareas 0.1 → 3.1.
- [ ] `apps/android-client/app/build/outputs/apk/debug/app-debug.apk` existe.

---

## Notas a registrar al ejecutar

- Dispositivos usados:
  - Phone 1 (modelo / Android version / OEM):
  - Phone 2 (modelo / Android version / OEM):
- Fecha de smoke test:
- Ejecutado por:
- Items rojos / blockers:
- Fecha de cierre del checklist:
