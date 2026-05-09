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

const MIN_QUERY_LENGTH = 2
const QUERY_TOO_SHORT_ERROR = 'LOCATION_LOOKUP_QUERY_TOO_SHORT'

// Allow tests to inject the lookup dependency while production keeps the real API client. Keep
// only raw source state here; trimmed input and canSearch stay derived.
export function useLocationLookup(options: UseLocationLookupOptions = {}) {
  const lookup = options.search ?? searchLocations
  const query = shallowRef('')
  const results = shallowRef<LocationSearchResult[]>([])
  const selectedResult = shallowRef<LocationSearchResult | null>(null)
  const status = shallowRef<LocationLookupStatus>('idle')
  const errorMessage = shallowRef<string | null>(null)
  const hasSubmittedTooShortQuery = shallowRef(false)

  const trimmedQuery = computed(() => query.value.trim())
  const queryValidationMessage = computed(() => {
    if (
      !hasSubmittedTooShortQuery.value ||
      trimmedQuery.value.length === 0 ||
      trimmedQuery.value.length >= MIN_QUERY_LENGTH
    ) {
      return null
    }

    return QUERY_TOO_SHORT_ERROR
  })
  const canSearch = computed(
    () => trimmedQuery.value.length > 0 && status.value !== 'loading',
  )

  // Ignore blank or duplicate submits, then reset the previous selection before the next result
  // set replaces it.
  async function search() {
    if (trimmedQuery.value.length === 0) {
      return
    }

    if (trimmedQuery.value.length < MIN_QUERY_LENGTH) {
      hasSubmittedTooShortQuery.value = true
      results.value = []
      selectedResult.value = null
      status.value = 'idle'
      errorMessage.value = QUERY_TOO_SHORT_ERROR
      return
    }

    hasSubmittedTooShortQuery.value = false
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

  function updateQuery(value: string) {
    query.value = value
    hasSubmittedTooShortQuery.value = false
    if (errorMessage.value === QUERY_TOO_SHORT_ERROR) {
      errorMessage.value = null
    }
  }

  return {
    query,
    results,
    selectedResult,
    status,
    errorMessage,
    queryValidationMessage,
    canSearch,
    search,
    selectResult,
    updateQuery,
  }
}
