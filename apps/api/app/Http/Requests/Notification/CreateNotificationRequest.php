<?php

namespace App\Http\Requests\Notification;

use Illuminate\Foundation\Http\FormRequest;

class CreateNotificationRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        return true;
    }

    /**
     * Get the validation rules that apply to the request.
     *
     * @return array<string, \Illuminate\Contracts\Validation\ValidationRule|array<mixed>|string>
     */
    public function rules(): array
    {
        return [
            'device_id' => ['required', 'string', 'uuid'],
            'source_app' => ['required', 'string', 'in:yape,plin,bcp,interbank,bbva,scotiabank'],
            'package_name' => ['nullable', 'string', 'max:255'],
            'android_user_id' => ['nullable', 'integer'],
            'android_uid' => ['nullable', 'integer'],
            'title' => ['nullable', 'string', 'max:255'],
            'body' => ['required', 'string'],
            'amount' => ['nullable', 'numeric', 'min:0'],
            'currency' => ['nullable', 'string', 'size:3'],
            'payer_name' => ['nullable', 'string', 'max:255'],
            'posted_at' => ['nullable', 'date'],
            'received_at' => ['nullable', 'date'],
            'raw_json' => ['nullable', 'array'],
            'status' => ['nullable', 'string', 'in:pending,validated,inconsistent'],
        ];
    }
}
