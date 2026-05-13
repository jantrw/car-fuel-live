import type { LocationSearchResult, LocationSearchResultType } from '@/api/locationSearch'

export interface LocationSuggestionGroup {
  type: LocationSearchResultType
  items: LocationSearchResult[]
}

const GROUP_ORDER: LocationSearchResultType[] = ['place', 'country', 'postalCode']

// Backend ranking stays flat. The UI groups visible suggestions by type while preserving each
// type's relative order so keyboard navigation and pointer selection follow the rendered list.
export function groupLocationSuggestions(
  results: readonly LocationSearchResult[],
): LocationSuggestionGroup[] {
  const groupedResults = new Map<LocationSearchResultType, LocationSearchResult[]>()

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

export function flattenLocationSuggestionGroups(
  groups: readonly LocationSuggestionGroup[],
): LocationSearchResult[] {
  return groups.flatMap((group) => group.items)
}
