<?php

use Illuminate\Support\Facades\Broadcast;

/*
|--------------------------------------------------------------------------
| Broadcast Channels
|--------------------------------------------------------------------------
|
| Here you may register all of the event broadcasting channels that your
| application supports. The given channel authorization callbacks are
| used to check if an authenticated user can listen to the channel.
|
*/

// Solo registrar canales si broadcasting está configurado correctamente
// Esto previene errores durante migraciones cuando las variables de Reverb/Pusher no están configuradas
try {
    $broadcastConnection = config('broadcasting.default', 'null');
    $isBroadcastingConfigured = false;

    if ($broadcastConnection === 'reverb') {
        // Verificar que las variables de Reverb estén configuradas
        $reverbConfig = config('broadcasting.connections.reverb', []);
        $isBroadcastingConfigured = !empty($reverbConfig['key'])
            && !empty($reverbConfig['secret'])
            && !empty($reverbConfig['app_id']);
    } elseif ($broadcastConnection === 'pusher') {
        // Verificar que las variables de Pusher estén configuradas
        $pusherConfig = config('broadcasting.connections.pusher', []);
        $isBroadcastingConfigured = !empty($pusherConfig['key'])
            && !empty($pusherConfig['secret'])
            && !empty($pusherConfig['app_id']);
    }
    // Si es 'null' o 'log', no registrar canales (no se necesita broadcasting)

    if ($isBroadcastingConfigured) {
        Broadcast::channel('commerce.{commerceId}', function ($user, $commerceId) {
            // Only allow users from the same commerce to listen to the channel
            return $user->commerce_id === (int) $commerceId;
        });
    }
} catch (\Exception $e) {
    // Si hay cualquier error al cargar la configuración de broadcasting,
    // simplemente no registrar canales (modo seguro)
    // Esto previene errores fatales durante migraciones o cuando broadcasting no está configurado
}


