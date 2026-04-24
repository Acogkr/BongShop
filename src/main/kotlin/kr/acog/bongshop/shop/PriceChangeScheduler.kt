package kr.acog.bongshop.shop

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class PriceChangeScheduler(
    private val shopManager: ShopManager,
    private val plugin: JavaPlugin
) {
    private var task: BukkitTask? = null

    fun start() {
        task?.cancel()
        val intervalTicks = shopManager.getPluginConfig().priceChangeIntervalMinutes.toLong() * 60L * 20L
        task = object : BukkitRunnable() {
            override fun run() {
                shopManager.refreshPrices()
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks)
    }

    fun stop() {
        task?.cancel()
        task = null
    }
}
