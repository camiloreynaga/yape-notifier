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
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
