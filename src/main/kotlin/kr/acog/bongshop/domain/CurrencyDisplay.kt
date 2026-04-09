package kr.acog.bongshop.domain

import kr.acog.bongshop.item.resolveItem
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.text.DecimalFormat

private val numberFormat = DecimalFormat("#,###")

fun formatNumber(value: Int): String = numberFormat.format(value)

fun formatNumber(value: Long): String = numberFormat.format(value)

fun formatNumber(value: Double): String = numberFormat.format(value.toLong())

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
