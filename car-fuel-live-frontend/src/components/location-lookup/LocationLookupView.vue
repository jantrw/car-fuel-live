<script setup lang="ts">
import { computed } from 'vue'

import { useLocationLookup } from '@/composables/useLocationLookup'
import { resolveLocationLookupMessages } from '@/i18n/locationLookupMessages'

import LocationResultList from './LocationResultList.vue'
import LocationSearchForm from './LocationSearchForm.vue'
import SelectedLocationPanel from './SelectedLocationPanel.vue'

const messages = resolveLocationLookupMessages()
const lookup = useLocationLookup()
const isLoading = computed(() => lookup.status.value === 'loading')
const validationMessage = computed(() =>
  lookup.queryValidationMessage.value === 'LOCATION_LOOKUP_QUERY_TOO_SHORT'
    ? messages.searchTooShort
    : lookup.queryValidationMessage.value === 'LOCATION_LOOKUP_QUERY_TOO_LONG'
      ? messages.searchTooLong
      : null,
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

      <section class="grid gap-4">
        <LocationSearchForm
          :query="lookup.query.value"
          :can-search="lookup.canSearch.value"
          :is-loading="isLoading"
          :validation-message="validationMessage"
          :messages="messages"
          @update-query="lookup.updateQuery"
          @search="lookup.search"
        />
        <LocationResultList
          :results="lookup.results.value"
          :status="lookup.status.value"
          :messages="messages"
          @select-result="lookup.selectResult"
        />
        <SelectedLocationPanel
          :selected-result="lookup.selectedResult.value"
          :messages="messages"
        />
      </section>
    </div>
  </main>
</template>
