<template xmlns="http://www.w3.org/1999/html">
  <button
    :disabled="disabled"
    @click="onClick"
    @mouseenter="mouseEnter"
    @mouseleave="mouseLeave"
    type="button"
    ref="button"
    class="block rounded-lg bg-primary px-3 py-2.5 font-semibold text-gray-900 transition-all duration-200 ease-in-out
           hover:bg-primary-lighter hover:text-gray-700 min-w-20 focus:ring-2 focus:ring-black
           disabled:bg-gray-300 disabled:text-gray-600 disabled:cursor-not-allowed"
  >
    <slot />
  </button>
</template>

<script lang="ts">
export default {
  name: 'PrimaryButton',
  props: {
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: {
    click: (e: MouseEvent) => true
  },
  methods: {
    onClick(e: MouseEvent) {
      this.$emit('click', e)
      let button = this.$refs.button as HTMLButtonElement
      button.classList.add('screen-revolution')
      setTimeout(() => {
        button.classList.remove('screen-revolution')
      }, 1000)
    },
    mouseEnter(e: MouseEvent) {
      let button = this.$refs.button as HTMLButtonElement
      button.querySelectorAll('.button-hover').forEach((el: Element) => {
        for (let clazz of el.classList.values()) {
          if (clazz.startsWith('button-hover:')) {
            el.classList.add(clazz.slice(13))
          }
        }
      })
    },
    mouseLeave(e: MouseEvent) {
      let button = this.$refs.button as HTMLButtonElement
      button.querySelectorAll('.button-hover').forEach((el: Element) => {
        for (let clazz of el.classList.values()) {
          if (clazz.startsWith('button-hover:')) {
            el.classList.remove(clazz.slice(13))
          }
        }
      })
    }
  }
}
</script>

<style lang="scss">
.screen-revolution {
  animation: screen-revolution 1000ms cubic-bezier(0.55, -0.39, 0.43, 1.49);;
}

@keyframes screen-revolution {
  0% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(20px);
  }
  //50.01% {
  //  transform: translateX(-100vw);
  //}
  100% {
    transform: translateX(0);
  }
}
</style>