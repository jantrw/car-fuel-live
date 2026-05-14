import { computed, readonly, shallowRef, type Ref } from 'vue'

import {
  searchLocations,
  type LocationSearchResponse,
  type LocationSearchResult,
} from '@/api/locationSearch'
import { groupLocationSuggestions } from '@/lib/locationSuggestions'

export type LocationLookupStatus =
  | 'idle'
  | 'loading'
  | 'results'
  | 'noResults'
  | 'error'
  | 'validation'

export type LocationLookupValidationMessage = 'QUERY_TOO_SHORT'

interface UseLocationLookupOptions {
  countryCode?: Ref<string>
  search?: (
    query: string,
    options?: {
      countryCode?: string | null
      limit?: number
      signal?: AbortSignal
    },
  ) => Promise<LocationSearchResponse>
}

const MIN_QUERY_LENGTH = 2
const DEFAULT_LIMIT = 8

// The manual lookup flow is explicit again: input updates only local state, submit triggers the
// backend request, and stale visible results are cleared as soon as the query changes.
export function useLocationLookup(options: UseLocationLookupOptions = {}) {
  const search = options.search ?? searchLocations
  const activeCountryCode = options.countryCode

  const query = shallowRef('')
  const results = shallowRef<LocationSearchResult[]>([])
  const status = shallowRef<LocationLookupStatus>('idle')
  const validationMessage = shallowRef<LocationLookupValidationMessage | null>(null)
  const lastSubmittedQuery = shallowRef('')
  let currentController: AbortController | null = null

  const trimmedQuery = computed(() => query.value.trim())
  const groupedResults = computed(() => groupLocationSuggestions(results.value))
  const hasVisibleResults = computed(() => status.value === 'results')

  async function submitSearch() {
    if (status.value === 'loading') {
      return
    }

    const nextQuery = trimmedQuery.value
    lastSubmittedQuery.value = nextQuery

    if (nextQuery.length < MIN_QUERY_LENGTH) {
      results.value = []
      status.value = 'validation'
      validationMessage.value = 'QUERY_TOO_SHORT'
      return
    }

    currentController?.abort()
    const controller = new AbortController()
    currentController = controller
    status.value = 'loading'
    validationMessage.value = null

    try {
      const response = await search(nextQuery, {
        countryCode: activeCountryCode?.value ?? null,
        limit: DEFAULT_LIMIT,
        signal: controller.signal,
      })

      if (controller.signal.aborted || currentController !== controller) {
        return
      }

      results.value = response.items
      status.value = response.items.length > 0 ? 'results' : 'noResults'
    } catch {
      if (controller.signal.aborted || currentController !== controller) {
        return
      }

      results.value = []
      status.value = 'error'
    } finally {
      if (currentController === controller) {
        currentController = null
      }
    }
  }

  function updateQuery(value: string) {
    query.value = value

    if (status.value === 'idle' && validationMessage.value === null) {
      return
    }

    currentController?.abort()
    currentController = null
    results.value = []
    status.value = 'idle'
    validationMessage.value = null
  }

  function selectResult(result: LocationSearchResult) {
    currentController?.abort()
    currentController = null
    query.value = result.label
    results.value = []
    status.value = 'idle'
    validationMessage.value = null
  }

  return {
    query: readonly(query),
    groupedResults,
    status: readonly(status),
    validationMessage: readonly(validationMessage),
    hasVisibleResults,
    lastSubmittedQuery: readonly(lastSubmittedQuery),
    submitSearch,
    updateQuery,
    selectResult,
  }
}

export type UseLocationLookupReturn = ReturnType<typeof useLocationLookup>
