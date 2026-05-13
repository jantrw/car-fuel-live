import { defineStore } from 'pinia'
import { shallowRef } from 'vue'

import {
  deriveInitialCountryCode,
  formatCountryContextLabel,
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

      countryCode.value = deriveInitialCountryCode()
      isInitialized.value = true
    }

    function countryLabel(locale: string = DEFAULT_LOCALE) {
      return formatCountryContextLabel(countryCode.value, locale)
    }

    return {
      countryCode,
      initializeCountryContext,
      countryLabel,
    }
  },
)
