<script lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { defineComponent } from 'vue'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import { robotCount } from '@/components/3D/robots'

export default defineComponent({
  name: 'App',
  components: {
    Header,
    Footer,
    RouterLink,
    RouterView
  },
  computed: {
    links() {
      return [
        { name: 'Connected: ' + robotCount.value, to: '/' }
      ]
    },
  },
})
</script>

<template>
  <Header :links="links" />

  <div id="main" class="px-8 pt-24 md:px-16 md:pt-28 xl:px-36">
    <RouterView />
  </div>

  <Footer :links="links" />
</template>
