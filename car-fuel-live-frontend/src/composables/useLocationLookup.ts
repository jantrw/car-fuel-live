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

export function useLocationLookup(options: UseLocationLookupOptions = {}) {
  // Tests can inject the lookup function while production keeps the real API client.
  const lookup = options.search ?? searchLocations
  // Source state stays minimal; derived values such as trimmed query and canSearch stay computed.
  const query = shallowRef('')
  const results = shallowRef<LocationSearchResult[]>([])
  const selectedResult = shallowRef<LocationSearchResult | null>(null)
  const status = shallowRef<LocationLookupStatus>('idle')
  const errorMessage = shallowRef<string | null>(null)

  const trimmedQuery = computed(() => query.value.trim())
  const canSearch = computed(
    () => trimmedQuery.value.length > 0 && status.value !== 'loading',
  )

  async function search() {
    // Ignore duplicate submits while loading and blank submits after trimming.
    if (!canSearch.value) {
      return
    }

    // A new search invalidates the previous selection because coordinates belong to the old result
    // set.
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

  function selectResult(result: LocationSearchResult) {
    // Selection is view-local for this MVP; raw coordinates must not be persisted.
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
