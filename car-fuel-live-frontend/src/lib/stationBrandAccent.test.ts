import { describe, expect, it } from 'vitest'

import { getStationBrandAccent } from './stationBrandAccent'

describe('getStationBrandAccent', () => {
  it('should return the expected accent when the station brand is known', () => {
    expect(getStationBrandAccent('ARAL Tankstelle')).toBe('#006fba')
    expect(getStationBrandAccent('Shell')).toBe('#f5a623')
    expect(getStationBrandAccent('JET')).toBe('#ffd100')
  })

  it('should return the neutral accent when the station brand is unknown', () => {
    expect(getStationBrandAccent(null)).toBe('#0d9488')
    expect(getStationBrandAccent('Freie Tankstelle')).toBe('#0d9488')
  })
})
