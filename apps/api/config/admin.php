<?php

return [
    /*
    |--------------------------------------------------------------------------
    | Super admin (seed / ops)
    |--------------------------------------------------------------------------
    |
    | Leer con config('admin.*') — no usar env() en seeders ni en runtime
    | cuando exista config:cache en producción.
    |
    */
    'super_admin_email' => env('SUPER_ADMIN_EMAIL'),
    'super_admin_password' => env('SUPER_ADMIN_PASSWORD'),
    'super_admin_name' => env('SUPER_ADMIN_NAME', 'Super Admin'),
];
