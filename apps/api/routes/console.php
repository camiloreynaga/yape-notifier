<?php

use App\Jobs\SuspendExpiredCommercesJob;
use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Schedule;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote')->hourly();

Schedule::job(new SuspendExpiredCommercesJob())->dailyAt('02:00')->name('suspend-expired-commerces');
