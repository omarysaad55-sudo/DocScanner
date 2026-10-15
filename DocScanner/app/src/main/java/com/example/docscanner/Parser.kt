package com.example.docscanner

data class Fields(
    val recipient: String = "",
    val date: String = "",
    val quantity: String = "",
    val outgoing: String = ""
)

/**
 * Heuristic extraction of the four columns from OCR text (Arabic or English).
 * Results are only a first guess; the app lets the user correct them before saving.
 */
object Parser {

    private fun normalizeDigits(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            sb.append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')   // Arabic-Indic
                    in '۰'..'۹' -> '0' + (c - '۰')   // Persian
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    private val recipientKeys = listOf("إلى", "الى", "السيد", "السادة", "الأستاذ", "الاستاذ", "المحترم", "To", "Attn", "Attention", "Dear", "Recipient")
    private val dateKeys = listOf("التاريخ", "تاريخ", "Date")
    private val outgoingKeys = listOf("رقم الصادر", "الصادر", "صادر", "العدد", "Outgoing", "Out No", "Ref No", "Ref", "Reference")
    private val quantityKeys = listOf("الكمية", "كمية", "Quantity", "Qty", "Pcs", "Pieces", "قطعة")

    /** Text after "key" and an optional ':' on the same line, or the next line if nothing follows. */
    private fun after(lines: List<String>, keys: List<String>): String {
        for ((i, line) in lines.withIndex()) {
            for (k in keys) {
                val m = Regex("(?i)(^|[\\s:])" + Regex.escape(k) + "\\s*[:：\\-]?\\s*(.*)$").find(line) ?: continue
                val rest = m.groupValues[2].trim()
                if (rest.isNotEmpty()) return rest
                if (i + 1 < lines.size) return lines[i + 1].trim()
            }
        }
        return ""
    }

    fun parse(raw: String): Fields {
        val text = normalizeDigits(raw)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var recipient = after(lines, recipientKeys)
        // Titles like "السيد" are part of the name; keep what follows the key only.
        recipient = recipient.trim(':', '-', ' ')

        val dateRegex = Regex("\\b(\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4}|\\d{4}[/.\\-]\\d{1,2}[/.\\-]\\d{1,2})\\b")
        val dateLine = after(lines, dateKeys)
        val date = dateRegex.find(dateLine)?.value ?: dateRegex.find(text)?.value ?: dateLine

        val outLine = after(lines, outgoingKeys)
        val outgoing = Regex("[\\p{L}\\d][\\p{L}\\d/\\-.]*").find(outLine)?.value ?: ""

        val qtyLine = after(lines, quantityKeys)
        val quantity = Regex("\\d+([.,]\\d+)?").find(qtyLine)?.value ?: qtyLine

        return Fields(recipient, date, quantity, outgoing)
    }
}
