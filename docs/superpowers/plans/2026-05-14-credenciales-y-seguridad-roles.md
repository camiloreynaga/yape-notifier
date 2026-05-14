# Employee Credentials + Role Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give admins a usable web credential (encrypted, viewable `password_visible` column) and close the role-security gaps: only admins/super-admins manage employees, `system` role is not creatable, and PIN login is restricted to captadores.

**Architecture:** Backend-first, TDD. New `password_visible` column stores the password encrypted with Laravel `Crypt` (AES-256, reversible with APP_KEY) — separate from the existing bcrypt `password` used for auth. A new `RequireAdmin` middleware guards the `users` routes. Each role gets one credential type: captador → PIN, admin → password. Then the frontend EmployeesPage surfaces the password column with view/copy/regenerate.

**Tech Stack:** Laravel 11 (PHP 8.2), PHPUnit, PostgreSQL. React 18 + TypeScript + Tailwind, lucide-react.

**Source spec:** `docs/superpowers/specs/2026-05-14-credenciales-y-seguridad-roles.md`

---

## File Structure

### Backend (Laravel — apps/api)
- Create: `database/migrations/2026_05_14_000001_add_password_visible_to_users_table.php`
- Modify: `app/Models/User.php` — `$fillable` + `$hidden` for `password_visible`
- Create: `app/Http/Middleware/RequireAdmin.php` — 403 unless admin/super_admin
- Modify: `bootstrap/app.php` — register `require_admin` alias
- Modify: `routes/api.php` — wrap `users` routes with `require_admin`, add `regenerate-password` route
- Modify: `app/Http/Requests/User/CreateUserRequest.php` — drop `system` from role rule
- Modify: `app/Http/Requests/User/UpdateUserRequest.php` — drop `system` from role rule
- Modify: `app/Http/Controllers/UserController.php` — credential-by-role in `store`, decrypt in `index`, restrict `regeneratePin`, add `regeneratePassword`
- Modify: `app/Http/Controllers/PinAuthController.php` — restrict PIN login to `captador`
- Create: `tests/Feature/EmployeeCredentialsTest.php`
- Create: `tests/Feature/RoleSecurityTest.php`

### Frontend (React — apps/web-dashboard)
- Modify: `src/types/index.ts` — add `password_visible` to `User`
- Modify: `src/config/api.ts` — add `regeneratePassword` endpoint
- Modify: `src/services/api.ts` — `createUser` returns password, add `regenerateUserPassword`
- Modify: `src/pages/EmployeesPage.tsx` — password column (view/copy/regenerate), create modal shows password for admin

---

## Phase 1 — Database + model

### Task 1: Migration — add `password_visible` to `users`

**Files:**
- Create: `apps/api/database/migrations/2026_05_14_000001_add_password_visible_to_users_table.php`
- Modify: `apps/api/app/Models/User.php`

- [ ] **Step 1: Write the migration**

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
            // Password encrypted with Laravel Crypt (AES-256, reversible with
            // APP_KEY). Separate from `password` (bcrypt, used for auth) so the
            // commerce owner can view/copy an admin employee's credential.
            $table->text('password_visible')->nullable()->after('password');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn('password_visible');
        });
    }
};
```

- [ ] **Step 2: Run migration in dev**

Run: `cd infra/docker/environments/development && docker compose --env-file .env exec -T php-fpm php artisan migrate`
Expected: `Migrated: 2026_05_14_000001_add_password_visible_to_users_table`.

- [ ] **Step 3: Add `password_visible` to User `$fillable` and `$hidden`**

In `apps/api/app/Models/User.php`, update both arrays:

```php
protected $fillable = [
    'name',
    'email',
    'phone',
    'password',
    'password_visible',
    'pin',
    'commerce_id',
    'role',
    'is_active',
];
```

```php
protected $hidden = [
    'password',
    'password_visible',
    'pin',
    'remember_token',
];
```

(`password_visible` is hidden so it never leaks raw/encrypted in default serialization — the controller adds the decrypted value explicitly only where needed.)

- [ ] **Step 4: Commit**

```bash
git add apps/api/database/migrations/2026_05_14_000001_add_password_visible_to_users_table.php apps/api/app/Models/User.php
git commit -m "feat(api): add encrypted password_visible column to users"
```

---

## Phase 2 — Role security: middleware + validation

### Task 2: `RequireAdmin` middleware

**Files:**
- Create: `apps/api/app/Http/Middleware/RequireAdmin.php`
- Modify: `apps/api/bootstrap/app.php`

- [ ] **Step 1: Create the middleware**

```php
<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class RequireAdmin
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (! $user || ! ($user->isAdmin() || $user->isSuperAdmin())) {
            return response()->json([
                'message' => 'No autorizado. Se requiere rol de administrador.',
            ], 403);
        }

        return $next($request);
    }
}
```

- [ ] **Step 2: Register the alias in bootstrap/app.php**

In `apps/api/bootstrap/app.php`, inside the `$middleware->alias([...])` block, add the `require_admin` line:

```php
$middleware->alias([
    'verified'         => \App\Http\Middleware\EnsureEmailIsVerified::class,
    'super_admin'      => \App\Http\Middleware\RequireSuperAdmin::class,
    'commerce.active'  => \App\Http\Middleware\EnsureCommerceActive::class,
    'require_admin'    => \App\Http\Middleware\RequireAdmin::class,
]);
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/app/Http/Middleware/RequireAdmin.php apps/api/bootstrap/app.php
git commit -m "feat(api): add require_admin middleware"
```

---

### Task 3: Guard `users` routes with `require_admin` + add regenerate-password route

**Files:**
- Modify: `apps/api/routes/api.php`
- Create: `apps/api/tests/Feature/RoleSecurityTest.php`

- [ ] **Step 1: Write the failing test**

Create `apps/api/tests/Feature/RoleSecurityTest.php`:

```php
<?php

