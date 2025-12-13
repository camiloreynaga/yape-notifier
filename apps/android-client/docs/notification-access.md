# Guía de Acceso a Notificaciones - Yape Notifier

Esta guía explica cómo habilitar y diagnosticar el acceso a notificaciones en Yape Notifier, con instrucciones específicas para dispositivos OPPO/realme/OnePlus y otros OEMs.

## 📋 Índice

1. [Habilitar el Permiso de Notificaciones](#habilitar-el-permiso-de-notificaciones)
2. [Instrucciones Específicas para OPPO/realme/OnePlus](#instrucciones-específicas-para-opporealmeoneplus)
3. [Instrucciones para Otros OEMs](#instrucciones-para-otros-oems)
4. [Checklist de Verificación](#checklist-de-verificación)
5. [Diagnóstico y Solución de Problemas](#diagnóstico-y-solución-de-problemas)
6. [Cómo Probar](#cómo-probar)

---

## Habilitar el Permiso de Notificaciones

### Pasos Generales (Todos los Dispositivos)

1. **Abrir la app Yape Notifier**
2. **Ir a la pantalla de Diagnóstico** (botón "Abrir Diagnóstico de Notificaciones" en la pantalla principal)
3. **Tocar el botón "Abrir Ajustes de Notificaciones"**
4. **En la pantalla del sistema:**
   - Buscar "Yape Notifier" en la lista
   - Activar el interruptor junto a "Yape Notifier"
   - Confirmar el diálogo de permisos si aparece
5. **Volver a la app** y verificar que el estado muestre "✅ Acceso a Notificaciones: HABILITADO"

### Método Alternativo (Manual)

1. Ir a **Configuración del sistema** → **Aplicaciones** → **Acceso especial** → **Acceso a notificaciones**
2. Buscar "Yape Notifier"
3. Activar el interruptor

---

## Instrucciones Específicas para OPPO/realme/OnePlus

Los dispositivos OPPO, realme y OnePlus (que usan ColorOS/realme UI) tienen restricciones adicionales que pueden impedir que el servicio funcione correctamente. Sigue estos pasos **en orden**:

### Paso 1: Habilitar Acceso a Notificaciones

1. Abre **Configuración** → **Aplicaciones** → **Gestión de aplicaciones**
2. Busca "Yape Notifier"
3. Toca en "Permisos"
4. Asegúrate de que "Notificaciones" esté habilitado
5. Ve a **Configuración** → **Aplicaciones** → **Acceso especial** → **Acceso a notificaciones**
6. Activa "Yape Notifier"

### Paso 2: Desactivar Optimización de Batería

1. Abre **Configuración** → **Batería**
2. Toca en **Optimización de batería** o **Ahorro de batería**
3. Busca "Yape Notifier"
4. Selecciona "No optimizar" o "Sin restricciones"
5. Confirma los cambios

### Paso 3: Permitir Auto-inicio (Auto-start)

1. Abre **Configuración** → **Aplicaciones** → **Gestión de aplicaciones**
2. Busca "Yape Notifier"
3. Toca en **Auto-inicio** o **Inicio automático**
4. Activa el interruptor para "Yape Notifier"

### Paso 4: Permitir Ejecución en Segundo Plano

1. Abre **Configuración** → **Aplicaciones** → **Gestión de aplicaciones**
2. Busca "Yape Notifier"
3. Toca en **Ejecución en segundo plano**
4. Selecciona "Permitir" o "Sin restricciones"

### Paso 5: Verificar Configuración de Notificaciones

1. Abre **Configuración** → **Aplicaciones** → **Gestión de aplicaciones**
2. Busca "Yape Notifier"
3. Toca en **Notificaciones**
4. Asegúrate de que todas las opciones estén habilitadas:
   - Mostrar notificaciones
   - Sonido
   - Vibración (opcional)

### Paso 6: Reiniciar el Listener (si es necesario)

Si después de seguir todos los pasos el servicio no funciona:

1. Abre la app Yape Notifier
2. Ve a **Diagnóstico de Notificaciones**
3. Toca el botón **"Reiniciar Listener"**
4. Espera unos segundos
5. Verifica que el estado muestre "✅ Acceso a Notificaciones: HABILITADO"

---

## Instrucciones para Otros OEMs

### Xiaomi/Redmi/POCO (MIUI)

1. **Habilitar acceso a notificaciones** (igual que pasos generales)
2. **Desactivar optimización de batería:**
   - Configuración → Batería → Optimización de batería
   - Buscar "Yape Notifier" → No optimizar
3. **Permitir auto-inicio:**
   - Configuración → Aplicaciones → Gestión de aplicaciones → Yape Notifier → Auto-inicio
4. **Desactivar restricción de actividad en segundo plano:**
   - Configuración → Aplicaciones → Gestión de aplicaciones → Yape Notifier → Restricciones → Sin restricciones

### Huawei/Honor (EMUI)

1. **Habilitar acceso a notificaciones**
2. **Desactivar optimización de batería:**
   - Configuración → Batería → Inicio de aplicaciones
   - Buscar "Yape Notifier" → Gestión manual → Permitir
3. **Permitir inicio automático:**
   - Configuración → Aplicaciones → Inicio de aplicaciones → Yape Notifier → Activar
4. **Añadir a apps protegidas:**
   - Configuración → Batería → Apps protegidas → Yape Notifier → Activar

### Samsung (One UI)

1. **Habilitar acceso a notificaciones**
2. **Desactivar optimización de batería:**
   - Configuración → Mantenimiento del dispositivo → Batería → Aplicaciones que nunca se duermen
   - Añadir "Yape Notifier"
3. **Permitir ejecución en segundo plano:**
   - Configuración → Aplicaciones → Yape Notifier → Batería → Sin restricciones

---

## Checklist de Verificación

Usa esta lista para verificar que todo esté configurado correctamente:

### ✅ Permisos y Acceso

- [ ] Acceso a notificaciones habilitado en Configuración del sistema
- [ ] El estado en la app muestra "✅ Acceso a Notificaciones: HABILITADO"
- [ ] El componente del servicio está habilitado (verificado en Diagnóstico)

### ✅ Optimización de Batería

- [ ] Optimización de batería desactivada para Yape Notifier
- [ ] El estado en la app muestra "✅ Optimización de Batería: DESACTIVADA"

### ✅ OEM Específico (OPPO/Xiaomi/Huawei)

- [ ] Auto-inicio habilitado (si aplica)
- [ ] Ejecución en segundo plano permitida
- [ ] App añadida a apps protegidas (Huawei)

### ✅ Funcionamiento

- [ ] El servicio se conecta correctamente (ver logs)
- [ ] Las notificaciones de prueba se capturan
- [ ] Las notificaciones reales de apps de pago se procesan

---

## Diagnóstico y Solución de Problemas

### Pantalla de Diagnóstico

La app incluye una pantalla de diagnóstico completa que puedes acceder desde:
- **MainActivity** → Botón "Abrir Diagnóstico de Notificaciones"

Esta pantalla muestra:
- Estado del acceso a notificaciones
- Estado del componente del servicio
- Estado de optimización de batería
- Información del dispositivo y OEM
- Recomendaciones específicas para tu dispositivo
- Acciones para habilitar acceso y reiniciar el servicio

### Ver Notificaciones Capturadas (Debug)

Para inspeccionar las últimas 50 notificaciones capturadas:
1. Abre **Diagnóstico de Notificaciones**
2. Toca **"Ver Notificaciones Capturadas (Debug)"**
3. Revisa la lista de notificaciones con:
   - Package name
   - Título y texto
   - Fecha y hora
   - Claves de extras

### Problemas Comunes

#### El servicio no se conecta

**Síntomas:** El estado muestra "❌ Acceso a Notificaciones: DESHABILITADO" o el servicio se desconecta frecuentemente.

**Soluciones:**
1. Verifica que el acceso esté habilitado en Configuración del sistema
2. Desactiva optimización de batería
3. Usa el botón "Reiniciar Listener" en Diagnóstico
4. Reinicia el dispositivo
5. Si persiste, deshabilita y vuelve a habilitar el acceso manualmente

#### No se capturan notificaciones

**Síntomas:** El servicio está conectado pero no se reciben notificaciones.

**Soluciones:**
1. Verifica que la app de origen (Yape, BCP, etc.) esté enviando notificaciones
2. Revisa "Ver Notificaciones Capturadas" para ver si alguna se captura
3. Verifica los logs con `adb logcat | grep PaymentNotificationService`
4. Asegúrate de que la app de origen tenga permisos de notificación

#### El servicio se desconecta después de un tiempo

**Síntomas:** El servicio funciona inicialmente pero se desconecta después de minutos u horas.

**Soluciones:**
1. **OPPO/Xiaomi/Huawei:** Sigue todos los pasos de optimización de batería y auto-inicio
2. Verifica que la app no esté en la lista de "Apps que se duermen"
3. Asegúrate de que "Ejecución en segundo plano" esté permitida
4. Considera desactivar el modo de ahorro de energía

### Logs para Debugging

Para ver logs detallados del servicio:

```bash
adb logcat | grep PaymentNotificationService
```

Los logs incluyen:
- Eventos de conexión/desconexión
- Notificaciones capturadas (package, título, texto, extras)
- Errores y excepciones

---

## Cómo Probar

### Prueba Básica

1. **Habilitar el acceso:**
   - Abre Yape Notifier
   - Ve a Diagnóstico de Notificaciones
   - Toca "Abrir Ajustes de Notificaciones"
   - Habilita Yape Notifier
   - Vuelve a la app y verifica el estado

2. **Enviar notificación de prueba:**
   - En MainActivity, toca "Enviar Notificación de Prueba"
   - Verifica en "Ver Notificaciones Capturadas" que se capturó

3. **Verificar logs:**
   - Conecta el dispositivo vía USB
   - Ejecuta: `adb logcat | grep PaymentNotificationService`
   - Deberías ver logs de `onListenerConnected` y `onNotificationPosted`

### Prueba con App Real

1. **Configurar app de pago:**
   - Instala una app de pago (Yape, BCP, etc.)
   - Asegúrate de que tenga permisos de notificación

2. **Generar notificación real:**
   - Realiza una transacción o acción que genere una notificación
   - O pide a alguien que te envíe dinero (si es Yape)

3. **Verificar captura:**
   - Abre "Ver Notificaciones Capturadas"
   - Deberías ver la notificación con el package name correcto
   - Verifica los logs para confirmar el procesamiento

### Prueba de Reconexión (OPPO)

1. **Simular desconexión:**
   - Desactiva y reactiva el acceso a notificaciones
   - O mata la app desde Configuración

2. **Reconectar:**
   - Abre la app
   - Ve a Diagnóstico
   - Toca "Reiniciar Listener"
   - Verifica que el estado vuelva a "HABILITADO"

3. **Verificar funcionamiento:**
   - Envía una notificación de prueba
   - Confirma que se capture correctamente

### Checklist de Pruebas

- [ ] El servicio se conecta al habilitar el acceso
- [ ] Las notificaciones de prueba se capturan
- [ ] Las notificaciones reales se capturan
- [ ] Los logs muestran eventos correctamente
- [ ] El botón "Reiniciar Listener" funciona
- [ ] El servicio se reconecta después de reiniciar
- [ ] La optimización de batería no afecta el servicio (después de desactivarla)
- [ ] El servicio persiste después de reiniciar el dispositivo

---

## Notas Técnicas

### Componentes del Sistema

- **PaymentNotificationListenerService:** Servicio que escucha notificaciones
- **NotificationAccessChecker:** Helper para verificar y abrir configuración
- **ServiceRebinder:** Utilidad para forzar reconexión del servicio
- **OemDetector:** Detecta el OEM y proporciona recomendaciones
- **AppDatabase (Room):** Almacena últimas 50 notificaciones para debug

### Limitaciones Conocidas

1. **OPPO/ColorOS:** Puede requerir múltiples pasos de configuración
2. **Xiaomi/MIUI:** Restricciones agresivas de batería
3. **Huawei/EMUI:** Requiere añadir a apps protegidas
4. **Android 12+:** Algunos OEMs tienen restricciones adicionales

### Mejores Prácticas

1. **Siempre verifica el estado** antes de asumir que el servicio funciona
2. **Usa la pantalla de diagnóstico** para troubleshooting
3. **Revisa los logs** cuando algo no funciona
4. **Documenta problemas específicos** del dispositivo para futuras referencias

---

## Soporte

Si después de seguir todos los pasos el servicio no funciona:

1. Revisa los logs con `adb logcat`
2. Verifica que todos los pasos del checklist estén completados
3. Prueba en otro dispositivo si es posible
4. Documenta el modelo del dispositivo, versión de Android y OEM

---

**Última actualización:** 2024

