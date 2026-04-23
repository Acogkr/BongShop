package kr.acog.bongshop.config

import kotlinx.serialization.json.Json
import java.io.File

internal val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun loadPluginConfig(dataFolder: File): PluginConfig {
    val file = File(dataFolder, "config.json")
    if (!file.exists()) {
        val default = PluginConfig()
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(default))
        return default
    }
    return json.decodeFromString(file.readText())
}

fun savePluginConfig(dataFolder: File, config: PluginConfig) {
    val file = File(dataFolder, "config.json")
    file.parentFile.mkdirs()
    file.writeText(json.encodeToString(config))
}

fun loadShopsConfig(dataFolder: File): ShopsConfig {
    val file = File(dataFolder, "shops.json")
    if (!file.exists()) {
        val default = ShopsConfig()
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(default))
        return default
    }
    return json.decodeFromString(file.readText())
}

fun saveShopsConfig(dataFolder: File, config: ShopsConfig) {
    val file = File(dataFolder, "shops.json")
    file.parentFile.mkdirs()
    file.writeText(json.encodeToString(config))
}

fun loadShopItemsConfig(dataFolder: File): ShopItemsConfig {
    val itemFolder = File(dataFolder, "shop_items")
    itemFolder.mkdirs()

    val items = itemFolder.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
        .orEmpty()
        .sortedBy { it.nameWithoutExtension }
        .flatMap { file ->
            val shopId = file.nameWithoutExtension
            val fileConfig = json.decodeFromString<ShopItemsFileConfig>(file.readText())

            fileConfig.items.map { item ->
                ShopItemConfig(
                    id = item.id,
                    shopId = shopId,
                    itemName = item.itemName,
                    item = item.item,
                    payment = item.payment,
                    quantity = item.quantity,
                    basePrice = item.basePrice,
                    minPrice = item.minPrice,
                    maxPrice = item.maxPrice,
                    slot = item.slot,
                    stock = item.stock,
                    dailyBuyLimit = item.dailyBuyLimit,
                    buyLimit = item.buyLimit,
                    dailySellLimit = item.dailySellLimit,
                    page = item.page,
                    showPriceChange = item.showPriceChange
                )
            }
        }

    return ShopItemsConfig(items)
}

fun createShopItemsFile(dataFolder: File, shopId: String) {
    val itemFolder = File(dataFolder, "shop_items")
    itemFolder.mkdirs()
    val file = File(itemFolder, "$shopId.json")
    if (!file.exists()) {
        file.writeText(json.encodeToString(ShopItemsFileConfig()))
    }
}

fun deleteShopItemsFile(dataFolder: File, shopId: String) {
    val file = File(File(dataFolder, "shop_items"), "$shopId.json")
    if (file.exists()) {
        file.delete()
    }
}

fun saveShopItemsFile(dataFolder: File, shopId: String, items: List<ShopItemConfig>) {
    val file = File(File(dataFolder, "shop_items"), "$shopId.json")
    file.parentFile.mkdirs()
    val entries = items.map { item ->
        ShopItemFileEntry(
            id = item.id,
            itemName = item.itemName,
            item = item.item,
            payment = item.payment,
            quantity = item.quantity,
            basePrice = item.basePrice,
            minPrice = item.minPrice,
            maxPrice = item.maxPrice,
            slot = item.slot,
            stock = item.stock,
            dailyBuyLimit = item.dailyBuyLimit,
            buyLimit = item.buyLimit,
            dailySellLimit = item.dailySellLimit,
            page = item.page,
            showPriceChange = item.showPriceChange
        )
    }
    file.writeText(json.encodeToString(ShopItemsFileConfig(entries)))
}
