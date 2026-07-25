import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { LocationSearchResult } from '@/api/locationSearch'

import LocationLookupView from './LocationLookupView.vue'

const { searchGasStations, searchLocations } = vi.hoisted(() => ({
  searchLocations: vi.fn(),
  searchGasStations: vi.fn(),
}))

vi.mock('@/api/locationSearch', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/locationSearch')>()),
  searchLocations,
}))

vi.mock('@/api/gasStations', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/gasStations')>()),
  searchGasStations,
}))

const berlin: LocationSearchResult = {
  type: 'place',
  id: '2950159',
  label: 'Berlin, Germany',
  countryCode: 'DE',
  latitude: 52.52437,
  longitude: 13.41053,
  postalCode: null,
  directResolution: true,
}

const belgium: LocationSearchResult = {
  type: 'country',
  id: 'BE',
  label: 'Belgium',
  countryCode: 'BE',
  latitude: null,
  longitude: null,
  postalCode: null,
  directResolution: false,
}

const fuelStop = {
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
}

describe('LocationLookupView', () => {
  beforeEach(() => {
    localStorage.clear()
    searchLocations.mockReset()
    searchGasStations.mockReset()
  })

  it('should load prices without a second selection when the submitted place resolves directly', async () => {
    searchLocations.mockResolvedValue({ items: [berlin] })
    searchGasStations.mockResolvedValue({ items: [fuelStop] })
    localStorage.setItem('car-fuel-live.selected-country', 'BE')
    const wrapper = mount(LocationLookupView)

    await wrapper.get('#location-query').setValue('Berlin')

    expect(searchLocations).not.toHaveBeenCalled()

    await wrapper.get('form').trigger('submit')

    await vi.waitFor(() =>
      expect(searchGasStations).toHaveBeenCalledWith(52.52437, 13.41053, {
        signal: expect.any(AbortSignal),
      }),
    )
    expect(wrapper.text()).toContain('Fuel Stop')
    expect(wrapper.find('li button').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Wähle zuerst einen konkreten Ort.')
    expect(localStorage.getItem('car-fuel-live.selected-country')).toBe('DE')
  })

  it('should keep ambiguous matches selectable without loading prices', async () => {
    searchLocations.mockResolvedValue({
      items: [
        { ...berlin, directResolution: false },
        {
          ...berlin,
          id: '3169070',
          label: 'Berlin, El Salvador',
          countryCode: 'SV',
          latitude: 13.5,
          longitude: -88.5,
          directResolution: false,
        },
      ],
    })
    const wrapper = mount(LocationLookupView)

    await wrapper.get('#location-query').setValue('Berlin')
    await wrapper.get('form').trigger('submit')

    await vi.waitFor(() => expect(wrapper.findAll('li button')).toHaveLength(2))
    expect(searchGasStations).not.toHaveBeenCalled()
  })

  it('should update country context without prices when a country is selected', async () => {
    searchLocations.mockResolvedValue({ items: [belgium] })
    const wrapper = mount(LocationLookupView)

    await wrapper.get('#location-query').setValue('Belgium')
    await wrapper.get('form').trigger('submit')
    await vi.waitFor(() => expect(searchLocations).toHaveBeenCalledTimes(1))
    await wrapper.get('li button').trigger('click')

    expect(searchGasStations).not.toHaveBeenCalled()
    expect(localStorage.getItem('car-fuel-live.selected-country')).toBe('BE')

    await wrapper.get('#location-query').setValue('Berlin')
    await wrapper.get('form').trigger('submit')

    await vi.waitFor(() => expect(searchLocations).toHaveBeenCalledTimes(2))
    expect(searchLocations).toHaveBeenLastCalledWith('Berlin', {
      countryCode: 'BE',
      limit: 8,
      signal: expect.any(AbortSignal),
    })
  })
})
