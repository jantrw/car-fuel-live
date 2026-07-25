<script setup lang="ts">
import { LoaderCircle, MapPinned, TriangleAlert } from 'lucide-vue-next'

import type { GasStationResult } from '@/api/gasStations'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'
import { getStationBrandAccent } from '@/lib/stationBrandAccent'

const props = defineProps<{
  stations: readonly GasStationResult[]
  hasMoreStations: boolean
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

const emit = defineEmits<{
  showMoreStations: []
}>()

function formatPrice(value: number | null) {
  if (value === null) {
    return props.messages.stationPriceUnavailable
  }

  return `${new Intl.NumberFormat(props.messages.locale, {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  }).format(value)} €`
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
    class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6"
    aria-live="polite"
  >
    <div class="flex items-center justify-between gap-3">
      <div>
        <p
          class="text-xs font-semibold tracking-[0.24em] text-emerald-700 uppercase"
        >
          {{ props.messages.stationResultsEyebrow }}
        </p>
        <h2 class="mt-1 text-3xl font-bold tracking-tight text-[#063b37]">
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

    <p
      v-if="props.status === 'loading'"
      class="mt-4 flex items-center gap-2 text-sm text-slate-600"
    >
      <LoaderCircle
        class="size-4 animate-spin motion-reduce:animate-none"
        aria-hidden="true"
      />
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

    <div v-else class="mt-5 grid gap-3">
      <div class="grid gap-1">
        <article
          v-for="station in props.stations"
          :key="station.id"
          class="overflow-hidden rounded-xl border border-l-8 border-slate-200 border-l-teal-500 bg-white"
          :style="{ borderLeftColor: getStationBrandAccent(station.brand) }"
        >
          <div class="border-b border-slate-200 bg-white px-5 py-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0">
                <h3
                  class="truncate text-2xl font-bold tracking-tight text-[#063b37]"
                >
                  {{ station.name }}
                </h3>
                <p class="mt-1 text-base text-slate-700">
                  {{ station.brand ?? props.messages.stationBrandFallback }} ·
                  {{ formatAddress(station) }}
                </p>
              </div>
              <div
                class="flex flex-wrap items-center gap-2 text-base font-semibold"
              >
                <span
                  class="rounded-full bg-teal-100 px-3 py-1.5 text-teal-900"
                >
                  {{ formatDistance(station.distanceKm) }}
                </span>
                <span
                  :class="
                    station.isOpen === true
                      ? 'bg-emerald-100 text-emerald-900'
                      : station.isOpen === false
                        ? 'bg-red-100 text-red-900'
                        : 'bg-slate-200 text-slate-700'
                  "
                  class="rounded-full px-3 py-1.5"
                >
                  {{ openStateLabel(station.isOpen) }}
                </span>
              </div>
            </div>
          </div>

          <div class="grid gap-2 px-5 py-4 sm:grid-cols-3">
            <div class="border-l border-slate-200 px-3 py-2 first:border-l-0">
              <div class="text-sm text-slate-700">E5</div>
              <p class="mt-1 text-2xl font-bold text-[#063b37]">
                {{ formatPrice(station.e5) }}
              </p>
            </div>
            <div class="border-l border-slate-200 px-3 py-2">
              <div class="text-sm text-slate-700">E10</div>
              <p class="mt-1 text-2xl font-bold text-[#063b37]">
                {{ formatPrice(station.e10) }}
              </p>
            </div>
            <div class="border-l border-slate-200 px-3 py-2">
              <div class="text-sm text-slate-700">Diesel</div>
              <p class="mt-1 text-2xl font-bold text-[#063b37]">
                {{ formatPrice(station.diesel) }}
              </p>
            </div>
          </div>
        </article>
      </div>

      <button
        v-if="props.hasMoreStations"
        class="inline-flex min-h-11 w-full items-center justify-center rounded-md border border-teal-700 bg-white px-4 text-sm font-semibold text-teal-800 transition hover:bg-teal-50 focus:ring-2 focus:ring-teal-200 focus:outline-none"
        type="button"
        @click="emit('showMoreStations')"
      >
        {{ props.messages.stationShowMore }}
      </button>

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
