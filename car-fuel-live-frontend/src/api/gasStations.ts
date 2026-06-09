export interface GasStationResult {
  id: string
  name: string
  brand: string | null
  street: string | null
  houseNumber: string | null
  postCode: string | null
  place: string | null
  latitude: number
  longitude: number
  distanceKm: number | null
  isOpen: boolean | null
  e5: number | null
  e10: number | null
  diesel: number | null
}

export interface GasStationSearchResponse {
  items: GasStationResult[]
}

interface ApiErrorResponse {
  code: string
  message: string
}

interface SearchGasStationsOptions {
  signal?: AbortSignal
  fetcher?: typeof fetch
}

export class GasStationLookupError extends Error {
  readonly status: number
  readonly code: string | null

  constructor(message: string, status: number, code: string | null = null) {
    super(message)
    this.name = 'GasStationLookupError'
    this.status = status
    this.code = code
  }
}

// Keep gas-station requests behind the shared API layer so location selection logic stays decoupled
// from the transport details and can evolve with later filter support.
export async function searchGasStations(
  latitude: number,
  longitude: number,
  options: SearchGasStationsOptions = {},
): Promise<GasStationSearchResponse> {
  const params = new URLSearchParams({
    lat: latitude.toString(),
    lng: longitude.toString(),
  })

  const response = await (options.fetcher ?? fetch)(
    `/api/v1/gas-stations?${params.toString()}`,
    { signal: options.signal },
  )

  if (!response.ok) {
    throw await toGasStationLookupError(response)
  }

  return parseGasStationSearchResponse(await response.json())
}

async function toGasStationLookupError(
  response: Response,
): Promise<GasStationLookupError> {
  let apiErrorResponse: ApiErrorResponse | null = null

  try {
    apiErrorResponse = parseApiErrorResponse(await response.json())
  } catch {
    apiErrorResponse = null
  }

  return new GasStationLookupError(
    apiErrorResponse?.message ?? 'Gas station lookup failed.',
    response.status,
    apiErrorResponse?.code ?? null,
  )
}

function parseGasStationSearchResponse(value: unknown): GasStationSearchResponse {
  if (!isRecord(value) || !Array.isArray(value.items)) {
    throw new Error('Invalid gas station lookup response.')
  }

  return {
    items: value.items.map(parseGasStationResult),
  }
}

function parseGasStationResult(value: unknown): GasStationResult {
  if (!isRecord(value)) {
    throw new Error('Invalid gas station lookup result.')
  }

  return {
    id: parseString(value.id, 'id'),
    name: parseString(value.name, 'name'),
    brand: parseNullableString(value.brand, 'brand'),
    street: parseNullableString(value.street, 'street'),
    houseNumber: parseNullableString(value.houseNumber, 'houseNumber'),
    postCode: parseNullableString(value.postCode, 'postCode'),
    place: parseNullableString(value.place, 'place'),
    latitude: parseNumber(value.latitude, 'latitude'),
    longitude: parseNumber(value.longitude, 'longitude'),
    distanceKm: parseNullableNumber(value.distanceKm, 'distanceKm'),
    isOpen: parseNullableBoolean(value.isOpen, 'isOpen'),
    e5: parseNullableNumber(value.e5, 'e5'),
    e10: parseNullableNumber(value.e10, 'e10'),
    diesel: parseNullableNumber(value.diesel, 'diesel'),
  }
}

function parseString(value: unknown, field: string): string {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }

  throw new Error(`Invalid gas station lookup ${field}.`)
}

function parseNullableString(value: unknown, field: string): string | null {
  if (value === null || typeof value === 'string') {
    return value
  }

  throw new Error(`Invalid gas station lookup ${field}.`)
}

function parseNumber(value: unknown, field: string): number {
  if (typeof value === 'number') {
    return value
  }

  throw new Error(`Invalid gas station lookup ${field}.`)
}

function parseNullableNumber(value: unknown, field: string): number | null {
  if (value === null || typeof value === 'number') {
    return value
  }

  throw new Error(`Invalid gas station lookup ${field}.`)
}

function parseNullableBoolean(value: unknown, field: string): boolean | null {
  if (value === null || typeof value === 'boolean') {
    return value
  }

  throw new Error(`Invalid gas station lookup ${field}.`)
}

function parseApiErrorResponse(value: unknown): ApiErrorResponse {
  if (!isRecord(value)) {
    throw new Error('Invalid gas station lookup error response.')
  }

  return {
    code: parseString(value.code, 'code'),
    message: parseString(value.message, 'message'),
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
