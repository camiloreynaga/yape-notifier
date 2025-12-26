<?php

use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        channels: __DIR__.'/../routes/channels.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware) {
        // CORS middleware must be in global middleware to handle preflight requests
        $middleware->prepend([
            \Illuminate\Http\Middleware\HandleCors::class,
        ]);
        
        // NOTA: EnsureFrontendRequestsAreStateful NO se aplica a rutas API
        // Las rutas API usan tokens Bearer (stateless), no cookies (stateful)
        // Si en el futuro necesitas cookies para alguna ruta específica, aplica el middleware solo a esa ruta
        // $middleware->api(prepend: [
        //     \Laravel\Sanctum\Http\Middleware\EnsureFrontendRequestsAreStateful::class,
        // ]);

        $middleware->alias([
            'verified' => \App\Http\Middleware\EnsureEmailIsVerified::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions) {
        //
    })->create();
