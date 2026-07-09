/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        navy: {
          950: "#060B18",
          900: "#091224",
          850: "#0D1933",
          800: "#132347",
          700: "#1C3364",
          600: "#274785",
        },
        ocean: {
          600: "#0284C7",
          500: "#0EA5E9",
          400: "#38BDF8",
          300: "#7DD3FC",
        },
      },
    },
  },
  plugins: [],
};
