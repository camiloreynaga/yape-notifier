<?php

namespace App\Http\Requests\Device;

use Illuminate\Foundation\Http\FormRequest;

class UpdateDeviceRequest extends FormRequest
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
            'name' => ['sometimes', 'string', 'max:255'],
            'platform' => ['sometimes', 'nullable', 'string', 'in:android'],
            'is_active' => ['sometimes', 'boolean'],
        ];
    }

    /**
     * Prepare the data for validation.
     */
    protected function prepareForValidation(): void
    {
        // Set default platform to 'android' if provided but empty
        if ($this->has('platform') && $this->platform) {
            $this->merge([
                'platform' => strtolower($this->platform),
            ]);
        } elseif ($this->has('platform') && empty($this->platform)) {
            $this->merge([
                'platform' => 'android',
            ]);
        }
    }
}
