<script setup lang="ts">
import { ArrowRight, LoaderCircle, Search } from 'lucide-vue-next'

import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'

const props = defineProps<{
  query: string
  countryCode: string
  countryLabel: string
  isLoading: boolean
  validationMessage: 'QUERY_TOO_SHORT' | 'QUERY_TOO_LONG' | null
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
  <section>
    <form class="grid gap-3" @submit.prevent="onSubmit">
      <label class="text-base font-medium text-[#063b37]" for="location-query">
        {{ props.messages.searchLabel }}
      </label>
      <div class="relative">
        <div class="relative flex flex-col gap-3 sm:flex-row">
          <div class="relative min-w-0 flex-1">
            <Search
              class="pointer-events-none absolute top-1/2 left-5 size-6 -translate-y-1/2 text-teal-700"
              aria-hidden="true"
            />
            <input
              id="location-query"
              :value="props.query"
              :placeholder="props.messages.searchPlaceholder"
              :aria-invalid="props.validationMessage !== null || undefined"
              :aria-describedby="
                props.validationMessage === null
                  ? undefined
                  : 'location-query-validation'
              "
              class="min-h-15 w-full rounded-xl border border-slate-300 bg-white pr-5 pl-15 text-xl text-slate-950 shadow-sm transition outline-none placeholder:text-slate-500 focus:border-teal-700 focus:ring-2 focus:ring-teal-200"
              type="search"
              @input="onInput"
            />
          </div>
          <button
            :disabled="props.isLoading"
            class="inline-flex min-h-15 items-center justify-center gap-4 rounded-xl bg-teal-700 px-7 text-lg font-semibold text-white transition hover:bg-teal-800 focus:ring-2 focus:ring-teal-200 focus:ring-offset-2 focus:outline-none disabled:cursor-not-allowed disabled:bg-slate-400"
            type="submit"
          >
            <LoaderCircle
              v-if="props.isLoading"
              class="size-5 animate-spin motion-reduce:animate-none"
              aria-hidden="true"
            />
            <template v-else>
              {{ props.messages.searchAction }}
              <ArrowRight class="size-6" aria-hidden="true" />
            </template>
            <span v-if="props.isLoading">{{ props.messages.loading }}</span>
          </button>
          <div
            v-if="$slots.results"
            class="mt-2 w-full sm:absolute sm:top-full sm:z-10"
          >
            <slot name="results" />
          </div>
        </div>
      </div>
      <p
        v-if="props.validationMessage !== null"
        id="location-query-validation"
        role="alert"
        class="text-sm font-medium text-amber-700"
      >
        {{ props.messages.validationMessage[props.validationMessage] }}
      </p>
      <p class="text-sm text-slate-600">
        {{ props.messages.currentCountryLabel }}: {{ props.countryLabel }} ({{
          props.countryCode
        }})
      </p>
      <p class="text-xs leading-5 text-slate-600">
        {{ props.messages.privacyNotice }}
      </p>
    </form>
  </section>
</template>
