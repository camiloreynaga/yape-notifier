# 🚀 Plan de Implementación: Sistema de Login con PIN

> **Objetivo:** Migrar de sistema sin login a sistema con PIN para trazabilidad completa

---

## 📋 Resumen Ejecutivo

### Cambios Principales

| Componente | Cambios | Archivos Afectados |
|------------|---------|-------------------|
| **Backend (Laravel)** | Agregar PIN, actualizar auth, migraciones | ~15 archivos |
| **Android** | Pantalla de PIN, actualizar flujo | ~8 archivos |
| **Dashboard** | Gestión de empleados con PIN | ~5 archivos |
| **Base de Datos** | Migraciones para PIN y NOT NULL | 3 migraciones |

### Tiempo Estimado

- **Backend:** 1 día
- **Android:** 1 día
- **Dashboard:** 0.5 días
- **Testing:** 0.5 días
- **Total:** 3 días

---

## 🗓️ Plan de Implementación (3 Días)

### Día 1: Backend (Laravel)

```
Mañana (4 horas):
├─ Migración: Agregar columna PIN a users
├─ Migración: Hacer user_id NOT NULL en devices
├─ Migración: Hacer user_id NOT NULL en notifications
└─ Migración: Crear usuario "Sistema" para datos huérfanos

Tarde (4 horas):
├─ Endpoint: POST /api/auth/login-pin
├─ Endpoint: POST /api/auth/verify-pin
├─ Actualizar: DeviceLinkService (auto-registro con PIN)
└─ Tests: Validar flujo de PIN
```

### Día 2: Android

```
Mañana (4 horas):
├─ Pantalla: PinLoginActivity
├─ ViewModel: PinLoginViewModel
├─ Actualizar: Flujo de onboarding
└─ Actualizar: PreferencesManager (guardar PIN)

Tarde (4 horas):
├─ Actualizar: LinkDeviceViewModel (usar token)
├─ Actualizar: NotificationRepository (enviar token)
├─ UI: Pantalla de PIN con teclado numérico
└─ Tests: Validar flujo completo
```

### Día 3: Dashboard + Testing

```
Mañana (2 horas):
├─ Dashboard: Formulario "Agregar Empleado" con PIN
├─ Dashboard: Lista de empleados con PIN
└─ Dashboard: Resetear PIN

Tarde (2 horas):
├─ Testing end-to-end
├─ Migración de datos existentes
├─ Documentación
└─ Deploy a producción
```

---

## 📝 Cambios Detallados

## PARTE 1: BACKEND (LARAVEL)

### 1.1 Migración: Agregar PIN a Users

**Archivo:** `apps/api/database/migrations/2026_01_10_000001_add_pin_to_users_table.php`

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            // PIN de 4-6 dígitos, único, nullable (para usuarios existentes)
            $table->string('pin', 6)->unique()->nullable()->after('password');
            
            // Índice para búsquedas rápidas por PIN
            $table->index('pin');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropIndex(['pin']);
            $table->dropColumn('pin');
        });
    }
};
```

---

### 1.2 Migración: Hacer user_id NOT NULL en devices

**Archivo:** `apps/api/database/migrations/2026_01_10_000002_make_user_id_required_in_devices.php`

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use App\Models\User;
use App\Models\Device;

return new class extends Migration
{
    public function up(): void
    {
        // Paso 1: Crear usuario "Sistema" para dispositivos huérfanos
        $systemUser = User::firstOrCreate(
            ['email' => 'system@yapenotifier.internal'],
            [
                'name' => 'Sistema',
                'password' => bcrypt(Str::random(32)),
                'role' => 'system',
                'commerce_id' => 1, // Asignar al primer commerce
                'is_active' => false,
            ]
        );

        // Paso 2: Asignar user_id a dispositivos sin usuario
        Device::whereNull('user_id')->update([
            'user_id' => $systemUser->id,
        ]);

        // Paso 3: Hacer user_id NOT NULL
        Schema::table('devices', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
        });
    }

    public function down(): void
    {
        Schema::table('devices', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable()->change();
        });
    }
};
```

