import { useMemo } from 'react'
import katex from 'katex'

/** Small, dependency-free markdown renderer for notes.
 * Supports headings, bold, italic, inline code, bullet/numbered lists,
 * simple pipe tables and $math$ / $$math$$. It is deliberately limited —
 * enough for study notes without shipping a heavy editor. */
export function Markdown({ source }: { source: string }) {
  const html = useMemo(() => toHtml(source), [source])
  return <div className="prose-notes space-y-2 text-sm leading-relaxed" dangerouslySetInnerHTML={{ __html: html }} />
}

function inline(text: string): string {
  let out = escapeHtml(text)
  out = out.replace(/`([^`]+)`/g, '<code class="rounded bg-raised px-1 py-0.5 text-[0.9em]">$1</code>')
  out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  out = out.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
  out = out.replace(/\[([^\]]+)\]\((https?:[^)\s]+)\)/g, '<a class="text-accent underline" href="$2" target="_blank" rel="noreferrer noopener">$1</a>')
  out = out.replace(/\$\$([\s\S]+?)\$\$/g, (_m, e: string) => math(e, true))
  out = out.replace(/\$([^$\n]+?)\$/g, (_m, e: string) => math(e, false))
  return out
}

function math(expr: string, display: boolean): string {
  try {
    return katex.renderToString(expr.trim(), { displayMode: display, throwOnError: false, output: 'html' })
  } catch {
    return escapeHtml(expr)
  }
}

function toHtml(source: string): string {
  const lines = source.split(/\r?\n/)
  const out: string[] = []
  let listType: 'ul' | 'ol' | null = null
  let inTable = false

  const closeList = () => {
    if (listType) {
      out.push(`</${listType}>`)
      listType = null
    }
  }
  const closeTable = () => {
    if (inTable) {
      out.push('</tbody></table></div>')
      inTable = false
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i]
    const line = raw.trim()

    if (!line) {
      closeList()
      closeTable()
      continue
    }

    // Table block: header row followed by a separator row.
    if (line.startsWith('|') && line.endsWith('|') && lines[i + 1]?.trim().match(/^\|[\s:-|]+\|$/)) {
      closeList()
      closeTable()
      const headers = splitRow(line)
      const rows: string[][] = []
      let j = i + 2
      while (j < lines.length && lines[j].trim().startsWith('|')) {
        rows.push(splitRow(lines[j].trim()))
        j++
      }
      out.push('<div class="overflow-x-auto"><table class="w-full border-collapse text-xs">')
      out.push(`<thead><tr>${headers.map((h) => `<th class="border border-line bg-raised px-2 py-1 text-left">${inline(h)}</th>`).join('')}</tr></thead><tbody>`)
      for (const r of rows) out.push(`<tr>${r.map((c) => `<td class="border border-line px-2 py-1">${inline(c)}</td>`).join('')}</tr>`)
      out.push('</tbody></table></div>')
      i = j - 1
      continue
    }

    const heading = line.match(/^(#{1,4})\s+(.*)$/)
    if (heading) {
      closeList()
      closeTable()
      const level = heading[1].length
      const sizes = ['text-lg font-semibold', 'text-base font-semibold', 'text-sm font-semibold', 'text-xs font-semibold uppercase tracking-wide']
      out.push(`<h${level} class="${sizes[level - 1]} mt-3">${inline(heading[2])}</h${level}>`)
      continue
    }

    const ul = line.match(/^[-*]\s+(.*)$/)
    const ol = line.match(/^\d+[.)]\s+(.*)$/)
    if (ul || ol) {
      const type = ul ? 'ul' : 'ol'
      if (listType !== type) {
        closeList()
        out.push(type === 'ul' ? '<ul class="ml-4 list-disc space-y-0.5">' : '<ol class="ml-4 list-decimal space-y-0.5">')
        listType = type
      }
      out.push(`<li>${inline((ul ?? ol)![1])}</li>`)
      continue
    }

    closeList()
    out.push(`<p>${inline(line)}</p>`)
  }
  closeList()
  closeTable()
  return out.join('\n')
}

function splitRow(line: string): string[] {
  return line.replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim())
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
