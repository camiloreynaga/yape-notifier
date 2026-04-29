import Badge from '@/components/UI/Badge';
import type { Notification } from '@/types';

export function isPossibleDuplicate(target: Notification, all: Notification[]): boolean {
  return all.some(
    (n) =>
      n.id !== target.id &&
      n.amount === target.amount &&
      n.security_code !== null &&
      n.security_code === target.security_code &&
      Math.abs(new Date(n.created_at).getTime() - new Date(target.created_at).getTime()) < 60_000
  );
}

export default function PossibleDuplicateBadge() {
  return <Badge tone="yellow">POSIBLE DUPLICADO</Badge>;
}
