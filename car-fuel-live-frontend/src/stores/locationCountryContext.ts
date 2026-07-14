import { defineStore } from 'pinia'
import { shallowRef } from 'vue'

import {
  deriveInitialCountryCode,
  formatCountryContextLabel,
  normalizeCountryCode,
  persistCountryCode,
} from '@/lib/locationCountryContext'

const DEFAULT_LOCALE = 'en'

export const useLocationCountryContextStore = defineStore(
  'location-country-context',
  () => {
    const countryCode = shallowRef('')
    const isInitialized = shallowRef(false)

    function initializeCountryContext() {
      if (isInitialized.value) {
        return
      }

      // Initialization is intentionally one-shot: later edits come only from explicit user
      // selections, while persistence remains limited to the country code.
      countryCode.value = deriveInitialCountryCode()
      isInitialized.value = true
    }

    function countryLabel(locale: string = DEFAULT_LOCALE) {
      return formatCountryContextLabel(countryCode.value, locale)
    }

    function setCountryCode(nextCountryCode: string) {
      const normalizedCountryCode = normalizeCountryCode(nextCountryCode)
      if (normalizedCountryCode === null) {
        return
      }

      countryCode.value = normalizedCountryCode
      persistCountryCode(normalizedCountryCode)
    }

    return {
      countryCode,
      initializeCountryContext,
      countryLabel,
      setCountryCode,
    }
  },
)
