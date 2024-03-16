<script lang="ts">
import { defineComponent } from 'vue'
import Brand from '@/components/util/Brand.vue'
import ContactInfo from '@/components/ContactInfo.vue'

export default defineComponent({
  name: 'Footer',
  components: { ContactInfo, Brand },
  props: {
    links: {
      type: Array as () => { name: string; to: string }[]
    }
  },
  computed: {
    linksWithIconCredits() {
      if (!this.links) return []
      const copy = this.links.slice()
      copy.push({
        name: 'Icon by Flowicon',
        to: 'https://www.freepik.com/icon/bot_6819644'
      })
      return copy
    }
  }
})
</script>

<template>
  <footer class="mt-16 bg-background p-8 text-white">
    <div class="flex flex-col gap-8 md:flex-row md:justify-evenly">
      <Brand grow-in-md class="mx-auto md:mx-0" />
      <ul class="flex flex-col px-4">
        <li v-for="link in linksWithIconCredits"
            class="mt-1.5 text-center last:pt-8 last:mt-auto text-lg last:text-sm last:italic">
          <RouterLink v-if="! link.to.startsWith('http')" :to="link.to" class="text-inherit md:text-left">
            {{ link.name }}
          </RouterLink>
          <a v-else :href="link.to" class="text-inherit md:text-left" target="_blank" rel="noreferrer">
            {{ link.name }}
          </a>
        </li>
      </ul>
      <ContactInfo class="justify-center md:justify-start" />
    </div>
  </footer>
</template>
