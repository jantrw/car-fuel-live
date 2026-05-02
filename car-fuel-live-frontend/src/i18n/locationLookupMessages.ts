export interface LocationLookupMessages {
  eyebrow: string
  title: string
  intro: string
  searchLabel: string
  searchPlaceholder: string
  searchButton: string
  loading: string
  resultsTitle: string
  noResultsTitle: string
  noResultsBody: string
  errorTitle: string
  errorBody: string
  selectedTitle: string
  latitude: string
  longitude: string
  coordinatesUnavailable: string
  privacyNotice: string
  resultType: Record<'country' | 'place' | 'postalCode', string>
}

const messages: Record<'en' | 'de', LocationLookupMessages> = {
  en: {
    eyebrow: 'Manual location lookup',
    title: 'Find seeded locations',
    intro:
      'Search the local PostgreSQL dataset for countries, places, and German postal codes.',
    searchLabel: 'Search term',
    searchPlaceholder: 'Berlin, Belgium, 10115',
    searchButton: 'Search',
    loading: 'Searching...',
    resultsTitle: 'Matches',
    noResultsTitle: 'No local match',
    noResultsBody: 'Try another spelling or a nearby place.',
    errorTitle: 'Lookup failed',
    errorBody: 'The location service did not return usable results.',
    selectedTitle: 'Selected location',
    latitude: 'Latitude',
    longitude: 'Longitude',
    coordinatesUnavailable: 'Coordinates not available',
    privacyNotice:
      'This MVP searches local seed data only. No browser location is requested or stored.',
    resultType: {
      country: 'Country',
      place: 'Place',
      postalCode: 'Postal code',
    },
  },
  de: {
    eyebrow: 'Manuelle Standortsuche',
    title: 'Gespeicherte Orte finden',
    intro:
      'Durchsuche den lokalen PostgreSQL-Datensatz nach Ländern, Orten und deutschen Postleitzahlen.',
    searchLabel: 'Suchbegriff',
    searchPlaceholder: 'Berlin, Belgien, 10115',
    searchButton: 'Suchen',
    loading: 'Suche laeuft...',
    resultsTitle: 'Treffer',
    noResultsTitle: 'Kein lokaler Treffer',
    noResultsBody:
      'Versuche eine andere Schreibweise oder einen nahegelegenen Ort.',
    errorTitle: 'Suche fehlgeschlagen',
    errorBody: 'Der Standortdienst hat keine nutzbaren Ergebnisse geliefert.',
    selectedTitle: 'Ausgewaehlter Standort',
    latitude: 'Breitengrad',
    longitude: 'Laengengrad',
    coordinatesUnavailable: 'Koordinaten nicht verfuegbar',
    privacyNotice:
      'Dieses MVP durchsucht nur lokale Seed-Daten. Browser-Standort wird nicht abgefragt oder gespeichert.',
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
