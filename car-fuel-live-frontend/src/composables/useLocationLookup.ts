import {
  computed,
  readonly,
  shallowRef,
  type ComputedRef,
  type Ref,
  watch,
} from 'vue'

import {
  suggestLocations,
  type LocationSearchResponse,
  type LocationSearchResult,
} from '@/api/locationSearch'
import {
  flattenLocationSuggestionGroups,
  groupLocationSuggestions,
} from '@/lib/locationSuggestions'

export type LocationLookupStatus =
  | 'idle'
  | 'loading'
  | 'results'
  | 'noResults'
  | 'error'

interface UseLocationLookupOptions {
  countryCode?: Ref<string>
  debounceMs?: number
  suggest?: (
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
const DEFAULT_DEBOUNCE_MS = 250

// The lookup flow is now autocomplete-driven. Keep source state minimal, derive grouped output,
// and let the watcher own debounce and request cancellation so stale requests never leak through.
export function useLocationLookup(options: UseLocationLookupOptions = {}) {
  const suggest = options.suggest ?? suggestLocations
  const debounceMs = options.debounceMs ?? DEFAULT_DEBOUNCE_MS
  const activeCountryCode = options.countryCode

  const query = shallowRef('')
  const results = shallowRef<LocationSearchResult[]>([])
  const status = shallowRef<LocationLookupStatus>('idle')
  const isInputFocused = shallowRef(false)
  const highlightedIndex = shallowRef(-1)
  const pointerSelecting = shallowRef(false)
  let skipNextLookup = false

  const trimmedQuery = computed(() => query.value.trim())
  const groupedResults = computed(() => groupLocationSuggestions(results.value))
  const visibleResults = computed(() =>
    flattenLocationSuggestionGroups(groupedResults.value),
  )
  const activeResult = computed(() =>
    highlightedIndex.value < 0
      ? null
      : visibleResults.value[highlightedIndex.value] ?? null,
  )
  const isAutocompleteOpen = computed(
    () =>
      isInputFocused.value &&
      (status.value === 'loading' ||
        status.value === 'results' ||
        status.value === 'noResults' ||
        status.value === 'error'),
  )

  watch(
    [trimmedQuery, activeCountryCode ?? computed(() => '')],
    ([nextQuery, nextCountryCode], _previous, onCleanup) => {
      if (skipNextLookup) {
        skipNextLookup = false
        return
      }

      highlightedIndex.value = -1

      if (nextQuery.length < MIN_QUERY_LENGTH) {
        results.value = []
        status.value = 'idle'
        return
      }

      const controller = new AbortController()
      const timeoutId = globalThis.setTimeout(async () => {
        status.value = 'loading'

        try {
          const response = await suggest(nextQuery, {
            countryCode: nextCountryCode,
            limit: DEFAULT_LIMIT,
            signal: controller.signal,
          })

          if (controller.signal.aborted) {
            return
          }

          results.value = response.items
          status.value = response.items.length > 0 ? 'results' : 'noResults'
        } catch {
          if (controller.signal.aborted) {
            return
          }

          results.value = []
          status.value = 'error'
        }
      }, debounceMs)

      onCleanup(() => {
        globalThis.clearTimeout(timeoutId)
        controller.abort()
      })
    },
  )

  function updateQuery(value: string) {
    query.value = value
    isInputFocused.value = true
  }

  function focusInput() {
    isInputFocused.value = true
  }

  function blurInput() {
    if (pointerSelecting.value) {
      return
    }

    isInputFocused.value = false
    highlightedIndex.value = -1
  }

  function moveHighlightNext() {
    if (visibleResults.value.length === 0) {
      return
    }

    highlightedIndex.value =
      highlightedIndex.value >= visibleResults.value.length - 1
        ? 0
        : highlightedIndex.value + 1
  }

  function moveHighlightPrevious() {
    if (visibleResults.value.length === 0) {
      return
    }

    highlightedIndex.value =
      highlightedIndex.value <= 0
        ? visibleResults.value.length - 1
        : highlightedIndex.value - 1
  }

  function confirmHighlightedResult() {
    if (activeResult.value === null) {
      return
    }

    selectResult(activeResult.value)
  }

  function closeAutocomplete() {
    isInputFocused.value = false
    highlightedIndex.value = -1
  }

  function markPointerSelectionStart() {
    pointerSelecting.value = true
  }

  function selectResult(result: LocationSearchResult) {
    pointerSelecting.value = false
    skipNextLookup = true
    query.value = result.label
    results.value = []
    status.value = 'idle'
    isInputFocused.value = false
    highlightedIndex.value = -1
  }

  function cancelPointerSelection() {
    pointerSelecting.value = false
  }

  return {
    query: readonly(query),
    groupedResults,
    status: readonly(status),
    isAutocompleteOpen,
    highlightedIndex: readonly(highlightedIndex),
    activeResult,
    updateQuery,
    focusInput,
    blurInput,
    moveHighlightNext,
    moveHighlightPrevious,
    confirmHighlightedResult,
    closeAutocomplete,
    markPointerSelectionStart,
    cancelPointerSelection,
    selectResult,
  }
}

export type UseLocationLookupReturn = ReturnType<typeof useLocationLookup>
export type LocationSuggestionGroup = ComputedRef<
  ReturnType<typeof groupLocationSuggestions>
>
