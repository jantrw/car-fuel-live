import { describe, expect, it, vi } from 'vitest'

import { suggestLocations } from './locationSearch'

describe('suggestLocations', () => {
  it('should call the suggestions API with country context when provided', async () => {
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

    const response = await suggestLocations('Berlin', {
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

  it('should omit blank country context from suggestions requests', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(JSON.stringify({ items: [] }))
    })

    await suggestLocations('Belgium', {
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
      suggestLocations('Berlin', { fetcher }),
    ).rejects.toThrow('Invalid location lookup result type.')
  })
})