namespace Tests\Feature;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class RoleSecurityTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Create an active commerce with an owner of the given role.
     */
    private function commerceWithOwner(string $role): array
    {
        $owner = User::factory()->create(['role' => $role]);
        $commerce = Commerce::factory()->create([
            'owner_user_id' => $owner->id,
            'status' => 'active',
        ]);
        $owner->update(['commerce_id' => $commerce->id]);
        return [$owner, $commerce];
    }

    public function test_captador_cannot_list_employees(): void
    {
        [$admin, $commerce] = $this->commerceWithOwner('admin');
        $captador = User::factory()->create([
            'role' => 'captador',
            'commerce_id' => $commerce->id,
        ]);

        $this->actingAs($captador, 'sanctum');
        $this->getJson('/api/users')->assertStatus(403);
    }

    public function test_captador_cannot_create_employees(): void
    {
        [$admin, $commerce] = $this->commerceWithOwner('admin');
        $captador = User::factory()->create([
            'role' => 'captador',
            'commerce_id' => $commerce->id,
        ]);

        $this->actingAs($captador, 'sanctum');
        $this->postJson('/api/users', [
            'name' => 'Hack Admin',
            'email' => 'hack@test.com',
            'role' => 'admin',
        ])->assertStatus(403);
    }

    public function test_admin_can_list_employees(): void
    {
        [$admin] = $this->commerceWithOwner('admin');
        $this->actingAs($admin, 'sanctum');
        $this->getJson('/api/users')->assertOk();
    }
}
```

- [ ] **Step 2: Run, expect fail**

Run: `cd infra/docker/environments/development && docker compose --env-file .env exec -T php-fpm php artisan test --filter=RoleSecurityTest`
Expected: `test_captador_cannot_list_employees` and `test_captador_cannot_create_employees` FAIL (captador currently gets 200/201 — no role guard).

- [ ] **Step 3: Wrap routes in `require_admin` + add regenerate-password route**

In `apps/api/routes/api.php`, find these two lines (inside the `Route::middleware(['auth:sanctum', 'commerce.active'])->group(...)` block):

```php
    Route::apiResource('users', UserController::class);
    Route::post('/users/{id}/regenerate-pin', [UserController::class, 'regeneratePin']);
```

Replace with:

```php
    // Employee management — only admins / super admins
    Route::middleware('require_admin')->group(function () {
        Route::apiResource('users', UserController::class);
        Route::post('/users/{id}/regenerate-pin', [UserController::class, 'regeneratePin']);
        Route::post('/users/{id}/regenerate-password', [UserController::class, 'regeneratePassword']);
    });
```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=RoleSecurityTest`
Expected: 3 tests PASS.

(`regeneratePassword` controller method doesn't exist yet — that's fine, no test hits it in this task. The route registration just needs the method to exist before the route is *called*, which happens in Task 7.)

- [ ] **Step 5: Commit**

```bash
git add apps/api/routes/api.php apps/api/tests/Feature/RoleSecurityTest.php
git commit -m "feat(api): guard employee routes with require_admin middleware"
```

---

### Task 4: Remove `system` from role validation

**Files:**
- Modify: `apps/api/app/Http/Requests/User/CreateUserRequest.php`
- Modify: `apps/api/app/Http/Requests/User/UpdateUserRequest.php`
- Modify: `apps/api/tests/Feature/RoleSecurityTest.php`

- [ ] **Step 1: Append failing test**

Append to `RoleSecurityTest.php` (inside the class, before the closing brace):

