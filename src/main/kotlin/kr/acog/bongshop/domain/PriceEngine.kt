package kr.acog.bongshop.domain

import kotlin.math.max
import kotlin.math.min
import java.util.Random as JavaRandom

private val gaussianRandom = JavaRandom()

fun calculateRandomPrice(minPrice: Int, maxPrice: Int, volatility: Double = 1.0): Int {
    if (minPrice >= maxPrice) {
        return minPrice
    }
    val safeVolatility = volatility.coerceAtLeast(0.1)
    val mean = (minPrice + maxPrice) / 2.0
    val stddev = (maxPrice - minPrice) * safeVolatility / 6.0
    val raw = gaussianRandom.nextGaussian() * stddev + mean
    return raw.toInt().coerceIn(minPrice, maxPrice)
}

fun calculateDemandPrice(
    minPrice: Int,
    maxPrice: Int,
    shopType: ShopType,
    itemVolume: Int,
    maxVolumeInShop: Int
): Int {
    val ratio = if (maxVolumeInShop > 0) {
        itemVolume.toDouble() / maxVolumeInShop
    } else {
        0.0
    }
    val price = when (shopType) {
        ShopType.BUY -> minPrice + ((maxPrice - minPrice) * ratio).toInt()
        ShopType.SELL -> maxPrice - ((maxPrice - minPrice) * ratio).toInt()
    }
    return max(minPrice, min(maxPrice, price))
}
