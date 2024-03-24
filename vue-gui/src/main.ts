import './assets/style.scss'

import { createApp, ref, watch } from 'vue'
import App from './App.vue'
import router from './router'
import { initSearchFilterWatching } from '@/components/3D/robots'


// the text of the search input
export const searchFilter = ref('')

router.afterEach((newRoute, oldRoute) => {
  if (newRoute.query.s === oldRoute.query.s) return
  searchFilter.value = (newRoute.query.s as string | undefined) || ''
})

const app = createApp(App)

app.use(router)

app.mount('#app')

watch(searchFilter, (newFilter: string) => {
  router.replace({ query: { s: newFilter || undefined } })
})

initSearchFilterWatching()
