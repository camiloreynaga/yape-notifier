<?php
namespace App\Http\Requests\Commerce;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class CustomizeReferralCodeRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'referral_code' => [
                'required', 'string', 'min:4', 'max:20',
                'regex:/^[a-z0-9-]+$/',
                'regex:/[0-9-]/', // must contain at least one digit or dash
                Rule::unique('commerces', 'referral_code')->ignore($this->user()->commerce_id),
            ],
        ];
    }

    public function messages(): array
    {
        return [
            'referral_code.regex' => 'El código solo puede contener minúsculas, números y guiones, y debe incluir al menos un guión o un dígito.',
            'referral_code.unique' => 'Este código ya está en uso.',
            'referral_code.min' => 'El código debe tener al menos 4 caracteres.',
            'referral_code.max' => 'El código no puede exceder 20 caracteres.',
        ];
    }
}
