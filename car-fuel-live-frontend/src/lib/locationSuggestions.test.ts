import { describe, expect, it } from 'vitest'

import {
  flattenLocationSuggestionGroups,
  groupLocationSuggestions,
} from './locationSuggestions'

describe('locationSuggestions', () => {
  it('should group flat backend results by visible suggestion section order', () => {
    const results = [
      {
        type: 'country' as const,
        id: 'BE',
        label: 'Belgium',
        countryCode: 'BE',
        latitude: null,
        longitude: null,
        postalCode: null,
      },
      {
        type: 'place' as const,
        id: '2950159',
        label: 'Berlin, Germany',
        countryCode: 'DE',
        latitude: 52.52437,
        longitude: 13.41053,
        postalCode: null,
      },
      {
        type: 'postalCode' as const,
        id: 'DE-10115-Berlin',
        label: '10115 Berlin, Germany',
        countryCode: 'DE',
        latitude: 52.532,
        longitude: 13.3849,
        postalCode: '10115',
      },
    ]

    const groups = groupLocationSuggestions(results)

    expect(groups.map((group) => group.type)).toEqual([
      'place',
      'country',
      'postalCode',
    ])
    expect(groups[0].items[0].id).toBe('2950159')
    expect(groups[1].items[0].id).toBe('BE')
    expect(groups[2].items[0].id).toBe('DE-10115-Berlin')
  })

  it('should flatten grouped suggestions back into keyboard navigation order', () => {
    const groups = [
      {
        type: 'place' as const,
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
      },
      {
        type: 'country' as const,
        items: [
          {
            type: 'country' as const,
            id: 'BE',
            label: 'Belgium',
            countryCode: 'BE',
            latitude: null,
            longitude: null,
            postalCode: null,
          },
        ],
      },
    ]

    expect(flattenLocationSuggestionGroups(groups).map((result) => result.id)).toEqual([
      '2950159',
      'BE',
    ])
  })
})
