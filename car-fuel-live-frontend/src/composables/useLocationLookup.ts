import { computed, shallowRef } from 'vue'

import {
  searchLocations,
  type LocationSearchResponse,
  type LocationSearchResult,
} from '@/api/locationSearch'

export type LocationLookupStatus =
  | 'idle'
  | 'loading'
  | 'results'
  | 'noResults'
  | 'error'

interface UseLocationLookupOptions {
  search?: (query: string, limit?: number) => Promise<LocationSearchResponse>
}

// Allow tests to inject the lookup dependency while production keeps the real API client. Keep
// only raw source state here; trimmed input and canSearch stay derived.
export function useLocationLookup(options: UseLocationLookupOptions = {}) {
  const lookup = options.search ?? searchLocations
  const query = shallowRef('')
  const results = shallowRef<LocationSearchResult[]>([])
  const selectedResult = shallowRef<LocationSearchResult | null>(null)
  const status = shallowRef<LocationLookupStatus>('idle')
  const errorMessage = shallowRef<string | null>(null)

  const trimmedQuery = computed(() => query.value.trim())
  const canSearch = computed(
    () => trimmedQuery.value.length > 0 && status.value !== 'loading',
  )

  // Ignore blank or duplicate submits, then reset the previous selection before the next result
  // set replaces it.
  async function search() {
    if (!canSearch.value) {
      return
    }

    status.value = 'loading'
    errorMessage.value = null
    selectedResult.value = null

    try {
      const response = await lookup(trimmedQuery.value, 8)
      results.value = response.items
      status.value = response.items.length > 0 ? 'results' : 'noResults'
    } catch {
      // UI copy is localized by state, not by raw backend or network error text.
      results.value = []
      status.value = 'error'
      errorMessage.value = 'LOCATION_LOOKUP_FAILED'
    }
  }

  // Selection is view-local for this MVP; raw coordinates must not be persisted.
  function selectResult(result: LocationSearchResult) {
    selectedResult.value = result
  }

  return {
    query,
    results,
    selectedResult,
    status,
    errorMessage,
    canSearch,
    search,
    selectResult,
  }
}
