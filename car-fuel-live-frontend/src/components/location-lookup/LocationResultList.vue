<script setup lang="ts">
import { MapPin } from 'lucide-vue-next'

import type { LocationSearchResult } from '@/api/locationSearch'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'
import type { LocationSuggestionGroup } from '@/lib/locationSuggestions'

const props = defineProps<{
  groups: LocationSuggestionGroup[]
  status: string
  query: string
  messages: LocationLookupMessages
}>()

const emit = defineEmits<{
  selectResult: [result: LocationSearchResult]
}>()
</script>

<template>
  <section
    v-if="props.status !== 'idle'"
    class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
    aria-live="polite"
  >
    <div class="flex items-center justify-between gap-3">
      <h2 class="text-sm font-semibold tracking-wide text-slate-500 uppercase">
        {{ props.messages.resultsTitle }}
      </h2>
      <p class="text-sm text-slate-500">
        {{ props.messages.resultsQueryPrefix }}
        <span class="font-semibold text-slate-900">{{ props.query }}</span>
      </p>
    </div>

    <p
      v-if="props.status === 'validation'"
      class="mt-3 text-sm font-medium text-amber-700"
    >
      {{ props.messages.validationMessage.QUERY_TOO_SHORT }}
    </p>

    <p v-else-if="props.status === 'noResults'" class="mt-3 text-sm text-slate-600">
      <span class="block font-semibold text-slate-950">
        {{ props.messages.noResultsTitle }}
      </span>
      {{ props.messages.noResultsBody }}
    </p>

    <p v-else-if="props.status === 'error'" class="mt-3 text-sm text-red-700">
      <span class="block font-semibold">{{ props.messages.errorTitle }}</span>
      {{ props.messages.errorBody }}
    </p>

    <p
      v-else-if="props.status === 'loading'"
      class="mt-3 text-sm text-slate-600"
    >
      {{ props.messages.loading }}
    </p>

    <div v-else-if="props.groups.length > 0" class="mt-3 grid gap-4">
      <section v-for="group in props.groups" :key="group.type" class="grid gap-2">
        <h3 class="text-xs font-semibold tracking-[0.18em] text-slate-500 uppercase">
          {{ props.messages.suggestionGroupTitle[group.type] }}
        </h3>
        <ul class="grid gap-2">
          <li
            v-for="result in group.items"
            :key="`${result.type}:${result.id}`"
          >
            <button
              class="flex w-full items-start gap-3 rounded-md border border-slate-200 bg-slate-50 p-3 text-left transition hover:border-sky-300 hover:bg-sky-50 focus:outline-none focus:ring-2 focus:ring-sky-100"
              type="button"
              @click="emit('selectResult', result)"
            >
              <span
                class="mt-0.5 inline-flex size-8 shrink-0 items-center justify-center rounded-md bg-sky-100 text-sky-800"
              >
                <MapPin class="size-4" aria-hidden="true" />
              </span>
              <span class="min-w-0">
                <span class="block font-semibold text-slate-950">
                  {{ result.label }}
                </span>
                <span class="mt-1 block text-sm text-slate-600">
                  {{ props.messages.resultType[result.type] }} ·
                  {{ result.countryCode }}
                </span>
              </span>
            </button>
          </li>
        </ul>
      </section>
    </div>
  </section>
</template>
