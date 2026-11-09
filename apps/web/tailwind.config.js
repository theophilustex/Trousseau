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
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          500: '#334155',
          700: '#0f172a',
          900: '#020617',
          accent: '#c59b27', // warm luxury antique gold accent for Trousseau
          accentLight: '#fbf4db'
        }
      }
    },
  },
  plugins: [],
}
