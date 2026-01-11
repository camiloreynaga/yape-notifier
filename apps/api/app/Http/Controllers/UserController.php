<?php

namespace App\Http\Controllers;

use App\Http\Requests\User\CreateUserRequest;
use App\Http\Requests\User\UpdateUserRequest;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;

class UserController extends Controller
{
    /**
     * List users for the authenticated user's commerce.
     * 
     * GET /api/users
     */
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();

        if (!$user->commerce_id) {
            return response()->json([
                'message' => 'Usuario no asociado a un comercio',
            ], 403);
        }

        // Listar usuarios del mismo comercio (excluyendo usuarios del sistema)
        $users = User::where('commerce_id', $user->commerce_id)
            ->where('email', '!=', 'system@yapenotifier.internal')
            ->orderBy('created_at', 'desc')
            ->get()
            ->map(function ($user) {
                return [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'role' => $user->role,
                    'is_active' => $user->is_active,
                    'pin' => $user->pin, // Mostrar PIN para que el admin lo vea
                    'created_at' => $user->created_at,
                    'updated_at' => $user->updated_at,
                ];
            });

        return response()->json([
            'users' => $users,
        ]);
    }

    /**
     * Create a new employee user with auto-generated PIN.
     * 
     * POST /api/users
     */
    public function store(CreateUserRequest $request): JsonResponse
    {
        try {
            $adminUser = $request->user();

            if (!$adminUser->commerce_id) {
                return response()->json([
                    'message' => 'Usuario no asociado a un comercio',
                ], 403);
            }

            // Generar PIN único automáticamente
            $pin = User::generateUniquePin(4);

            // Crear usuario con PIN
            $user = User::create([
                'name' => $request->name,
                'email' => $request->email,
                'password' => Hash::make(bin2hex(random_bytes(16))), // Password aleatorio seguro
                'pin' => $pin,
                'role' => $request->role ?? 'captador',
                'commerce_id' => $adminUser->commerce_id,
                'is_active' => $request->is_active ?? true,
            ]);

            Log::info('Employee created with PIN', [
                'user_id' => $user->id,
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
                    'pin' => $pin, // Devolver PIN para mostrarlo al admin
                ],
            ], 201);
        } catch (\Exception $e) {
            Log::error('Failed to create employee', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);

            return response()->json([
                'message' => 'Error al crear empleado. Por favor, intenta nuevamente.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Update an employee user.
     * 
     * PUT /api/users/{id}
     */
    public function update(UpdateUserRequest $request, int $id): JsonResponse
    {
        try {
            $adminUser = $request->user();
            $user = User::findOrFail($id);

            // Verificar que el usuario pertenece al mismo comercio
            if ($user->commerce_id !== $adminUser->commerce_id) {
                return response()->json([
                    'message' => 'No tienes permiso para actualizar este usuario',
                ], 403);
            }

            // No permitir actualizar usuarios del sistema
            if ($user->email === 'system@yapenotifier.internal') {
                return response()->json([
                    'message' => 'No se puede actualizar el usuario del sistema',
                ], 403);
            }

            // Actualizar campos permitidos
            $user->fill($request->only(['name', 'email', 'role', 'is_active']));
            $user->save();

            Log::info('Employee updated', [
                'user_id' => $user->id,
                'updated_by' => $adminUser->id,
            ]);

            return response()->json([
                'message' => 'Empleado actualizado exitosamente',
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'role' => $user->role,
                    'is_active' => $user->is_active,
                    'pin' => $user->pin,
                ],
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to update employee', [
                'user_id' => $id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Error al actualizar empleado.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Regenerate PIN for an employee.
     * 
     * POST /api/users/{id}/regenerate-pin
     */
    public function regeneratePin(Request $request, int $id): JsonResponse
    {
        try {
            $adminUser = $request->user();
            $user = User::findOrFail($id);

            // Verificar que el usuario pertenece al mismo comercio
            if ($user->commerce_id !== $adminUser->commerce_id) {
                return response()->json([
                    'message' => 'No tienes permiso para regenerar el PIN de este usuario',
                ], 403);
            }

            // No permitir regenerar PIN de usuarios del sistema
            if ($user->email === 'system@yapenotifier.internal') {
                return response()->json([
                    'message' => 'No se puede regenerar el PIN del usuario del sistema',
                ], 403);
            }

            // Generar nuevo PIN único
            $newPin = User::generateUniquePin(4);
            $user->pin = $newPin;
            $user->save();

            Log::info('PIN regenerated', [
                'user_id' => $user->id,
                'regenerated_by' => $adminUser->id,
            ]);

            return response()->json([
                'message' => 'PIN regenerado exitosamente',
                'pin' => $newPin,
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to regenerate PIN', [
                'user_id' => $id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Error al regenerar PIN.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }

    /**
     * Delete an employee user.
     * 
     * DELETE /api/users/{id}
     */
    public function destroy(Request $request, int $id): JsonResponse
    {
        try {
            $adminUser = $request->user();
            $user = User::findOrFail($id);

            // Verificar que el usuario pertenece al mismo comercio
            if ($user->commerce_id !== $adminUser->commerce_id) {
                return response()->json([
                    'message' => 'No tienes permiso para eliminar este usuario',
                ], 403);
            }

            // No permitir eliminar usuarios del sistema
            if ($user->email === 'system@yapenotifier.internal') {
                return response()->json([
                    'message' => 'No se puede eliminar el usuario del sistema',
                ], 403);
            }

            // No permitir auto-eliminarse
            if ($user->id === $adminUser->id) {
                return response()->json([
                    'message' => 'No puedes eliminar tu propia cuenta',
                ], 403);
            }

            $user->delete();

            Log::info('Employee deleted', [
                'user_id' => $id,
                'deleted_by' => $adminUser->id,
            ]);

            return response()->json([
                'message' => 'Empleado eliminado exitosamente',
            ]);
        } catch (\Exception $e) {
            Log::error('Failed to delete employee', [
                'user_id' => $id,
                'error' => $e->getMessage(),
            ]);

            return response()->json([
                'message' => 'Error al eliminar empleado.',
                'error' => config('app.debug') ? $e->getMessage() : null,
            ], 500);
        }
    }
}

