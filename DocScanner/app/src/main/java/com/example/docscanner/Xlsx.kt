package com.example.docscanner

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Minimal dependency-free .xlsx writer (one sheet, right-to-left, bilingual headers). */
object Xlsx {
    private val headers = listOf(
        "Intended recipient / المرسل إليه",
        "Date / التاريخ",
        "Quantity / الكمية",
        "Outgoing / الصادر",
        "Full text / النص الكامل"
    )

    private fun esc(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c == '\n' || c == '\t' || c == '\r' || c >= ' ' -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun cell(col: Int, row: Int, v: String) =
        "<c r=\"${'A' + col}$row\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>"

    fun write(file: File, records: List<Record>) {
        val sheet = StringBuilder()
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sheet.append("<sheetViews><sheetView rightToLeft=\"1\" workbookViewId=\"0\"/></sheetViews>")
        sheet.append("<cols><col min=\"1\" max=\"1\" width=\"32\" customWidth=\"1\"/><col min=\"2\" max=\"4\" width=\"20\" customWidth=\"1\"/><col min=\"5\" max=\"5\" width=\"60\" customWidth=\"1\"/></cols>")
        sheet.append("<sheetData>")
        sheet.append("<row r=\"1\">")
        headers.forEachIndexed { i, h -> sheet.append(cell(i, 1, h)) }
        sheet.append("</row>")
        records.forEachIndexed { idx, r ->
            val row = idx + 2
            sheet.append("<row r=\"$row\">")
            listOf(r.recipient, r.date, r.quantity, r.outgoing, r.rawText).forEachIndexed { i, v ->
                sheet.append(cell(i, row, v))
            }
            sheet.append("</row>")
        }
        sheet.append("</sheetData></worksheet>")

        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream()).use { z ->
            fun add(name: String, content: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(content.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            add("[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>")
            add("_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>")
            add("xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Documents\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            add("xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "</Relationships>")
            add("xl/worksheets/sheet1.xml", sheet.toString())
        }
    }
}
