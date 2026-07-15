<script setup lang="ts">
import { useGasStationResults } from '@/composables/useGasStationResults'
import { useLocationCountryContext } from '@/composables/useLocationCountryContext'
import { useLocationLookup } from '@/composables/useLocationLookup'
import { resolveLocationLookupMessages } from '@/i18n/locationLookupMessages'

import GasStationResultPanel from './GasStationResultPanel.vue'
import LocationResultList from './LocationResultList.vue'
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

async function handleResultSelection(
  result: Parameters<typeof lookup.selectResult>[0],
) {
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
              <LocationResultList
                :groups="lookup.groupedResults.value"
                :messages="messages"
                :query="lookup.lastSubmittedQuery.value"
                :status="lookup.status.value"
                @select-result="handleResultSelection"
              />
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