---

### 1.3 Migración: Hacer user_id NOT NULL en notifications

**Archivo:** `apps/api/database/migrations/2026_01_10_000003_make_user_id_required_in_notifications.php`

```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use App\Models\Notification;

return new class extends Migration
{
    public function up(): void
    {
        // Paso 1: Asignar user_id del dispositivo a notificaciones huérfanas
        DB::statement('
            UPDATE notifications n
            SET user_id = (
                SELECT d.user_id 
                FROM devices d 
                WHERE d.id = n.device_id
            )
            WHERE n.user_id IS NULL
        ');

        // Paso 2: Hacer user_id NOT NULL
        Schema::table('notifications', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
        });
    }

    public function down(): void
    {
        Schema::table('notifications', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable()->change();
        });
    }
};
```

---

### 1.4 Actualizar Modelo User

**Archivo:** `apps/api/app/Models/User.php`

```php
// Agregar en $fillable
protected $fillable = [
    'name',
    'email',
    'password',
    'pin',  // ✅ NUEVO
    'role',
    'commerce_id',
    'is_active',
];

// Agregar en $hidden
protected $hidden = [
    'password',
    'pin',  // ✅ NUEVO: No exponer PIN en responses
    'remember_token',
];

// Agregar método para validar PIN
public function validatePin(string $pin): bool
{
    return $this->pin === $pin;
}

// Agregar método para generar PIN único
public static function generateUniquePin(): string
{
    do {
        $pin = str_pad(random_int(0, 9999), 4, '0', STR_PAD_LEFT);
    } while (self::where('pin', $pin)->exists());
    
    return $pin;
}
```

---

### 1.5 Crear PinAuthController

**Archivo:** `apps/api/app/Http/Controllers/PinAuthController.php`

```php
<?php

namespace App\Http\Controllers;

use App\Http\Requests\Auth\LoginPinRequest;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Log;

class PinAuthController extends Controller
{
    /**
     * Login with PIN.
     * 
     * POST /api/auth/login-pin
     * Body: { "pin": "1234" }
     */
    public function loginWithPin(LoginPinRequest $request): JsonResponse
    {
        try {
            $pin = $request->input('pin');
            
            // Buscar usuario por PIN
            $user = User::where('pin', $pin)
                ->where('is_active', true)
                ->first();
            
            if (!$user) {
                Log::warning('PIN login failed: invalid PIN', [
                    'pin' => $pin,
                    'ip' => $request->ip(),
                ]);
                
                return response()->json([
                    'message' => 'PIN inválido',
                ], 401);
            }
            
            // Verificar que el usuario tenga commerce_id
            if (!$user->commerce_id) {
                Log::error('User without commerce_id attempted login', [
                    'user_id' => $user->id,
                ]);
                
                return response()->json([
                    'message' => 'Usuario no asociado a un comercio',
                ], 403);
            }
            
            // Generar token de autenticación
            $token = $user->createToken('android-device')->plainTextToken;
            
            Log::info('PIN login successful', [
                'user_id' => $user->id,
                'user_name' => $user->name,
                'commerce_id' => $user->commerce_id,
            ]);
            
            return response()->json([
                'message' => 'Login exitoso',
                'token' => $token,
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'role' => $user->role,
                    'commerce_id' => $user->commerce_id,
                ],
            ], 200);
            
        } catch (\Exception $e) {
            Log::error('PIN login error', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);
            
            return response()->json([
                'message' => 'Error al iniciar sesión',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
}
```

---

### 1.6 Crear LoginPinRequest

**Archivo:** `apps/api/app/Http/Requests/Auth/LoginPinRequest.php`

```php
<?php

namespace App\Http\Requests\Auth;

use Illuminate\Foundation\Http\FormRequest;

class LoginPinRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'pin' => ['required', 'string', 'regex:/^[0-9]{4,6}$/'],
        ];
    }

    public function messages(): array
    {
        return [
            'pin.required' => 'El PIN es requerido',
            'pin.regex' => 'El PIN debe tener entre 4 y 6 dígitos',
        ];
    }
}
```

