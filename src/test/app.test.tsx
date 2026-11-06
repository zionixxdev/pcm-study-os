import { describe, expect, it } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../App'
import { db } from '../lib/db'

/** End-to-end-ish smoke test: boots the real app against a fresh IndexedDB,
 * which exercises the seed installer, routing, layout and dashboard queries. */
describe('application bootstrap', () => {
  it('seeds the syllabus, renders the dashboard and navigates', async () => {
    await db.delete()
    await db.open()

    render(<App />)

    // Bootstrap finishes: the dashboard renders from seeded records.
    expect(await screen.findByRole('heading', { name: 'Dashboard' }, { timeout: 45000 })).toBeInTheDocument()
    expect(screen.getAllByText(/Overall progress/i).length).toBeGreaterThan(0)

    // Five seeded subjects exist in the database.
    await waitFor(async () => {
      const subjects = await db.subjects.toArray()
      expect(subjects.map((s) => s.name)).toEqual(
        expect.arrayContaining(['Physics', 'Chemistry', 'Mathematics', 'English Core', 'Computer Science']),
      )
    })

    const topics = await db.topics.count()
    expect(topics).toBeGreaterThan(150)

    // Navigation works (client-side route change renders the Syllabus page).
    await userEvent.click(screen.getAllByRole('link', { name: /Syllabus/i })[0])
    expect(await screen.findByRole('heading', { name: 'Syllabus' }, { timeout: 20000 })).toBeInTheDocument()
    expect((await screen.findAllByText('Physics')).length).toBeGreaterThan(0)
  }, 60000)
})
