import { describe, expect, it, vi } from 'vitest'

import { useLocationLookup } from './useLocationLookup'

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
})
