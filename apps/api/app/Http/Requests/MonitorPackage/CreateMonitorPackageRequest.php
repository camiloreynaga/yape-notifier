<?php

namespace App\Http\Requests\MonitorPackage;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class CreateMonitorPackageRequest extends FormRequest
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
            'package_name' => [
                'required',
                'string',
                'max:255',
                'regex:/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/', // Android package name format
                Rule::unique('monitor_packages', 'package_name'),
            ],
            'app_name' => ['nullable', 'string', 'max:255'],
            'description' => ['nullable', 'string', 'max:1000'],
            'is_active' => ['nullable', 'boolean'],
            'priority' => ['nullable', 'integer', 'min:0', 'max:100'],
        ];
    }

    /**
     * Get custom messages for validator errors.
     *
     * @return array<string, string>
     */
    public function messages(): array
    {
        return [
            'package_name.required' => 'El nombre del paquete es requerido.',
            'package_name.unique' => 'Este paquete ya está registrado.',
            'package_name.regex' => 'El formato del nombre del paquete no es válido. Debe ser un package name de Android válido (ej: com.example.app).',
        ];
    }

    /**
     * Prepare the data for validation.
     */
    protected function prepareForValidation(): void
    {
        // Set defaults
        if (! $this->has('is_active')) {
            $this->merge(['is_active' => true]);
        }

        if (! $this->has('priority')) {
            $this->merge(['priority' => 0]);
        }
    }
}

