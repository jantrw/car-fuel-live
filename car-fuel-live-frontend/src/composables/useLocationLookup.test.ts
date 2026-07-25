import { shallowRef } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import type { LocationSearchResponse } from '@/api/locationSearch'

import { useLocationLookup } from './useLocationLookup'

type SearchOptions = {
  countryCode?: string | null
  limit?: number
  signal?: AbortSignal
}

type SearchFn = (
  query: string,
  options?: SearchOptions,
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
  it('should request results only after an explicit submit with country context', async () => {
    const countryCode = shallowRef('DE')
    const search = vi.fn<SearchFn>(async () => ({
      items: [
        {
          type: 'place' as const,
          id: '2950159',
          label: 'Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.52437,
          longitude: 13.41053,
          postalCode: null,
          directResolution: false,
        },
      ],
    }))
    const lookup = useLocationLookup({ countryCode, search })

    lookup.updateQuery('Ber')

    expect(search).not.toHaveBeenCalled()

    await lookup.submitSearch()

    expect(search).toHaveBeenCalledTimes(1)
    const [calledQuery, calledOptions] = search.mock.calls[0] as [
      string,
      SearchOptions,
    ]

    expect(calledQuery).toBe('Ber')
    expect(calledOptions).toMatchObject({
      countryCode: 'DE',
      limit: 8,
    })
    expect(calledOptions.signal).toBeInstanceOf(AbortSignal)
    expect(lookup.status.value).toBe('results')
    expect(lookup.groupedResults.value).toHaveLength(1)
    expect(lookup.groupedResults.value[0].type).toBe('place')
  })

  it('should show submit-time validation when the trimmed query is too short', async () => {
    const search = vi.fn<SearchFn>(async () => ({ items: [] }))
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('B')
    await lookup.submitSearch()

    expect(search).not.toHaveBeenCalled()
    expect(lookup.status.value).toBe('validation')
    expect(lookup.validationMessage.value).toBe('QUERY_TOO_SHORT')
  })

  it('should show submit-time validation when the trimmed query is too long', async () => {
    const search = vi.fn<SearchFn>(async () => ({ items: [] }))
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('a'.repeat(81))
    await lookup.submitSearch()

    expect(search).not.toHaveBeenCalled()
    expect(lookup.status.value).toBe('validation')
    expect(lookup.validationMessage.value).toBe('QUERY_TOO_LONG')
  })

  it('should clear previous visible state when the user edits the query after a search', async () => {
    const search = vi.fn<SearchFn>(async () => ({
      items: [
        {
          type: 'place' as const,
          id: '2950159',
          label: 'Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.52437,
          longitude: 13.41053,
          postalCode: null,
          directResolution: false,
        },
      ],
    }))
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('Ber')
    await lookup.submitSearch()

    expect(lookup.status.value).toBe('results')

    lookup.updateQuery('Bern')

    expect(lookup.status.value).toBe('idle')
    expect(lookup.groupedResults.value).toEqual([])
    expect(lookup.validationMessage.value).toBeNull()
  })

  it('should ignore duplicate submits while a request is already loading', async () => {
    const deferredResponse = createDeferredResponse<LocationSearchResponse>()
    const search = vi.fn<SearchFn>(() => deferredResponse.promise)
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('Ber')
    const firstSubmit = lookup.submitSearch()
    const secondSubmit = lookup.submitSearch()

    deferredResponse.resolve({ items: [] })
    await Promise.all([firstSubmit, secondSubmit])

    expect(search).toHaveBeenCalledTimes(1)
    expect(lookup.status.value).toBe('noResults')
  })

  it('should ignore an aborted earlier request when a newer submit starts', async () => {
    const firstResponse = createDeferredResponse<LocationSearchResponse>()
    const secondResponse = createDeferredResponse<LocationSearchResponse>()
    const signals: AbortSignal[] = []
    const search = vi
      .fn<SearchFn>()
      .mockImplementationOnce((_query, options) => {
        signals.push(options?.signal as AbortSignal)
        return firstResponse.promise
      })
      .mockImplementationOnce((_query, options) => {
        signals.push(options?.signal as AbortSignal)
        return secondResponse.promise
      })
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('Ber')
    const firstSubmit = lookup.submitSearch()

    lookup.updateQuery('Berlin')
    const secondSubmit = lookup.submitSearch()

    expect(signals[0].aborted).toBe(true)

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
          directResolution: false,
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
          directResolution: false,
        },
      ],
    })

    await Promise.all([firstSubmit, secondSubmit])
    await flushPromises()

    expect(lookup.status.value).toBe('results')
    expect(lookup.groupedResults.value[0].items[0].id).toBe('2950159')
  })

  it('should fill the input and clear visible results when a result is selected', async () => {
    const search = vi.fn<SearchFn>(async () => ({
      items: [
        {
          type: 'postalCode' as const,
          id: 'DE-10115-Berlin',
          label: '10115 Berlin, Germany',
          countryCode: 'DE',
          latitude: 52.532,
          longitude: 13.3849,
          postalCode: '10115',
          directResolution: false,
        },
      ],
    }))
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('10115')
    await lookup.submitSearch()
    lookup.selectResult(lookup.groupedResults.value[0].items[0])

    expect(lookup.query.value).toBe('10115 Berlin, Germany')
    expect(lookup.status.value).toBe('idle')
    expect(lookup.groupedResults.value).toEqual([])
  })
})
