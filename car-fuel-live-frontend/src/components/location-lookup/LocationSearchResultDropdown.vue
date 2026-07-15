<script setup lang="ts">
import { Building2, ChevronRight, Globe2, MapPin } from 'lucide-vue-next'

import type { LocationSearchResult } from '@/api/locationSearch'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'

export interface LocationSearchDropdownItem extends LocationSearchResult {
  context: string
}

export interface LocationSearchDropdownGroup {
  id: string
  label: string
  items: LocationSearchDropdownItem[]
}

const props = defineProps<{
  query: string
  groups: LocationSearchDropdownGroup[]
  messages: Pick<
    LocationLookupMessages,
    'resultsTitle' | 'dropdownHelperText' | 'noResultsTitle' | 'noResultsBody'
  >
}>()

const emit = defineEmits<{
  selectItem: [item: LocationSearchDropdownItem]
}>()
</script>

<template>
  <section
    class="w-full overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg shadow-slate-950/10"
  >
    <header class="px-4 pt-4 pb-3 sm:px-5">
      <h2 class="text-base font-semibold text-slate-950">
        {{ props.messages.resultsTitle }}
        <span class="break-all">„{{ props.query }}“</span>
      </h2>
      <p class="mt-1 text-sm text-slate-600">
        {{ props.messages.dropdownHelperText }}
      </p>
    </header>

    <div
      v-if="props.groups.length === 0"
      class="border-t border-slate-200 px-4 py-5 sm:px-5"
    >
      <p class="font-semibold text-slate-950">
        {{ props.messages.noResultsTitle }}
      </p>
      <p class="mt-1 text-sm text-slate-600">
        {{ props.messages.noResultsBody }}
      </p>
    </div>

    <div v-else class="border-t border-slate-200 px-2 py-2 sm:px-3">
      <section v-for="group in props.groups" :key="group.id" class="py-2">
        <h3 class="px-2 pb-1 text-sm font-semibold text-slate-800">
          {{ group.label }}
        </h3>
        <ul>
          <li v-for="item in group.items" :key="item.id">
            <button
              class="group flex w-full items-center gap-3 rounded-lg px-2 py-2.5 text-left transition duration-200 ease-out hover:bg-emerald-50 focus-visible:bg-emerald-50 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-1 focus-visible:outline-none"
              type="button"
              :aria-label="`${item.label}, ${item.context}`"
              @click="emit('selectItem', item)"
            >
              <span
                class="inline-flex size-9 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-slate-700 group-hover:border-emerald-200 group-hover:bg-white group-focus-visible:border-emerald-300"
              >
                <Globe2
                  v-if="item.type === 'country'"
                  class="size-4"
                  aria-hidden="true"
                />
                <Building2
                  v-else-if="item.type === 'place'"
                  class="size-4"
                  aria-hidden="true"
                />
                <MapPin v-else class="size-4" aria-hidden="true" />
              </span>
              <span class="min-w-0 flex-1">
                <span
                  class="block truncate font-semibold text-slate-950"
                  :title="item.label"
                >
                  {{ item.label }}
                </span>
                <span
                  class="mt-0.5 block truncate text-sm text-slate-600"
                  :title="item.context"
                >
                  {{ item.context }}
                </span>
              </span>
              <ChevronRight
                class="size-5 shrink-0 text-slate-700"
                aria-hidden="true"
              />
            </button>
          </li>
        </ul>
      </section>
    </div>
  </section>
</template>
