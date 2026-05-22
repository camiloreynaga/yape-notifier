import { Pencil } from 'lucide-react';
import { useSuperAdminPlans } from '@/hooks/useSuperAdminPlans';
import type { Plan } from '@/types';

interface Props {
  onEdit: (plan: Plan) => void;
}

export default function PlansTable({ onEdit }: Props) {
  const { data, isLoading } = useSuperAdminPlans();

  if (isLoading) return <div className="rounded-xl bg-white p-8 text-center text-gray-500">Cargando planes...</div>;
  if (!data || data.length === 0) return <div className="rounded-xl bg-white p-8 text-center text-gray-500">No hay planes</div>;

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Plan</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Precio</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Dispositivos</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Notif/dia</th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600">Estado</th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-gray-600">Accion</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {data.map((plan) => (
            <tr key={plan.id}>
              <td className="px-4 py-3 font-medium text-gray-900">{plan.name}</td>
              <td className="px-4 py-3 text-sm text-gray-700">S/{plan.price}</td>
              <td className="px-4 py-3 text-sm text-gray-700">{plan.max_devices ?? 'Ilimitado'}</td>
              <td className="px-4 py-3 text-sm text-gray-700">{plan.max_notifications_per_day ?? 'Ilimitado'}</td>
              <td className="px-4 py-3">
                <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${plan.is_active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'}`}>
                  {plan.is_active ? 'Activo' : 'Inactivo'}
                </span>
              </td>
              <td className="px-4 py-3 text-right">
                <button onClick={() => onEdit(plan)} className="text-primary-600 hover:text-primary-700">
                  <Pencil className="h-4 w-4 inline" /> <span className="text-sm">Editar</span>
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
