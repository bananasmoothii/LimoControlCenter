<script lang="ts">
import { defineComponent } from 'vue'
import { AtSymbolIcon, MapPinIcon, PhoneIcon } from '@heroicons/vue/24/outline'
import IconAndText from '@/components/util/IconAndText.vue'

export default defineComponent({
  name: 'ContactInfo',
  components: { IconAndText, MapPinIcon, AtSymbolIcon, PhoneIcon }
})
</script>

<template>
  <div class="px-4 py-2">
    <IconAndText class="mt-1">
      <MapPinIcon />
      <span>Number 17<br />Cherry Tree Lane</span>
    </IconAndText>
    <IconAndText class="mt-1">
      <AtSymbolIcon />
      <a href="mailto://mary@poppins.co.uk">mary@poppins.co.uk</a>
    </IconAndText>
    <IconAndText class="mt-1">
      <PhoneIcon />
      <a href="tel:+44 1234 567890">+44 1234 567890</a>
    </IconAndText>
  </div>
</template>
