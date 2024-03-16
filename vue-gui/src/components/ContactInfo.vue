<script lang="ts">
import { defineComponent } from 'vue'
import { AtSymbolIcon, MapPinIcon, PhoneIcon } from '@heroicons/vue/24/outline'
import IconAndText from '@/components/util/IconAndText.vue'
import GithubIcon from '@/components/util/GithubIcon.vue'

export default defineComponent({
  name: 'ContactInfo',
  components: { GithubIcon, IconAndText, MapPinIcon, AtSymbolIcon, PhoneIcon }
})
</script>

<template>
  <div class="px-4 py-2">
    <IconAndText class="mt-1">
      <GithubIcon />
      <a href="https://github.com/bananasmoothii/LimoControlCenter" target="_blank">Github repository</a>
    </IconAndText>
    <IconAndText class="mt-1">
      <AtSymbolIcon />
      <a href="mailto://contact.bananabubble@gmail.com">contact.bananabubble@gmail.com</a>
    </IconAndText>
    <IconAndText class="mt-1">
      <GithubIcon />
      <a href="https://github.com/bananasmoothii">Made by Bananasmoothii</a>
    </IconAndText>
  </div>
</template>
