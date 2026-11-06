package dev.zionixx.pcmstudyos.domain

/**
 * Lightweight notation formatting.
 *
 * The web app renders LaTeX with KaTeX. A native app has no TeX engine and this
 * one deliberately ships no WebView, so formulas are stored as plain text and
 * prettified here: ^2 becomes a superscript, _1 a subscript, \sqrt a radical
 * sign, and common operators become their Unicode forms. This is honest
 * "mathematical notation", not full LaTeX typesetting.
 */
object MathFormat {

    private val superscripts = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '-' to '⁻', '+' to '⁺', 'n' to 'ⁿ', 'i' to 'ⁱ',
    )

    private val subscripts = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '-' to '₋', '+' to '₊',
    )

    fun pretty(input: String): String {
        var text = input
            .replace("\\sqrt", "√")
            .replace("\\pi", "π")
            .replace("\\theta", "θ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\lambda", "λ")
            .replace("\\mu", "μ")
            .replace("\\omega", "ω")
            .replace("\\Delta", "Δ")
            .replace("\\delta", "δ")
            .replace("\\infty", "∞")
            .replace("\\int", "∫")
            .replace("\\sum", "∑")
            .replace("\\pm", "±")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\approx", "≈")
            .replace("\\neq", "≠")
            .replace("\\leq", "≤")
            .replace("\\geq", "≥")
            .replace("\\to", "→")
            .replace("->", "→")
            .replace("<=", "≤")
            .replace(">=", "≥")
            .replace("!=", "≠")
            .replace("*", "×")

        // √{x} reads badly; render it as √(x).
        text = Regex("""√\{([^}]*)\}""").replace(text) { "√(" + it.groupValues[1] + ")" }
        text = replaceScripts(text, '^', superscripts)
        text = replaceScripts(text, '_', subscripts)
        return text
    }

    private fun replaceScripts(text: String, marker: Char, table: Map<Char, Char>): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == marker && i + 1 < text.length) {
                if (text[i + 1] == '{') {
                    val end = text.indexOf('}', i + 2)
                    if (end > 0) {
                        val inner = text.substring(i + 2, end)
                        out.append(inner.map { table[it] ?: it }.joinToString(""))
                        i = end + 1
                        continue
                    }
                } else {
                    val next = text[i + 1]
                    val mapped = table[next]
                    if (mapped != null) {
                        out.append(mapped)
                        i += 2
                        continue
                    }
                }
            }
            out.append(ch)
            i++
        }
        return out.toString()
    }

    /** Strips TeX delimiters so stored expressions display cleanly. */
    fun stripDelimiters(input: String): String =
        input.replace("$$", "").replace("$", "").trim()
}
