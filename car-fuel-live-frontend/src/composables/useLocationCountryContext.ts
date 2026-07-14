import { computed, readonly, shallowRef } from 'vue'

import {
  deriveInitialCountryCode,
  formatCountryContextLabel,
  persistCountryCode,
} from '@/lib/locationCountryContext'

const countryCode = shallowRef('')
let isInitialized = false

export function useLocationCountryContext() {
  function initializeCountryContext() {
    if (isInitialized) {
      return
    }

    countryCode.value = deriveInitialCountryCode()
    isInitialized = true
  }

  function countryLabel(locale: string) {
    return computed(() => formatCountryContextLabel(countryCode.value, locale))
  }

  function setCountryCode(nextCountryCode: string) {
    countryCode.value = nextCountryCode
    persistCountryCode(nextCountryCode)
  }

  return {
    countryCode: readonly(countryCode),
    initializeCountryContext,
    countryLabel,
    setCountryCode,
  }
}
