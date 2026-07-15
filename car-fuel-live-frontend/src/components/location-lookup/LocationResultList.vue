<script setup lang="ts">
import { ArrowRight, Building2, LoaderCircle, MapPin } from 'lucide-vue-next'

import type { LocationSearchResult } from '@/api/locationSearch'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'
import type { LocationSuggestionGroup } from '@/lib/locationSuggestions'

const props = defineProps<{
  groups: LocationSuggestionGroup[]
  status: string
  query: string
  validationMessage: 'QUERY_TOO_SHORT' | 'QUERY_TOO_LONG' | null
  messages: LocationLookupMessages
}>()

const emit = defineEmits<{
  selectResult: [result: LocationSearchResult]
}>()
</script>

<template>
  <section
    v-if="props.status !== 'idle'"
    class="mt-2 max-h-[32rem] overflow-y-auto rounded-xl border border-slate-200 bg-white p-3 shadow-lg shadow-slate-950/10 sm:p-5"
    aria-live="polite"
  >
    <div>
      <h2 class="text-xl font-bold text-slate-950">
        {{ props.messages.resultsTitle }}
        {{ props.messages.resultsQueryPrefix.toLowerCase() }} „{{
          props.query
        }}“
      </h2>
      <p class="mt-1 text-base text-slate-600">
        {{ props.messages.selectionHint }}
      </p>
    </div>

    <p
      v-if="props.status === 'validation'"
      class="mt-3 text-sm font-medium text-amber-700"
    >
      {{
        props.validationMessage === null
          ? props.messages.validationMessage.QUERY_TOO_SHORT
          : props.messages.validationMessage[props.validationMessage]
      }}
    </p>

    <p
      v-else-if="props.status === 'noResults'"
      class="mt-3 text-sm text-slate-600"
    >
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
      class="mt-3 flex items-center gap-2 text-sm text-slate-600"
    >
      <LoaderCircle
        class="size-4 animate-spin motion-reduce:animate-none"
        aria-hidden="true"
      />
      {{ props.messages.loading }}
    </p>

    <div v-else-if="props.groups.length > 0" class="mt-5 grid gap-4">
      <section
        v-for="group in props.groups"
        :key="group.type"
        class="grid gap-2"
      >
        <h3 class="text-base font-semibold text-[#063b37]">
          {{ props.messages.suggestionGroupTitle[group.type] }}
        </h3>
        <ul class="grid gap-1">
          <li
            v-for="result in group.items"
            :key="`${result.type}:${result.id}`"
          >
            <button
              class="flex w-full items-center gap-3 rounded-lg px-2 py-2.5 text-left transition hover:bg-teal-50 focus:bg-teal-50 focus:ring-2 focus:ring-teal-200 focus:outline-none"
              type="button"
              @click="emit('selectResult', result)"
            >
              <span
                class="inline-flex size-10 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-slate-700"
              >
                <Building2
                  v-if="result.type !== 'postalCode'"
                  class="size-5"
                  aria-hidden="true"
                />
                <MapPin v-else class="size-5" aria-hidden="true" />
              </span>
              <span class="min-w-0">
                <span class="block text-lg font-semibold text-slate-950">
                  {{ result.label }}
                </span>
                <span class="block text-sm text-slate-600">
                  {{ props.messages.resultType[result.type] }} ·
                  {{ result.countryCode }}
                </span>
              </span>
              <ArrowRight
                class="ml-auto size-5 shrink-0 text-slate-950"
                aria-hidden="true"
              />
            </button>
          </li>
        </ul>
      </section>
    </div>
  </section>
</template>
