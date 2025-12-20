<?php

namespace App\Http\Requests\Device;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class UpdateDeviceMonitoredAppsRequest extends FormRequest
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
        $user = $this->user();
        $commerceId = $user->commerce_id;

        return [
            'package_names' => ['required', 'array', 'min:1'],
            'package_names.*' => [
                'required',
                'string',
                'regex:/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/',
                Rule::exists('monitor_packages', 'package_name')
                    ->where('commerce_id', $commerceId)
                    ->where('is_active', true),
            ],
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
            'package_names.required' => 'La lista de paquetes es requerida',
            'package_names.array' => 'La lista de paquetes debe ser un array',
            'package_names.min' => 'Debe incluir al menos un paquete',
            'package_names.*.required' => 'Cada paquete es requerido',
            'package_names.*.string' => 'Cada paquete debe ser una cadena de texto',
            'package_names.*.regex' => 'El formato del paquete no es válido',
            'package_names.*.exists' => 'Uno o más paquetes no existen o no están activos en tu negocio',
        ];
    }
}

