# Análisis del Sistema Commerce - Requisitos y Cambios Necesarios

## 📋 Resumen Ejecutivo

El sistema ha sido actualizado para requerir un **Commerce (Negocio)** asociado a cada usuario. Esto es necesario para:
- Multi-tenancy (aislamiento de datos por negocio)
- Soporte de dual apps (AppInstance requiere commerce_id)
- Organización de usuarios y dispositivos por negocio

**Problema Actual:** El error 500 ocurre porque los usuarios existentes no tienen `commerce_id`, y el sistema intenta crear `AppInstance` sin un commerce válido.

---

## 🔍 Estado Actual del Sistema

### Backend (Laravel API)

#### ✅ Implementado:
- Modelo `Commerce` con relaciones completas
- Endpoints API para crear y consultar commerce:
  - `POST /api/commerces` - Crear commerce
  - `GET /api/commerces/me` - Obtener commerce del usuario
- Servicio `CommerceService` con lógica de negocio
- Migraciones para agregar `commerce_id` a:
  - `users` (nullable)
  - `devices` (nullable)
  - `notifications` (nullable)
  - `app_instances` (NOT NULL - **PROBLEMA**)

#### ⚠️ Problemas Identificados:

1. **AppInstance requiere commerce_id (NOT NULL)**
   - Migración: `app_instances.commerce_id` es `constrained()` sin `nullable()`
   - `AppInstanceService::findOrCreate()` retorna `null` si no hay `commerce_id`
   - Esto causa que las notificaciones fallen cuando se intenta crear AppInstance

2. **Registro de usuarios no crea commerce automáticamente**
   - `AuthController::register()` solo crea el usuario
   - No asigna `commerce_id` ni crea un commerce por defecto

3. **NotificationService depende de commerce_id**
   - Línea 42: `$commerceId = $device->commerce_id ?? $device->user->commerce_id;`
   - Si ambos son `null`, la notificación se crea con `commerce_id = null` (OK)
   - Pero si hay `package_name` y `android_user_id`, intenta crear AppInstance (FALLA)

---

### Android App

#### ❌ No Implementado:
- **No hay funcionalidad para crear commerce**
- **No hay UI para gestionar commerce**
- **No verifica si el usuario tiene commerce antes de enviar notificaciones**

#### ✅ Implementado:
- Registro de usuarios funciona
- Login funciona
- Envío de notificaciones funciona (pero falla en backend)

---

### Web Dashboard

#### ✅ Implementado:
- Página para crear commerce (`CreateCommercePage.tsx`)
- Servicio API para crear/obtener commerce
- Integración con el sistema de autenticación

---

## 🔧 Cambios Necesarios

### 1. Backend - Correcciones Críticas

#### A. Hacer `commerce_id` nullable en `app_instances`
**Archivo:** `database/migrations/2025_01_15_000004_create_app_instances_table.php`

**Problema:** `commerce_id` es NOT NULL pero puede no existir.

**Solución:** Crear nueva migración para hacer `commerce_id` nullable:

```php
Schema::table('app_instances', function (Blueprint $table) {
    $table->foreignId('commerce_id')->nullable()->change();
});
```

#### B. Actualizar `AppInstanceService` para manejar commerce_id null
**Archivo:** `app/Services/AppInstanceService.php`

**Cambio:** Permitir crear AppInstance sin commerce_id si no está disponible:

```php
public function findOrCreate(...): ?AppInstance {
    // Si no hay commerce_id, aún podemos crear la instancia
    // pero sin asociarla a un commerce
    if ($androidUserId === null) {
        return null;
    }

    $commerceId = $device->commerce_id ?? $device->user->commerce_id;
    
    // Si no hay commerce_id, retornar null (no crear AppInstance)
    // O crear sin commerce_id si la migración lo permite
    if (!$commerceId) {
        return null; // Por ahora, retornar null
    }
    
    // ... resto del código
}
```

