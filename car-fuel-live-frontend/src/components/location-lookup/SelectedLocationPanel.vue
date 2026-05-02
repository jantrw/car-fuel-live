<script setup lang="ts">
import { LocateFixed } from 'lucide-vue-next'

import type { LocationSearchResult } from '@/api/locationSearch'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'

const props = defineProps<{
  selectedResult: LocationSearchResult | null
  messages: LocationLookupMessages
}>()

function formatCoordinate(value: number | null) {
  // Five decimals are precise enough for display while avoiding noisy raw seed-data precision.
  return value === null ? null : value.toFixed(5)
}
</script>

<template>
  <section
    v-if="props.selectedResult"
    class="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-emerald-950 shadow-sm"
  >
    <div class="flex items-start gap-3">
      <span
        class="inline-flex size-9 shrink-0 items-center justify-center rounded-md bg-emerald-700 text-white"
      >
        <LocateFixed class="size-4" aria-hidden="true" />
      </span>
      <div class="min-w-0">
        <h2
          class="text-sm font-semibold tracking-wide text-emerald-700 uppercase"
        >
          {{ props.messages.selectedTitle }}
        </h2>
        <p class="mt-1 text-lg font-semibold text-emerald-950">
          {{ props.selectedResult.label }}
        </p>
        <dl
          v-if="
            props.selectedResult.latitude !== null &&
            props.selectedResult.longitude !== null
          "
          class="mt-3 grid gap-2 text-sm sm:grid-cols-2"
        >
          <div>
            <dt class="font-medium text-emerald-800">
              {{ props.messages.latitude }}
            </dt>
            <dd>{{ formatCoordinate(props.selectedResult.latitude) }}</dd>
          </div>
          <div>
            <dt class="font-medium text-emerald-800">
              {{ props.messages.longitude }}
            </dt>
            <dd>{{ formatCoordinate(props.selectedResult.longitude) }}</dd>
          </div>
        </dl>
        <p v-else class="mt-3 text-sm text-emerald-800">
          {{ props.messages.coordinatesUnavailable }}
        </p>
      </div>
    </div>
  </section>
</template>
