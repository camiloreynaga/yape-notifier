# Filtrado Inteligente de Notificaciones de Pago

## Estado: ✅ FASE 2 IMPLEMENTADA (API) | ⚠️ FASE 1 PENDIENTE (Android)

**Prioridad:** Media  
**Componentes afectados:** Android App, API (Laravel)

**✅ COMPLETADO (2025-01-21):**

- ✅ Fase 2 (Validación en API) - Implementada completamente
- ✅ `PaymentNotificationValidator` creado con todas las validaciones
- ✅ `NotificationService` actualizado con validación
- ✅ Tests unitarios con cobertura > 80%
- ✅ Logging detallado de notificaciones rechazadas
- ✅ Notificaciones inválidas marcadas como `status='inconsistent'`

**⚠️ PENDIENTE:**

- ⚠️ Fase 1 (Filtrado en Android) - Pendiente de implementar

**Ver:** `docs/07-reference/CHANGELOG.md` para detalles de implementación

---

## Contexto del Problema

La aplicación Android está capturando y enviando **TODAS** las notificaciones de las apps de pago (Yape, Plin, BCP, Interbank, etc.), incluyendo:

- Notificaciones de publicidad y promociones
- Recordatorios y mensajes informativos
- Ofertas y descuentos
- Notificaciones que mencionan montos pero no son pagos reales

**Ejemplos de notificaciones que NO deberían capturarse:**

- "¿Ya te depositaron? 💰💰 👀👀 Ingresa al app y revisa tu dinero disponible..."
- "Hasta $150 dscto. 💸 Solo hoy 15/12 en Despegar exclusivo con Tarjetas Interbank..."
- "¡No dejes que tu recibo venza! Recuerda que puedes yapear tus pagos..."
- "¿Por vender dólares? 💰$ Hazlo al toque desde Yape. ¡Cambia ahora! ⭐💰"

**Objetivo:** La app debe capturar **SOLO** notificaciones de pagos/transferencias reales recibidos o enviados, excluyendo publicidad, promociones y mensajes informativos.

---

## Análisis Técnico y Recomendación

### ¿Dónde implementar el filtrado?

**RECOMENDACIÓN: FILTRADO HÍBRIDO (Cliente + Servidor)**

#### 1. **FILTRADO PRINCIPAL EN ANDROID (Cliente)** ⭐ PRIORITARIO

**Ventajas:**

- ✅ **Eficiencia de red**: No envía datos innecesarios al servidor
- ✅ **Ahorro de batería**: Procesa localmente, menos transmisión de datos
- ✅ **Mejor UX**: Respuesta más rápida, menos carga en el servidor
- ✅ **Privacidad**: No envía información innecesaria al servidor
- ✅ **Menor costo**: Reduce ancho de banda y procesamiento del servidor

**Ubicación:** `PaymentNotificationParser.kt` y `PaymentNotificationListenerService.kt`

#### 2. **VALIDACIÓN SECUNDARIA EN API (Servidor)** ⚠️ RECOMENDADO

**Ventajas:**

- ✅ **Doble verificación**: Segunda capa de seguridad por si el cliente falla
- ✅ **Actualización sin app**: Puede actualizar reglas sin actualizar la app
- ✅ **Auditoría**: Puede registrar intentos de envío de notificaciones no válidas
- ✅ **Protección contra clientes maliciosos o versiones antiguas**

**Ubicación:** `NotificationService.php` (método `createNotification`)

---

## Requisitos de Implementación

### FASE 1: FILTRADO EN ANDROID (Cliente) - PRIORITARIO

