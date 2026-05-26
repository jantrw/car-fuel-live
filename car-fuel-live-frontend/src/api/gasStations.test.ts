import { describe, expect, it, vi } from 'vitest'

import { searchGasStations } from './gasStations'

describe('searchGasStations', () => {
  it('should call the backend gas station endpoint with coordinates', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(
        JSON.stringify({
          items: [
            {
              id: 'station-1',
              name: 'Fuel Stop',
              brand: 'Brand',
              street: 'Main Street',
              houseNumber: '10',
              postCode: '10115',
              place: 'Berlin',
              latitude: 52.52,
              longitude: 13.41,
              distanceKm: 0.5,
              isOpen: true,
              e5: 1.759,
              e10: null,
              diesel: 1.589,
            },
          ],
        }),
      )
    })
    const controller = new AbortController()

    const response = await searchGasStations(52.52437, 13.41053, {
      signal: controller.signal,
      fetcher,
    })

    expect(fetcher).toHaveBeenCalledWith(
      '/api/v1/gas-stations?lat=52.52437&lng=13.41053',
      { signal: controller.signal },
    )
    expect(response.items[0]).toMatchObject({
      id: 'station-1',
      name: 'Fuel Stop',
      e5: 1.759,
      e10: null,
    })
  })

  it('should throw error when gas station response shape is invalid', async () => {
    const fetcher = vi.fn(async () => {
      return new Response(JSON.stringify({ items: [{ id: 'station-1' }] }))
    })

    await expect(
      searchGasStations(52.52437, 13.41053, { fetcher }),
    ).rejects.toThrow('Invalid gas station lookup name.')
  })
})