```php
public function test_system_role_is_rejected_on_create(): void
{
    [$admin] = $this->commerceWithOwner('admin');
    $this->actingAs($admin, 'sanctum');

    $this->postJson('/api/users', [
        'name' => 'Sys User',
        'email' => 'sys@test.com',
        'role' => 'system',
    ])->assertStatus(422)
      ->assertJsonValidationErrors('role');
}

public function test_admin_and_captador_roles_are_accepted_on_create(): void
{
    [$admin] = $this->commerceWithOwner('admin');
    $this->actingAs($admin, 'sanctum');

    $this->postJson('/api/users', [
        'name' => 'Captador Uno',
        'email' => 'cap1@test.com',
        'role' => 'captador',
    ])->assertStatus(201);

    $this->postJson('/api/users', [
        'name' => 'Admin Dos',
        'email' => 'adm2@test.com',
        'role' => 'admin',
    ])->assertStatus(201);
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_system_role_is_rejected_on_create`
Expected: FAIL — `system` currently passes validation (gets 201 or 500, not 422).

- [ ] **Step 3: Update `CreateUserRequest`**

In `apps/api/app/Http/Requests/User/CreateUserRequest.php`:
- Change the `role` rule from `Rule::in(['admin', 'captador', 'system'])` to `Rule::in(['admin', 'captador'])`.
- Change the `role.in` message from `'El rol debe ser: admin, captador o system.'` to `'El rol debe ser: admin o captador.'`.

- [ ] **Step 4: Update `UpdateUserRequest`**

In `apps/api/app/Http/Requests/User/UpdateUserRequest.php`:
- Change the `role` rule from `Rule::in(['admin', 'captador', 'system'])` to `Rule::in(['admin', 'captador'])`.
- Change the `role.in` message from `'El rol debe ser: admin, captador o system.'` to `'El rol debe ser: admin o captador.'`.

- [ ] **Step 5: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=RoleSecurityTest`
Expected: all PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Requests/User/CreateUserRequest.php apps/api/app/Http/Requests/User/UpdateUserRequest.php apps/api/tests/Feature/RoleSecurityTest.php
git commit -m "feat(api): reject system role on employee create/update"
```

---

## Phase 3 — PIN login restricted to captador

### Task 5: Restrict `loginWithPin` to `captador` role

**Files:**
- Modify: `apps/api/app/Http/Controllers/PinAuthController.php`
- Modify: `apps/api/tests/Feature/RoleSecurityTest.php`

- [ ] **Step 1: Append failing test**

Append to `RoleSecurityTest.php`:

```php
public function test_admin_cannot_login_with_pin(): void
{
    [$admin, $commerce] = $this->commerceWithOwner('admin');
    // give the admin a PIN directly (legacy data scenario)
    $admin->update(['pin' => '4321', 'is_active' => true]);

    $this->postJson('/api/auth/login-pin', ['pin' => '4321'])
        ->assertStatus(403);
}

public function test_captador_can_login_with_pin(): void
{
    [$admin, $commerce] = $this->commerceWithOwner('admin');
    $captador = User::factory()->create([
        'role' => 'captador',
        'commerce_id' => $commerce->id,
        'pin' => '8765',
        'is_active' => true,
    ]);

    $this->postJson('/api/auth/login-pin', ['pin' => '8765'])
        ->assertOk()
        ->assertJsonPath('user.role', 'captador');
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_admin_cannot_login_with_pin`
Expected: FAIL — admin currently logs in via PIN (gets 200).

- [ ] **Step 3: Add role check in `loginWithPin`**

In `apps/api/app/Http/Controllers/PinAuthController.php`, find the block right after the `commerce_id` check (after the `if (!$user->commerce_id)` block, before `Cache::forget($cacheKey);`). Add:

```php
            // PIN login is for captadores only — admins/super_admins use
            // email + password on the web dashboard.
            if ($user->role !== 'captador') {
                Log::warning('Non-captador attempted PIN login', [
                    'user_id' => $user->id,
                    'role' => $user->role,
                ]);

                return response()->json([
                    'message' => 'Este acceso es solo para captadores. Usa tu correo y contraseña.',
                ], 403);
            }

```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=RoleSecurityTest`
Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Http/Controllers/PinAuthController.php apps/api/tests/Feature/RoleSecurityTest.php
git commit -m "feat(api): restrict PIN login to captador role"
```

---

## Phase 4 — Credential-by-role in UserController

### Task 6: `store` — generate PIN for captador, visible password for admin

**Files:**
- Modify: `apps/api/app/Http/Controllers/UserController.php`
- Create: `apps/api/tests/Feature/EmployeeCredentialsTest.php`

