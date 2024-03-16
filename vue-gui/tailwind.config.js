/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#FFC107', // hsv(45, 97, 100)
        'primary-darker': '#e8b007', // hsv(45, 97, 89)
        'primary-lighter': '#ffcc33', // hsv(45, 80, 100)
        'primary-light': '#ffe699', // hsv(45, 40, 100)
        highlight: '#DF3817',
        background: '#1F2937'
      },
      fontFamily: {
        header: ['Anta', 'Arial']
      }
    }
  },
  plugins: [],
  safelist: [
    'translate-x-2.5' // added dynamically in login primary button
  ]
}

