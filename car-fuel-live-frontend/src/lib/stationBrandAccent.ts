const DEFAULT_ACCENT = '#0d9488'

const brandAccents = [
  ['totalenergies', '#e3252d'],
  ['circle k', '#e30613'],
  ['tankpool24', '#005ca9'],
  ['raiffeisen', '#005ca9'],
  ['westfalen', '#005ca9'],
  ['aral', '#006fba'],
  ['shell', '#f5a623'],
  ['esso', '#0053a6'],
  ['avia', '#d71920'],
  ['jet', '#ffd100'],
  ['orlen', '#e30613'],
  ['star', '#009bdb'],
  ['agip', '#ffd100'],
  ['eni', '#ffd100'],
  ['hem', '#ee7d00'],
  ['omv', '#003b7a'],
  ['oil!', '#f58220'],
  ['q1', '#00853f'],
  ['baywa', '#005ca9'],
  ['bft', '#008a4b'],
  ['hoyer', '#e30613'],
  ['team', '#d71920'],
  ['score', '#6dbd45'],
  ['calpam', '#e31b23'],
  ['sprint', '#e30613'],
  ['classic', '#005ca9'],
] as const

export function getStationBrandAccent(brand: string | null): string {
  const normalizedBrand = brand?.trim().toLowerCase() ?? ''
  const accent = brandAccents.find(([name]) => normalizedBrand.includes(name))

  return accent?.[1] ?? DEFAULT_ACCENT
}
