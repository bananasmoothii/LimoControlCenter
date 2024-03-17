<template>
  <div class="-mt-9">
    <ThreeWrapper />
  </div>
</template>

<script lang="ts">
import ThreeWrapper from '@/components/3D/ThreeWrapper.vue'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'HomeView',
  components: { ThreeWrapper }
})
</script>