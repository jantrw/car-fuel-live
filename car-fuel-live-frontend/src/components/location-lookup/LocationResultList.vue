<script setup lang="ts">
import { computed } from 'vue'

import type { LocationSearchResult } from '@/api/locationSearch'
import type { LocationLookupStatus } from '@/composables/useLocationLookup'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'
import type { LocationSuggestionGroup } from '@/lib/locationSuggestions'

import LocationSearchResultDropdown, {
  type LocationSearchDropdownItem,
} from './LocationSearchResultDropdown.vue'

const props = defineProps<{
  groups: LocationSuggestionGroup[]
  status: LocationLookupStatus
  query: string
  messages: LocationLookupMessages
}>()

const emit = defineEmits<{
  selectResult: [result: LocationSearchResult]
}>()

const dropdownGroups = computed(() =>
  props.groups.map((group) => ({
    id: group.type,
    label: props.messages.suggestionGroupTitle[group.type],
    items: group.items.map((result) => ({
      id: result.id,
      label: result.label,
      context: `${result.countryCode} · ${props.messages.resultType[result.type]}`,
      type: result.type,
    })),
  })),
)

function selectDropdownItem(item: LocationSearchDropdownItem) {
  const result = props.groups
    .find((group) => group.type === item.type)
    ?.items.find((candidate) => candidate.id === item.id)

  if (result !== undefined) {
    emit('selectResult', result)
  }
}
</script>

<template>
  <section
    v-if="props.status !== 'idle' && props.status !== 'validation'"
    :class="{
      'rounded-xl border border-slate-200 bg-white p-4 shadow-sm':
        props.status !== 'results' && props.status !== 'noResults',
    }"
    aria-live="polite"
  >
    <p v-if="props.status === 'error'" class="text-sm text-red-700">
      <span class="block font-semibold">{{ props.messages.errorTitle }}</span>
      {{ props.messages.errorBody }}
    </p>

    <p v-else-if="props.status === 'loading'" class="text-sm text-slate-600">
      {{ props.messages.loading }}
    </p>

    <LocationSearchResultDropdown
      v-else
      :groups="dropdownGroups"
      :messages="props.messages"
      :query="props.query"
      @select-item="selectDropdownItem"
    />
  </section>
</template>
