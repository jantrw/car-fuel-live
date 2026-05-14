export type LocationSearchResultType = 'country' | 'place' | 'postalCode'

export interface LocationSearchResult {
  type: LocationSearchResultType
  id: string
  label: string
  countryCode: string
  latitude: number | null
  longitude: number | null
  postalCode: string | null
}

export interface LocationSearchResponse {
  items: LocationSearchResult[]
}

interface SearchLocationsOptions {
  countryCode?: string | null
  limit?: number
  signal?: AbortSignal
  fetcher?: typeof fetch
}

// Keep all backend communication behind this API boundary so components never call fetch
// directly, even when the UI switches between autocomplete and explicit search flows.
export async function searchLocations(
  query: string,
  options: SearchLocationsOptions = {},
): Promise<LocationSearchResponse> {
  const params = new URLSearchParams({
    q: query,
    limit: (options.limit ?? 8).toString(),
  })
  const normalizedCountryCode = normalizeCountryCode(options.countryCode)
  if (normalizedCountryCode !== null) {
    params.set('countryCode', normalizedCountryCode)
  }

  const response = await (options.fetcher ?? fetch)(
    `/api/v1/locations/suggestions?${params.toString()}`,
    { signal: options.signal },
  )

  if (!response.ok) {
    throw new Error('Location lookup failed.')
  }

  return parseLocationSearchResponse(await response.json())
}

// Backend responses are external input at runtime, even when the endpoint is owned by us.
function parseLocationSearchResponse(value: unknown): LocationSearchResponse {
  if (!isRecord(value) || !Array.isArray(value.items)) {
    throw new Error('Invalid location lookup response.')
  }

  return {
    items: value.items.map(parseLocationSearchResult),
  }
}

// Validate every result before UI state accepts it so malformed payloads cannot leak into
// components as trusted TypeScript shapes.
function parseLocationSearchResult(value: unknown): LocationSearchResult {
  if (!isRecord(value)) {
    throw new Error('Invalid location lookup result.')
  }

  const type = parseResultType(value.type)
  const id = parseString(value.id, 'id')
  const label = parseString(value.label, 'label')
  const countryCode = parseString(value.countryCode, 'countryCode')

  return {
    type,
    id,
    label,
    countryCode,
    latitude: parseNullableNumber(value.latitude, 'latitude'),
    longitude: parseNullableNumber(value.longitude, 'longitude'),
    postalCode: parseNullableString(value.postalCode, 'postalCode'),
  }
}

// Result type drives UI copy and selection behavior, so unknown backend types must fail closed.
function parseResultType(value: unknown): LocationSearchResultType {
  if (value === 'country' || value === 'place' || value === 'postalCode') {
    return value
  }

  throw new Error('Invalid location lookup result type.')
}

// Empty identifiers and labels are unusable for selection, even if the JSON type is correct.
function parseString(value: unknown, field: string): string {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }

  throw new Error(`Invalid location lookup ${field}.`)
}

function parseNullableString(value: unknown, field: string): string | null {
  if (value === null || typeof value === 'string') {
    return value
  }

  throw new Error(`Invalid location lookup ${field}.`)
}

// Countries can have missing coordinates in the seed data, but invalid coordinate types should
// still be rejected.
function parseNullableNumber(value: unknown, field: string): number | null {
  if (value === null || typeof value === 'number') {
    return value
  }

  throw new Error(`Invalid location lookup ${field}.`)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function normalizeCountryCode(value: string | null | undefined): string | null {
  if (typeof value !== 'string') {
    return null
  }

  const normalizedValue = value.trim().toUpperCase()
  return normalizedValue.length === 2 ? normalizedValue : null
}
