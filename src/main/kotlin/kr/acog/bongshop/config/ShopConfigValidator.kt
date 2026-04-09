package kr.acog.bongshop.config

import java.util.logging.Logger

data class ValidatedShopData(
    val pluginConfig: PluginConfig,
    val shopsConfig: ShopsConfig,
    val shopItemsConfig: ShopItemsConfig
)

fun validateShopData(
    pluginConfig: PluginConfig,
    shopsConfig: ShopsConfig,
    shopItemsConfig: ShopItemsConfig,
    logger: Logger
): ValidatedShopData {
    val seenShopIds = mutableSetOf<String>()
    val normalizedShops = shopsConfig.shops.mapNotNull { shop ->
        when {
            shop.id.isBlank() -> {
                logger.warning("Ignoring a shop with a blank id.")
                null
            }
            !seenShopIds.add(shop.id) -> {
                logger.warning("Ignoring duplicate shop id: ${shop.id}")
                null
            }
            else -> normalizeShopConfig(shop, logger)
        }
    }

    val validShopIds = normalizedShops.map { it.id }.toSet()
    val seenItemIdsByShop = mutableMapOf<String, MutableSet<String>>()
    val normalizedItems = shopItemsConfig.items.mapNotNull { item ->
        when {
            item.shopId !in validShopIds -> {
                logger.warning("Ignoring item with unknown shop: ${item.id} -> ${item.shopId}")
                null
            }
            item.id.isBlank() -> {
                logger.warning("Ignoring an item with a blank id.")
                null
            }
            item.itemName.isBlank() -> {
                logger.warning("Ignoring item with a blank itemName: ${item.id}")
                null
            }
            else -> {
                val seenItemIds = seenItemIdsByShop.getOrPut(item.shopId) { mutableSetOf() }
                if (!seenItemIds.add(item.id)) {
                    logger.warning("Ignoring duplicate item id in shop: ${item.shopId}/${item.id}")
                    null
                } else {
                    normalizeItemConfig(item, logger)
                }
            }
        }
    }

    return ValidatedShopData(
        pluginConfig = pluginConfig,
        shopsConfig = ShopsConfig(normalizedShops),
        shopItemsConfig = ShopItemsConfig(normalizedItems)
    )
}

private fun normalizeShopConfig(shop: ShopGuiConfig, logger: Logger): ShopGuiConfig? {
    val normalizedRows = shop.rows.coerceIn(1, 6)
    if (normalizedRows != shop.rows) {
        logger.warning("Adjusted rows for shop ${shop.id}: ${shop.rows} -> $normalizedRows")
    }
    return shop.copy(rows = normalizedRows)
}

private fun normalizeItemConfig(item: ShopItemConfig, logger: Logger): ShopItemConfig? {
    if (item.basePrice < 0) {
        logger.warning("Ignoring item ${item.id} because basePrice is negative.")
        return null
    }
    if (item.stock != null && item.stock <= 0) {
        logger.warning("Ignoring item ${item.id} because stock must be at least 1.")
        return null
    }
    if (item.dailyBuyLimit != null && item.dailyBuyLimit <= 0) {
        logger.warning("Ignoring item ${item.id} because dailyBuyLimit must be at least 1.")
        return null
    }
    if (item.buyLimit != null && item.buyLimit <= 0) {
        logger.warning("Ignoring item ${item.id} because buyLimit must be at least 1.")
        return null
    }
    if (item.dailySellLimit != null && item.dailySellLimit <= 0) {
        logger.warning("Ignoring item ${item.id} because dailySellLimit must be at least 1.")
        return null
    }
    if (item.quantity <= 0) {
        logger.warning("Ignoring item ${item.id} because quantity must be at least 1.")
        return null
    }

    val normalizedPayment = when (val payment = item.payment) {
        is VaultPaymentConfig -> payment
        is CoinsEnginePaymentConfig -> {
            if (payment.coinName.isBlank()) {
                logger.warning("Ignoring item ${item.id} because CoinsEngine coinName is blank.")
                return null
            }
            payment
        }
        is ItemPaymentConfig -> {
            if (payment.currencyItem.isBlank()) {
                logger.warning("Ignoring item ${item.id} because item currency is blank.")
                return null
            }
            payment
        }
    }

    return item.copy(payment = normalizedPayment)
}
