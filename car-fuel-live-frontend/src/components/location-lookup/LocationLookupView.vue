<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'

import { useLocationLookup } from '@/composables/useLocationLookup'
import { resolveLocationLookupMessages } from '@/i18n/locationLookupMessages'
import { useLocationCountryContextStore } from '@/stores/locationCountryContext'

import LocationResultList from './LocationResultList.vue'
import LocationSearchForm from './LocationSearchForm.vue'

const messages = resolveLocationLookupMessages()
const countryContextStore = useLocationCountryContextStore()
countryContextStore.initializeCountryContext()
const { countryCode } = storeToRefs(countryContextStore)

const lookup = useLocationLookup({
  countryCode,
})

const countryLabel = computed(() =>
  countryContextStore.countryLabel(messages.locale),
)
const activeSuggestionId = computed(() =>
  lookup.activeResult.value === null
    ? null
    : `location-suggestion-${lookup.activeResult.value.type}-${lookup.activeResult.value.id}`,
)
</script>

<template>
  <main
    class="min-h-screen bg-slate-100 px-4 py-8 text-slate-950 sm:px-6 lg:px-8"
  >
    <div class="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[0.9fr_1.1fr]">
      <section
        class="flex flex-col justify-between rounded-lg bg-slate-950 p-6 text-white"
      >
        <div>
          <p class="text-sm font-semibold tracking-wide text-sky-300 uppercase">
            {{ messages.eyebrow }}
          </p>
          <h1 class="mt-3 text-3xl font-bold tracking-normal sm:text-4xl">
            {{ messages.title }}
          </h1>
          <p class="mt-4 max-w-prose text-base text-slate-300">
            {{ messages.intro }}
          </p>
        </div>
        <p
          class="mt-8 rounded-md border border-slate-700 bg-slate-900 p-3 text-sm text-slate-300"
        >
          {{ messages.privacyNotice }}
        </p>
      </section>

      <section class="grid content-start gap-4">
        <LocationSearchForm
          :active-suggestion-id="activeSuggestionId"
          :country-code="countryCode"
          :country-label="countryLabel"
          :is-autocomplete-open="lookup.isAutocompleteOpen.value"
          :messages="messages"
          :query="lookup.query.value"
          @blur-input="lookup.blurInput"
          @close-autocomplete="lookup.closeAutocomplete"
          @confirm-highlighted-result="lookup.confirmHighlightedResult"
          @focus-input="lookup.focusInput"
          @move-highlight-next="lookup.moveHighlightNext"
          @move-highlight-previous="lookup.moveHighlightPrevious"
          @update-query="lookup.updateQuery"
        />
        <LocationResultList
          :active-suggestion-id="activeSuggestionId"
          :groups="lookup.groupedResults.value"
          :is-open="lookup.isAutocompleteOpen.value"
          :messages="messages"
          :status="lookup.status.value"
          @pointer-selection-cancel="lookup.cancelPointerSelection"
          @pointer-selection-start="lookup.markPointerSelectionStart"
          @select-result="lookup.selectResult"
        />
      </section>
    </div>
  </main>
</template>
