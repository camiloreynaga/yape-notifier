<?php

namespace App\Services;

use App\Models\Commerce;
use App\Models\User;

class CommerceService
{
    /**
     * Create a new commerce.
     */
    public function createCommerce(User $owner, array $data): Commerce
    {
        $commerce = Commerce::create([
            'name' => $data['name'],
            'owner_user_id' => $owner->id,
        ]);

        // Assign commerce to owner
        $owner->update([
            'commerce_id' => $commerce->id,
            'role' => 'admin',
        ]);

        return $commerce;
    }

    /**
     * Get commerce for a user.
     */
    public function getUserCommerce(User $user): ?Commerce
    {
        if (!$user->commerce_id) {
            return null;
        }

        return Commerce::with(['owner', 'devices', 'users'])
            ->find($user->commerce_id);
    }

    /**
     * Add user to commerce.
     */
    public function addUserToCommerce(Commerce $commerce, User $user, string $role = 'captador'): void
    {
        $user->update([
            'commerce_id' => $commerce->id,
            'role' => $role,
        ]);
    }

    /**
     * Remove user from commerce.
     */
    public function removeUserFromCommerce(User $user): void
    {
        $user->update([
            'commerce_id' => null,
            'role' => 'admin',
        ]);
    }
}


