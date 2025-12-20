<?php

namespace App\Http\Requests\Device;

use Illuminate\Foundation\Http\FormRequest;

class UpdateDeviceHealthRequest extends FormRequest
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
            'battery_level' => ['nullable', 'integer', 'min:0', 'max:100'],
            'battery_optimization_disabled' => ['nullable', 'boolean'],
            'notification_permission_enabled' => ['nullable', 'boolean'],
        ];
    }
}

