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
const MAX_QUERY_LENGTH = 80
const QUERY_TOO_SHORT_ERROR = 'LOCATION_LOOKUP_QUERY_TOO_SHORT'
const QUERY_TOO_LONG_ERROR = 'LOCATION_LOOKUP_QUERY_TOO_LONG'

// Allow tests to inject the lookup dependency while production keeps the real API client. Keep
// only raw source state here; trimmed input and canSearch stay derived.
export function useLocationLookup(options: UseLocationLookupOptions = {}) {
  const lookup = options.search ?? searchLocations
  const query = shallowRef('')
  const results = shallowRef<LocationSearchResult[]>([])
  const selectedResult = shallowRef<LocationSearchResult | null>(null)
  const status = shallowRef<LocationLookupStatus>('idle')
  const errorMessage = shallowRef<string | null>(null)
  const hasSubmittedInvalidQuery = shallowRef(false)
  let latestSearchRequestId = 0

  const trimmedQuery = computed(() => query.value.trim())
  const queryValidationMessage = computed(() => {
    if (!hasSubmittedInvalidQuery.value || trimmedQuery.value.length === 0) {
      return null
    }

    return validateQuery(trimmedQuery.value)
  })
  const canSearch = computed(
    () => trimmedQuery.value.length > 0 && status.value !== 'loading',
  )

  // Ignore blank or duplicate submits, then reset the previous selection before the next result
  // set replaces it.
  async function search() {
    if (status.value === 'loading') {
      return
    }

    if (trimmedQuery.value.length === 0) {
      return
    }

    const validationError = validateQuery(trimmedQuery.value)
    if (validationError !== null) {
      hasSubmittedInvalidQuery.value = true
      results.value = []
      selectedResult.value = null
      status.value = 'idle'
      errorMessage.value = validationError
      return
    }

    const requestId = ++latestSearchRequestId
    hasSubmittedInvalidQuery.value = false
    results.value = []
    status.value = 'loading'
    errorMessage.value = null
    selectedResult.value = null

    try {
      const response = await lookup(trimmedQuery.value, 8)
      if (requestId !== latestSearchRequestId) {
        return
      }

      results.value = response.items
      status.value = response.items.length > 0 ? 'results' : 'noResults'
    } catch {
      if (requestId !== latestSearchRequestId) {
        return
      }

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
    latestSearchRequestId += 1
    results.value = []
    selectedResult.value = null
    status.value = 'idle'
    errorMessage.value = null
    hasSubmittedInvalidQuery.value = false
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

function validateQuery(query: string): string | null {
  if (query.length < MIN_QUERY_LENGTH) {
    return QUERY_TOO_SHORT_ERROR
  }

  if (query.length > MAX_QUERY_LENGTH) {
    return QUERY_TOO_LONG_ERROR
  }

  return null
}