#### 1.1 Crear Filtro de Exclusión de Publicidad

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/util/PaymentNotificationFilter.kt`

**Funcionalidad:**

- Crear una nueva clase `PaymentNotificationFilter` que valide si una notificación es realmente un pago
- Implementar lista de **palabras clave de exclusión** (publicidad, promociones, recordatorios)
- Implementar lista de **patrones de exclusión** (regex para detectar publicidad)
- Implementar lista de **patrones de inclusión** (solo pagos reales)
- Validar que la notificación tenga estructura de pago real (remitente + monto + acción de pago)

**Palabras clave de EXCLUSIÓN (no es pago real):**

```kotlin
// Publicidad y promociones
"descuento", "dscto", "oferta", "promoción", "promocion", "aprovecha", "solo hoy",
"exclusivo", "campaña", "gana", "participa", "sorteo", "regalo", "gratis",
"hasta", "desde", "despegar", "booking", "trivago", "viaje", "vuelo", "hotel",

// Recordatorios e informativos
"recuerda", "recordatorio", "no dejes", "venza", "vencer", "revisa", "ingresa",
"ya te depositaron", "disponible", "úsalo", "cuando quieras", "cambia ahora",
"vender dólares", "comprar dólares", "cambio", "tipo de cambio",

// Mensajes genéricos sin pago
"realizaste un consumo", "consumo con tu tarjeta", "movimiento", "saldo",
"tu saldo", "disponible", "revisa tu", "consulta", "información",

// Emojis comunes en publicidad (opcional, como indicador adicional)
"💰💰", "👀👀", "⭐", "💸", "🎁", "🎉"
```

**Patrones de EXCLUSIÓN (regex):**

```kotlin
// Patrones que indican publicidad/promoción
- "hasta.*(S/|\$|soles|dólares).*dscto|descuento" (hasta X descuento)
- "solo hoy|mañana|esta semana" (promociones temporales)
- "ya te depositaron|revisa tu dinero|ingresa al app" (recordatorios)
- "recuerda que puedes|no dejes que.*venza" (recordatorios)
- "por vender|comprar.*dólares" (cambio de moneda)
- "realizaste un consumo|movimiento en tu" (consumos, no pagos recibidos)
```

**Patrones de INCLUSIÓN (solo estos son pagos reales):**

```kotlin
// Patrones que SÍ indican pago real recibido
- ".*te envió un pago por (S/|\$).*" (Yape)
- ".*te ha plineado (S/|\$).*" (Plin)
- ".*te (envió|transferió) (un pago|dinero) (por|de) (S/|\$).*" (Bancos)
- ".*recibiste (un pago|dinero) (de|por) (S/|\$).*" (Genérico)
- ".*pago recibido.*(S/|\$).*" (Confirmación)
- ".*transferencia recibida.*(S/|\$).*" (Transferencia)
```

**Validaciones adicionales:**

1. **Validar estructura mínima**: Debe tener remitente + monto + acción de pago
2. **Validar monto válido**: El monto debe ser > 0 y < límite razonable (ej: 1,000,000)
3. **Excluir si contiene múltiples palabras de exclusión**: Si tiene 2+ palabras de exclusión, descartar
4. **Validar contexto**: El monto debe estar en contexto de pago recibido, no de oferta/descuento

#### 1.2 Actualizar PaymentNotificationParser

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/util/PaymentNotificationParser.kt`

**Cambios:**

- Integrar `PaymentNotificationFilter` antes de intentar parsear
- Si el filtro indica que NO es un pago real, retornar `null` inmediatamente
- Mantener la lógica de parsing existente para notificaciones que pasan el filtro
- Agregar logging detallado para debugging:
  - Log cuando se excluye una notificación (con razón)
  - Log cuando se incluye una notificación (con patrón detectado)

**Flujo propuesto:**

```kotlin
fun parse(title: String, text: String): PaymentDetails? {
    // PASO 1: Filtrar publicidad/promociones
    if (!PaymentNotificationFilter.isValidPaymentNotification(title, text)) {
        Log.d(TAG, "Notification excluded by filter: Title='$title', Text='$text'")
        return null
    }

    // PASO 2: Intentar parsear (lógica existente)
    // ... resto del código actual
}
```

#### 1.3 Actualizar PaymentNotificationListenerService

