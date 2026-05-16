<?php
namespace App\Observers;

use App\Models\Commerce;
use Illuminate\Support\Str;

class CommerceObserver
{
    public function creating(Commerce $commerce): void
    {
        if (! empty($commerce->referral_code)) {
            return;
        }
        $commerce->referral_code = $this->generateUniqueCode($commerce->name ?? '');
    }

    private function generateUniqueCode(string $name): string
    {
        $base = Str::slug(Str::limit($name, 8, ''), '-');
        if ($base === '') {
            $base = 'com';
        }
        for ($i = 0; $i < 5; $i++) {
            $code = $base . '-' . Str::lower(Str::random(4));
            if (! Commerce::where('referral_code', $code)->exists()) {
                return $code;
            }
        }
        return $base . '-' . time() . Str::lower(Str::random(2));
    }
}
