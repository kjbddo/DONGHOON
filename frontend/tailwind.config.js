/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ai: {
          DEFAULT: '#7c3aed',
          50: '#f5f3ff',
          500: '#7c3aed',
          700: '#6d28d9',
        },
      },
    },
  },
  plugins: [],
};