**Ubicación:** `apps/android-client/app/src/main/java/com/yapenotifier/android/service/PaymentNotificationListenerService.kt`

**Cambios:**

- El servicio ya verifica `paymentDetails != null`, esto seguirá funcionando
- Agregar logging adicional cuando se descarta una notificación
- Opcional: Mostrar contador de notificaciones descartadas en la UI

#### 1.4 Crear Tests Unitarios

**Ubicación:** `apps/android-client/app/src/test/java/com/yapenotifier/android/util/PaymentNotificationFilterTest.kt`

**Casos de prueba:**

- ✅ Notificaciones de pago real (deben pasar)
- ❌ Notificaciones de publicidad (deben ser excluidas)
- ❌ Notificaciones de recordatorios (deben ser excluidas)
- ❌ Notificaciones de promociones (deben ser excluidas)
- ❌ Notificaciones de consumo con tarjeta (deben ser excluidas)
- ✅ Notificaciones con montos válidos (deben pasar)
- ❌ Notificaciones con montos en contexto de oferta (deben ser excluidas)

**Ejemplos de tests:**

```kotlin
// Debe EXCLUIR
"¿Ya te depositaron? 💰💰 👀👀 Ingresa al app y revisa tu dinero disponible..."
"Hasta $150 dscto. 💸 Solo hoy 15/12 en Despegar exclusivo..."
"¡No dejes que tu recibo venza! Recuerda que puedes yapear..."
"¿Por vender dólares? 💰$ Hazlo al toque desde Yape..."

// Debe INCLUIR
"JOHN DOE te envió un pago por S/ 50. El cód. de seguridad es: 427"
"MARIA GARCIA te ha plineado S/ 25.50"
"PEDRO LOPEZ te transferió un pago de S/ 100"
```

---

### FASE 2: VALIDACIÓN EN API (Servidor) - ✅ IMPLEMENTADO

#### 2.1 ✅ PaymentNotificationValidator Creado

**Ubicación:** `apps/api/app/Services/PaymentNotificationValidator.php`

**Funcionalidad Implementada:**

- ✅ Servicio de validación que verifica si la notificación es realmente un pago
- ✅ Implementa las mismas reglas de exclusión que Android (en PHP)
- ✅ Retorna `['valid' => bool, 'reason' => string|null]`
- ✅ Incluye razón del rechazo para logging

**Validaciones Implementadas:**

1. ✅ Verifica que `body` no contenga palabras clave de exclusión (2+ keywords = rechazo)
2. ✅ Verifica que `body` coincida con patrones de inclusión
3. ✅ Verifica que `amount` sea válido (> 0.01 y < 1,000,000)
4. ✅ Verifica patrones de exclusión (regex)
5. ✅ Combina title y body para validación completa

#### 2.2 ✅ NotificationService Actualizado

**Ubicación:** `apps/api/app/Services/NotificationService.php`

**Cambios Implementados en método `createNotification`:**

- ✅ Llama a `PaymentNotificationValidator::isValid()` antes de crear
- ✅ Si no es válida:
  - ✅ Log detallado de la notificación rechazada (con razón)
  - ✅ Marca como `status = 'inconsistent'` (permite auditoría)
  - ✅ Continúa con el flujo normal (no rompe funcionalidad)

**Flujo Implementado:**

```php
public function createNotification(array $data, Device $device): Notification
{
    // Validar que sea realmente un pago (no publicidad)
    $validation = PaymentNotificationValidator::isValid($data);

    if (!$validation['valid']) {
        Log::warning('Notification rejected by validator', [
            'device_id' => $device->id,
            'reason' => $validation['reason'],
        ]);

        // Marca como inconsistent para auditoría
        $data['status'] = 'inconsistent';
    }

    // ... resto del código existente
}
```

#### 2.3 ✅ Tests Unitarios Creados

**Ubicación:** `apps/api/tests/Unit/PaymentNotificationValidatorTest.php`

