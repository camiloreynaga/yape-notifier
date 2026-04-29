import Badge from './Badge';

type Status = 'pending' | 'validated' | 'inconsistent';

const CONFIG: Record<Status, { tone: 'yellow' | 'green' | 'red'; label: string }> = {
  pending:      { tone: 'yellow', label: 'Pendiente' },
  validated:    { tone: 'green',  label: 'Validada' },
  inconsistent: { tone: 'red',    label: 'Inconsistente' },
};

export default function StatusBadge({ status }: { status: Status }) {
  const cfg = CONFIG[status];
  return <Badge tone={cfg.tone} dot>{cfg.label}</Badge>;
}