- [ ] **Step 1: Write the failing test**

Create `apps/api/tests/Feature/EmployeeCredentialsTest.php`:

```php
<?php

namespace Tests\Feature;

use App\Models\Commerce;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Crypt;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class EmployeeCredentialsTest extends TestCase
{
    use RefreshDatabase;

    private function actingAdmin(): array
    {
        $owner = User::factory()->create(['role' => 'admin']);
        $commerce = Commerce::factory()->create([
            'owner_user_id' => $owner->id,
            'status' => 'active',
        ]);
        $owner->update(['commerce_id' => $commerce->id]);
        $this->actingAs($owner, 'sanctum');
        return [$owner, $commerce];
    }

    public function test_creating_captador_generates_pin_and_no_visible_password(): void
    {
        $this->actingAdmin();

        $response = $this->postJson('/api/users', [
            'name' => 'Cap Uno',
            'email' => 'cap.uno@test.com',
            'role' => 'captador',
        ]);

        $response->assertStatus(201)
            ->assertJsonPath('user.role', 'captador');
        $this->assertNotEmpty($response->json('user.pin'));

        $user = User::where('email', 'cap.uno@test.com')->first();
        $this->assertNotNull($user->pin);
        $this->assertNull($user->password_visible);
    }

    public function test_creating_admin_generates_visible_password_and_no_pin(): void
    {
        $this->actingAdmin();

        $response = $this->postJson('/api/users', [
            'name' => 'Admin Uno',
            'email' => 'admin.uno@test.com',
            'role' => 'admin',
        ]);

        $response->assertStatus(201)
            ->assertJsonPath('user.role', 'admin');

        // The plaintext password is returned once, in the create response
        $plain = $response->json('user.password');
        $this->assertNotEmpty($plain);

        $user = User::where('email', 'admin.uno@test.com')->first();
        $this->assertNull($user->pin);
        $this->assertNotNull($user->password_visible);
        // password_visible decrypts to the same plaintext
        $this->assertEquals($plain, Crypt::decryptString($user->password_visible));
        // and the bcrypt password actually matches that plaintext (can log in)
        $this->assertTrue(Hash::check($plain, $user->password));
    }
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=EmployeeCredentialsTest`
Expected: FAIL — `store` currently always generates a PIN and never sets `password_visible`.

- [ ] **Step 3: Rewrite `store` in UserController**

In `apps/api/app/Http/Controllers/UserController.php`:

First, add the imports at the top (after the existing `use` lines):

```php
use Illuminate\Support\Facades\Crypt;
use Illuminate\Support\Str;
```

Then replace the `store` method body (the part inside the `try`) with this credential-by-role logic. Replace from `// Generar PIN único automáticamente` through the `return response()->json([...], 201);`:

```php
            $role = $request->role ?? 'captador';

            // Each role gets one credential type:
            //   captador → PIN (Android app)
            //   admin    → password (web dashboard)
            $pin = null;
            $plainPassword = null;

            if ($role === 'captador') {
                $pin = User::generateUniquePin(4);
                // captadores don't log in with a password — store a random one
                $passwordHash = Hash::make(bin2hex(random_bytes(16)));
                $passwordVisible = null;
            } else { // admin
                $plainPassword = Str::random(12);
                $passwordHash = Hash::make($plainPassword);
                $passwordVisible = Crypt::encryptString($plainPassword);
            }

            $user = User::create([
                'name' => $request->name,
                'email' => $request->email,
                'password' => $passwordHash,
                'password_visible' => $passwordVisible,
                'pin' => $pin,
                'role' => $role,
                'commerce_id' => $adminUser->commerce_id,
                'is_active' => $request->is_active ?? true,
            ]);

            Log::info('Employee created', [
                'user_id' => $user->id,
                'role' => $role,
                'created_by' => $adminUser->id,
                'commerce_id' => $adminUser->commerce_id,
            ]);

            return response()->json([
                'message' => 'Empleado creado exitosamente',
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'role' => $user->role,
                    'is_active' => $user->is_active,
                    'pin' => $pin,                 // null for admin
                    'password' => $plainPassword,  // null for captador, shown once
                ],
            ], 201);
```

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=EmployeeCredentialsTest`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api/app/Http/Controllers/UserController.php apps/api/tests/Feature/EmployeeCredentialsTest.php
git commit -m "feat(api): credential-by-role on employee create (PIN vs password)"
```

---

### Task 7: `regeneratePassword` + restrict `regeneratePin` to captador

**Files:**
- Modify: `apps/api/app/Http/Controllers/UserController.php`
- Modify: `apps/api/tests/Feature/EmployeeCredentialsTest.php`

- [ ] **Step 1: Append failing tests**

