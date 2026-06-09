import { readonly, shallowRef } from 'vue'

import {
  GasStationLookupError,
  searchGasStations,
  type GasStationResult,
  type GasStationSearchResponse,
} from '@/api/gasStations'
import type { LocationSearchResult } from '@/api/locationSearch'

export type GasStationResultsStatus =
  | 'idle'
  | 'loading'
  | 'results'
  | 'empty'
  | 'error'
  | 'rateLimited'
  | 'countrySelected'

interface UseGasStationResultsOptions {
  search?: (
    latitude: number,
    longitude: number,
    options?: {
      signal?: AbortSignal
    },
  ) => Promise<GasStationSearchResponse>
}

// Gas-station lookup is a second explicit action after local search selection. Keep that state
// isolated so search input edits do not accidentally persist raw coordinates anywhere.
export function useGasStationResults(options: UseGasStationResultsOptions = {}) {
  const search = options.search ?? searchGasStations

  const stations = shallowRef<GasStationResult[]>([])
  const status = shallowRef<GasStationResultsStatus>('idle')
  const selectedLocationLabel = shallowRef('')
  let currentController: AbortController | null = null

  function reset() {
    currentController?.abort()
    currentController = null
    stations.value = []
    status.value = 'idle'
    selectedLocationLabel.value = ''
  }

  async function loadForSelection(result: LocationSearchResult) {
    currentController?.abort()
    currentController = null
    selectedLocationLabel.value = result.label

    // Country results currently update only the country context. They intentionally stop here
    // because this MVP needs concrete coordinates before it can call the live price endpoint.
    if (result.type === 'country' || result.latitude === null || result.longitude === null) {
      stations.value = []
      status.value = 'countrySelected'
      return
    }

    const controller = new AbortController()
    currentController = controller
    stations.value = []
    status.value = 'loading'

    try {
      const response = await search(result.latitude, result.longitude, {
        signal: controller.signal,
      })

      if (controller.signal.aborted || currentController !== controller) {
        return
      }

      stations.value = response.items
      status.value = response.items.length > 0 ? 'results' : 'empty'
    } catch (error) {
      if (controller.signal.aborted || currentController !== controller) {
        return
      }

      stations.value = []
      status.value =
        error instanceof GasStationLookupError &&
        error.status === 429 &&
        error.code === 'RATE_LIMITED'
          ? 'rateLimited'
          : 'error'
    } finally {
      if (currentController === controller) {
        currentController = null
      }
    }
  }

  return {
    stations: readonly(stations),
    status: readonly(status),
    selectedLocationLabel: readonly(selectedLocationLabel),
    loadForSelection,
    reset,
  }
}
