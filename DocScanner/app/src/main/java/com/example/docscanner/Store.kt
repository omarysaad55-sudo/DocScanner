package com.example.docscanner

import org.json.JSONArray
import org.json.JSONObject
import android.content.Context

data class Record(
    val recipient: String,
    val date: String,
    val quantity: String,
    val outgoing: String,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Keeps saved records in app-private storage (SharedPreferences, JSON). */
object Store {
    private const val PREFS = "docscanner"
    private const val KEY = "records"

    fun load(ctx: Context): MutableList<Record> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(json)
        val out = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Record(
                o.optString("recipient"), o.optString("date"), o.optString("quantity"),
                o.optString("outgoing"), o.optString("raw"), o.optLong("t")
            )
        }
        return out
    }

    fun save(ctx: Context, list: List<Record>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("recipient", it.recipient).put("date", it.date)
                .put("quantity", it.quantity).put("outgoing", it.outgoing)
                .put("raw", it.rawText).put("t", it.createdAt))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