Append to `EmployeeCredentialsTest.php`:

```php
public function test_regenerate_password_for_admin_returns_new_usable_password(): void
{
    [$owner, $commerce] = $this->actingAdmin();
    $employee = User::factory()->create([
        'role' => 'admin',
        'commerce_id' => $commerce->id,
        'pin' => null,
    ]);

    $response = $this->postJson("/api/users/{$employee->id}/regenerate-password");

    $response->assertOk();
    $plain = $response->json('password');
    $this->assertNotEmpty($plain);

    $employee->refresh();
    $this->assertTrue(\Illuminate\Support\Facades\Hash::check($plain, $employee->password));
    $this->assertEquals($plain, \Illuminate\Support\Facades\Crypt::decryptString($employee->password_visible));
}

public function test_regenerate_password_for_captador_is_rejected(): void
{
    [$owner, $commerce] = $this->actingAdmin();
    $captador = User::factory()->create([
        'role' => 'captador',
        'commerce_id' => $commerce->id,
        'pin' => '1111',
    ]);

    $this->postJson("/api/users/{$captador->id}/regenerate-password")
        ->assertStatus(400);
}

public function test_regenerate_pin_for_admin_is_rejected(): void
{
    [$owner, $commerce] = $this->actingAdmin();
    $employee = User::factory()->create([
        'role' => 'admin',
        'commerce_id' => $commerce->id,
        'pin' => null,
    ]);

    $this->postJson("/api/users/{$employee->id}/regenerate-pin")
        ->assertStatus(400);
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=EmployeeCredentialsTest`
Expected: 3 new tests FAIL — `regeneratePassword` doesn't exist (500/404), `regeneratePin` accepts admins.

- [ ] **Step 3: Add `regeneratePassword` method**

In `apps/api/app/Http/Controllers/UserController.php`, add this method after `regeneratePin`:

```php
    /**
     * Regenerate the visible password for an admin employee.
     *
     * POST /api/users/{id}/regenerate-password
     */
    public function regeneratePassword(Request $request, int $id): JsonResponse
    {
        try {
            $adminUser = $request->user();
            $user = User::findOrFail($id);

            if ($user->commerce_id !== $adminUser->commerce_id && ! $adminUser->isSuperAdmin()) {
                return response()->json([
                    'message' => 'No tienes permiso para regenerar la contraseña de este usuario',
                ], 403);
            }

            if ($user->email === 'system@yapenotifier.internal') {
                return response()->json([
                    'message' => 'No se puede regenerar la contraseña del usuario del sistema',
                ], 403);
            }

            if ($user->role !== 'admin') {
                return response()->json([
                    'message' => 'Los captadores usan PIN, no contraseña.',
                ], 400);
            }

            $plainPassword = Str::random(12);
            $user->password = Hash::make($plainPassword);
            $user->password_visible = Crypt::encryptString($plainPassword);
            $user->save();

            Log::info('Password regenerated', [
                'user_id' => $user->id,
                'regenerated_by' => $adminUser->id,
            ]);

            return response()->json([
                'message' => 'Contraseña regenerada exitosamente',
                'password' => $plainPassword,
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to regenerate password', [
                'user_id' => $id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Error al regenerar contraseña.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
```

- [ ] **Step 4: Restrict `regeneratePin` to captador**

In the same file, in the `regeneratePin` method, find the block that checks for the system user (the `if ($user->email === 'system@yapenotifier.internal')` block). Immediately AFTER that block, add:

```php
            if ($user->role !== 'captador') {
                return response()->json([
                    'message' => 'Los administradores usan contraseña, no PIN.',
                ], 400);
            }

```

- [ ] **Step 5: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=EmployeeCredentialsTest`
Expected: all 5 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/UserController.php apps/api/tests/Feature/EmployeeCredentialsTest.php
git commit -m "feat(api): add regenerate-password, restrict regenerate-pin to captador"
```

---

### Task 8: `index` — return decrypted `password_visible`

**Files:**
- Modify: `apps/api/app/Http/Controllers/UserController.php`
- Modify: `apps/api/tests/Feature/EmployeeCredentialsTest.php`

- [ ] **Step 1: Append failing test**

Append to `EmployeeCredentialsTest.php`:

