/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#F0F4F1',
          100: '#DCE6DE',
          200: '#B8CCBC',
          300: '#8FAD96',
          400: '#648E6F',
          500: '#406E4F',
          600: '#2A5238',
          700: '#1F3D2A',
          800: '#1A2E2A',
          900: '#14211F',
        },
        accent: {
          50:  '#F4FBE3',
          100: '#E8F5C4',
          200: '#DAEDA0',
          300: '#C5E865',
          400: '#B0D850',
          500: '#94BC34',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          dark: '#1A2E2A',
          warm: '#F7F7F2',
        },
        // Fintech CTA emerald — used for "Validar" and other money-affecting actions
        cta: {
          50:  '#ECFDF5',
          100: '#D1FAE5',
          400: '#34D399',
          500: '#10B981',
          600: '#059669',
          700: '#047857',
          800: '#065F46',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      fontFeatureSettings: {
        tabular: '"tnum"',
      },
    },
  },
  plugins: [],
}
