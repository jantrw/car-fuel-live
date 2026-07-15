<script setup lang="ts">
import { computed } from 'vue'

import type { LocationSearchResult } from '@/api/locationSearch'
import { useGasStationResults } from '@/composables/useGasStationResults'
import { useLocationCountryContext } from '@/composables/useLocationCountryContext'
import { useLocationLookup } from '@/composables/useLocationLookup'
import { resolveLocationLookupMessages } from '@/i18n/locationLookupMessages'

import GasStationResultPanel from './GasStationResultPanel.vue'
import LocationSearchResultDropdown, {
  type LocationSearchDropdownGroup,
} from './LocationSearchResultDropdown.vue'
import LocationSearchForm from './LocationSearchForm.vue'

const messages = resolveLocationLookupMessages()
const countryContext = useLocationCountryContext()
countryContext.initializeCountryContext()
const countryCode = countryContext.countryCode
const countryLabel = countryContext.countryLabel(messages.locale)

const lookup = useLocationLookup({
  countryCode,
})
const gasStationResults = useGasStationResults()
const dropdownGroups = computed<LocationSearchDropdownGroup[]>(() =>
  lookup.groupedResults.value.map((group) => ({
    id: group.type,
    label: messages.suggestionGroupTitle[group.type],
    items: group.items.map((result) => ({
      ...result,
      context: `${result.countryCode} · ${messages.resultType[result.type]}`,
    })),
  })),
)

async function handleResultSelection(result: LocationSearchResult) {
  lookup.selectResult(result)
  countryContext.setCountryCode(result.countryCode)
  await gasStationResults.loadForSelection(result)
}

function handleQueryUpdate(value: string) {
  lookup.updateQuery(value)
  gasStationResults.reset()
}
</script>

<template>
  <main
    class="min-h-screen w-full bg-slate-100 px-4 py-8 text-slate-950 sm:px-6 lg:px-8"
  >
    <div class="flex w-full flex-col items-center gap-6">
      <section
        class="flex w-full max-w-6xl flex-col items-center justify-start rounded-lg bg-slate-950 p-6 text-center text-white sm:p-8"
      >
        <div class="flex max-w-3xl flex-col items-center gap-4">
          <p class="text-sm font-semibold tracking-wide text-sky-300 uppercase">
            {{ messages.eyebrow }}
          </p>
          <h1 class="mt-3 text-3xl font-bold tracking-normal sm:text-4xl">
            {{ messages.title }}
          </h1>
          <p class="mx-auto max-w-2xl text-base text-slate-300 sm:text-lg">
            {{ messages.intro }}
          </p>
          <p
            class="mx-auto max-w-2xl text-lg font-semibold tracking-tight text-sky-100 sm:text-xl"
          >
            {{ messages.mvpCatchPhrase }}
          </p>
        </div>
        <p
          class="mt-6 max-w-2xl rounded-md border border-slate-800 bg-slate-900/80 px-4 py-2 text-xs text-slate-400 sm:text-sm"
        >
          {{ messages.privacyNotice }}
        </p>
      </section>

      <section class="flex w-full flex-col items-center gap-4">
        <div class="w-full max-w-4xl">
          <LocationSearchForm
            :country-code="countryCode"
            :country-label="countryLabel"
            :is-loading="lookup.status.value === 'loading'"
            :messages="messages"
            :query="lookup.query.value"
            :validation-message="lookup.validationMessage.value"
            @submit-search="lookup.submitSearch"
            @update-query="handleQueryUpdate"
          >
            <template #results>
              <div
                v-if="
                  lookup.status.value !== 'idle' &&
                  lookup.status.value !== 'validation'
                "
                aria-live="polite"
              >
                <p
                  v-if="lookup.status.value === 'error'"
                  class="rounded-xl border border-slate-200 bg-white p-4 text-sm text-red-700 shadow-sm"
                >
                  <span class="block font-semibold">{{
                    messages.errorTitle
                  }}</span>
                  {{ messages.errorBody }}
                </p>
                <p
                  v-else-if="lookup.status.value === 'loading'"
                  class="rounded-xl border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm"
                >
                  {{ messages.loading }}
                </p>
                <LocationSearchResultDropdown
                  v-else
                  :groups="dropdownGroups"
                  :messages="messages"
                  :query="lookup.lastSubmittedQuery.value"
                  @select-item="handleResultSelection"
                />
              </div>
            </template>
          </LocationSearchForm>
        </div>
        <div class="w-full max-w-6xl">
          <GasStationResultPanel
            :messages="messages"
            :selected-location-label="
              gasStationResults.selectedLocationLabel.value
            "
            :stations="gasStationResults.stations.value"
            :status="gasStationResults.status.value"
          />
        </div>
      </section>
    </div>
  </main>
</template>
