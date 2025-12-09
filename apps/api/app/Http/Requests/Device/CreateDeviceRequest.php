<?php

namespace App\Http\Requests\Device;

use Illuminate\Foundation\Http\FormRequest;

class CreateDeviceRequest extends FormRequest
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
            'uuid' => ['nullable', 'string', 'uuid', 'unique:devices,uuid'],
            'name' => ['required', 'string', 'max:255'],
            'platform' => ['nullable', 'string', 'in:android'],
            'is_active' => ['nullable', 'boolean'],
        ];
    }

    /**
     * Prepare the data for validation.
     */
    protected function prepareForValidation(): void
    {
        // Set default platform to 'android' if not provided
        if (! $this->has('platform')) {
            $this->merge([
                'platform' => 'android',
            ]);
        } else {
            $this->merge([
                'platform' => strtolower($this->platform),
            ]);
        }
    }
}
