<script lang="ts">
import { defineComponent } from 'vue'
import { Dialog, DialogDescription, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'
import PrimaryButton from '@/components/buttons/PrimaryButton.vue'

export default defineComponent({
  name: 'NoWebGLDialog',
  components: { DialogDescription, PrimaryButton, DialogTitle, DialogPanel, Dialog, TransitionChild, TransitionRoot },
  data() {
    return {
      isOpen: this.display
    }
  },
  props: {
    display: {
      type: Boolean,
      required: true
    }
  },
  methods: {
    closeModal() {
      this.isOpen = false
    }
  }
})
</script>

<template>
  <TransitionRoot appear :show="isOpen" as="template">
    <Dialog as="div" @close="closeModal" class="relative z-10">
      <TransitionChild
        as="template"
        enter="duration-300 ease-out"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="duration-200 ease-in"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-black/25" />
      </TransitionChild>

      <div class="fixed inset-0 overflow-y-auto">
        <div
          class="flex min-h-full items-center justify-center p-4 text-center"
        >
          <TransitionChild
            as="template"
            enter="duration-300 ease-out"
            enter-from="opacity-0 scale-95"
            enter-to="opacity-100 scale-100"
            leave="duration-200 ease-in"
            leave-from="opacity-100 scale-100"
            leave-to="opacity-0 scale-95"
          >
            <DialogPanel
              class="w-full max-w-md transform overflow-hidden rounded-2xl bg-white p-6 text-left align-middle shadow-xl transition-all"
            >
              <DialogTitle
                as="h3"
                class="text-lg font-medium leading-6 text-gray-900 text-center"
              >
                WebGL Not Supported
              </DialogTitle>
              <!--div class="mt-2">
                <p class="text-sm text-gray-500">
                  Your browser does not support WebGL. Please use a different browser or device.
                </p>
              </div-->
              <DialogDescription class="mt-2 text-sm text-gray-500 text-center">
                WebGL is required to view this site and your browser does not support WebGL. Please use a different
                browser or device.
              </DialogDescription>

              <div class="mt-4">
                <PrimaryButton @click="closeModal" class="mx-auto">Ok</PrimaryButton>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>
