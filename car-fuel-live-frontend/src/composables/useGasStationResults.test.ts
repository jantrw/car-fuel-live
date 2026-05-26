import { describe, expect, it, vi } from 'vitest'

import { GasStationLookupError } from '@/api/gasStations'

import { useGasStationResults } from './useGasStationResults'

describe('useGasStationResults', () => {
  it('should load gas stations when a place with coordinates is selected', async () => {
    const search = vi.fn(async () => ({
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
    }))
    const gasStationResults = useGasStationResults({ search })

    await gasStationResults.loadForSelection({
      type: 'place',
      id: '2950159',
      label: 'Berlin, Germany',
      countryCode: 'DE',
      latitude: 52.52437,
      longitude: 13.41053,
      postalCode: null,
    })

    expect(search).toHaveBeenCalledWith(52.52437, 13.41053, {
      signal: expect.any(AbortSignal),
    })
    expect(gasStationResults.status.value).toBe('results')
    expect(gasStationResults.stations.value[0].id).toBe('station-1')
  })

  it('should not trigger a gas station request when a country is selected', async () => {
    const search = vi.fn(async () => ({ items: [] }))
    const gasStationResults = useGasStationResults({ search })

    await gasStationResults.loadForSelection({
      type: 'country',
      id: 'DE',
      label: 'Germany',
      countryCode: 'DE',
      latitude: null,
      longitude: null,
      postalCode: null,
    })

    expect(search).not.toHaveBeenCalled()
    expect(gasStationResults.status.value).toBe('countrySelected')
    expect(gasStationResults.selectedLocationLabel.value).toBe('Germany')
  })

  it('should clear visible gas station state when the results are reset', async () => {
    const search = vi.fn(async () => ({
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
    }))
    const gasStationResults = useGasStationResults({ search })

    await gasStationResults.loadForSelection({
      type: 'place',
      id: '2950159',
      label: 'Berlin, Germany',
      countryCode: 'DE',
      latitude: 52.52437,
      longitude: 13.41053,
      postalCode: null,
    })

    gasStationResults.reset()

    expect(gasStationResults.status.value).toBe('idle')
    expect(gasStationResults.stations.value).toEqual([])
    expect(gasStationResults.selectedLocationLabel.value).toBe('')
  })

  it('should expose a dedicated rate-limited state for backend 429 responses', async () => {
    const search = vi.fn(async () => {
      throw new GasStationLookupError(
        'Too many fuel price requests from this client.',
        429,
        'RATE_LIMITED',
      )
    })
    const gasStationResults = useGasStationResults({ search })

    await gasStationResults.loadForSelection({
      type: 'place',
      id: '2950159',
      label: 'Berlin, Germany',
      countryCode: 'DE',
      latitude: 52.52437,
      longitude: 13.41053,
      postalCode: null,
    })

    expect(gasStationResults.status.value).toBe('rateLimited')
    expect(gasStationResults.stations.value).toEqual([])
  })
})
