package kr.acog.bongshop.plugin

import kotlinx.serialization.json.Json
import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.PriceChangeType
import kr.acog.bongshop.domain.ShopType
import org.bukkit.Material
import java.io.File

private val json = Json {
    prettyPrint = true
    encodeDefaults = true
}

fun generateDefaultConfigs(dataFolder: File) {
    generatePluginConfig(dataFolder)
    generateShopsConfig(dataFolder)
    generateShopItemsConfig(dataFolder)
}

private fun generatePluginConfig(dataFolder: File) {
    val file = File(dataFolder, "config.json")
    if (file.exists()) return

    val config = PluginConfig(
        priceChangeIntervalMinutes = 60,
        stockResetTime = "00:00"
    )

    file.parentFile.mkdirs()
    file.writeText(json.encodeToString(config))
}

private fun generateShopsConfig(dataFolder: File) {
    val file = File(dataFolder, "shops.json")
    if (file.exists()) return

    val config = ShopsConfig(
        shops = listOf(
            ShopGuiConfig(
                id = "buy_cosmetic",
                name = "치장상점",
                shopType = ShopType.BUY,
                title = "<gold>치장 구매상점",
                rows = 3,
                priceChangeType = PriceChangeType.RANDOM,
                backgroundMaterial = Material.AIR,
                prevPageButton = PageButtonConfig(
                    slot = 18,
                    material = Material.ARROW,
                    displayName = "<white>이전 페이지"
                ),
                nextPageButton = PageButtonConfig(
                    slot = 26,
                    material = Material.ARROW,
                    displayName = "<white>다음 페이지"
                ),
                timerButton = TimerButtonConfig(
                    slot = 22,
                    material = Material.CLOCK,
                    displayName = "<yellow>가격 변동까지",
                    lore = listOf("<white><hour>시간 <minute>분 <second>초")
                )
            ),
            ShopGuiConfig(
                id = "sell_resource",
                name = "자원판매상점",
                shopType = ShopType.SELL,
                title = "<gold>자원 판매상점",
                rows = 3,
                priceChangeType = PriceChangeType.DEMAND,
                backgroundMaterial = Material.AIR
            )
        )
    )

    file.parentFile.mkdirs()
    file.writeText(json.encodeToString(config))
}

private fun generateShopItemsConfig(dataFolder: File) {
    val itemFolder = File(dataFolder, "shop_items")
    itemFolder.mkdirs()

    val buyFile = File(itemFolder, "buy_cosmetic.json")
    if (!buyFile.exists()) {
        val buyConfig = ShopItemsFileConfig(
            items = listOf(
                ShopItemFileEntry(
                    id = "diamond_sword_buy",
                    itemName = "diamond_sword",
                    displayName = "<yellow>다이아몬드 검",
                    payment = VaultPaymentConfig(),
                    basePrice = 5000,
                    minPrice = 3000,
                    maxPrice = 7000,
                    slot = 10,
                    stock = 10,
                    dailyBuyLimit = 5,
                    buyLimit = 2
                ),
                ShopItemFileEntry(
                    id = "golden_apple_buy",
                    itemName = "golden_apple",
                    displayName = "<gold>황금 사과",
                    payment = CoinsEnginePaymentConfig(coinName = "gems"),
                    basePrice = 100,
                    minPrice = 50,
                    maxPrice = 150,
                    slot = 12,
                    stock = 5
                )
            )
        )
        buyFile.writeText(json.encodeToString(buyConfig))
    }

    val sellFile = File(itemFolder, "sell_resource.json")
    if (!sellFile.exists()) {
        val sellConfig = ShopItemsFileConfig(
            items = listOf(
                ShopItemFileEntry(
                    id = "potato_sell",
                    itemName = "potato",
                    displayName = "<yellow>감자",
                    payment = VaultPaymentConfig(),
                    quantity = 30,
                    basePrice = 5,
                    minPrice = 3,
                    maxPrice = 8,
                    slot = 10,
                    dailySellLimit = 100
                )
            )
        )
        sellFile.writeText(json.encodeToString(sellConfig))
    }
}
