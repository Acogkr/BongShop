package kr.acog.bongshop.shop

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class PriceChangeScheduler(
    private val shopManager: ShopManager,
    private val plugin: JavaPlugin
) {
    fun start() {
        val intervalMinutes = shopManager.getPluginConfig().priceChangeIntervalMinutes
        val intervalTicks = intervalMinutes.toLong() * 60L * 20L

        object : BukkitRunnable() {
            override fun run() {
                shopManager.refreshPrices()
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks)
    }
}
