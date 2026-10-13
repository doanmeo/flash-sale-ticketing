/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fff5f0',
          100: '#ffe8dd',
          200: '#ffd0bc',
          500: '#ff4d00',
          600: '#e04400',
          700: '#b83400',
          dark: '#121316',
        },
        surface: {
          light: '#faf9fd',
          card: '#ffffff',
          dark: '#121316',
          cardDark: '#1a1d24',
        },
        emerald: {
          available: '#10b981',
          selected: '#f59e0b',
        }
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
