<?php

namespace App\Services;

use App\Models\Commerce;
use App\Models\Plan;
use App\Models\User;

class CommerceService
{
    /**
     * Create a new commerce in 'pending' state with the Starter plan assigned by default.
     */
    public function createCommerce(User $owner, array $data): Commerce
    {
        $starterPlan = Plan::where('slug', 'starter')->first();

        $commerce = Commerce::create([
            'name'          => $data['name'],
            'owner_user_id' => $owner->id,
            'status'        => 'pending',
            'plan_id'       => $starterPlan?->id,
        ]);

        // Assign commerce to owner. Preserve super_admin role — those users
        // own the platform, not the commerce, and should never be downgraded.
        $updates = ['commerce_id' => $commerce->id];
        if (!$owner->isSuperAdmin()) {
            $updates['role'] = 'admin';
        }
        $owner->update($updates);

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

    /**
     * Approve a commerce: set it active, assign a plan, and set plan_expires_at to now+30 days.
     */
    public function approve(Commerce $commerce, Plan $plan, User $approvedBy): Commerce
    {
        $commerce->update([
            'status'          => 'active',
            'plan_id'         => $plan->id,
            'plan_expires_at' => now()->addDays(30),
            'approved_at'     => now(),
            'approved_by'     => $approvedBy->id,
        ]);
        return $commerce->fresh();
    }
}