#### C. Opcional: Crear commerce automáticamente en registro
**Archivo:** `app/Http/Controllers/AuthController.php`

**Opción 1:** Crear commerce automáticamente con nombre por defecto
**Opción 2:** Requerir que el usuario cree commerce después del registro

**Recomendación:** Opción 2 (más control, pero requiere cambios en Android)

---

### 2. Android App - Funcionalidad Faltante

#### A. Agregar modelos para Commerce
**Archivo:** `app/src/main/java/com/yapenotifier/android/data/model/Commerce.kt` (nuevo)

```kotlin
data class Commerce(
    val id: Int,
    val name: String,
    val owner_user_id: Int,
    val created_at: String,
    val updated_at: String
)

data class CreateCommerceRequest(
    val name: String
)
```

#### B. Agregar endpoints en ApiService
**Archivo:** `app/src/main/java/com/yapenotifier/android/data/api/ApiService.kt`

```kotlin
@POST("api/commerces")
suspend fun createCommerce(@Body request: CreateCommerceRequest): Response<CommerceResponse>

@GET("api/commerces/me")
suspend fun getCommerce(): Response<CommerceResponse>
```

#### C. Agregar verificación de commerce en registro/login
**Archivo:** `app/src/main/java/com/yapenotifier/android/ui/viewmodel/RegisterViewModel.kt`

**Flujo propuesto:**
1. Usuario se registra
2. Verificar si tiene commerce (`GET /api/commerces/me`)
3. Si no tiene, mostrar pantalla para crear commerce
4. Después de crear commerce, continuar con registro de dispositivo

#### D. Crear pantalla para crear commerce
**Archivo:** `app/src/main/java/com/yapenotifier/android/ui/CreateCommerceActivity.kt` (nuevo)

- Formulario simple con campo "Nombre del Negocio"
- Botón "Crear Negocio"
- Navegación automática después de crear

---

### 3. Actualización de Registros Existentes

#### Opción A: Crear commerce para usuarios existentes (Recomendado)

**Script SQL o Seeder:**

```php
// En tinker o seeder
$usersWithoutCommerce = User::whereNull('commerce_id')->get();

foreach ($usersWithoutCommerce as $user) {
    $commerce = Commerce::create([
        'name' => $user->name . ' - Negocio',
        'owner_user_id' => $user->id,
    ]);
    
    $user->update([
        'commerce_id' => $commerce->id,
        'role' => 'admin',
    ]);
    
    // Actualizar dispositivos del usuario
    Device::where('user_id', $user->id)
        ->whereNull('commerce_id')
        ->update(['commerce_id' => $commerce->id]);
}
```

#### Opción B: Usar un commerce de prueba para desarrollo

```php
$testCommerce = Commerce::firstOrCreate(
    ['name' => 'Comercio de Prueba'],
    ['owner_user_id' => 1] // Asignar al primer usuario
);

User::whereNull('commerce_id')->update(['commerce_id' => $testCommerce->id]);
Device::whereNull('commerce_id')->update(['commerce_id' => $testCommerce->id]);
```

---

## 📝 Plan de Acción para Pruebas

### Escenario 1: Usuario Nuevo (Recomendado para pruebas)

1. **Crear nuevo usuario desde Android:**
   - Abrir app Android
   - Ir a "Registrarse"
   - Completar formulario
   - **NUEVO:** Después del registro, mostrar pantalla para crear commerce
   - Crear commerce con nombre "Mi Negocio de Prueba"
   - Continuar con registro de dispositivo

2. **Verificar en backend:**
   - Usuario tiene `commerce_id` asignado
   - Commerce creado correctamente
   - Device tiene `commerce_id` asignado

3. **Probar envío de notificación:**
   - Enviar notificación de prueba
   - Verificar que se crea correctamente
   - Verificar que AppInstance se crea si aplica

### Escenario 2: Usuario Existente (Requiere actualización)

