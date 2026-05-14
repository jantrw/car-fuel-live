import { describe, expect, it, vi } from 'vitest'

import { searchLocations } from './locationSearch'

describe('searchLocations', () => {
  it('should call the backend search endpoint with country context when provided', async () => {
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
    const controller = new AbortController()

    const response = await searchLocations('Berlin', {
      countryCode: ' de ',
      signal: controller.signal,
      fetcher,
    })

    expect(fetcher).toHaveBeenCalledWith(
      '/api/v1/locations/suggestions?q=Berlin&limit=8&countryCode=DE',
      { signal: controller.signal },
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

  it('should omit blank country context from explicit search requests', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(JSON.stringify({ items: [] }))
    })

    await searchLocations('Belgium', {
      countryCode: ' ',
      fetcher,
    })

    expect(fetcher).toHaveBeenCalledWith(
      '/api/v1/locations/suggestions?q=Belgium&limit=8',
      { signal: undefined },
    )
  })

  it('should throw error when API response shape is invalid', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(JSON.stringify({ items: [{ type: 'region' }] }))
    })

    await expect(
      searchLocations('Berlin', { fetcher }),
    ).rejects.toThrow('Invalid location lookup result type.')
  })
})
