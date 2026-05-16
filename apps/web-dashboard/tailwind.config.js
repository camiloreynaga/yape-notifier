/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {  // Yape purple
          50:  '#F4EFFB',
          100: '#E5D6F7',
          200: '#CCB1EE',
          300: '#B08AE3',
          400: '#9265D6',
          500: '#7B3FCC',
          600: '#722EA8',
          700: '#5B249A',
          800: '#4A1A7A',
          900: '#2E0F52',
        },
        accent: {  // Yape teal
          50:  '#E6FAFA',
          100: '#CCF7F7',
          200: '#99F0F0',
          300: '#5EE0E0',
          400: '#3DD9D9',
          500: '#1FD4D4',
          600: '#16AEAE',
          700: '#0F8989',
          800: '#0A6868',
          900: '#054040',
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
