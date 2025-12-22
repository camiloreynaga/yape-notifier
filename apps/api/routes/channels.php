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

Broadcast::channel('commerce.{commerceId}', function ($user, $commerceId) {
    // Only allow users from the same commerce to listen to the channel
    return $user->commerce_id === (int) $commerceId;
});


