<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'IconAndText',
  props: {
    gap: {
      type: Number,
      default: 3
    }
  }
})
</script>

<template>
  <div class="icon-and-text flex items-center" :style="{ gap: gap / 4 + 'rem' }">
    <slot />
  </div>
</template>

<style lang="scss">
.icon-and-text {
  justify-content: inherit;

  & svg {
    height: 1em;
  }
}
</style>
