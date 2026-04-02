import { describe, expect, it } from 'vitest'

import { cn } from './utils'

describe('cn', () => {
  it('should merge conflicting Tailwind classes when duplicate utilities are provided', () => {
    const result = cn('px-2', 'text-sm', 'px-4')

    expect(result).toBe('text-sm px-4')
  })

  it('should ignore falsy values when optional classes are provided', () => {
    const result = cn('flex', undefined, false && 'hidden', null, 'items-center')

    expect(result).toBe('flex items-center')
  })
})
