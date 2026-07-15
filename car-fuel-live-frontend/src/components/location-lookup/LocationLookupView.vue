<script setup lang="ts">
import { computed } from 'vue'

import { CodeXml, Info } from 'lucide-vue-next'

import logoUrl from '@/assets/car-fuel-live-logo.png'
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
  <main class="min-h-screen bg-[#f9fbfb] text-[#062f2c]">
    <header class="bg-[#003f3a] text-white">
      <div
        class="mx-auto flex min-h-14 max-w-[1440px] items-center px-5 sm:px-8"
      >
        <img
          :src="logoUrl"
          alt=""
          class="size-8 object-contain"
          aria-hidden="true"
        />
        <span class="ml-2 text-2xl font-bold tracking-tight">{{
          messages.appName
        }}</span>
      </div>
    </header>

    <div
      class="mx-auto flex w-full max-w-[1440px] flex-col gap-8 px-5 py-9 sm:px-8 lg:py-10"
    >
      <section
        class="relative grid rounded-[2rem] bg-[linear-gradient(115deg,#eff9f7_5%,#f8fbfb_55%,#e5f4f1)] px-6 py-8 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] lg:gap-12 lg:px-10 lg:py-10"
      >
        <div class="flex max-w-xl flex-col items-start text-left">
          <h1
            class="max-w-lg text-5xl font-extrabold tracking-[-0.035em] text-[#002e2b] sm:text-[3.5rem] lg:text-6xl"
          >
            {{ messages.title }}
          </h1>
          <p class="mt-4 text-lg text-[#123d3a] sm:text-xl">
            {{ messages.intro }}
          </p>
          <p
            class="mt-6 inline-flex items-center gap-3 rounded-xl border border-teal-700/25 px-4 py-2 text-base text-teal-800"
          >
            <CodeXml class="size-5" aria-hidden="true" />
            {{ messages.sourceAvailability }}
          </p>
        </div>
        <div class="mt-8 min-w-0 lg:mt-0">
          <ol
            class="mb-6 flex items-center gap-3 text-sm sm:gap-5 sm:text-base"
          >
            <li class="flex items-center gap-3 font-medium text-teal-700">
              <span
                class="inline-flex size-7 items-center justify-center rounded-full bg-teal-700 text-white"
                >1</span
              >
              {{ messages.stepSelectLocation }}
            </li>
            <li class="h-px min-w-8 flex-1 bg-slate-300" aria-hidden="true" />
            <li class="flex items-center gap-3 text-slate-400">
              <span
                class="inline-flex size-7 items-center justify-center rounded-full bg-slate-200 text-slate-700"
                >2</span
              >
              {{ messages.stepComparePrices }}
            </li>
          </ol>
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
                  class="flex items-center gap-2 rounded-xl border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm"
                >
                  <span
                    class="size-4 animate-spin rounded-full border-2 border-slate-300 border-t-teal-700 motion-reduce:animate-none"
                    aria-hidden="true"
                  />
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
      </section>

      <section
        class="flex items-center gap-5 rounded-2xl border border-slate-300 bg-white px-6 py-7 text-lg shadow-sm"
      >
        <span
          class="inline-flex size-12 shrink-0 items-center justify-center rounded-full bg-emerald-200 text-emerald-950"
        >
          <Info class="size-7" aria-hidden="true" />
        </span>
        <p>{{ messages.selectionHint }}</p>
      </section>

      <section>
        <GasStationResultPanel
          :messages="messages"
          :selected-location-label="
            gasStationResults.selectedLocationLabel.value
          "
          :stations="gasStationResults.stations.value"
          :status="gasStationResults.status.value"
        />
      </section>
    </div>

    <footer
      class="mx-auto flex max-w-[1440px] flex-col gap-3 border-t border-slate-300 px-5 py-5 text-sm text-slate-600 sm:grid sm:grid-cols-[1fr_auto_1fr] sm:items-center sm:px-8"
    >
      <span>{{ messages.footerPriceData }}</span>
      <span>{{ messages.footerPriceNotice }}</span>
      <a
        class="text-teal-700 underline underline-offset-4 hover:text-teal-900 focus:ring-2 focus:ring-teal-200 focus:outline-none sm:justify-self-end"
        href="https://github.com/jantrw/car-fuel-live"
      >
        {{ messages.footerSourceLink }}
      </a>
    </footer>
  </main>
</template>
