<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Reverb Configuration
    |--------------------------------------------------------------------------
    |
    | Here you may configure your Reverb server settings. Reverb is Laravel's
    | native WebSocket server that provides real-time bidirectional
    | communication between your Laravel application and your clients.
    |
    */

    'id' => env('REVERB_APP_ID', 'yape-notifier'),

    'key' => env('REVERB_APP_KEY'),

    'secret' => env('REVERB_APP_SECRET'),

    'app_id' => env('REVERB_APP_ID'),

    'options' => [
        'host' => env('REVERB_HOST', '127.0.0.1'),
        'port' => env('REVERB_PORT', 8080),
        'scheme' => env('REVERB_SCHEME', 'http'),
        'useTLS' => env('REVERB_SCHEME', 'http') === 'https',
    ],

];



