import { forwardRef, ButtonHTMLAttributes } from 'react';
import { Loader2 } from 'lucide-react';

type Variant = 'primary' | 'dark' | 'outline' | 'ghost' | 'danger' | 'success';
type Size = 'sm' | 'md' | 'lg';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  icon?: React.ReactNode;
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-accent-300 text-primary-900 hover:bg-accent-400 disabled:bg-accent-100 disabled:text-primary-400',
  dark:    'bg-primary-800 text-white hover:bg-primary-700 disabled:bg-primary-300',
  outline: 'border border-gray-300 bg-white text-gray-800 hover:bg-gray-50 disabled:opacity-50',
  ghost:   'bg-transparent text-gray-700 hover:bg-gray-100 disabled:opacity-50',
  danger:  'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300',
  success: 'bg-green-600 text-white hover:bg-green-700 disabled:bg-green-300',
};

const SIZES: Record<Size, string> = {
  sm: 'px-2.5 py-1.5 text-xs gap-1.5',
  md: 'px-3.5 py-2 text-sm gap-2',
  lg: 'px-5 py-2.5 text-base gap-2',
};

const Button = forwardRef<HTMLButtonElement, Props>(
  ({ variant = 'primary', size = 'md', loading = false, icon, children, className = '', disabled, ...rest }, ref) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={`inline-flex items-center justify-center rounded-lg font-medium transition-colors disabled:cursor-not-allowed ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
        {...rest}
      >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : icon}
        {children}
      </button>
    );
  }
);
Button.displayName = 'Button';
export default Button;