```php
public function test_index_returns_decrypted_password_for_admin_employees(): void
{
    [$owner, $commerce] = $this->actingAdmin();

    // create an admin employee via the endpoint so password_visible is set
    $create = $this->postJson('/api/users', [
        'name' => 'Admin Visible',
        'email' => 'admin.visible@test.com',
        'role' => 'admin',
    ]);
    $plain = $create->json('user.password');

    $response = $this->getJson('/api/users');
    $response->assertOk();

    $row = collect($response->json('users'))
        ->firstWhere('email', 'admin.visible@test.com');

    $this->assertNotNull($row);
    $this->assertEquals($plain, $row['password_visible']);
}

public function test_index_returns_null_password_for_captadores(): void
{
    [$owner, $commerce] = $this->actingAdmin();

    $this->postJson('/api/users', [
        'name' => 'Cap Visible',
        'email' => 'cap.visible@test.com',
        'role' => 'captador',
    ]);

    $response = $this->getJson('/api/users');
    $row = collect($response->json('users'))
        ->firstWhere('email', 'cap.visible@test.com');

    $this->assertNull($row['password_visible']);
}
```

- [ ] **Step 2: Run, expect fail**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=test_index_returns_decrypted_password_for_admin_employees`
Expected: FAIL — `index` payload has no `password_visible` key.

- [ ] **Step 3: Add decrypted `password_visible` to `index` payload**

In `apps/api/app/Http/Controllers/UserController.php`, in the `index` method, the `->map(function ($user) {...})` callback currently returns an array. Update that callback to include `password_visible`:

```php
            ->map(function ($user) {
                $passwordVisible = null;
                if ($user->password_visible) {
                    try {
                        $passwordVisible = Crypt::decryptString($user->password_visible);
                    } catch (\Exception $e) {
                        // APP_KEY changed or corrupt value — degrade gracefully
                        $passwordVisible = null;
                    }
                }

                return [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'role' => $user->role,
                    'is_active' => $user->is_active,
                    'pin' => $user->pin,
                    'password_visible' => $passwordVisible,
                    'created_at' => $user->created_at,
                    'updated_at' => $user->updated_at,
                ];
            });
```

(The `Crypt` import was already added in Task 6.)

- [ ] **Step 4: Run, expect pass**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter=EmployeeCredentialsTest`
Expected: all PASS.

- [ ] **Step 5: Run the full backend super-admin suite to confirm no regressions**

Run: `docker compose --env-file .env exec -T php-fpm php artisan test --filter='SuperAdmin|CommerceExpiry|SuspendExpired|RoleSecurity|EmployeeCredentials|NotificationTest'`
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add apps/api/app/Http/Controllers/UserController.php apps/api/tests/Feature/EmployeeCredentialsTest.php
git commit -m "feat(api): include decrypted password_visible in employee list"
```

---

## Phase 5 — Frontend

### Task 9: Types + API client

**Files:**
- Modify: `apps/web-dashboard/src/types/index.ts`
- Modify: `apps/web-dashboard/src/config/api.ts`
- Modify: `apps/web-dashboard/src/services/api.ts`

- [ ] **Step 1: Add `password_visible` to the `User` type**

In `apps/web-dashboard/src/types/index.ts`, find the `User` interface and add the `password_visible` field after `pin`:

```typescript
export interface User {
  id: number;
  name: string;
  email: string;
  commerce_id: number | null;
  role: 'super_admin' | 'admin' | 'captador' | 'system';
  pin?: string | null;
  password_visible?: string | null;
  is_active?: boolean;
  created_at: string;
  updated_at: string;
}
```

- [ ] **Step 2: Add the `regeneratePassword` endpoint**

In `apps/web-dashboard/src/config/api.ts`, in the `users` group, add the `regeneratePassword` line:

```typescript
  users: {
    list: "/api/users",
    create: "/api/users",
    show: (id: number) => `/api/users/${id}`,
    update: (id: number) => `/api/users/${id}`,
    delete: (id: number) => `/api/users/${id}`,
    regeneratePin: (id: number) => `/api/users/${id}/regenerate-pin`,
    regeneratePassword: (id: number) => `/api/users/${id}/regenerate-password`,
  },
```

- [ ] **Step 3: Update `createUser` and add `regenerateUserPassword` in api.ts**

In `apps/web-dashboard/src/services/api.ts`, replace the `createUser` method with this version that also returns the password:

```typescript
  async createUser(data: {
    name: string;
    email: string;
    role: 'admin' | 'captador';
    is_active?: boolean;
  }): Promise<{ user: User; pin: string | null; password: string | null }> {
    const response = await this.client.post<{
      message: string;
      user: User & { pin: string | null; password: string | null };
    }>(API_ENDPOINTS.users.create, data);
    return {
      user: response.data.user,
      pin: response.data.user.pin ?? null,
      password: response.data.user.password ?? null,
    };
  }
```

Then add a new method right after `regenerateUserPin`:

```typescript
  async regenerateUserPassword(id: number): Promise<{ password: string }> {
    const response = await this.client.post<{
      message: string;
      password: string;
    }>(API_ENDPOINTS.users.regeneratePassword(id));
    return { password: response.data.password };
  }
