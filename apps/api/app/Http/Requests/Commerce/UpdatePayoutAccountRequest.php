<?php
namespace App\Http\Requests\Commerce;

use Illuminate\Foundation\Http\FormRequest;

class UpdatePayoutAccountRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'payout_bank' => ['required', 'string', 'max:80'],
            'payout_account_type' => ['required', 'in:corriente,ahorros,cci'],
            'payout_account_number' => ['required', 'string', 'max:40'],
            'payout_account_holder' => ['required', 'string', 'max:150'],
            'payout_account_holder_doc' => ['required', 'string', 'max:20'],
        ];
    }
}
