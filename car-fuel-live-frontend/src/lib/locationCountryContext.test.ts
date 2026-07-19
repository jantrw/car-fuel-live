import { describe, expect, it } from 'vitest'

import {
  deriveCountryCodeFromLanguages,
  deriveCountryCodeFromTimeZone,
  deriveInitialCountryCode,
  formatCountryContextLabel,
} from './locationCountryContext'

function createStorage(initialValues: Record<string, string> = {}) {
  const values = new Map(Object.entries(initialValues))

  return {
    getItem(key: string) {
      return values.get(key) ?? null
    },
    setItem(key: string, value: string) {
      values.set(key, value)
    },
  }
}

describe('locationCountryContext', () => {
  it('should prefer the stored country code when local storage already has one', () => {
    const storage = createStorage({ 'car-fuel-live.selected-country': 'BE' })

    const countryCode = deriveInitialCountryCode({
      storage,
      languages: ['de-DE'],
      timeZone: 'Europe/Berlin',
    })

    expect(countryCode).toBe('BE')
  })

  it('should derive the country code from the first locale with a region', () => {
    expect(deriveCountryCodeFromLanguages(['de', 'en-GB', 'fr-FR'])).toBe('GB')
  })

  it('should derive the country code from the browser time zone when locales have no region', () => {
    const storage = createStorage()

    const countryCode = deriveInitialCountryCode({
      storage,
      languages: ['de', 'en'],
      timeZone: 'Europe/Brussels',
    })

    expect(countryCode).toBe('BE')
  })

  it('should fall back to Germany when neither locale nor time zone resolve a country', () => {
    const storage = createStorage()

    const countryCode = deriveInitialCountryCode({
      storage,
      languages: ['de'],
      timeZone: 'Etc/UTC',
    })

    expect(countryCode).toBe('DE')
  })

  it('should derive and return a country when storage access is unavailable', () => {
    const unavailableStorage = {
      getItem() {
        throw new Error('Storage is unavailable')
      },
      setItem() {
        throw new Error('Storage is unavailable')
      },
    }

    expect(
      deriveInitialCountryCode({
        storage: unavailableStorage,
        languages: ['de-AT'],
      }),
    ).toBe('AT')
  })

  it('should format the visible country label through Intl display names when available', () => {
    expect(formatCountryContextLabel('DE', 'en')).toBe('Germany')
    expect(formatCountryContextLabel('DE', 'de')).toBe('Deutschland')
  })

  it('should keep the dedicated time zone mapping bounded and explicit', () => {
    expect(deriveCountryCodeFromTimeZone('Europe/Berlin')).toBe('DE')
    expect(deriveCountryCodeFromTimeZone('America/New_York')).toBeNull()
  })
})
