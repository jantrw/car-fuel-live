<script setup lang="ts">
import { MapPinned, Search } from 'lucide-vue-next'

import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'

const props = defineProps<{
  query: string
  countryCode: string
  countryLabel: string
  isLoading: boolean
  validationMessage: 'QUERY_TOO_SHORT' | null
  messages: LocationLookupMessages
}>()

const emit = defineEmits<{
  updateQuery: [value: string]
  submitSearch: []
}>()

function onInput(event: Event) {
  emit('updateQuery', (event.target as HTMLInputElement).value)
}

function onSubmit() {
  emit('submitSearch')
}
</script>

<template>
  <section class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
    <div
      class="rounded-md border border-sky-200 bg-sky-50/80 p-3 text-sm text-slate-800"
    >
      <div class="flex items-start gap-3">
        <span
          class="inline-flex size-9 shrink-0 items-center justify-center rounded-md bg-sky-700 text-white"
        >
          <MapPinned class="size-4" aria-hidden="true" />
        </span>
        <div class="min-w-0">
          <p class="font-semibold text-slate-950">
            {{ props.messages.currentCountryLabel }}
          </p>
          <p class="mt-1 text-base font-semibold text-sky-950">
            {{ props.countryLabel }} ({{ props.countryCode }})
          </p>
          <p class="mt-1 text-sm text-slate-600">
            {{ props.messages.currentCountryHint }}
          </p>
        </div>
      </div>
    </div>

    <form class="mt-4 grid gap-3" @submit.prevent="onSubmit">
      <label class="text-sm font-semibold text-slate-900" for="location-query">
        {{ props.messages.searchLabel }}
      </label>
      <div class="relative">
        <Search
          class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400"
          aria-hidden="true"
        />
        <input
          id="location-query"
          :value="props.query"
          :placeholder="props.messages.searchPlaceholder"
          class="min-h-12 w-full rounded-md border border-slate-300 bg-white pr-3 pl-10 text-base text-slate-950 transition outline-none focus:border-sky-600 focus:ring-2 focus:ring-sky-100"
          type="search"
          @input="onInput"
        />
      </div>
      <p
        v-if="props.validationMessage !== null"
        class="text-sm font-medium text-amber-700"
      >
        {{ props.messages.validationMessage[props.validationMessage] }}
      </p>
      <div class="flex flex-wrap items-center gap-3">
        <button
          :disabled="props.isLoading"
          class="inline-flex min-h-11 items-center justify-center rounded-md bg-sky-700 px-4 text-sm font-semibold text-white transition hover:bg-sky-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          type="submit"
        >
          {{ props.isLoading ? props.messages.loading : props.messages.searchAction }}
        </button>
        <p class="text-sm text-slate-600">
          {{ props.messages.searchHint }}
        </p>
      </div>
    </form>
  </section>
</template>
