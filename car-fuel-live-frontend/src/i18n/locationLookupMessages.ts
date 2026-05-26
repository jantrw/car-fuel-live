export interface LocationLookupMessages {
  locale: 'en' | 'de'
  eyebrow: string
  title: string
  intro: string
  mvpCatchPhrase: string
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
  stationResultsEyebrow: string
  stationResultsTitle: string
  stationResultsSelectionPrefix: string
  stationLoading: string
  stationEmptyTitle: string
  stationEmptyBody: string
  stationErrorTitle: string
  stationErrorBody: string
  countrySelectionTitle: string
  countrySelectionBody: string
  stationAttribution: string
  stationFreshnessNotice: string
  stationPriceUnavailable: string
  stationDistanceUnavailable: string
  stationAddressUnavailable: string
  stationBrandFallback: string
  stationOpen: string
  stationClosed: string
  stationOpenUnknown: string
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
    eyebrow: 'Cheap & Fast',
    title: 'Find the nearest fair fuel price',
    intro:
      'Search a place once and load live your fuel prices nearby.',
    mvpCatchPhrase:
      'Compare real prices in seconds before you pick the next station.',
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
      'This UI uses local seed data for place lookup and loads live fuel prices after you choose a result. It stores the selected country context, but not coordinates.',
    stationResultsEyebrow: 'Tankerkönig prices',
    stationResultsTitle: 'Fuel prices nearby',
    stationResultsSelectionPrefix: 'Selected location',
    stationLoading: 'Loading nearby fuel prices...',
    stationEmptyTitle: 'No nearby stations',
    stationEmptyBody: 'Try another place or search in a larger nearby city later.',
    stationErrorTitle: 'Fuel prices unavailable',
    stationErrorBody:
      'The upstream fuel price service did not return usable station data.',
    countrySelectionTitle: 'Country selected',
    countrySelectionBody:
      'This MVP loads live prices only for cities, places, and postal codes with coordinates. Choose a concrete place next.',
    stationAttribution:
      'Fuel price data provided via Tankerkönig under CC BY 4.0 and subject to MTS-K usage rules.',
    stationFreshnessNotice:
      'Prices are loaded on demand for the selected location and are not kept in a long-lived cache.',
    stationPriceUnavailable: 'Not offered',
    stationDistanceUnavailable: 'Distance unavailable',
    stationAddressUnavailable: 'Address unavailable',
    stationBrandFallback: 'Independent station',
    stationOpen: 'Open now',
    stationClosed: 'Closed',
    stationOpenUnknown: 'Open state unknown',
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
    eyebrow: 'Günstig & Schnell',
    title: 'Finde deinen nächsten fairen Spritpreis',
    intro:
      'Suche einen Ort und lade sofort echte Kraftstoffpreise in deiner Nähe.',
    mvpCatchPhrase:
      'Preise vergleichen, besser tanken.',
    currentCountryLabel: 'Länderkontext',
    currentCountryHint:
      'Suchergebnisse bevorzugen Treffer aus diesem Land, greifen bei Bedarf aber weiter länderübergreifend.',
    searchLabel: 'Suche',
    searchPlaceholder: 'Berlin, Belgien, 10115',
    searchAction: 'Orte suchen',
    searchHint:
      'Ergebnisse erscheinen erst nach einer bewussten Suche. Der Länderkontext ist eine Präferenz, kein Filter.',
    loading: 'Suche läuft...',
    resultsTitle: 'Ergebnisse',
    resultsQueryPrefix: 'Suche nach',
    noResultsTitle: 'Kein lokaler Treffer',
    noResultsBody:
      'Versuche eine andere Schreibweise oder einen nahegelegenen Ort.',
    errorTitle: 'Suche fehlgeschlagen',
    errorBody: 'Der Standortdienst hat keine nutzbaren Ergebnisse geliefert.',
    privacyNotice:
      'Diese UI nutzt lokale Seed-Daten für die Ortssuche und lädt Live-Kraftstoffpreise nach deiner Auswahl. Gespeichert wird nur der Länderkontext, keine Koordinaten.',
    stationResultsEyebrow: 'Tankerkönig-Preise',
    stationResultsTitle: 'Kraftstoffpreise in der Nähe',
    stationResultsSelectionPrefix: 'Ausgewählter Ort',
    stationLoading: 'Nahe Kraftstoffpreise werden geladen...',
    stationEmptyTitle: 'Keine nahen Tankstellen gefunden',
    stationEmptyBody:
      'Versuche einen anderen Ort oder später eine größere Stadt in der Nähe.',
    stationErrorTitle: 'Kraftstoffpreise nicht verfügbar',
    stationErrorBody:
      'Der Upstream-Dienst für Kraftstoffpreise hat keine nutzbaren Stationsdaten geliefert.',
    countrySelectionTitle: 'Land ausgewählt',
    countrySelectionBody:
      'Dieses MVP lädt Live-Preise nur für Städte, Orte und Postleitzahlen mit Koordinaten. Wähle als Nächstes einen konkreten Ort.',
    stationAttribution:
      'Kraftstoffpreisdaten kommen über Tankerkönig unter CC BY 4.0 und unterliegen den MTS-K-Nutzungsbedingungen.',
    stationFreshnessNotice:
      'Preise werden bei Bedarf für den gewählten Ort geladen und nicht über lange Zeit zwischengespeichert.',
    stationPriceUnavailable: 'Nicht angeboten',
    stationDistanceUnavailable: 'Entfernung unbekannt',
    stationAddressUnavailable: 'Adresse unbekannt',
    stationBrandFallback: 'Freie Tankstelle',
    stationOpen: 'Jetzt offen',
    stationClosed: 'Geschlossen',
    stationOpenUnknown: 'Öffnungsstatus unbekannt',
    validationMessage: {
      QUERY_TOO_SHORT: 'Gib vor der Suche mindestens zwei Zeichen ein.',
      QUERY_TOO_LONG: 'Gib vor der Suche höchstens 80 Zeichen ein.',
    },
    suggestionGroupTitle: {
      country: 'Länder',
      place: 'Städte und Orte',
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
