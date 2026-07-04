<script setup lang="ts">
import { Fuel, MapPinned, TriangleAlert } from 'lucide-vue-next'

import type { GasStationResult } from '@/api/gasStations'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'

const props = defineProps<{
  stations: readonly GasStationResult[]
  status:
    | 'idle'
    | 'loading'
    | 'results'
    | 'empty'
    | 'error'
    | 'rateLimited'
    | 'countrySelected'
  selectedLocationLabel: string
  messages: LocationLookupMessages
}>()

function formatPrice(value: number | null) {
  if (value === null) {
    return props.messages.stationPriceUnavailable
  }

  return `${new Intl.NumberFormat(props.messages.locale, {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  }).format(value)} €/L`
}

function formatDistance(value: number | null) {
  if (value === null) {
    return props.messages.stationDistanceUnavailable
  }

  return `${new Intl.NumberFormat(props.messages.locale, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value)} km`
}

function formatAddress(station: GasStationResult) {
  const streetLine = [station.street, station.houseNumber]
    .filter(
      (segment): segment is string => segment !== null && segment.length > 0,
    )
    .join(' ')
  const placeLine = [station.postCode, station.place]
    .filter(
      (segment): segment is string => segment !== null && segment.length > 0,
    )
    .join(' ')

  const address = [streetLine, placeLine]
    .filter((segment) => segment.length > 0)
    .join(' · ')

  return address.length > 0 ? address : props.messages.stationAddressUnavailable
}

function openStateLabel(isOpen: boolean | null) {
  if (isOpen === true) {
    return props.messages.stationOpen
  }
  if (isOpen === false) {
    return props.messages.stationClosed
  }

  return props.messages.stationOpenUnknown
}
</script>

<template>
  <section
    v-if="props.status !== 'idle'"
    class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
    aria-live="polite"
  >
    <div class="flex items-center justify-between gap-3">
      <div>
        <p
          class="text-xs font-semibold tracking-[0.24em] text-emerald-700 uppercase"
        >
          {{ props.messages.stationResultsEyebrow }}
        </p>
        <h2 class="mt-1 text-lg font-semibold text-slate-950">
          {{ props.messages.stationResultsTitle }}
        </h2>
      </div>
      <p class="text-sm text-slate-500">
        {{ props.messages.stationResultsSelectionPrefix }}
        <span class="font-semibold text-slate-950">
          {{ props.selectedLocationLabel }}
        </span>
      </p>
    </div>

    <p v-if="props.status === 'loading'" class="mt-4 text-sm text-slate-600">
      {{ props.messages.stationLoading }}
    </p>

    <p
      v-else-if="props.status === 'countrySelected'"
      class="mt-4 text-sm text-amber-700"
    >
      <span class="block font-semibold text-amber-900">
        {{ props.messages.countrySelectionTitle }}
      </span>
      {{ props.messages.countrySelectionBody }}
    </p>

    <p v-else-if="props.status === 'empty'" class="mt-4 text-sm text-slate-600">
      <span class="block font-semibold text-slate-950">
        {{ props.messages.stationEmptyTitle }}
      </span>
      {{ props.messages.stationEmptyBody }}
    </p>

    <p v-else-if="props.status === 'error'" class="mt-4 text-sm text-red-700">
      <span class="block font-semibold text-red-900">
        {{ props.messages.stationErrorTitle }}
      </span>
      {{ props.messages.stationErrorBody }}
    </p>

    <p
      v-else-if="props.status === 'rateLimited'"
      class="mt-4 text-sm text-amber-700"
    >
      <span class="block font-semibold text-amber-900">
        {{ props.messages.stationRateLimitedTitle }}
      </span>
      {{ props.messages.stationRateLimitedBody }}
    </p>

    <div v-else class="mt-4 grid gap-3">
      <div class="grid gap-3 xl:grid-cols-2">
        <article
          v-for="station in props.stations"
          :key="station.id"
          class="overflow-hidden rounded-xl border border-slate-200 bg-slate-50"
        >
          <div class="border-b border-slate-200 bg-white px-4 py-3">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0">
                <h3 class="truncate text-base font-semibold text-slate-950">
                  {{ station.name }}
                </h3>
                <p class="mt-1 text-sm text-slate-600">
                  {{ station.brand ?? props.messages.stationBrandFallback }} ·
                  {{ formatAddress(station) }}
                </p>
              </div>
              <div
                class="flex flex-wrap items-center gap-2 text-xs font-semibold"
              >
                <span
                  class="rounded-full bg-emerald-100 px-2.5 py-1 text-emerald-900"
                >
                  {{ formatDistance(station.distanceKm) }}
                </span>
                <span
                  class="rounded-full bg-slate-200 px-2.5 py-1 text-slate-700"
                >
                  {{ openStateLabel(station.isOpen) }}
                </span>
              </div>
            </div>
          </div>

          <div class="grid gap-2 px-4 py-4 sm:grid-cols-3">
            <div
              class="rounded-lg bg-white p-3 shadow-sm ring-1 ring-slate-200"
            >
              <div
                class="flex items-center gap-2 text-xs font-semibold tracking-[0.18em] text-slate-500 uppercase"
              >
                <Fuel class="size-3.5" aria-hidden="true" />
                E5
              </div>
              <p class="mt-2 text-lg font-semibold text-slate-950">
                {{ formatPrice(station.e5) }}
              </p>
            </div>
            <div
              class="rounded-lg bg-white p-3 shadow-sm ring-1 ring-slate-200"
            >
              <div
                class="flex items-center gap-2 text-xs font-semibold tracking-[0.18em] text-slate-500 uppercase"
              >
                <Fuel class="size-3.5" aria-hidden="true" />
                E10
              </div>
              <p class="mt-2 text-lg font-semibold text-slate-950">
                {{ formatPrice(station.e10) }}
              </p>
            </div>
            <div
              class="rounded-lg bg-white p-3 shadow-sm ring-1 ring-slate-200"
            >
              <div
                class="flex items-center gap-2 text-xs font-semibold tracking-[0.18em] text-slate-500 uppercase"
              >
                <Fuel class="size-3.5" aria-hidden="true" />
                Diesel
              </div>
              <p class="mt-2 text-lg font-semibold text-slate-950">
                {{ formatPrice(station.diesel) }}
              </p>
            </div>
          </div>
        </article>
      </div>

      <div
        class="flex flex-wrap items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-950"
      >
        <MapPinned class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>{{ props.messages.stationAttribution }}</span>
      </div>
      <div
        class="flex flex-wrap items-start gap-2 rounded-lg border border-slate-200 bg-slate-100 px-4 py-3 text-sm text-slate-700"
      >
        <TriangleAlert class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>{{ props.messages.stationFreshnessNotice }}</span>
      </div>
    </div>
  </section>
</template>