---

### 1.7 Actualizar Rutas

**Archivo:** `apps/api/routes/api.php`

```php
// Agregar después de las rutas públicas existentes

// PIN Authentication (public endpoint)
Route::post('/auth/login-pin', [PinAuthController::class, 'loginWithPin']);

// Actualizar ruta de vinculación para requerir autenticación
// ANTES: Route::post('/devices/link-by-code', ...)
// AHORA:
Route::middleware('auth:sanctum')->post('/devices/link-by-code', [DeviceLinkController::class, 'linkByCode']);

// Actualizar ruta de notificaciones para requerir autenticación
// ANTES: Route::post('/notifications', ...)
// AHORA:
Route::middleware('auth:sanctum')->post('/notifications', [NotificationController::class, 'store']);
```

---

### 1.8 Actualizar DeviceLinkService

**Archivo:** `apps/api/app/Services/DeviceLinkService.php`

```php
// Actualizar método linkDevice para REQUERIR usuario

public function linkDevice(string $code, string $deviceUuid, User $user, ?string $deviceName = null): array
{
    // ✅ CAMBIO: Usuario ahora es REQUERIDO (no nullable)
    if (!$user) {
        return [
            'success' => false,
            'device' => null,
            'message' => 'Autenticación requerida. Por favor, inicia sesión con tu PIN.',
        ];
    }
    
    // Validar código...
    $validation = $this->validateCode($code);
    if (!$validation['valid']) {
        return [
            'success' => false,
            'device' => null,
            'message' => $validation['message'],
        ];
    }
    
    $linkCode = DeviceLinkCode::where('code', strtoupper($code))->first();
    
    // Validar UUID...
    $uuidPattern = '/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i';
    if (!preg_match($uuidPattern, $deviceUuid)) {
        return [
            'success' => false,
            'device' => null,
            'message' => 'Formato de UUID inválido',
        ];
    }
    
    // Buscar dispositivo por UUID
    $device = Device::where('uuid', $deviceUuid)->first();
    
    if (!$device) {
        // ✅ CAMBIO: Crear dispositivo CON user_id (requerido)
        $device = Device::create([
            'uuid' => $deviceUuid,
            'user_id' => $user->id,  // ✅ SIEMPRE con usuario
            'commerce_id' => $linkCode->commerce_id,
            'name' => $deviceName ?? 'Android Device',
            'alias' => $user->name ? "Teléfono de {$user->name}" : null,  // ✅ NUEVO
            'platform' => 'android',
            'is_active' => true,
            'last_seen_at' => now(),
        ]);
        
        Log::info('Device created with user', [
            'device_id' => $device->id,
            'user_id' => $user->id,
            'user_name' => $user->name,
            'commerce_id' => $linkCode->commerce_id,
        ]);
    } else {
        // Verificar que el dispositivo pertenezca al mismo usuario
        if ($device->user_id !== $user->id) {
            return [
                'success' => false,
                'device' => null,
                'message' => 'Este dispositivo ya está vinculado a otro usuario',
            ];
        }
        
        // Actualizar commerce_id si es diferente
        if ($device->commerce_id !== $linkCode->commerce_id) {
            $device->update([
                'commerce_id' => $linkCode->commerce_id,
                'last_seen_at' => now(),
            ]);
        }
    }
    
    // Marcar código como usado
    $linkCode->markAsUsed();
    $linkCode->update(['device_id' => $device->id]);
    
    return [
        'success' => true,
        'device' => $device->fresh(),
        'message' => 'Dispositivo vinculado exitosamente',
    ];
}
```

---

## PARTE 2: ANDROID

