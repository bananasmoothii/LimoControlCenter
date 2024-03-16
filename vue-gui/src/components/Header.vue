<template>
  <header class="fixed top-0 z-20 w-full bg-white bg-opacity-50 shadow-lg backdrop-blur">
    <div class="mx-auto flex max-w-7xl justify-between px-6 py-3">
      <nav class="mx-auto flex grow items-center justify-start gap-8 lg:gap-12 xl:gap-16" aria-label="Navbar">
        <div class="flex">
          <Brand class="-m-1.5" darken />
        </div>
        <div class="hidden grow items-center gap-4 md:flex lg:gap-12">
          <RouterLink
            v-for="link of links"
            :to="link.to"
            class="rounded-md px-3 py-2.5 font-header uppercase text-gray-900 hover:bg-gray-100"
          >
            {{ link.name }}
          </RouterLink>
          <div class="grow">
            <input
              v-if="$route.path === '/'"
              type="search"
              name="search"
              class="h-10 w-[7rem] rounded-lg border-[1px] border-gray-300 bg-gray-200 bg-opacity-50 p-2 focus:w-[90%] lg:w-[9rem] lg:focus:w-[80%] xl:w-[12rem] xl:focus:w-[24rem]"
              placeholder="Search..."
              v-model="searchFilter.value"
            />
          </div>
        </div>
      </nav>
      <div class="hidden items-center md:flex">
        <LogInButton class="" />
      </div>
      <div class="flex items-center md:hidden">
        <button
          type="button"
          class="-m-2.5 inline-flex items-center justify-center rounded-md bg-black bg-opacity-5 px-2.5 py-2 text-gray-700 hover:bg-gray-200"
          @click="mobileMenuButtonClicked"
        >
          <Bars3Icon :style="{ opacity: mobileMenuOpen ? 0 : 1 }" class="h-8 w-8" aria-hidden="true" />
          <XMarkIcon :style="{ opacity: mobileMenuOpen ? 1 : 0 }" class="absolute h-8 w-8" aria-hidden="true" />
        </button>
      </div>
    </div>
    <TransitionRoot :show="mobileMenuOpen">
      <Dialog class="md:hidden" @close="modalClose">
        <TransitionChild
          enter="transition-opacity duration-[800ms] ease-in-out"
          enter-from="opacity-0"
          enter-to="opacity-100"
          leave="transition-opacity duration-[800ms] ease-in-out"
          leave-from="opacity-100"
          leave-to="opacity-0"
        >
          <div class="z-70 fixed inset-0 top-14 bg-black opacity-20" />
        </TransitionChild>
        <TransitionChild
          class="z-80 fixed bottom-0 right-0 top-14 w-full overflow-y-auto bg-white bg-opacity-60 px-6 py-4 backdrop-blur sm:max-w-sm sm:ring-1 sm:ring-gray-900/10"
          enter="transition ease-in-out duration-[800ms] transform"
          enter-from="translate-x-full"
          enter-to="translate-x-0"
          leave="transition ease-in-out duration-[800ms] transform"
          leave-from="translate-x-0"
          leave-to="translate-x-full"
        >
          <DialogPanel>
            <div class="mobile-menu mt-6 flow-root w-full">
              <div class="-my-6">
                <div class="-mx-3 space-y-2 py-6">
                  <RouterLink
                    v-for="link in links"
                    :to="link.to"
                    @click="modalClose"
                    class="block rounded-lg px-3 py-2 font-header uppercase leading-7 text-gray-900 hover:bg-gray-100"
                  >
                    {{ link.name }}
                  </RouterLink>
                </div>
                <input
                  v-if="$route.path === '/'"
                  type="search"
                  name="search"
                  class="mb-4 h-10 w-full rounded-lg border-[1px] border-gray-300 bg-gray-200 bg-opacity-50 p-2"
                  placeholder="Search..."
                  v-model="searchFilter.value"
                  @keyup.enter="modalClose"
                />
                <div class="py-6">
                  <LogInButton @click="modalClose" />
                </div>
              </div>
            </div>
          </DialogPanel>
        </TransitionChild>
      </Dialog>
    </TransitionRoot>
  </header>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import {
  Dialog,
  DialogPanel,
  Disclosure,
  DisclosureButton,
  DisclosurePanel,
  Popover,
  PopoverButton,
  PopoverGroup,
  PopoverPanel,
  TransitionChild,
  TransitionRoot
} from '@headlessui/vue'
import {
  ArrowLongRightIcon,
  ArrowPathIcon,
  Bars3Icon,
  ChartPieIcon,
  CursorArrowRaysIcon,
  FingerPrintIcon,
  SquaresPlusIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import { ChevronDownIcon, PhoneIcon, PlayCircleIcon } from '@heroicons/vue/20/solid'
import PrimaryButton from '@/components/buttons/PrimaryButton.vue'
import Brand from '@/components/util/Brand.vue'
import { searchFilter } from '@/main'
import ImportantButtonToRepurpose from '@/components/buttons/ImportantButtonToRepurpose.vue'

export default defineComponent({
  name: 'Header',
  data() {
    return {
      mobileMenuOpen: false,
      lastClosedTime: 0
    }
  },
  props: {
    links: {
      type: Array as () => { name: string; to: string }[]
    }
  },
  components: {
    LogInButton: ImportantButtonToRepurpose,
    Brand,
    PrimaryButton,
    TransitionChild,
    TransitionRoot,
    ArrowPathIcon,
    ArrowLongRightIcon,
    Bars3Icon,
    ChartPieIcon,
    CursorArrowRaysIcon,
    FingerPrintIcon,
    SquaresPlusIcon,
    XMarkIcon,
    ChevronDownIcon,
    PhoneIcon,
    PlayCircleIcon,
    Dialog,
    DialogPanel,
    Disclosure,
    DisclosureButton,
    DisclosurePanel,
    Popover,
    PopoverButton,
    PopoverGroup,
    PopoverPanel
  },
  methods: {
    mobileMenuButtonClicked() {
      if (this.mobileMenuOpen) {
        this.mobileMenuOpen = false
        return
      }
      // we have to do this because teh click will trigger twice:
      // once for clicking the modal away and once for clicking the button
      if (Date.now() - this.lastClosedTime > 100) {
        this.mobileMenuOpen = true
      }
    },
    modalClose() {
      this.mobileMenuOpen = false
      this.lastClosedTime = Date.now()
    }
  },
  watch: {
    '$route.path'(newPath: string) {
      if (newPath !== '/') searchFilter.value = ''
    }
  },
  computed: {
    searchFilter() {
      return searchFilter
    }
  }
})
</script>
