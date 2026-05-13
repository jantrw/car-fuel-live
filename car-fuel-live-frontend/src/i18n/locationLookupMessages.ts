export interface LocationLookupMessages {
  locale: 'en' | 'de'
  eyebrow: string
  title: string
  intro: string
  currentCountryLabel: string
  currentCountryHint: string
  searchLabel: string
  searchPlaceholder: string
  loading: string
  suggestionsTitle: string
  noResultsTitle: string
  noResultsBody: string
  errorTitle: string
  errorBody: string
  privacyNotice: string
  keyboardHint: string
  suggestionGroupTitle: Record<'country' | 'place' | 'postalCode', string>
  resultType: Record<'country' | 'place' | 'postalCode', string>
}

const messages: Record<'en' | 'de', LocationLookupMessages> = {
  en: {
    locale: 'en',
    eyebrow: 'Manual location lookup',
    title: 'Search local suggestions',
    intro:
      'Type to browse local PostgreSQL suggestions for countries, cities, places, and German postal codes.',
    currentCountryLabel: 'Country context',
    currentCountryHint:
      'Suggestions prefer matches from this country when the backend can rank them.',
    searchLabel: 'Search',
    searchPlaceholder: 'Berlin, Belgium, 10115',
    loading: 'Searching...',
    suggestionsTitle: 'Suggestions',
    noResultsTitle: 'No local match',
    noResultsBody: 'Try another spelling or a nearby place.',
    errorTitle: 'Lookup failed',
    errorBody: 'The location service did not return usable results.',
    privacyNotice:
      'This UI uses local seed data only. It stores the selected country context, but not coordinates.',
    keyboardHint:
      'Use Arrow keys to move through suggestions, Enter to choose one, and Escape to close the list.',
    suggestionGroupTitle: {
      country: 'Countries',
      place: 'Cities and places',
      postalCode: 'Postal codes',
    },
    resultType: {
      country: 'Country',
      place: 'Place',
      postalCode: 'Postal code',
    },
  },
  de: {
    locale: 'de',
    eyebrow: 'Manuelle Standortsuche',
    title: 'Lokale Vorschlaege durchsuchen',
    intro:
      'Tippe, um lokale PostgreSQL-Vorschlaege fuer Laender, Staedte, Orte und deutsche Postleitzahlen zu durchsuchen.',
    currentCountryLabel: 'Laenderkontext',
    currentCountryHint:
      'Vorschlaege bevorzugen Treffer aus diesem Land, wenn das Backend sie hoeher einstufen kann.',
    searchLabel: 'Suche',
    searchPlaceholder: 'Berlin, Bernau bei Berlin, 10115',
    loading: 'Suche laeuft...',
    suggestionsTitle: 'Vorschlaege',
    noResultsTitle: 'Kein lokaler Treffer',
    noResultsBody:
      'Versuche eine andere Schreibweise oder einen nahegelegenen Ort.',
    errorTitle: 'Suche fehlgeschlagen',
    errorBody: 'Der Standortdienst hat keine nutzbaren Ergebnisse geliefert.',
    privacyNotice:
      'Diese UI nutzt nur lokale Seed-Daten. Gespeichert wird nur der Laenderkontext, keine Koordinaten.',
    keyboardHint:
      'Mit den Pfeiltasten durch Vorschlaege gehen, mit Enter auswaehlen und mit Escape die Liste schliessen.',
    suggestionGroupTitle: {
      country: 'Laender',
      place: 'Staedte und Orte',
      postalCode: 'Postleitzahlen',
    },
    resultType: {
      country: 'Land',
      place: 'Ort',
      postalCode: 'Postleitzahl',
    },
  },
}

// The MVP only needs app chrome localization; GeoNames labels stay canonical from the database.
export function resolveLocationLookupMessages(
  languages: readonly string[] = getBrowserLanguages(),
): LocationLookupMessages {
  const primaryLanguage = languages
    .map((language) => language.toLowerCase())
    .find((language) => language.startsWith('de') || language.startsWith('en'))

  return primaryLanguage?.startsWith('de') ? messages.de : messages.en
}

// Tests and server-side tooling may not provide navigator, so keep message resolution browser-safe.
function getBrowserLanguages(): readonly string[] {
  if (typeof navigator === 'undefined') {
    return ['en']
  }

  return navigator.languages.length > 0
    ? navigator.languages
    : [navigator.language]
}
