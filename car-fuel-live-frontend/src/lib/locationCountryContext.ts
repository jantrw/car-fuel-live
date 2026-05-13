export interface StorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
}

export interface CountryContextEnvironment {
  storage?: StorageLike | null
  languages?: readonly string[]
  timeZone?: string | null
}

const COUNTRY_STORAGE_KEY = 'car-fuel-live.selected-country'
const DEFAULT_COUNTRY_CODE = 'DE'

const TIME_ZONE_COUNTRY_CODES: Record<string, string> = {
  'Europe/Amsterdam': 'NL',
  'Europe/Andorra': 'AD',
  'Europe/Athens': 'GR',
  'Europe/Belgrade': 'RS',
  'Europe/Berlin': 'DE',
  'Europe/Bratislava': 'SK',
  'Europe/Brussels': 'BE',
  'Europe/Bucharest': 'RO',
  'Europe/Budapest': 'HU',
  'Europe/Copenhagen': 'DK',
  'Europe/Dublin': 'IE',
  'Europe/Helsinki': 'FI',
  'Europe/Lisbon': 'PT',
  'Europe/Ljubljana': 'SI',
  'Europe/London': 'GB',
  'Europe/Luxembourg': 'LU',
  'Europe/Madrid': 'ES',
  'Europe/Malta': 'MT',
  'Europe/Monaco': 'MC',
  'Europe/Oslo': 'NO',
  'Europe/Paris': 'FR',
  'Europe/Podgorica': 'ME',
  'Europe/Prague': 'CZ',
  'Europe/Riga': 'LV',
  'Europe/Rome': 'IT',
  'Europe/San_Marino': 'SM',
  'Europe/Sarajevo': 'BA',
  'Europe/Skopje': 'MK',
  'Europe/Sofia': 'BG',
  'Europe/Stockholm': 'SE',
  'Europe/Tallinn': 'EE',
  'Europe/Tirane': 'AL',
  'Europe/Vaduz': 'LI',
  'Europe/Vatican': 'VA',
  'Europe/Vienna': 'AT',
  'Europe/Vilnius': 'LT',
  'Europe/Warsaw': 'PL',
  'Europe/Zagreb': 'HR',
  'Europe/Zurich': 'CH',
}

// Selected country is the only allowed persisted location context. Read it first so later visits
// reuse the last explicit or derived country without touching browser geolocation.
export function deriveInitialCountryCode(
  environment: CountryContextEnvironment = {},
): string {
  const storage = environment.storage ?? defaultStorage()
  const storedCountryCode = readStoredCountryCode(storage)
  if (storedCountryCode !== null) {
    return storedCountryCode
  }

  const localeCountryCode = deriveCountryCodeFromLanguages(
    environment.languages ?? getBrowserLanguages(),
  )
  if (localeCountryCode !== null) {
    persistCountryCode(localeCountryCode, storage)
    return localeCountryCode
  }

  const timeZoneCountryCode = deriveCountryCodeFromTimeZone(
    environment.timeZone ?? getBrowserTimeZone(),
  )
  if (timeZoneCountryCode !== null) {
    persistCountryCode(timeZoneCountryCode, storage)
    return timeZoneCountryCode
  }

  persistCountryCode(DEFAULT_COUNTRY_CODE, storage)
  return DEFAULT_COUNTRY_CODE
}

export function persistCountryCode(
  countryCode: string,
  storage: StorageLike | null = defaultStorage(),
) {
  const normalizedCountryCode = normalizeCountryCode(countryCode)
  if (normalizedCountryCode === null || storage === null) {
    return
  }

  storage.setItem(COUNTRY_STORAGE_KEY, normalizedCountryCode)
}

export function formatCountryContextLabel(
  countryCode: string,
  locale: string,
): string {
  const normalizedCountryCode = normalizeCountryCode(countryCode)
  if (normalizedCountryCode === null) {
    return DEFAULT_COUNTRY_CODE
  }

  if (typeof Intl.DisplayNames === 'undefined') {
    return normalizedCountryCode
  }

  const displayNames = new Intl.DisplayNames([locale], { type: 'region' })
  return displayNames.of(normalizedCountryCode) ?? normalizedCountryCode
}

export function normalizeCountryCode(value: string | null | undefined): string | null {
  if (typeof value !== 'string') {
    return null
  }

  const normalizedValue = value.trim().toUpperCase()
  return /^[A-Z]{2}$/.test(normalizedValue) ? normalizedValue : null
}

export function deriveCountryCodeFromLanguages(
  languages: readonly string[],
): string | null {
  for (const language of languages) {
    const normalizedCountryCode = countryCodeFromLanguage(language)
    if (normalizedCountryCode !== null) {
      return normalizedCountryCode
    }
  }

  return null
}

export function deriveCountryCodeFromTimeZone(
  timeZone: string | null | undefined,
): string | null {
  if (typeof timeZone !== 'string' || timeZone.length === 0) {
    return null
  }

  return TIME_ZONE_COUNTRY_CODES[timeZone] ?? null
}

function readStoredCountryCode(storage: StorageLike | null): string | null {
  if (storage === null) {
    return null
  }

  return normalizeCountryCode(storage.getItem(COUNTRY_STORAGE_KEY))
}

function countryCodeFromLanguage(language: string): string | null {
  const segments = language
    .split(/[-_]/)
    .map((segment) => segment.trim())
    .filter((segment) => segment.length > 0)

  for (let index = 1; index < segments.length; index += 1) {
    const segment = segments[index]
    if (/^[A-Za-z]{2}$/.test(segment)) {
      return segment.toUpperCase()
    }
  }

  return null
}

function getBrowserLanguages(): readonly string[] {
  if (typeof navigator === 'undefined') {
    return []
  }

  return navigator.languages.length > 0 ? navigator.languages : [navigator.language]
}

function getBrowserTimeZone(): string | null {
  if (typeof Intl === 'undefined' || typeof Intl.DateTimeFormat === 'undefined') {
    return null
  }

  return Intl.DateTimeFormat().resolvedOptions().timeZone ?? null
}

function defaultStorage(): StorageLike | null {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage
}
