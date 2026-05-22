import { useState } from 'react';
import PlansTable from '@/components/SuperAdmin/PlansTable';
import EditPlanModal from '@/components/SuperAdmin/EditPlanModal';
import type { Plan } from '@/types';

export default function SuperAdminPlansTab() {
  const [editing, setEditing] = useState<Plan | null>(null);

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Planes de suscripcion</h2>
        <p className="text-sm text-gray-600">Edita precio y limites de los planes existentes.</p>
      </div>
      <PlansTable onEdit={setEditing} />
      {editing && <EditPlanModal plan={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}
