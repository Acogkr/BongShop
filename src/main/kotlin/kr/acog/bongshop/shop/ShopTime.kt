package kr.acog.bongshop.shop

import java.time.Duration
import java.time.Instant

fun getPriceChangeRemaining(shopManager: ShopManager): Duration {
    val intervalMinutes = shopManager.getPluginConfig().priceChangeIntervalMinutes.toLong()
    val elapsed = Duration.between(shopManager.lastPriceChangeTime, Instant.now())
    return Duration.ofMinutes(intervalMinutes).minus(elapsed).coerceAtLeast(Duration.ZERO)
}