### 2.1 Crear PinLoginActivity

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/PinLoginActivity.kt`

```kotlin
package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.databinding.ActivityPinLoginBinding
import com.yapenotifier.android.ui.viewmodel.PinLoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PinLoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPinLoginBinding
    private val viewModel: PinLoginViewModel by viewModels()
    
    private var pin = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        // Configurar teclado numérico
        binding.btn0.setOnClickListener { addDigit("0") }
        binding.btn1.setOnClickListener { addDigit("1") }
        binding.btn2.setOnClickListener { addDigit("2") }
        binding.btn3.setOnClickListener { addDigit("3") }
        binding.btn4.setOnClickListener { addDigit("4") }
        binding.btn5.setOnClickListener { addDigit("5") }
        binding.btn6.setOnClickListener { addDigit("6") }
        binding.btn7.setOnClickListener { addDigit("7") }
        binding.btn8.setOnClickListener { addDigit("8") }
        binding.btn9.setOnClickListener { addDigit("9") }
        binding.btnBackspace.setOnClickListener { removeDigit() }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is PinLoginViewModel.LoginState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is PinLoginViewModel.LoginState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is PinLoginViewModel.LoginState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@PinLoginActivity, 
                            "¡Bienvenido ${state.userName}!", 
                            Toast.LENGTH_SHORT).show()
                        navigateToLinkDevice()
                    }
                    is PinLoginViewModel.LoginState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@PinLoginActivity, 
                            state.message, 
                            Toast.LENGTH_LONG).show()
                        clearPin()
                    }
                }
            }
        }
    }
    
    private fun addDigit(digit: String) {
        if (pin.length < 6) {
            pin += digit
            updatePinDisplay()
            
            // Auto-submit cuando llega a 4 dígitos
            if (pin.length == 4) {
                submitPin()
            }
        }
    }
    
    private fun removeDigit() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            updatePinDisplay()
        }
    }
    
    private fun updatePinDisplay() {
        binding.pinDot1.isActivated = pin.length >= 1
        binding.pinDot2.isActivated = pin.length >= 2
        binding.pinDot3.isActivated = pin.length >= 3
        binding.pinDot4.isActivated = pin.length >= 4
    }
    
    private fun submitPin() {
        Timber.d("Submitting PIN")
        viewModel.loginWithPin(pin)
    }
    
    private fun clearPin() {
        pin = ""
        updatePinDisplay()
    }
    
    private fun navigateToLinkDevice() {
        val intent = Intent(this, LinkDeviceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
```

---

### 2.2 Crear PinLoginViewModel

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/viewmodel/PinLoginViewModel.kt`

```kotlin
package com.yapenotifier.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.LoginPinRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PinLoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val userName: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
    
    fun loginWithPin(pin: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                val request = LoginPinRequest(pin = pin)
                val response = apiService.loginWithPin(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    // Guardar token y datos del usuario
                    preferencesManager.saveAuthToken(body.token)
                    preferencesManager.saveUserId(body.user.id.toString())
                    preferencesManager.saveUserName(body.user.name)
                    preferencesManager.saveCommerceId(body.user.commerceId.toString())
                    
                    Timber.d("PIN login successful: ${body.user.name}")
                    _loginState.value = LoginState.Success(body.user.name)
                    
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "PIN inválido. Verifica con tu administrador."
                        403 -> "Usuario no autorizado."
                        else -> "Error al iniciar sesión. Código: ${response.code()}"
                    }
                    
                    Timber.e("PIN login failed: ${response.code()}")
                    _loginState.value = LoginState.Error(errorMessage)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "PIN login exception")
                _loginState.value = LoginState.Error(
                    "Error de conexión. Verifica tu internet."
                )
            }
        }
    }
}
```

---

### 2.3 Crear Layout de PIN

**Archivo:** `apps/android-client/app/src/main/res/layout/activity_pin_login.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="24dp">
    
    <!-- Logo -->
    <ImageView
        android:id="@+id/logo"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:src="@drawable/ic_launcher_foreground"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="48dp"/>
    
    <!-- Título -->
    <TextView
        android:id="@+id/title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Ingresa tu PIN"
        android:textSize="24sp"
        android:textStyle="bold"
        app:layout_constraintTop_toBottomOf="@id/logo"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="24dp"/>
    
    <!-- PIN Dots -->
    <LinearLayout
        android:id="@+id/pinDotsContainer"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        app:layout_constraintTop_toBottomOf="@id/title"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="32dp">
        
        <View
            android:id="@+id/pinDot1"
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:background="@drawable/pin_dot"
            android:layout_margin="8dp"/>
        
        <View
            android:id="@+id/pinDot2"
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:background="@drawable/pin_dot"
            android:layout_margin="8dp"/>
        
        <View
            android:id="@+id/pinDot3"
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:background="@drawable/pin_dot"
            android:layout_margin="8dp"/>
        
        <View
            android:id="@+id/pinDot4"
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:background="@drawable/pin_dot"
            android:layout_margin="8dp"/>
    </LinearLayout>
    
    <!-- Teclado Numérico -->
    <GridLayout
        android:id="@+id/numericKeypad"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:columnCount="3"
        android:rowCount="4"
        app:layout_constraintTop_toBottomOf="@id/pinDotsContainer"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="48dp">
        
        <!-- Fila 1 -->
        <Button android:id="@+id/btn1" style="@style/PinButton" android:text="1"/>
        <Button android:id="@+id/btn2" style="@style/PinButton" android:text="2"/>
        <Button android:id="@+id/btn3" style="@style/PinButton" android:text="3"/>
        
        <!-- Fila 2 -->
        <Button android:id="@+id/btn4" style="@style/PinButton" android:text="4"/>
        <Button android:id="@+id/btn5" style="@style/PinButton" android:text="5"/>
        <Button android:id="@+id/btn6" style="@style/PinButton" android:text="6"/>
        
        <!-- Fila 3 -->
        <Button android:id="@+id/btn7" style="@style/PinButton" android:text="7"/>
        <Button android:id="@+id/btn8" style="@style/PinButton" android:text="8"/>
        <Button android:id="@+id/btn9" style="@style/PinButton" android:text="9"/>
        
        <!-- Fila 4 -->
        <View style="@style/PinButton" android:visibility="invisible"/>
        <Button android:id="@+id/btn0" style="@style/PinButton" android:text="0"/>
        <Button android:id="@+id/btnBackspace" style="@style/PinButton" android:text="⌫"/>
    </GridLayout>
    
    <!-- Progress Bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toBottomOf="@id/numericKeypad"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="24dp"/>
    
    <!-- Texto de ayuda -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="¿Olvidaste tu PIN?\nContacta a tu administrador"
        android:textSize="12sp"
        android:textAlignment="center"
        android:alpha="0.6"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="24dp"/>
    
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### 2.4 Actualizar ApiService

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/data/api/ApiService.kt`

```kotlin
// Agregar data class para request
data class LoginPinRequest(
    val pin: String
)

// Agregar data class para response
data class LoginPinResponse(
    val message: String,
    val token: String,
    val user: UserData
)

data class UserData(
    val id: Long,
    val name: String,
    val email: String?,
    val role: String,
    val commerceId: Long
)

// Agregar endpoint
@POST("api/auth/login-pin")
suspend fun loginWithPin(@Body request: LoginPinRequest): Response<LoginPinResponse>
```

---

### 2.5 Actualizar Flujo de Onboarding

**Archivo:** `apps/android-client/app/src/main/java/com/yapenotifier/android/ui/SplashActivity.kt`

```kotlin
// Actualizar lógica de navegación

private fun checkAuthAndNavigate() {
    lifecycleScope.launch {
        val authToken = preferencesManager.authToken.first()
        
        if (authToken.isNullOrBlank()) {
            // Sin token → Ir a PinLoginActivity
            navigateToPinLogin()
        } else {
            // Con token → Verificar si dispositivo está vinculado
            val deviceId = preferencesManager.deviceId.first()
            if (deviceId.isNullOrBlank()) {
                // Token válido pero sin dispositivo → Ir a LinkDevice
                navigateToLinkDevice()
            } else {
                // Token y dispositivo → Ir a MainActivity
                navigateToMain()
            }
        }
    }
}

private fun navigateToPinLogin() {
    val intent = Intent(this, PinLoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

---

## PARTE 3: DASHBOARD

### 3.1 Agregar Empleado con PIN

**Archivo:** `apps/web-dashboard/src/components/EmployeeForm.tsx`

```typescript
import { useState } from 'react';
import { api } from '../services/api';

export function EmployeeForm() {
  const [name, setName] = useState('');
  const [pin, setPin] = useState('');
  const [loading, setLoading] = useState(false);
  
  const generatePin = () => {
    const newPin = Math.floor(1000 + Math.random() * 9000).toString();
    setPin(newPin);
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      await api.post('/api/users', {
        name,
        pin,
        role: 'capturer',
      });
      
      alert(`Empleado creado. PIN: ${pin}\nComparte este PIN con ${name}`);
      setName('');
      setPin('');
    } catch (error) {
      alert('Error al crear empleado');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label>Nombre del Empleado</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
      </div>
      
      <div>
        <label>PIN (4 dígitos)</label>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            type="text"
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            pattern="[0-9]{4}"
            maxLength={4}
            required
          />
          <button type="button" onClick={generatePin}>
            Generar PIN
          </button>
        </div>
      </div>
      
      <button type="submit" disabled={loading}>
        {loading ? 'Creando...' : 'Crear Empleado'}
      </button>
    </form>
  );
}
```

---

## 📊 Checklist de Implementación

### Backend ✅

- [ ] Migración: Agregar PIN a users
- [ ] Migración: user_id NOT NULL en devices
- [ ] Migración: user_id NOT NULL en notifications
- [ ] Modelo User: Agregar PIN
- [ ] Controller: PinAuthController
- [ ] Request: LoginPinRequest
- [ ] Rutas: POST /api/auth/login-pin
- [ ] Actualizar: DeviceLinkService
- [ ] Tests: Flujo de PIN

### Android ✅

- [ ] Activity: PinLoginActivity
- [ ] ViewModel: PinLoginViewModel
- [ ] Layout: activity_pin_login.xml
- [ ] ApiService: Endpoint login-pin
- [ ] Actualizar: SplashActivity
- [ ] Actualizar: LinkDeviceViewModel
- [ ] Actualizar: NotificationRepository
- [ ] Tests: Flujo completo

### Dashboard ✅

- [ ] Componente: EmployeeForm
- [ ] Página: Empleados
- [ ] Función: Generar PIN
- [ ] Función: Resetear PIN

---

## 🚀 Comandos de Deploy

```bash
# 1. Commit cambios
git add .
git commit -m "feat: Implement PIN authentication system

- Add PIN column to users table
- Make user_id NOT NULL in devices and notifications
- Add PIN login endpoint
- Implement PinLoginActivity in Android
- Update dashboard with employee management"

# 2. Push a repositorio
git push origin main

# 3. En servidor de producción
cd /ruta/proyecto/infra/docker/environments/production

# 4. Ejecutar migraciones
docker compose --env-file .env exec php-fpm php artisan migrate --force

# 5. Verificar
docker compose --env-file .env exec php-fpm php artisan migrate:status
```

---

## ⏱️ Timeline

```
Día 1 (Backend):
09:00 - 10:00  Migraciones
10:00 - 11:00  PinAuthController
11:00 - 12:00  Actualizar DeviceLinkService
12:00 - 13:00  Almuerzo
13:00 - 14:00  Actualizar rutas
14:00 - 15:00  Tests backend
15:00 - 17:00  Deploy y verificación

Día 2 (Android):
09:00 - 10:00  PinLoginActivity layout
10:00 - 11:00  PinLoginViewModel
11:00 - 12:00  Actualizar ApiService
12:00 - 13:00  Almuerzo
13:00 - 14:00  Actualizar flujo de onboarding
14:00 - 15:00  Actualizar LinkDeviceViewModel
15:00 - 16:00  Tests Android
16:00 - 17:00  Build y deploy APK

Día 3 (Dashboard + Testing):
09:00 - 10:00  EmployeeForm component
10:00 - 11:00  Página de empleados
11:00 - 12:00  Testing end-to-end
12:00 - 13:00  Almuerzo
13:00 - 14:00  Migración de datos existentes
14:00 - 15:00  Documentación
15:00 - 17:00  Deploy final y verificación
```

---

**¿Quieres que empiece con la implementación?**

