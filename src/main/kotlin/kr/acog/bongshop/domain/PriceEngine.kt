package kr.acog.bongshop.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun calculateRandomPrice(minPrice: Int, maxPrice: Int, random: Random = Random.Default): Int {
    if (minPrice >= maxPrice) return minPrice
    return random.nextInt(minPrice, maxPrice + 1)
}

fun calculateDemandPrice(
    basePrice: Int,
    minPrice: Int,
    maxPrice: Int,
    recentSales: List<Int>
): Int {
    if (recentSales.isEmpty()) return basePrice

    val avgSales = recentSales.average()
    val maxSales = recentSales.max()
    val ratio = if (maxSales > 0) avgSales / maxSales else 0.5

    val priceRange = maxPrice - minPrice
    val adjustedPrice = maxPrice - (priceRange * ratio).toInt()

    return max(minPrice, min(maxPrice, adjustedPrice))
}
