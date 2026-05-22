import { ReactNode } from 'react';

type Tone = 'gray' | 'green' | 'yellow' | 'red' | 'blue' | 'purple' | 'lime';

interface Props {
  tone?: Tone;
  children: ReactNode;
  dot?: boolean;
}

const TONES: Record<Tone, { bg: string; text: string; ring: string; dot: string }> = {
  gray:   { bg: 'bg-gray-100',    text: 'text-gray-700',    ring: 'ring-gray-300',    dot: 'bg-gray-500'   },
  green:  { bg: 'bg-green-50',    text: 'text-green-700',   ring: 'ring-green-300',   dot: 'bg-green-500'  },
  yellow: { bg: 'bg-yellow-50',   text: 'text-yellow-800',  ring: 'ring-yellow-300',  dot: 'bg-yellow-500' },
  red:    { bg: 'bg-red-50',      text: 'text-red-700',     ring: 'ring-red-300',     dot: 'bg-red-500'    },
  blue:   { bg: 'bg-blue-50',     text: 'text-blue-700',    ring: 'ring-blue-300',    dot: 'bg-blue-500'   },
  purple: { bg: 'bg-purple-50',   text: 'text-purple-700',  ring: 'ring-purple-300',  dot: 'bg-purple-500' },
  lime:   { bg: 'bg-accent-100',  text: 'text-primary-800', ring: 'ring-accent-300',  dot: 'bg-accent-300' },
};

export default function Badge({ tone = 'gray', children, dot = false }: Props) {
  const t = TONES[tone];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${t.bg} ${t.text} ${t.ring}`}>
      {dot && <span className={`h-1.5 w-1.5 rounded-full ${t.dot}`} />}
      {children}
    </span>
  );
}
