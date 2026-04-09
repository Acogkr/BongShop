package kr.acog.bongshop.shop

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration

class StockResetScheduler(
    private val shopManager: ShopManager,
    private val plugin: JavaPlugin
) {
    fun start() {
        val resetTimeStr = shopManager.getPluginConfig().stockResetTime
        val resetTime = parseTime(resetTimeStr)
        val ticksUntil = ticksUntilNextReset(resetTime)

        object : BukkitRunnable() {
            override fun run() {
                shopManager.resetStockAndLimits()
                scheduleDailyTask(resetTime)
            }
        }.runTaskLater(plugin, ticksUntil)
    }

    private fun scheduleDailyTask(resetTime: LocalTime) {
        val ticksPerDay = 86400L * 20L
        object : BukkitRunnable() {
            override fun run() {
                shopManager.resetStockAndLimits()
            }
        }.runTaskTimer(plugin, ticksPerDay, ticksPerDay)
    }

    private fun ticksUntilNextReset(resetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        var nextReset = LocalDate.now().atTime(resetTime)
        if (now >= nextReset) {
            nextReset = nextReset.plusDays(1)
        }
        val seconds = Duration.between(now, nextReset).seconds
        return seconds * 20L
    }

    private fun parseTime(timeStr: String): LocalTime {
        return runCatching { LocalTime.parse(timeStr) }.getOrElse { LocalTime.MIDNIGHT }
    }
}
