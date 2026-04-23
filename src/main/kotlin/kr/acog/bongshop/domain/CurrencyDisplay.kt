package kr.acog.bongshop.domain

import kr.acog.bongshop.config.LoreConfig
import kr.acog.bongshop.item.resolveItem
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.text.DecimalFormat

private val numberFormat = DecimalFormat("#,###")

fun formatNumber(value: Int): String = numberFormat.format(value)

fun formatNumber(value: Long): String = numberFormat.format(value)

fun formatNumber(value: Double): String = numberFormat.format(value.toLong())

fun formatPriceWithChange(
    currentPrice: Int,
    basePrice: Int,
    loreConfig: LoreConfig,
    showPriceChange: Boolean
): String {
    val priceText = formatNumber(currentPrice)
    if (!showPriceChange || basePrice <= 0) return priceText
    val diff = currentPrice - basePrice
    val suffix = when {
        diff > 0 -> loreConfig.priceUpFormat.replace("[diff]", formatNumber(diff))
        diff < 0 -> loreConfig.priceDownFormat.replace("[diff]", formatNumber(-diff))
        else -> ""
    }
    return priceText + suffix
}

fun resolveCurrencyDisplayName(coinName: String?): String {
    if (coinName == null) return "아이템"
    val resolved = resolveItem(coinName) ?: return coinName
    val meta = resolved.itemMeta ?: return coinName
    val displayName = meta.displayName()
    if (displayName != null) {
        return PlainTextComponentSerializer.plainText().serialize(displayName)
    }
    return "<lang:${resolved.translationKey()}>"
}
