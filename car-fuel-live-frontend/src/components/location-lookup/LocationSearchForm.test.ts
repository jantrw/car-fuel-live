import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import { resolveLocationLookupMessages } from '@/i18n/locationLookupMessages'

import LocationSearchForm from './LocationSearchForm.vue'

describe('LocationSearchForm', () => {
  it('should keep results in flow on mobile and overlay them below the field on desktop', () => {
    const wrapper = mount(LocationSearchForm, {
      props: {
        query: 'Ber',
        countryCode: 'DE',
        countryLabel: 'Germany',
        isLoading: false,
        validationMessage: null,
        messages: resolveLocationLookupMessages(['en']),
      },
      slots: {
        results: '<div data-testid="search-results">Results</div>',
      },
    })

    const inputContainer = wrapper.get('#location-query').element.parentElement
    const results = wrapper.get('[data-testid="search-results"]').element
    const resultsContainer = results.parentElement

    expect(inputContainer?.parentElement).toContain(results)
    expect(inputContainer).not.toContain(results)
    expect(resultsContainer?.className).toContain('sm:absolute')
    expect(resultsContainer?.className).not.toMatch(/(^|\s)absolute(\s|$)/)
  })

  it('should describe validation feedback to screen readers', () => {
    const wrapper = mount(LocationSearchForm, {
      props: {
        query: 'B',
        countryCode: 'DE',
        countryLabel: 'Germany',
        isLoading: false,
        validationMessage: 'QUERY_TOO_SHORT',
        messages: resolveLocationLookupMessages(['en']),
      },
    })

    expect(wrapper.get('#location-query').attributes('aria-describedby')).toBe(
      'location-query-validation',
    )
    expect(wrapper.get('#location-query').attributes('aria-invalid')).toBe(
      'true',
    )
    expect(wrapper.get('#location-query-validation').attributes('role')).toBe(
      'alert',
    )
  })
})
