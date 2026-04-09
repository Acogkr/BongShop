package kr.acog.bongshop.item

import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.*
import kr.acog.bongshop.state.ShopItemState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.inventory.ItemStack

private enum class TradeMode {
    BUY,
    SELL
}

fun buildBuyDisplayItem(
    base: ItemStack,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String
): ItemStack {
    return buildDisplayItem(base, itemConfig, itemState, playerUuid, TradeMode.BUY)
}

fun buildSellDisplayItem(
    base: ItemStack,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String
): ItemStack {
    return buildDisplayItem(base, itemConfig, itemState, playerUuid, TradeMode.SELL)
}

private fun buildDisplayItem(
    base: ItemStack,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String,
    mode: TradeMode
): ItemStack {
    val clone = base.clone()
    val meta = clone.itemMeta ?: return clone

    if (itemConfig.displayName != null) {
        meta.displayName(MiniMessage.miniMessage().deserialize(itemConfig.displayName))
    }

    val existingLore = meta.lore() ?: emptyList()
    val customLore = itemConfig.lore.map { line ->
        MiniMessage.miniMessage().deserialize(replacePlaceholders(line, itemConfig, itemState, playerUuid))
    }
    val autoLore = buildAutoLore(itemConfig, itemState, playerUuid, mode)

    meta.lore(existingLore + customLore + autoLore)
    clone.itemMeta = meta
    return clone
}

private fun buildAutoLore(
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String,
    mode: TradeMode
): List<Component> {
    val currentPrice = formatNumber(itemState.currentPrice)
    val action = if (mode == TradeMode.BUY) "구매" else "판매"

    val lines = buildList {
        when (itemConfig.payment) {
            is VaultPaymentConfig -> add("<white>$action 가격: $currentPrice 골드")
            is CoinsEnginePaymentConfig -> add("<white>$action 가격: $currentPrice ${itemConfig.payment.coinName}")
            is ItemPaymentConfig -> {
                val itemDisplayName = resolveCurrencyDisplayName(itemConfig.payment.currencyItem)
                add("<white>$action 가격: $itemDisplayName ${currentPrice}개")
            }
        }

        if (mode == TradeMode.BUY) {
            if (itemConfig.stock != null) {
                val stockRemaining = itemState.stockRemaining ?: 0
                if (stockRemaining <= 0) {
                    add("<white>남은 재고 : <red>재고 없음")
                } else {
                    add("<white>남은 재고 : ${formatNumber(stockRemaining)}/${formatNumber(itemConfig.stock)}")
                }
            }

            if (itemConfig.dailyBuyLimit != null) {
                val dailyPurchased = itemState.playerDailyBuyCounts[playerUuid] ?: 0
                val remaining = itemConfig.dailyBuyLimit - dailyPurchased
                if (remaining <= 0) {
                    add("<white>하루당 구매 갯수 : <red>구매 불가")
                } else {
                    add("<white>하루당 구매 갯수 : ${formatNumber(remaining)}/${formatNumber(itemConfig.dailyBuyLimit)}")
                }
            }

            if (itemConfig.buyLimit != null) {
                val purchased = itemState.playerBuyCounts[playerUuid] ?: 0
                val remaining = itemConfig.buyLimit - purchased
                if (remaining <= 0) {
                    add("<white>구매 제한 갯수 : <red>구매 불가")
                } else {
                    add("<white>구매 제한 갯수 : ${formatNumber(remaining)}/${formatNumber(itemConfig.buyLimit)}")
                }
            }
        } else {
            if (itemConfig.quantity > 1) {
                add("<white>판매 단위: ${formatNumber(itemConfig.quantity)}개")
            }

            if (itemConfig.dailySellLimit != null) {
                val sold = itemState.playerSellCounts[playerUuid] ?: 0
                val remaining = itemConfig.dailySellLimit - sold
                if (remaining <= 0) {
                    add("<white>하루당 판매 갯수 : <red>판매 불가")
                } else {
                    add("<white>하루당 판매 갯수 : ${formatNumber(remaining)}/${formatNumber(itemConfig.dailySellLimit)}")
                }
            }
        }
    }

    return lines.map { MiniMessage.miniMessage().deserialize(it) }
}

private fun replacePlaceholders(
    line: String,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String
): String {
    val stockRemain = itemState.stockRemaining?.let { formatNumber(it) } ?: "∞"
    val stockMax = itemConfig.stock?.let { formatNumber(it) } ?: "∞"
    val dailyBuyLimitMax = itemConfig.dailyBuyLimit?.let { formatNumber(it) } ?: "∞"
    val dailyBuyLimitRemain = if (itemConfig.dailyBuyLimit != null) {
        val dailyPurchased = itemState.playerDailyBuyCounts[playerUuid] ?: 0
        formatNumber(itemConfig.dailyBuyLimit - dailyPurchased)
    } else "∞"
    val buyLimitMax = itemConfig.buyLimit?.let { formatNumber(it) } ?: "∞"
    val buyLimitRemain = if (itemConfig.buyLimit != null) {
        val purchased = itemState.playerBuyCounts[playerUuid] ?: 0
        formatNumber(itemConfig.buyLimit - purchased)
    } else "∞"
    val sellLimitMax = itemConfig.dailySellLimit?.let { formatNumber(it) } ?: "∞"
    val sellLimitRemain = if (itemConfig.dailySellLimit != null) {
        val sold = itemState.playerSellCounts[playerUuid] ?: 0
        formatNumber(itemConfig.dailySellLimit - sold)
    } else "∞"

    return line
        .replace("<price>", formatNumber(itemState.currentPrice))
        .replace("<base_price>", formatNumber(itemConfig.basePrice))
        .replace("<stock_remain>", stockRemain)
        .replace("<stock_max>", stockMax)
        .replace("<daily_buy_limit_remain>", dailyBuyLimitRemain)
        .replace("<daily_buy_limit_max>", dailyBuyLimitMax)
        .replace("<buy_limit_remain>", buyLimitRemain)
        .replace("<buy_limit_max>", buyLimitMax)
        .replace("<sell_limit_remain>", sellLimitRemain)
        .replace("<sell_limit_max>", sellLimitMax)
        .replace("<quantity>", formatNumber(itemConfig.quantity))
}
