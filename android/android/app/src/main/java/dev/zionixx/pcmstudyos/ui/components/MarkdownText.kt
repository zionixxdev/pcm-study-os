package dev.zionixx.pcmstudyos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.zionixx.pcmstudyos.domain.MathFormat

/**
 * Minimal Markdown renderer for notes: headings, bullets, numbered lists,
 * bold/italic/inline code, pipe tables and $maths$. Deliberately small — a full
 * Markdown engine would cost more RAM than the notes are worth.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.lines()
    Column(modifier = modifier) {
        var index = 0
        while (index < lines.size) {
            val raw = lines[index]
            val line = raw.trim()
            when {
                line.isEmpty() -> Spacer(Modifier.height(6.dp))

                line.startsWith("|") && index + 1 < lines.size && lines[index + 1].trim().matches(Regex("""^\|[\s:\-|]+\|$""")) -> {
                    val header = splitRow(line)
                    index += 2
                    val rows = mutableListOf<List<String>>()
                    while (index < lines.size && lines[index].trim().startsWith("|")) {
                        rows += splitRow(lines[index].trim())
                        index++
                    }
                    TableBlock(header, rows)
                    continue
                }

                line.startsWith("#") -> {
                    val level = line.takeWhile { it == '#' }.length
                    val text = line.dropWhile { it == '#' }.trim()
                    Text(
                        text = inline(text),
                        style = when (level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(inline(line.drop(2)), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Regex("""^\d+[.)]\s""").containsMatchIn(line) -> {
                    val number = line.takeWhile { it.isDigit() }
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("$number.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(6.dp))
                        Text(inline(line.drop(number.length + 1)), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                line == "---" -> HorizontalDivider(Modifier.padding(vertical = 6.dp))

                else -> Text(inline(line), style = MaterialTheme.typography.bodyMedium)
            }
            index++
        }
    }
}

@Composable
private fun TableBlock(header: List<String>, rows: List<List<String>>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            header.forEach { cell ->
                Text(
                    inline(cell),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f).padding(4.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Text(inline(cell), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(4.dp))
                }
            }
        }
    }
}

private fun splitRow(line: String): List<String> =
    line.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

/** Parses **bold**, *italic*, `code` and $maths$ into a styled string. */
fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val buffer = StringBuilder()

    fun flush() {
        if (buffer.isNotEmpty()) {
            append(buffer.toString())
            buffer.clear()
        }
    }

    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    flush()
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    buffer.append(text[i]); i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > 0) {
                    flush()
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    buffer.append(text[i]); i++
                }
            }
            text[i] == '$' -> {
                val end = text.indexOf('$', i + 1)
                if (end > 0) {
                    flush()
                    withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)) {
                        append(MathFormat.pretty(text.substring(i + 1, end)))
                    }
                    i = end + 1
                } else {
                    buffer.append(text[i]); i++
                }
            }
            text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                val end = text.indexOf('*', i + 1)
                if (end > 0) {
                    flush()
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    buffer.append(text[i]); i++
                }
            }
            else -> {
                buffer.append(text[i]); i++
            }
        }
    }
    flush()
}

@Composable
fun MathText(expression: String, modifier: Modifier = Modifier) {
    Text(
        text = MathFormat.pretty(MathFormat.stripDelimiters(expression)),
        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
        modifier = modifier,
    )
}
