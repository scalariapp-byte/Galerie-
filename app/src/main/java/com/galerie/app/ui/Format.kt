package com.galerie.app.ui

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Format {
    private val fr = Locale.FRANCE
    private val dayThisYear = SimpleDateFormat("EEEE d MMMM", fr)
    private val dayOtherYear = SimpleDateFormat("EEEE d MMMM yyyy", fr)
    private val month = SimpleDateFormat("MMMM yyyy", fr)
    private val full = SimpleDateFormat("EEEE d MMMM yyyy · HH:mm", fr)
    private val shortDate = SimpleDateFormat("d MMMM yyyy", fr)
    private val time = SimpleDateFormat("HH:mm", fr)
    private val searchFmt = SimpleDateFormat("EEEE d MMMM yyyy", fr)

    fun dayKey(ms: Long): Long {
        val off = TimeZone.getDefault().getOffset(ms)
        return Math.floorDiv(ms + off, 86_400_000L)
    }

    fun monthKey(ms: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH)
    }

    fun dayLabel(ms: Long): String {
        val today = dayKey(System.currentTimeMillis())
        val k = dayKey(ms)
        return when (k) {
            today -> "Aujourd'hui"
            today - 1 -> "Hier"
            else -> {
                val y = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)
                val cy = Calendar.getInstance().get(Calendar.YEAR)
                (if (y == cy) dayThisYear else dayOtherYear).format(Date(ms)).cap()
            }
        }
    }

    fun monthLabel(ms: Long) = month.format(Date(ms)).cap()
    fun fullDate(ms: Long) = full.format(Date(ms)).cap()
    fun shortDate(ms: Long) = shortDate.format(Date(ms))
    fun time(ms: Long) = time.format(Date(ms))
    fun searchDate(ms: Long) = searchFmt.format(Date(ms))

    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes o"
        val units = arrayOf("Ko", "Mo", "Go", "To")
        var v = bytes / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return String.format(fr, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
    }

    fun duration(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    fun plural(n: Int, one: String, many: String) = "$n ${if (n > 1) many else one}"

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(fr), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun String.cap() = replaceFirstChar { it.titlecase(fr) }
}
