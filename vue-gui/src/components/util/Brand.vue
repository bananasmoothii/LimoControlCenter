<template>
  <RouterLink
    to="/"
    class="brand-link flex items-center gap-3 p-1.5"
    :class="(darken ? 'text-primary-darker' : 'text-primary') + ' ' + (growInMd && 'md:gap-4 md:p-3')"
  >
    <img class="h-8 w-8" :class="growInMd && 'md:h-12 md:w-12'" src="/icon256.png" alt="icon" />
    <span class="font-header uppercase leading-4" :class="growInMd && 'md:text-xl md:leading-6'"
    >Limo Control<br />Center</span
    >
  </RouterLink>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'Brand',
  props: {
    darken: {
      type: Boolean,
      default: false
    },
    growInMd: {
      type: Boolean,
      default: false
    }
  }
})
</script>
