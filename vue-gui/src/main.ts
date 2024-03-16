import './assets/style.scss'

import { createApp, ref, watch } from 'vue'
import App from './App.vue'
import router from './router'


// the text of the search input
export const searchFilter = ref('')

router.afterEach((newRoute, oldRoute) => {
  if (newRoute.query.s === oldRoute.query.s) return
  searchFilter.value = (newRoute.query.s as string | undefined) || ''
})

const app = createApp(App)

app.use(router)

app.mount('#app')

watch(searchFilter, (newFilter, oldFilter) => {
  // if (newFilter.length < 3) {
  //     if (newFilter === "") router.replace({query: {}});
  //     return;
  // }
  router.replace({ query: { s: newFilter || undefined } })
})