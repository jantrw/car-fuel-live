import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import { resolveLocationLookupMessages } from '@/i18n/locationLookupMessages'

import LocationResultList from './LocationResultList.vue'

const result = {
  type: 'place' as const,
  id: '2950159',
  label: 'Berlin',
  countryCode: 'DE',
  latitude: 52.52437,
  longitude: 13.41053,
  postalCode: null,
}

describe('LocationResultList', () => {
  it('should emit the original location result when a dropdown row is selected', async () => {
    const wrapper = mount(LocationResultList, {
      props: {
        groups: [{ type: 'place', items: [result] }],
        status: 'results',
        query: 'Ber',
        messages: resolveLocationLookupMessages(['en']),
      },
    })

    await wrapper.get('li button').trigger('click')

    expect(wrapper.emitted('selectResult')).toEqual([[result]])
  })
})
