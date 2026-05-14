export interface LocationLookupMessages {
  locale: 'en' | 'de'
  eyebrow: string
  title: string
  intro: string
  currentCountryLabel: string
  currentCountryHint: string
  searchLabel: string
  searchPlaceholder: string
  searchAction: string
  searchHint: string
  loading: string
  resultsTitle: string
  resultsQueryPrefix: string
  noResultsTitle: string
  noResultsBody: string
  errorTitle: string
  errorBody: string
  privacyNotice: string
  validationMessage: {
    QUERY_TOO_SHORT: string
    QUERY_TOO_LONG: string
  }
  suggestionGroupTitle: Record<'country' | 'place' | 'postalCode', string>
  resultType: Record<'country' | 'place' | 'postalCode', string>
}

const messages: Record<'en' | 'de', LocationLookupMessages> = {
  en: {
    locale: 'en',
    eyebrow: 'Manual location lookup',
    title: 'Search local places',
    intro:
      'Search the local PostgreSQL dataset for countries, cities, places, regions, and German postal codes when you want explicit local results.',
    currentCountryLabel: 'Country context',
    currentCountryHint:
      'Search results prefer matches from this country first, but still fall back across borders when needed.',
    searchLabel: 'Search',
    searchPlaceholder: 'Berlin, Belgium, 10115',
    searchAction: 'Search places',
    searchHint:
      'Results appear only after you start a search. The country context is a preference, not a filter.',
    loading: 'Searching...',
    resultsTitle: 'Results',
    resultsQueryPrefix: 'Search for',
    noResultsTitle: 'No local match',
    noResultsBody: 'Try another spelling or a nearby place.',
    errorTitle: 'Lookup failed',
    errorBody: 'The location service did not return usable results.',
    privacyNotice:
      'This UI uses local seed data only. It stores the selected country context, but not coordinates.',
    validationMessage: {
      QUERY_TOO_SHORT: 'Enter at least two characters before searching.',
      QUERY_TOO_LONG: 'Enter at most 80 characters before searching.',
    },
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
    title: 'Lokale Orte gezielt suchen',
    intro:
      'Suche im lokalen PostgreSQL-Datensatz nach Laendern, Staedten, Orten, Regionen und deutschen Postleitzahlen, wenn du bewusst lokale Treffer laden willst.',
    currentCountryLabel: 'Laenderkontext',
    currentCountryHint:
      'Suchergebnisse bevorzugen Treffer aus diesem Land, greifen bei Bedarf aber weiter laenderuebergreifend.',
    searchLabel: 'Suche',
    searchPlaceholder: 'Berlin, Belgien, 10115',
    searchAction: 'Orte suchen',
    searchHint:
      'Ergebnisse erscheinen erst nach einer bewussten Suche. Der Laenderkontext ist eine Praeferenz, kein Filter.',
    loading: 'Suche laeuft...',
    resultsTitle: 'Ergebnisse',
    resultsQueryPrefix: 'Suche nach',
    noResultsTitle: 'Kein lokaler Treffer',
    noResultsBody:
      'Versuche eine andere Schreibweise oder einen nahegelegenen Ort.',
    errorTitle: 'Suche fehlgeschlagen',
    errorBody: 'Der Standortdienst hat keine nutzbaren Ergebnisse geliefert.',
    privacyNotice:
      'Diese UI nutzt nur lokale Seed-Daten. Gespeichert wird nur der Laenderkontext, keine Koordinaten.',
    validationMessage: {
      QUERY_TOO_SHORT: 'Gib vor der Suche mindestens zwei Zeichen ein.',
      QUERY_TOO_LONG: 'Gib vor der Suche hoechstens 80 Zeichen ein.',
    },
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
