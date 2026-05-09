import { describe, expect, it, vi } from 'vitest'

import type { LocationSearchResponse } from '@/api/locationSearch'

import { useLocationLookup } from './useLocationLookup'

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

describe('useLocationLookup', () => {
  it('should select result when search returns a matching location', async () => {
    const berlin = {
      type: 'place' as const,
      id: '2950159',
      label: 'Berlin, Germany',
      countryCode: 'DE',
      latitude: 52.52437,
      longitude: 13.41053,
      postalCode: null,
    }
    const search = vi.fn(async () => ({ items: [berlin] }))
    const lookup = useLocationLookup({ search })

    lookup.query.value = 'Berlin'
    await lookup.search()
    lookup.selectResult(lookup.results.value[0])

    expect(search).toHaveBeenCalledWith('Berlin', 8)
    expect(lookup.status.value).toBe('results')
    expect(lookup.selectedResult.value).toEqual(berlin)
  })

  it('should show no results when search returns empty items', async () => {
    const search = vi.fn(async () => ({ items: [] }))
    const lookup = useLocationLookup({ search })

    lookup.query.value = 'Missing'
    await lookup.search()

    expect(lookup.status.value).toBe('noResults')
    expect(lookup.results.value).toEqual([])
    expect(lookup.errorMessage.value).toBeNull()
  })

  it('should allow submit attempt and expose validation message only after a too-short query is searched', async () => {
    const search = vi.fn(async () => ({ items: [] }))
    const lookup = useLocationLookup({ search })

    lookup.query.value = '1'

    expect(lookup.canSearch.value).toBe(true)
    expect(lookup.queryValidationMessage.value).toBeNull()

    await lookup.search()

    expect(search).not.toHaveBeenCalled()
    expect(lookup.queryValidationMessage.value).toBe(
      'LOCATION_LOOKUP_QUERY_TOO_SHORT',
    )
    expect(lookup.errorMessage.value).toBe('LOCATION_LOOKUP_QUERY_TOO_SHORT')
    expect(lookup.status.value).toBe('idle')
  })

  it('should clear the short-query validation once the input becomes valid', async () => {
    const search = vi.fn(async () => ({ items: [] }))
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('B')
    await lookup.search()
    lookup.updateQuery('Be')

    expect(lookup.queryValidationMessage.value).toBeNull()
    expect(lookup.canSearch.value).toBe(true)
  })

  it('should hide the short-query validation again while the user continues typing after a failed submit', async () => {
    const search = vi.fn(async () => ({ items: [] }))
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('B')
    await lookup.search()

    expect(lookup.queryValidationMessage.value).toBe(
      'LOCATION_LOOKUP_QUERY_TOO_SHORT',
    )

    lookup.updateQuery('Be')

    expect(lookup.queryValidationMessage.value).toBeNull()
    expect(lookup.errorMessage.value).toBeNull()
    expect(lookup.canSearch.value).toBe(true)
  })

  it('should ignore stale search responses when a newer request finishes first', async () => {
    const firstResponse = createDeferredResponse({
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
    })
    const secondResponse = createDeferredResponse({
      items: [
        {
          type: 'place' as const,
          id: '2950150',
          label: 'Bern, Switzerland',
          countryCode: 'CH',
          latitude: 46.94809,
          longitude: 7.44744,
          postalCode: null,
        },
      ],
    })
    const search = vi
      .fn<(query: string, limit?: number) => Promise<LocationSearchResponse>>()
      .mockImplementationOnce(() => firstResponse.promise)
      .mockImplementationOnce(() => secondResponse.promise)
    const lookup = useLocationLookup({ search })

    lookup.updateQuery('Berlin')
    const firstSearch = lookup.search()
    lookup.updateQuery('Bern')
    const secondSearch = lookup.search()

    secondResponse.resolve({
      items: [
        {
          type: 'place',
          id: '2950150',
          label: 'Bern, Switzerland',
          countryCode: 'CH',
          latitude: 46.94809,
          longitude: 7.44744,
          postalCode: null,
        },
      ],
    })
    await secondSearch

    firstResponse.resolve({
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
    await firstSearch

    expect(search).toHaveBeenNthCalledWith(1, 'Berlin', 8)
    expect(search).toHaveBeenNthCalledWith(2, 'Bern', 8)
    expect(lookup.status.value).toBe('results')
    expect(lookup.results.value).toEqual([
      {
        type: 'place',
        id: '2950150',
        label: 'Bern, Switzerland',
        countryCode: 'CH',
        latitude: 46.94809,
        longitude: 7.44744,
        postalCode: null,
      },
    ])
  })
})
