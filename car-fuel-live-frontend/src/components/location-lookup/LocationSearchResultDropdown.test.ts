import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import LocationSearchResultDropdown, {
  type LocationSearchDropdownGroup,
} from './LocationSearchResultDropdown.vue'

const messages = {
  resultsTitle: 'Results for',
  dropdownHelperText: 'Choose the matching location.',
  noResultsTitle: 'No local match',
  noResultsBody: 'Try another spelling or a nearby place.',
}

const groups: LocationSearchDropdownGroup[] = [
  {
    id: 'places',
    label: 'Places and cities',
    items: [
      {
        id: 'berlin',
        label: 'Berlin',
        context: 'Germany · City',
        type: 'place',
        countryCode: 'DE',
        latitude: 52.52437,
        longitude: 13.41053,
        postalCode: null,
        directResolution: false,
      },
    ],
  },
]

describe('LocationSearchResultDropdown', () => {
  it('should emit the selected item when its row is selected', async () => {
    const wrapper = mount(LocationSearchResultDropdown, {
      props: { query: 'Ber', groups, messages },
    })

    await wrapper.get('li button').trigger('click')

    expect(wrapper.emitted('selectItem')).toEqual([[groups[0].items[0]]])
  })

  it('should render an accessible empty state when no groups are provided', () => {
    const wrapper = mount(LocationSearchResultDropdown, {
      props: { query: 'Unknown', groups: [], messages },
    })

    expect(wrapper.text()).toContain('No local match')
    expect(wrapper.find('ul').exists()).toBe(false)
  })
})