```

- [ ] **Step 4: Type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors (pre-existing StatCard.test.tsx / TabBadge.test.tsx errors are OK).

- [ ] **Step 5: Commit**

```bash
git add apps/web-dashboard/src/types/index.ts apps/web-dashboard/src/config/api.ts apps/web-dashboard/src/services/api.ts
git commit -m "feat(dashboard): add password_visible type and regenerate-password API"
```

---

### Task 10: EmployeesPage — password column + create modal credential

**Files:**
- Modify: `apps/web-dashboard/src/pages/EmployeesPage.tsx`

- [ ] **Step 1: Add state for the visible-password reveal map and generated credential**

In `apps/web-dashboard/src/pages/EmployeesPage.tsx`, after the existing state declarations (`copiedPin`, `error`), add:

```tsx
  const [revealedPasswords, setRevealedPasswords] = useState<Record<number, boolean>>({});
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);
  const [copiedPassword, setCopiedPassword] = useState(false);
```

- [ ] **Step 2: Update the `Eye`/`EyeOff` imports**

Change the lucide-react import line to include `Eye` and `EyeOff`:

```tsx
import { Plus, Edit, Trash2, Power, PowerOff, User as UserIcon, Key, Copy, Check, Eye, EyeOff, RefreshCw } from 'lucide-react';
```

- [ ] **Step 3: Update `handleCreate` and `handleEdit` to reset password state**

In `handleCreate`, after `setGeneratedPin(null);` add `setGeneratedPassword(null);`.
In `handleEdit`, after `setGeneratedPin(null);` add `setGeneratedPassword(null);`.

- [ ] **Step 4: Update `handleSubmit` to capture the generated password**

In `handleSubmit`, replace the `else` branch that calls `createUser`:

```tsx
      } else {
        const result = await apiService.createUser(formData);
        setGeneratedPin(result.pin);
        setGeneratedPassword(result.password);
      }
      // Keep the modal open if a credential was generated so the admin can copy it
      if (editingUser || (!generatedPin && !generatedPassword)) {
        setShowModal(false);
      }
```

- [ ] **Step 5: Add the regenerate-password handler**

After `handleRegeneratePin`, add:

```tsx
  const handleRegeneratePassword = async (user: User) => {
    if (!confirm(`¿Regenerar contraseña para ${user.name}? La contraseña actual dejará de funcionar.`)) {
      return;
    }
    try {
      const result = await apiService.regenerateUserPassword(user.id);
      setGeneratedPassword(result.password);
      setGeneratedPin(null);
      setShowModal(true);
      setEditingUser(user);
      loadUsers();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al regenerar contraseña');
    }
  };

  const copyPasswordToClipboard = () => {
    if (generatedPassword) {
      navigator.clipboard.writeText(generatedPassword);
      setCopiedPassword(true);
      setTimeout(() => setCopiedPassword(false), 2000);
    }
  };

  const togglePasswordReveal = (userId: number) => {
    setRevealedPasswords((prev) => ({ ...prev, [userId]: !prev[userId] }));
  };
```

- [ ] **Step 6: Add the "Contraseña" column header**

In the `<thead>`, add a new `<th>` between the PIN column and the Estado column:

```tsx
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Contraseña
              </th>
```

- [ ] **Step 7: Add the "Contraseña" column cell**

In the `<tbody>` row, add a new `<td>` right after the PIN `<td>` (the one that ends with `Sin PIN`). Also update the empty-state `colSpan` from `7` to `8`:

```tsx
                  <td className="px-6 py-4 whitespace-nowrap">
                    {user.role === 'admin' ? (
                      user.password_visible ? (
                        <div className="flex items-center gap-2">
                          <code className="text-sm font-mono bg-gray-100 px-2 py-1 rounded">
                            {revealedPasswords[user.id] ? user.password_visible : '••••••••'}
                          </code>
                          <button
                            onClick={() => togglePasswordReveal(user.id)}
                            className="text-gray-500 hover:text-gray-800"
                            title={revealedPasswords[user.id] ? 'Ocultar' : 'Ver contraseña'}
                          >
                            {revealedPasswords[user.id] ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                          <button
                            onClick={() => {
                              navigator.clipboard.writeText(user.password_visible!);
                            }}
                            className="text-gray-500 hover:text-gray-800"
                            title="Copiar contraseña"
                          >
                            <Copy className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => handleRegeneratePassword(user)}
                            className="text-primary-600 hover:text-primary-800"
                            title="Regenerar contraseña"
                          >
                            <RefreshCw className="h-4 w-4" />
                          </button>
                        </div>
                      ) : (
                        <button
                          onClick={() => handleRegeneratePassword(user)}
                          className="text-sm text-primary-600 hover:text-primary-800 underline"
                        >
                          Generar contraseña
                        </button>
                      )
                    ) : (
                      <span className="text-sm text-gray-400">—</span>
                    )}
                  </td>
