import { describe, expect, it } from 'vitest'
import { markDuplicates, parseAuto, parseBulkText, parseCsv, parseJsonImport } from '../parseImport'

describe('parseBulkText', () => {
  it('parses chapters, topics and indented subtopics', () => {
    const input = `Chapter 1: Units and Measurements
- Physical quantities
  - Fundamental units
  - Derived units
- Dimensional analysis

Chapter 2: Motion in a Straight Line
- Speed and velocity
- Acceleration`

    const result = parseBulkText(input)
    expect(result.chapters).toHaveLength(2)
    expect(result.chapters[0].name).toBe('Units and Measurements')
    expect(result.chapters[0].topics).toHaveLength(2)
    expect(result.chapters[0].topics[0].subtopics).toEqual(['Fundamental units', 'Derived units'])
    expect(result.topicCount).toBe(4)
    expect(result.subtopicCount).toBe(2)
    expect(result.warnings).toHaveLength(0)
  })

  it('treats a trailing-colon line as a chapter heading', () => {
    const result = parseBulkText('Kinematics:\n- Speed\n- Velocity')
    expect(result.chapters[0].name).toBe('Kinematics')
    expect(result.chapters[0].topics).toHaveLength(2)
  })

  it('handles malformed input gracefully', () => {
    const result = parseBulkText('- orphan topic')
    expect(result.chapters).toHaveLength(1)
    expect(result.warnings.length).toBeGreaterThan(0)
  })

  it('returns a warning for empty input', () => {
    expect(parseBulkText('   ').warnings.length).toBeGreaterThan(0)
  })
})

describe('parseCsv', () => {
  it('parses a header row and builds the hierarchy', () => {
    const csv = `chapter,topic,subtopic
Optics,Reflection,Laws of reflection
Optics,Reflection,Plane mirrors
Optics,Refraction,Snell's law`
    const result = parseCsv(csv)
    expect(result.chapters).toHaveLength(1)
    expect(result.chapters[0].topics).toHaveLength(2)
    expect(result.chapters[0].topics[0].subtopics).toHaveLength(2)
    expect(result.topicCount).toBe(2)
  })

  it('warns when a row lacks a topic column', () => {
    const result = parseCsv('Optics\nReflection')
    expect(result.warnings.length).toBeGreaterThan(0)
  })

  it('supports quoted values containing commas', () => {
    const result = parseCsv('"Optics, Light",Reflection,Mirrors')
    expect(result.chapters[0].name).toBe('Optics, Light')
  })
})

describe('parseJsonImport', () => {
  it('parses a chapter array', () => {
    const result = parseJsonImport(JSON.stringify([{ name: 'Waves', topics: [{ name: 'Beats', subtopics: ['Frequency'] }] }]))
    expect(result.chapters[0].topics[0].subtopics).toEqual(['Frequency'])
  })

  it('reports invalid JSON instead of throwing', () => {
    expect(parseJsonImport('{not json').warnings.length).toBeGreaterThan(0)
  })
})

describe('parseAuto', () => {
  it('detects JSON, CSV and plain text', () => {
    expect(parseAuto('[{"name":"A","topics":[]}]').chapters[0].name).toBe('A')
    // A header row is skipped, so the first data row becomes the chapter.
    expect(parseAuto('Chapter,Topic\nA,B').chapters[0].name).toBe('A')
    expect(parseAuto('Chapter 1: X\n- y').chapters[0].name).toBe('X')
  })
})

describe('markDuplicates', () => {
  it('flags topics that already exist', () => {
    const parsed = [{ name: 'Ch', topics: [{ name: 'Speed', subtopics: [] }, { name: 'Velocity', subtopics: [] }] }]
    const marked = markDuplicates(parsed, ['speed'])
    expect((marked[0].topics[0] as { duplicate?: boolean }).duplicate).toBe(true)
    expect((marked[0].topics[1] as { duplicate?: boolean }).duplicate).toBe(false)
  })
})
