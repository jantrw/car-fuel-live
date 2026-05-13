import { nextTick, shallowRef } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { LocationSearchResponse } from '@/api/locationSearch'

import { useLocationLookup } from './useLocationLookup'

type SuggestOptions = {
  countryCode?: string | null
  limit?: number
  signal?: AbortSignal
}

type SuggestFn = (
  query: string,
  options?: SuggestOptions,
) => Promise<LocationSearchResponse>

function createDeferredResponse<T>() {
  let resolvePromise: (value: T) => void = () => {}
  const promise = new Promise<T>((resolve) => {
    resolvePromise = resolve
  })

  return {
    promise,
    resolve(value: T) {
      resolvePromise(value)
    },
  }
}

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
}

describe('useLocationLookup', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should request suggestions with debounce and optional country context', async () => {
    const countryCode = shallowRef('DE')
    const suggest = vi.fn<SuggestFn>(async () => ({
      items: [
        {
          type: 'place' as const,
          id: '2950159',
          label: 'Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.52437,
          longitude: 13.41053,
          postalCode: null,
        },
      ],
    }))
    const lookup = useLocationLookup({ countryCode, suggest })

    lookup.focusInput()
    lookup.updateQuery('Be')
    await nextTick()

    expect(suggest).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(250)

    expect(suggest).toHaveBeenCalledTimes(1)
    const [calledQuery, calledOptions] = suggest.mock.calls[0] as [
      string,
      SuggestOptions,
    ]

    expect(calledQuery).toBe('Be')
    expect(calledOptions).toMatchObject({
      countryCode: 'DE',
      limit: 8,
    })
    expect(calledOptions.signal).toBeInstanceOf(AbortSignal)
    expect(lookup.status.value).toBe('results')
    expect(lookup.groupedResults.value).toHaveLength(1)
    expect(lookup.groupedResults.value[0].type).toBe('place')
  })

  it('should not request suggestions while the query is shorter than the autocomplete minimum', async () => {
    const suggest = vi.fn<SuggestFn>(async () => ({ items: [] }))
    const lookup = useLocationLookup({ suggest })

    lookup.focusInput()
    lookup.updateQuery('B')
    await nextTick()
    await vi.advanceTimersByTimeAsync(250)

    expect(suggest).not.toHaveBeenCalled()
    expect(lookup.status.value).toBe('idle')
  })

  it('should abort stale in-flight requests when a newer query replaces them', async () => {
    const firstResponse = createDeferredResponse<LocationSearchResponse>()
    const secondResponse = createDeferredResponse<LocationSearchResponse>()
    const signals: AbortSignal[] = []
    const suggest = vi
      .fn<SuggestFn>()
      .mockImplementationOnce((_query, options) => {
        signals.push(options?.signal as AbortSignal)
        return firstResponse.promise
      })
      .mockImplementationOnce((_query, options) => {
        signals.push(options?.signal as AbortSignal)
        return secondResponse.promise
      })
    const lookup = useLocationLookup({ suggest })

    lookup.focusInput()
    lookup.updateQuery('Be')
    await nextTick()
    await vi.advanceTimersByTimeAsync(250)

    expect(suggest).toHaveBeenCalledTimes(1)
    expect(signals[0].aborted).toBe(false)

    lookup.updateQuery('Ber')
    await nextTick()

    expect(signals[0].aborted).toBe(true)

    await vi.advanceTimersByTimeAsync(250)
    expect(suggest).toHaveBeenCalledTimes(2)

    firstResponse.resolve({
      items: [
        {
          type: 'country',
          id: 'BE',
          label: 'Belgium',
          countryCode: 'BE',
          latitude: null,
          longitude: null,
          postalCode: null,
        },
      ],
    })
    secondResponse.resolve({
      items: [
        {
          type: 'place',
          id: '2950159',
          label: 'Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.52437,
          longitude: 13.41053,
          postalCode: null,
        },
      ],
    })
    await flushPromises()

    expect(lookup.status.value).toBe('results')
    expect(lookup.groupedResults.value[0].items[0].id).toBe('2950159')
  })

  it('should expose grouped keyboard navigation in rendered suggestion order', async () => {
    const suggest = vi.fn<SuggestFn>(async () => ({
      items: [
        {
          type: 'country' as const,
          id: 'BE',
          label: 'Belgium',
          countryCode: 'BE',
          latitude: null,
          longitude: null,
          postalCode: null,
        },
        {
          type: 'place' as const,
          id: '2950159',
          label: 'Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.52437,
          longitude: 13.41053,
          postalCode: null,
        },
      ],
    }))
    const lookup = useLocationLookup({ suggest })

    lookup.focusInput()
    lookup.updateQuery('Be')
    await nextTick()
    await vi.advanceTimersByTimeAsync(250)

    lookup.moveHighlightNext()
    expect(lookup.activeResult.value?.id).toBe('2950159')

    lookup.moveHighlightNext()
    expect(lookup.activeResult.value?.id).toBe('BE')

    lookup.moveHighlightPrevious()
    expect(lookup.activeResult.value?.id).toBe('2950159')
  })

  it('should close the autocomplete and update the input when enter confirms the active suggestion', async () => {
    const suggest = vi.fn<SuggestFn>(async () => ({
      items: [
        {
          type: 'place' as const,
          id: '2950159',
          label: 'Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.52437,
          longitude: 13.41053,
          postalCode: null,
        },
      ],
    }))
    const lookup = useLocationLookup({ suggest })

    lookup.focusInput()
    lookup.updateQuery('Be')
    await nextTick()
    await vi.advanceTimersByTimeAsync(250)
    lookup.moveHighlightNext()

    lookup.confirmHighlightedResult()

    expect(lookup.query.value).toBe('Berlin, Germany')
    expect(lookup.status.value).toBe('idle')
    expect(lookup.isAutocompleteOpen.value).toBe(false)
    expect(lookup.groupedResults.value).toEqual([])
  })

  it('should preserve pointer selection when blur fires before the suggestion click completes', async () => {
    const suggest = vi.fn<SuggestFn>(async () => ({
      items: [
        {
          type: 'postalCode' as const,
          id: 'DE-10115-Berlin',
          label: '10115 Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.532,
          longitude: 13.3849,
          postalCode: '10115',
        },
      ],
    }))
    const lookup = useLocationLookup({ suggest })

    lookup.focusInput()
    lookup.updateQuery('10115')
    await nextTick()
    await vi.advanceTimersByTimeAsync(250)

    lookup.markPointerSelectionStart()
    lookup.blurInput()
    lookup.selectResult(lookup.groupedResults.value[0].items[0])

    expect(lookup.query.value).toBe('10115 Berlin, Germany')
    expect(lookup.isAutocompleteOpen.value).toBe(false)
    expect(lookup.status.value).toBe('idle')
  })
})
