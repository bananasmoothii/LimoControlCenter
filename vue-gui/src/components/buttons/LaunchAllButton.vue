<script lang="ts">
import PrimaryButton from '@/components/buttons/PrimaryButton.vue'
import { ArrowLongRightIcon } from '@heroicons/vue/20/solid'
import { defineComponent } from 'vue'
import { launchRobotsToGoals, robotGoals } from '@/components/3D/pin_goal'

export default defineComponent({
  name: 'LaunchAllButton',
  computed: {
    isRobotGoalsEmpty() {
      return Object.keys(robotGoals).length === 0
    }
  },
  components: { PrimaryButton, ArrowLongRightIcon },
  emits: ['click'],
  methods: {
    onClick() {
      this.$emit('click')
      launchRobotsToGoals()
    }
  }
})
</script>

<template>
  <PrimaryButton class="flex gap-3" @click="onClick" :disabled="isRobotGoalsEmpty">
    <span class="flex-1 whitespace-nowrap">Launch All</span>
    <ArrowLongRightIcon class="button-hover button-hover:translate-x-2.5 w-6 transition-all" aria-hidden="true" />
  </PrimaryButton>
</template>
