import { useMemo } from 'react'
import katex from 'katex'
import 'katex/dist/katex.min.css'

/** Renders text with $inline$ and $$block$$ maths using KaTeX. */
export function MathText({ text, className }: { text: string; className?: string }) {
  const html = useMemo(() => renderMath(text), [text])
  return <span className={className} dangerouslySetInnerHTML={{ __html: html }} />
}

export function renderMath(input: string): string {
  const escaped = escapeHtml(input)
  return escaped
    .replace(/\$\$([\s\S]+?)\$\$/g, (_m, expr: string) => katexSafe(expr, true))
    .replace(/\$([^$\n]+?)\$/g, (_m, expr: string) => katexSafe(expr, false))
}

function katexSafe(expr: string, displayMode: boolean): string {
  try {
    return katex.renderToString(unescapeHtml(expr.trim()), { displayMode, throwOnError: false, output: 'html' })
  } catch {
    return escapeHtml(expr)
  }
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function unescapeHtml(s: string): string {
  return s.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
}
