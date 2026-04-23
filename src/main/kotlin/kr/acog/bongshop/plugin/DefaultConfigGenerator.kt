package kr.acog.bongshop.plugin

import kotlinx.serialization.json.Json
import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.PriceChangeType
import kr.acog.bongshop.domain.ShopType
import org.bukkit.Material
import org.bukkit.Sound
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
        stockResetTime = "00:00",
        sounds = SoundsConfig(
            purchaseSuccess = SoundEntry(Sound.ENTITY_PLAYER_LEVELUP, volume = 1.0f, pitch = 1.2f),
            purchaseFail = SoundEntry(Sound.ENTITY_VILLAGER_NO, volume = 1.0f, pitch = 1.0f),
            sellSuccess = SoundEntry(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume = 1.0f, pitch = 1.0f),
            sellFail = SoundEntry(Sound.ENTITY_VILLAGER_NO, volume = 1.0f, pitch = 0.8f),
            outOfStock = SoundEntry(Sound.BLOCK_NOTE_BLOCK_BASS, volume = 1.0f, pitch = 0.5f),
            buyLimitReached = SoundEntry(Sound.BLOCK_NOTE_BLOCK_BASS, volume = 1.0f, pitch = 0.7f),
            inventoryFull = SoundEntry(Sound.BLOCK_NOTE_BLOCK_BASS, volume = 1.0f, pitch = 0.6f),
            stockRestocked = SoundEntry(Sound.BLOCK_NOTE_BLOCK_CHIME, volume = 1.0f, pitch = 1.2f),
            priceChanged = SoundEntry(Sound.BLOCK_NOTE_BLOCK_BELL, volume = 1.0f, pitch = 1.0f)
        )
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
                    slots = listOf(18),
                    material = Material.ARROW,
                    displayName = "<white>이전 페이지"
                ),
                nextPageButton = PageButtonConfig(
                    slots = listOf(26),
                    material = Material.ARROW,
                    displayName = "<white>다음 페이지"
                ),
                timerButton = TimerButtonConfig(
                    slots = listOf(22),
                    material = Material.CLOCK,
                    displayName = "<yellow>가격 변동까지",
                    lore = listOf("<white><hour>시간 <minute>분 <second>초")
                ),
                infoButton = InfoButtonConfig(
                    slots = listOf(4),
                    material = Material.BOOK,
                    displayName = "<yellow>상점 정보",
                    lore = listOf("<gray>치장 아이템을 구매할 수 있는 상점입니다.")
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
