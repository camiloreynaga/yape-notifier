# Pasos para Compilar la Solución en Android Studio

## 📋 Requisitos Previos

- ✅ Android Studio (versión estable recomendada: Hedgehog | 2023.1.1 o superior)
- ✅ JDK 17 o superior
- ✅ Android SDK configurado
- ✅ Dispositivo Android o Emulador para pruebas

---

## 🚀 Pasos para Compilar

### 1. Abrir el Proyecto

1. Abre **Android Studio**
2. Selecciona **File → Open** (o **Open an Existing Project**)
3. Navega a la carpeta del proyecto:
   ```
   yape-notifier/apps/android-client
   ```
4. Haz clic en **OK** y espera a que Android Studio sincronice el proyecto

---

### 2. Sincronizar Gradle

1. Android Studio debería detectar automáticamente que necesita sincronizar
2. Si aparece una notificación en la parte superior, haz clic en **Sync Now**
3. O manualmente: **File → Sync Project with Gradle Files**
4. Espera a que termine la sincronización (puede tomar 1-3 minutos la primera vez)

---

### 3. Verificar Configuración del Proyecto

1. Abre `build.gradle.kts` (nivel de proyecto) y verifica:
   - Versión de Android Gradle Plugin
   - Versión de Kotlin
   
2. Abre `app/build.gradle.kts` y verifica:
   - `compileSdk` y `targetSdk` están configurados
   - Dependencias están actualizadas

---

### 4. Limpiar el Proyecto (Recomendado)

1. **Build → Clean Project**
2. Espera a que termine
3. **Build → Rebuild Project**
4. Esto asegura que no haya archivos compilados antiguos que causen problemas

---

### 5. Verificar que No Haya Errores

1. Revisa la pestaña **Build** en la parte inferior
2. Debería mostrar: `BUILD SUCCESSFUL`
3. Si hay errores, revísalos y corrígelos:
   - Errores de sintaxis
   - Dependencias faltantes
   - Imports incorrectos

---

### 6. Verificar Archivos Nuevos

Asegúrate de que estos archivos estén presentes:

✅ `app/src/main/java/com/yapenotifier/android/util/SourceAppMapper.kt`
✅ `app/src/main/java/com/yapenotifier/android/util/PaymentNotificationParser.kt` (modificado)
✅ `app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt` (modificado)
✅ `app/src/main/java/com/yapenotifier/android/worker/SendNotificationWorker.kt` (modificado)

---

### 7. Compilar APK de Debug

**Opción A: Desde el Menú**
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Espera a que termine la compilación
3. Cuando termine, haz clic en **locate** en la notificación
4. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

**Opción B: Desde la Terminal (Gradle)**
```bash
cd yape-notifier/apps/android-client
./gradlew assembleDebug
```
El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

---

### 8. Instalar en Dispositivo/Emulador

**Opción A: Desde Android Studio**
1. Conecta tu dispositivo Android o inicia un emulador
2. Haz clic en el botón **Run** (▶️) en la barra superior
3. O presiona **Shift + F10** (Windows/Linux) o **Ctrl + R** (Mac)
4. Selecciona el dispositivo/emulador
5. La app se instalará y ejecutará automáticamente

**Opción B: Instalación Manual**
```bash
# Conecta el dispositivo y habilita USB Debugging
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### 9. Verificar Logs

1. Abre **Logcat** en Android Studio (pestaña inferior)
2. Filtra por tag:
   - `SourceAppMapper`
   - `PaymentParser`
   - `PaymentNotificationService`
   - `SendNotificationWorker`
3. Deberías ver logs informativos cuando se capturen notificaciones

---

## 🧪 Pruebas Recomendadas

### Prueba 1: Notificación de Prueba (Plin)
1. En la app, haz clic en **"Enviar Notificación de Prueba"**
2. Verifica en Logcat que:
   - ✅ Se capture la notificación
   - ✅ Se reconozca como pago de Plin
   - ✅ Se guarde con body original
   - ✅ Se envíe con `source_app = "plin"`

### Prueba 2: Verificar Mapeo
1. Revisa los logs de `SourceAppMapper`
2. Deberías ver logs cuando se mapee un `package_name` a `source_app`

### Prueba 3: Verificar Envío
1. Revisa los logs de `SendNotificationWorker`
2. Deberías ver:
   - ✅ `"Sending notification ID: X, sourceApp: yape, packageName: com.yape.android"`
   - ✅ `"Successfully sent notification ID: X"`

---

## ⚠️ Solución de Problemas

### Error: "Unresolved reference: SourceAppMapper"
**Solución:**
1. Verifica que el archivo `SourceAppMapper.kt` existe
2. **File → Invalidate Caches / Restart → Invalidate and Restart**
3. Sincroniza Gradle nuevamente

### Error: "Cannot find symbol: PaymentNotificationParser"
**Solución:**
1. Verifica que `PaymentNotificationParser.kt` esté en la carpeta `util`
2. Limpia y reconstruye el proyecto

### Error: "BUILD FAILED"
**Solución:**
1. Revisa el error completo en la pestaña **Build**
2. Verifica que todas las dependencias estén sincronizadas
3. Intenta **File → Invalidate Caches / Restart**

### La app no captura notificaciones
**Solución:**
1. Verifica que el permiso de notificaciones esté habilitado
2. Ve a **Configuración → Apps → Yape Notifier → Notificaciones**
3. Asegúrate de que el servicio de notificaciones esté activo

---

## 📱 Configuración del Dispositivo

### Para Probar Notificaciones Reales:
1. **Habilitar Acceso a Notificaciones:**
   - Configuración → Apps → Yape Notifier → Acceso a notificaciones
   - Activa el permiso

2. **Habilitar Modo de Prueba:**
   - En la app, verifica que el servicio esté activo
   - Revisa el estado en la pantalla principal

3. **Probar con App Real:**
   - Abre Yape, Plin, o cualquier app de banco
   - Envía una notificación de prueba desde esa app
   - Verifica que se capture en Yape Notifier

---

## 🔍 Verificación Final

Antes de considerar la compilación exitosa, verifica:

- ✅ El proyecto compila sin errores
- ✅ El APK se genera correctamente
- ✅ La app se instala en el dispositivo
- ✅ Los logs muestran actividad cuando se capturan notificaciones
- ✅ Las notificaciones se envían al backend correctamente

---

## 📝 Notas Adicionales

### Para Compilar APK de Release:
1. **Build → Generate Signed Bundle / APK**
2. Selecciona **APK**
3. Configura tu keystore
4. Selecciona **release** como build variant
5. Sigue los pasos del asistente

### Para Ver el Código Compilado:
1. **Build → Analyze APK**
2. Selecciona el APK generado
3. Puedes ver el código compilado y las dependencias

---

## ✅ Checklist de Compilación

- [ ] Proyecto abierto en Android Studio
- [ ] Gradle sincronizado sin errores
- [ ] Proyecto limpiado y reconstruido
- [ ] APK compilado exitosamente
- [ ] App instalada en dispositivo/emulador
- [ ] Logs verificados
- [ ] Notificación de prueba funciona
- [ ] Mapeo de `source_app` funciona correctamente
- [ ] Envío al backend funciona

---

## 🆘 Soporte

Si encuentras problemas:
1. Revisa los logs en Logcat
2. Verifica que todas las dependencias estén instaladas
3. Consulta la documentación de Android Studio
4. Revisa el archivo `CORRECCIONES_NOTIFICACIONES.md` para entender los cambios

---

**Última actualización:** 2025-01-XX

