<script lang="ts">
import PrimaryButton from '@/components/buttons/PrimaryButton.vue'
import { ArrowLongRightIcon } from '@heroicons/vue/20/solid'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'LogInButton',
  components: { PrimaryButton, ArrowLongRightIcon }
})
</script>

<template>
  <PrimaryButton class="flex gap-3">
    <span class="flex-1 whitespace-nowrap">Action</span>
    <ArrowLongRightIcon class="button-hover button-hover:translate-x-2.5 w-6 transition-all" aria-hidden="true" />
  </PrimaryButton>
</template>
