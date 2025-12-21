<?php

namespace App\Events;

use App\Models\Notification;
use Illuminate\Broadcasting\InteractsWithSockets;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcast;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class NotificationCreated implements ShouldBroadcast
{
    use Dispatchable, InteractsWithSockets, SerializesModels;

    /**
     * The notification instance.
     *
     * @var \App\Models\Notification
     */
    public $notification;

    /**
     * Create a new event instance.
     */
    public function __construct(Notification $notification)
    {
        $this->notification = $notification;
    }

    /**
     * Get the channels the event should broadcast on.
     *
     * @return array<int, \Illuminate\Broadcasting\Channel>
     */
    public function broadcastOn(): array
    {
        return [
            new PrivateChannel('commerce.'.$this->notification->commerce_id),
        ];
    }

    /**
     * The event's broadcast name.
     *
     * @return string
     */
    public function broadcastAs(): string
    {
        return 'notification.created';
    }

    /**
     * Get the data to broadcast.
     *
     * @return array<string, mixed>
     */
    public function broadcastWith(): array
    {
        return [
            'id' => $this->notification->id,
            'user_id' => $this->notification->user_id,
            'commerce_id' => $this->notification->commerce_id,
            'device_id' => $this->notification->device_id,
            'source_app' => $this->notification->source_app,
            'package_name' => $this->notification->package_name,
            'app_instance_id' => $this->notification->app_instance_id,
            'title' => $this->notification->title,
            'body' => $this->notification->body,
            'amount' => $this->notification->amount,
            'currency' => $this->notification->currency,
            'payer_name' => $this->notification->payer_name,
            'posted_at' => $this->notification->posted_at?->toIso8601String(),
            'received_at' => $this->notification->received_at->toIso8601String(),
            'status' => $this->notification->status,
            'is_duplicate' => $this->notification->is_duplicate,
            'created_at' => $this->notification->created_at->toIso8601String(),
        ];
    }
}

