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
})
