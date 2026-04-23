package kr.acog.bongshop.item

import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.*
import kr.acog.bongshop.state.ShopItemState
import kr.acog.bongshop.utils.lore
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

private enum class TradeMode {
    BUY,
    SELL
}

fun buildBuyDisplayItem(
    base: ItemStack,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String,
    playerBalance: Double,
    showBalance: Boolean = true,
    loreConfig: LoreConfig = LoreConfig()
): ItemStack {
    return buildDisplayItem(base, itemConfig, itemState, playerUuid, TradeMode.BUY, playerBalance, 0, showBalance, loreConfig)
}

fun buildSellDisplayItem(
    base: ItemStack,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String,
    playerItemCount: Int,
    showBalance: Boolean = true,
    loreConfig: LoreConfig = LoreConfig()
): ItemStack {
    return buildDisplayItem(base, itemConfig, itemState, playerUuid, TradeMode.SELL, 0.0, playerItemCount, showBalance, loreConfig)
}

private fun buildDisplayItem(
    base: ItemStack,
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String,
    mode: TradeMode,
    playerBalance: Double,
    playerItemCount: Int,
    showBalance: Boolean,
    loreConfig: LoreConfig
): ItemStack {
    val clone = base.clone()
    val meta = clone.itemMeta ?: return clone

    val existingLore = meta.lore() ?: emptyList()
    val autoLore = buildAutoLore(itemConfig, itemState, playerUuid, mode, playerBalance, playerItemCount, showBalance, loreConfig)

    meta.lore(existingLore + autoLore)
    clone.itemMeta = meta
    return clone
}

private fun buildAutoLore(
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    playerUuid: String,
    mode: TradeMode,
    playerBalance: Double,
    playerItemCount: Int,
    showBalance: Boolean,
    loreConfig: LoreConfig
): List<Component> {
    val currentPrice = formatPriceWithChange(
        itemState.currentPrice,
        itemConfig.basePrice,
        loreConfig,
        itemConfig.showPriceChange
    )

    var balanceLine: String? = null
    val lines = buildList {
        when (itemConfig.payment) {
            is VaultPaymentConfig -> {
                val template = if (mode == TradeMode.BUY) loreConfig.vaultBuyLore else loreConfig.vaultSellLore
                add(template.replace("[price]", currentPrice))
                if (showBalance && mode == TradeMode.BUY && loreConfig.vaultBalanceLore.isNotBlank()) {
                    balanceLine = loreConfig.vaultBalanceLore.replace("[balance]", formatNumber(playerBalance))
                }
            }
            is CoinsEnginePaymentConfig -> {
                val template = if (mode == TradeMode.BUY) loreConfig.coinsEngineBuyLore else loreConfig.coinsEngineSellLore
                add(template
                    .replace("[price]", currentPrice)
                    .replace("[coin_name]", itemConfig.payment.coinName))
                if (showBalance && mode == TradeMode.BUY && loreConfig.coinsEngineBalanceLore.isNotBlank()) {
                    balanceLine = loreConfig.coinsEngineBalanceLore
                        .replace("[balance]", formatNumber(playerBalance))
                        .replace("[coin_name]", itemConfig.payment.coinName)
                }
            }
            is ItemPaymentConfig -> {
                val itemDisplayName = resolveCurrencyDisplayName(itemConfig.payment.currencyItem)
                val template = if (mode == TradeMode.BUY) loreConfig.itemBuyLore else loreConfig.itemSellLore
                add(template
                    .replace("[price]", currentPrice)
                    .replace("[itemname]", itemDisplayName))
                if (showBalance && mode == TradeMode.BUY && loreConfig.itemBalanceLore.isNotBlank()) {
                    balanceLine = loreConfig.itemBalanceLore
                        .replace("[balance]", formatNumber(playerBalance))
                        .replace("[itemname]", itemDisplayName)
                }
            }
        }

        if (mode == TradeMode.BUY) {
            if (itemConfig.stock != null) {
                val stockRemaining = itemState.stockRemaining ?: 0
                if (stockRemaining <= 0) {
                    add(loreConfig.stockEmptyLore)
                } else {
                    add(loreConfig.stockLore
                        .replace("[amount]", formatNumber(stockRemaining))
                        .replace("[max_amount]", formatNumber(itemConfig.stock)))
                }
            }

            if (itemConfig.dailyBuyLimit != null) {
                val dailyPurchased = itemState.playerDailyBuyCounts[playerUuid] ?: 0
                val remaining = itemConfig.dailyBuyLimit - dailyPurchased
                if (remaining <= 0) {
                    add(loreConfig.buyLimitEmptyLore)
                } else {
                    add(loreConfig.buyLimitLore
                        .replace("[amount]", formatNumber(remaining))
                        .replace("[max_amount]", formatNumber(itemConfig.dailyBuyLimit)))
                }
            }

            if (itemConfig.buyLimit != null) {
                val purchased = itemState.playerBuyCounts[playerUuid] ?: 0
                val remaining = itemConfig.buyLimit - purchased
                if (remaining <= 0) {
                    add(loreConfig.buyLimitEmptyLore)
                } else {
                    add(loreConfig.buyLimitLore
                        .replace("[amount]", formatNumber(remaining))
                        .replace("[max_amount]", formatNumber(itemConfig.buyLimit)))
                }
            }
        } else {
            if (showBalance && loreConfig.sellItemHoldingLore.isNotBlank()) {
                balanceLine = loreConfig.sellItemHoldingLore
                    .replace("[itemname]", resolveCurrencyDisplayName(itemConfig.itemName))
                    .replace("[amount]", formatNumber(playerItemCount))
            }

            if (itemConfig.quantity > 1) {
                add("<white>판매 단위: ${formatNumber(itemConfig.quantity)}개")
            }

            if (itemConfig.dailySellLimit != null) {
                val sold = itemState.playerSellCounts[playerUuid] ?: 0
                val remaining = itemConfig.dailySellLimit - sold
                if (remaining <= 0) {
                    add(loreConfig.dailySellLimitEmptyLore)
                } else {
                    add(loreConfig.dailySellLimitLore
                        .replace("[amount]", formatNumber(remaining))
                        .replace("[max_amount]", formatNumber(itemConfig.dailySellLimit)))
                }
            }
        }

        balanceLine?.let {
            add("")
            add(it)
        }
    }

    return lines.map { lore(it) }
}
