import { describe, expect, it, vi } from 'vitest'

import { searchLocations } from './locationSearch'

describe('searchLocations', () => {
  it('should return typed location results when API response is valid', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(
        JSON.stringify({
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
        }),
      )
    })

    const response = await searchLocations('Berlin', 8, fetcher)

    expect(fetcher).toHaveBeenCalledWith(
      '/api/v1/locations/search?query=Berlin&limit=8',
    )
    expect(response.items[0]).toEqual({
      type: 'place',
      id: '2950159',
      label: 'Berlin, Germany',
      countryCode: 'DE',
      latitude: 52.52437,
      longitude: 13.41053,
      postalCode: null,
    })
  })

  it('should throw error when API response shape is invalid', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(JSON.stringify({ items: [{ type: 'region' }] }))
    })

    await expect(searchLocations('Berlin', 8, fetcher)).rejects.toThrow(
      'Invalid location lookup result type.',
    )
  })
})
