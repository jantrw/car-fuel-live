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

export async function searchLocations(
  query: string,
  limit = 8,
  fetcher: typeof fetch = fetch,
): Promise<LocationSearchResponse> {
  // Keep all backend communication behind this API boundary so components never call fetch
  // directly.
  const params = new URLSearchParams({
    query,
    limit: limit.toString(),
  })
  const response = await fetcher(
    `/api/v1/locations/search?${params.toString()}`,
  )

  if (!response.ok) {
    throw new Error('Location lookup failed.')
  }

  return parseLocationSearchResponse(await response.json())
}

function parseLocationSearchResponse(value: unknown): LocationSearchResponse {
  // Backend responses are external input at runtime, even when the endpoint is owned by us.
  if (!isRecord(value) || !Array.isArray(value.items)) {
    throw new Error('Invalid location lookup response.')
  }

  return {
    items: value.items.map(parseLocationSearchResult),
  }
}

function parseLocationSearchResult(value: unknown): LocationSearchResult {
  // Validate every result before UI state accepts it so malformed payloads cannot leak into
  // components as trusted TypeScript shapes.
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

function parseResultType(value: unknown): LocationSearchResultType {
  // Result type drives UI copy and selection behavior, so unknown backend types must fail closed.
  if (value === 'country' || value === 'place' || value === 'postalCode') {
    return value
  }

  throw new Error('Invalid location lookup result type.')
}

function parseString(value: unknown, field: string): string {
  // Empty identifiers and labels are unusable for selection, even if the JSON type is correct.
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

function parseNullableNumber(value: unknown, field: string): number | null {
  // Countries can have missing coordinates in the seed data, but invalid coordinate types should
  // still be rejected.
  if (value === null || typeof value === 'number') {
    return value
  }

  throw new Error(`Invalid location lookup ${field}.`)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