1. **Actualizar usuario existente:**
   - Ejecutar script para crear commerce
   - O crear commerce manualmente desde web dashboard
   - Asignar commerce al usuario

2. **Probar envío de notificación:**
   - Login desde Android
   - Enviar notificación de prueba
   - Verificar que funciona

### Escenario 3: Usuario Existente sin Commerce (Temporal)

1. **Hacer commerce_id nullable en app_instances:**
   - Ejecutar migración
   - Esto permite que el sistema funcione sin commerce

2. **Probar envío de notificación:**
   - Debería funcionar pero sin AppInstance

---

## 🎯 Recomendación Final

### Para Desarrollo/Pruebas Inmediatas:

1. **Crear commerce de prueba para usuarios existentes:**
   ```bash
   php artisan tinker
   # Ejecutar script de actualización
   ```

2. **Hacer commerce_id nullable en app_instances:**
   - Crear migración para hacer el campo nullable
   - Esto permite que el sistema funcione mientras se implementa la UI

3. **Probar con usuario existente actualizado:**
   - Login desde Android
   - Enviar notificación
   - Verificar que funciona

### Para Producción:

1. **Implementar UI en Android para crear commerce:**
   - Agregar pantalla de creación
   - Integrar en flujo de registro
   - Verificar commerce en login

2. **Actualizar usuarios existentes:**
   - Ejecutar script de migración
   - O requerir que creen commerce en primer login

3. **Hacer commerce_id requerido:**
   - Una vez que todos los usuarios tengan commerce
   - Hacer el campo NOT NULL nuevamente

---

## 📊 Checklist de Verificación

### Backend:
- [ ] Migración para hacer `commerce_id` nullable en `app_instances`
- [ ] Actualizar `AppInstanceService` para manejar null
- [ ] Script para crear commerce para usuarios existentes
- [ ] Verificar que endpoints de commerce funcionan

### Android:
- [ ] Modelo `Commerce` y `CreateCommerceRequest`
- [ ] Endpoints en `ApiService`
- [ ] Pantalla `CreateCommerceActivity`
- [ ] Integración en flujo de registro
- [ ] Verificación de commerce en login

### Pruebas:
- [ ] Crear nuevo usuario y commerce desde Android
- [ ] Login con usuario existente actualizado
- [ ] Enviar notificación de prueba
- [ ] Verificar que se crea AppInstance correctamente
- [ ] Verificar que notificaciones se guardan con commerce_id

---

## 🔗 Archivos a Modificar/Crear

### Backend:
1. `database/migrations/XXXX_XX_XX_make_commerce_id_nullable_in_app_instances.php` (nuevo)
2. `app/Services/AppInstanceService.php` (modificar)
3. `database/seeders/UpdateExistingUsersCommerceSeeder.php` (nuevo, opcional)

### Android:
1. `app/src/main/java/com/yapenotifier/android/data/model/Commerce.kt` (nuevo)
2. `app/src/main/java/com/yapenotifier/android/data/model/CreateCommerceRequest.kt` (nuevo)
3. `app/src/main/java/com/yapenotifier/android/data/api/ApiService.kt` (modificar)
4. `app/src/main/java/com/yapenotifier/android/ui/CreateCommerceActivity.kt` (nuevo)
5. `app/src/main/java/com/yapenotifier/android/ui/viewmodel/RegisterViewModel.kt` (modificar)
6. `app/src/main/java/com/yapenotifier/android/ui/viewmodel/LoginViewModel.kt` (modificar)

---

## ⚠️ Notas Importantes

1. **El sistema puede funcionar sin commerce temporalmente** si se hace `commerce_id` nullable en `app_instances`
2. **Para producción, commerce debe ser requerido** para mantener multi-tenancy
3. **Los usuarios existentes necesitan actualización** antes de poder usar el sistema completamente
4. **La app Android necesita actualización** para permitir crear commerce desde la app

---

**Última actualización:** 2025-12-18

