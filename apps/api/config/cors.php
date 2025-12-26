<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Cross-Origin Resource Sharing (CORS) Configuration
    |--------------------------------------------------------------------------
    */

    'paths' => ['api/*', 'sanctum/csrf-cookie'],

    'allowed_methods' => ['*'],

    'allowed_origins' => [
        env('APP_URL', 'http://localhost:8000'),
        'https://notificaciones.space',
        'https://www.notificaciones.space',
        'https://dashboard.notificaciones.space',
        'https://api.notificaciones.space',
        'http://localhost:3000', // Para desarrollo local
    ],

    'allowed_origins_patterns' => [
        '/^https:\/\/.*\.notificaciones\.space$/',
        '/^http:\/\/localhost:\d+$/', // Para desarrollo local con cualquier puerto
    ],

    'allowed_headers' => ['*'],

    'exposed_headers' => [],

    'max_age' => 3600, // Cache preflight requests for 1 hour

    'supports_credentials' => true,

];
