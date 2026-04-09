package kr.acog.bongshop.domain

import kr.acog.bongshop.config.ShopItemConfig
import kr.acog.bongshop.state.ShopItemState

sealed interface TransactionResult {
    data class Success(val amount: Int) : TransactionResult
    sealed interface Failure : TransactionResult

    data object OutOfStock : Failure
    data object BuyLimitReached : Failure
    data object DailyBuyLimitReached : Failure
    data object InsufficientFunds : Failure
    data object InventoryFull : Failure
    data object InsufficientItems : Failure
    data object ProviderUnavailable : Failure
    data object ItemUnavailable : Failure
    data object LimitReached : Failure
}

fun validateBuyTransaction(
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    buyerId: String,
    amount: Int,
    currentBalance: Double,
    currentPrice: Int
): TransactionResult {
    val stockRemaining = itemState.stockRemaining
    if (stockRemaining != null && stockRemaining < amount) {
        return TransactionResult.OutOfStock
    }

    val buyLimit = itemConfig.buyLimit
    if (buyLimit != null) {
        val purchased = itemState.playerBuyCounts[buyerId] ?: 0
        if (purchased + amount > buyLimit) {
            return TransactionResult.BuyLimitReached
        }
    }

    val dailyBuyLimit = itemConfig.dailyBuyLimit
    if (dailyBuyLimit != null) {
        val dailyPurchased = itemState.playerDailyBuyCounts[buyerId] ?: 0
        if (dailyPurchased + amount > dailyBuyLimit) {
            return TransactionResult.DailyBuyLimitReached
        }
    }

    if (currentBalance < currentPrice.toDouble() * amount) {
        return TransactionResult.InsufficientFunds
    }

    return TransactionResult.Success(amount)
}

fun validateSellTransaction(
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    sellerId: String,
    amount: Int,
    playerItemCount: Int
): TransactionResult {
    if (playerItemCount < amount * itemConfig.quantity) {
        return TransactionResult.InsufficientItems
    }

    val sellLimit = itemConfig.dailySellLimit
    if (sellLimit != null) {
        val sold = itemState.playerSellCounts[sellerId] ?: 0
        if (sold + amount > sellLimit) {
            return TransactionResult.LimitReached
        }
    }

    return TransactionResult.Success(amount)
}

fun calculateMaxBuyAmount(
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    buyerId: String,
    currentBalance: Double,
    currentPrice: Int
): Int {
    val stockLimit = itemState.stockRemaining ?: Int.MAX_VALUE
    val buyLimitRemaining = if (itemConfig.buyLimit != null) {
        itemConfig.buyLimit - (itemState.playerBuyCounts[buyerId] ?: 0)
    } else {
        Int.MAX_VALUE
    }
    val dailyBuyLimitRemaining = if (itemConfig.dailyBuyLimit != null) {
        itemConfig.dailyBuyLimit - (itemState.playerDailyBuyCounts[buyerId] ?: 0)
    } else {
        Int.MAX_VALUE
    }
    val balanceLimit = if (currentPrice > 0) (currentBalance / currentPrice).toInt() else Int.MAX_VALUE

    return maxOf(0, minOf(64, stockLimit, buyLimitRemaining, dailyBuyLimitRemaining, balanceLimit))
}

fun calculateMaxSellAmount(
    itemConfig: ShopItemConfig,
    itemState: ShopItemState,
    sellerId: String,
    playerItemCount: Int,
    cap: Int = 64
): Int {
    val itemLimit = playerItemCount / itemConfig.quantity
    val sellLimitRemaining = if (itemConfig.dailySellLimit != null) {
        itemConfig.dailySellLimit - (itemState.playerSellCounts[sellerId] ?: 0)
    } else {
        Int.MAX_VALUE
    }

    return maxOf(0, minOf(cap, itemLimit, sellLimitRemaining))
}
