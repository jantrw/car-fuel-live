import type {
  LocationSearchResult,
  LocationSearchResultType,
} from '@/api/locationSearch'

export interface LocationSuggestionGroup {
  type: LocationSearchResultType
  items: LocationSearchResult[]
}

const GROUP_ORDER: LocationSearchResultType[] = [
  'place',
  'country',
  'postalCode',
]

// Backend ranking stays flat. The UI groups visible search results by type while preserving each
// type's relative order from the API response.
export function groupLocationSuggestions(
  results: readonly LocationSearchResult[],
): LocationSuggestionGroup[] {
  const groupedResults = new Map<
    LocationSearchResultType,
    LocationSearchResult[]
  >()

  for (const result of results) {
    const items = groupedResults.get(result.type) ?? []
    items.push(result)
    groupedResults.set(result.type, items)
  }

  return GROUP_ORDER.flatMap((type) => {
    const items = groupedResults.get(type)
    return items === undefined ? [] : [{ type, items }]
  })
}
