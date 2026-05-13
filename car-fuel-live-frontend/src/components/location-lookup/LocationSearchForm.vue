<script setup lang="ts">
import { LoaderCircle, Search } from 'lucide-vue-next'

import { Button } from '@/components/ui/button'
import type { LocationLookupMessages } from '@/i18n/locationLookupMessages'

const props = defineProps<{
  query: string
  canSearch: boolean
  isLoading: boolean
  validationMessage: string | null
  messages: LocationLookupMessages
}>()

const emit = defineEmits<{
  updateQuery: [value: string]
  search: []
}>()

function onInput(event: Event) {
  emit('updateQuery', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <form
    class="grid gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
    @submit.prevent="emit('search')"
  >
    <label class="text-sm font-semibold text-slate-900" for="location-query">
      {{ props.messages.searchLabel }}
    </label>
    <div class="flex flex-col gap-3 sm:flex-row">
      <input
        id="location-query"
        :value="props.query"
        :placeholder="props.messages.searchPlaceholder"
        :aria-invalid="props.validationMessage !== null"
        :aria-describedby="
          props.validationMessage !== null ? 'location-query-validation' : undefined
        "
        class="min-h-11 flex-1 rounded-md border border-slate-300 bg-white px-3 text-base text-slate-950 transition outline-none focus:border-sky-600 focus:ring-2 focus:ring-sky-100 aria-[invalid=true]:border-red-500 aria-[invalid=true]:focus:border-red-600 aria-[invalid=true]:focus:ring-red-100"
        type="search"
        @input="onInput"
      />
      <Button
        class="min-h-11 bg-sky-700 text-white hover:bg-sky-800"
        :disabled="!props.canSearch"
        type="submit"
      >
        <LoaderCircle v-if="props.isLoading" class="animate-spin" />
        <Search v-else />
        {{
          props.isLoading ? props.messages.loading : props.messages.searchButton
        }}
      </Button>
    </div>
    <p
      v-if="props.validationMessage !== null"
      id="location-query-validation"
      class="text-sm font-medium text-red-700"
    >
      {{ props.validationMessage }}
    </p>
  </form>
</template>
