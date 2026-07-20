package com.badal.medtrack

import java.util.Calendar

data class DayStat(val dateLabel: String, val taken: Int, val missed: Int, val total: Int)

object StatsCalculator {

    fun adherencePercent(taken: Int, missed: Int): Int {
        val total = taken + missed
        if (total == 0) return 0
        return ((taken.toFloat() / total) * 100).toInt()
    }

    fun weeklyBreakdown(logs: List<DoseLog>, isEnglish: Boolean = false): List<DayStat> {
        val cal = Calendar.getInstance()
        val result = mutableListOf<DayStat>()
        val dayLabels = if (isEnglish)
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        else
            listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000

            val dayLogs = logs.filter { it.scheduledDateTime in dayStart until dayEnd }
            val taken = dayLogs.count { it.status == DoseStatus.TAKEN }
            val missed = dayLogs.count { it.status == DoseStatus.MISSED }

            val label = dayLabels[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
            result.add(DayStat(label, taken, missed, dayLogs.size))
        }
        return result
    }

    /** Current streak = consecutive days (ending today) with zero missed doses and at least one taken. */
    fun currentStreak(logs: List<DoseLog>): Int {
        val byDay = groupByDay(logs)
        var streak = 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        while (true) {
            val dayStart = cal.timeInMillis
            val dayLogs = byDay[dayStart] ?: emptyList()
            if (dayLogs.isEmpty()) break
            val missed = dayLogs.count { it.status == DoseStatus.MISSED }
            val taken = dayLogs.count { it.status == DoseStatus.TAKEN }
            if (missed == 0 && taken > 0) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun longestStreak(logs: List<DoseLog>): Int {
        val byDay = groupByDay(logs).toSortedMap()
        var longest = 0
        var current = 0
        var previousDay: Long? = null

        for ((dayStart, dayLogs) in byDay) {
            val missed = dayLogs.count { it.status == DoseStatus.MISSED }
            val taken = dayLogs.count { it.status == DoseStatus.TAKEN }
            val isGoodDay = missed == 0 && taken > 0

            if (isGoodDay) {
                current = if (previousDay != null && dayStart - previousDay == 86400000L) current + 1 else 1
                if (current > longest) longest = current
            } else {
                current = 0
            }
            previousDay = dayStart
        }
        return longest
    }

    private fun groupByDay(logs: List<DoseLog>): Map<Long, List<DoseLog>> {
        return logs.groupBy { log ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = log.scheduledDateTime
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
}
