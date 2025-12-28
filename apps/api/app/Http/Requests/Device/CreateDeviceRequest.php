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
     * UUID validation: Allow UUID if it belongs to the current user's device (find-or-create pattern)
     *
     * @return array<string, \Illuminate\Contracts\Validation\ValidationRule|array<mixed>|string>
     */
    public function rules(): array
    {
        return [
            'uuid' => [
                'nullable',
                'string',
                'uuid',
                function ($attribute, $value, $fail) {
                    if ($value) {
                        // Check if UUID exists for a different user
                        $existingDevice = \App\Models\Device::where('uuid', $value)
                            ->where('user_id', '!=', $this->user()->id)
                            ->first();
                        
                        if ($existingDevice) {
                            $fail('El UUID ya está en uso por otro usuario.');
                        }
                    }
                },
            ],
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