**Casos de prueba implementados:**

- ✅ Notificaciones válidas (pagos reales)
- ✅ Notificaciones rechazadas (publicidad, promociones, recordatorios)
- ✅ Validación de montos (válidos e inválidos)
- ✅ Casos edge (empty body, null amount, case insensitive)
- ✅ Ejemplos reales de documentación
- ✅ Cobertura > 80%

---

## Estructura de Archivos

```
apps/android-client/app/src/main/java/com/yapenotifier/android/
├── util/
│   ├── PaymentNotificationFilter.kt (NUEVO)
│   ├── PaymentNotificationParser.kt (MODIFICAR)
│   └── PaymentNotificationFilterTest.kt (NUEVO - tests)

apps/api/app/
├── Services/
│   ├── PaymentNotificationValidator.php (NUEVO)
│   └── NotificationService.php (MODIFICAR)
├── Exceptions/
│   └── InvalidNotificationException.php (NUEVO - opcional)
└── tests/Unit/
    └── PaymentNotificationValidatorTest.php (NUEVO)
```

---

## Criterios de Aceptación

### Android (Cliente)

- ✅ No envía notificaciones de publicidad/promociones al servidor
- ✅ No envía notificaciones de recordatorios informativos
- ✅ Solo envía notificaciones de pagos/transferencias reales
- ✅ Logging detallado de notificaciones excluidas (con razón)
- ✅ Tests unitarios con cobertura > 80%
- ✅ No rompe funcionalidad existente de parsing de pagos válidos

### API (Servidor)

- ✅ Rechaza notificaciones que no son pagos reales
- ✅ Logging de notificaciones rechazadas para auditoría
- ✅ Tests unitarios con cobertura > 80%
- ✅ Manejo de errores apropiado (excepciones o status inconsistent)

---

## Configuración y Mantenimiento

### Lista de palabras clave configurable (FUTURO)

- Considerar mover lista de palabras clave a configuración remota (Firebase Remote Config o API)
- Permitir actualizar filtros sin actualizar la app
- Implementar versionado de reglas de filtrado

### Métricas y monitoreo

- Contar notificaciones excluidas por tipo (publicidad, recordatorio, etc.)
- Alertar si tasa de exclusión es muy alta (posible problema con filtros)
- Dashboard de notificaciones rechazadas (opcional)

---

## Notas Técnicas

1. **Performance**: El filtrado debe ser rápido (< 10ms por notificación)
2. **Mantenibilidad**: Las listas de palabras clave deben ser fáciles de actualizar
3. **False positives**: Minimizar rechazo de pagos reales (mejor rechazar de más que aceptar de menos)
4. **Idioma**: Considerar variaciones de español (Perú, otros países)
5. **Evolución**: Las apps de pago pueden cambiar sus mensajes, los filtros deben ser flexibles

---

## Prioridad de Implementación

1. **ALTA**: Filtrado en Android (Fase 1) - Resuelve el problema principal
2. **MEDIA**: Validación en API (Fase 2) - Segunda capa de seguridad
3. **BAJA**: Configuración remota y métricas - Mejoras futuras

---

## Entregables

1. ✅ Clase `PaymentNotificationFilter.kt` con filtrado completo
2. ✅ Actualización de `PaymentNotificationParser.kt` integrando el filtro
3. ✅ Tests unitarios completos para Android
4. ✅ Clase `PaymentNotificationValidator.php` para API
5. ✅ Actualización de `NotificationService.php` con validación
6. ✅ Tests unitarios completos para API
7. ✅ Documentación de palabras clave y patrones
8. ✅ Logging detallado para debugging y auditoría

---

## Referencias

- **Roadmap**: Ver `docs/07-reference/ROADMAP.md`
- **Estado de implementación**: Ver `docs/07-reference/IMPLEMENTATION_STATUS.md`
- **Arquitectura**: Ver `docs/03-architecture/`