```

The empty-state row:
```tsx
                <td colSpan={8} className="px-6 py-4 text-center text-gray-500">
```

- [ ] **Step 8: Show generated password in the modal**

In the modal, right after the `{generatedPin && (...)}` block, add a parallel block for the password:

```tsx
              {generatedPassword && (
                <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-sm font-medium text-green-800 mb-2">
                    Contraseña generada:
                  </p>
                  <div className="flex items-center gap-2">
                    <code className="text-xl font-mono font-bold text-green-900 bg-white px-4 py-2 rounded border-2 border-green-300">
                      {generatedPassword}
                    </code>
                    <button
                      onClick={copyPasswordToClipboard}
                      className="p-2 text-green-700 hover:bg-green-100 rounded"
                      title="Copiar contraseña"
                    >
                      {copiedPassword ? <Check className="h-5 w-5" /> : <Copy className="h-5 w-5" />}
                    </button>
                  </div>
                  <p className="text-xs text-green-700 mt-2">
                    Guarda esta contraseña. El administrador la necesitará para iniciar sesión en el dashboard web.
                  </p>
                </div>
              )}
```

- [ ] **Step 9: Update the modal close-button condition**

The modal's Cancel/Close button label and the submit button visibility currently depend only on `generatedPin`. Update both to also consider `generatedPassword`:

The close button label:
```tsx
                    {(generatedPin || generatedPassword) ? 'Cerrar' : 'Cancelar'}
```

The close button onClick handler — also clear the password:
```tsx
                    onClick={() => {
                      setShowModal(false);
                      setGeneratedPin(null);
                      setGeneratedPassword(null);
                    }}
```

The submit button visibility:
```tsx
                  {!generatedPin && !generatedPassword && (
                    <button type="submit" className="btn btn-primary">
                      {editingUser ? 'Actualizar' : 'Crear'}
                    </button>
                  )}
```

- [ ] **Step 10: Type-check + lint**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 11: Commit**

```bash
git add apps/web-dashboard/src/pages/EmployeesPage.tsx
git commit -m "feat(dashboard): password column with view/copy/regenerate in EmployeesPage"
```

---

## Phase 6 — Verification

### Task 11: Manual smoke test

**Files:** none (manual)

- [ ] **Step 1: Run full backend suite**

Run: `cd infra/docker/environments/development && docker compose --env-file .env exec -T php-fpm php artisan test --filter='SuperAdmin|CommerceExpiry|SuspendExpired|RoleSecurity|EmployeeCredentials|NotificationTest'`
Expected: all green.

- [ ] **Step 2: Frontend type-check**

Run: `cd apps/web-dashboard && npm run type-check`
Expected: no new errors.

- [ ] **Step 3: Manual verification in the browser**

Log in as a commerce admin and go to Empleados. Confirm:
1. The table has a new "Contraseña" column.
2. Captador rows show "—" in the Contraseña column and a PIN in the PIN column.
3. Creating a new employee with role **Captador** → modal shows the generated PIN.
4. Creating a new employee with role **Administrador** → modal shows the generated password (with copy button), PIN column for that row shows "—".
5. For an admin employee, the password column shows `••••••••` with eye (reveal), copy, and regenerate buttons. Reveal shows the plaintext; regenerate produces a new one shown in the modal.
6. Existing admin employees (created before this change) show a "Generar contraseña" link — clicking it produces a usable password.

- [ ] **Step 4: Verify the new admin can actually log in**

Take the password generated for an admin employee, log out, and log in at `/login` with that employee's email + the generated password. Expected: successful login into the web dashboard.

- [ ] **Step 5: Commit any incidental fixes**

If anything needed tweaking during smoke test:
```bash
git add -A
git commit -m "fix: minor adjustments from credentials/roles smoke test"
```

---

## Verification checklist

- [ ] Captador gets 403 on every `users` endpoint
- [ ] `role: 'system'` rejected with 422
- [ ] Admin cannot log in via `/api/auth/login-pin` (403)
- [ ] Captador can still log in via PIN
- [ ] Creating an admin employee returns a plaintext password once
- [ ] Creating a captador returns a PIN, `password_visible` stays null
- [ ] `GET /api/users` returns decrypted `password_visible` for admin employees
- [ ] EmployeesPage shows the password column with view/copy/regenerate
- [ ] A freshly created admin employee can log into the web dashboard
- [ ] All pre-existing backend tests still green
