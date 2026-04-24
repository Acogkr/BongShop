package kr.acog.bongshop.state

import kotlinx.serialization.Serializable
import kr.acog.bongshop.config.json
import java.io.File

@Serializable
data class ShopItemState(
    val itemId: String,
    val stockRemaining: Int?,
    val currentPrice: Int = 0,
    val totalBought: Int = 0,
    val totalSold: Int = 0,
    val playerDailyBuyCounts: Map<String, Int> = emptyMap(),
    val playerBuyCounts: Map<String, Int> = emptyMap(),
    val playerSellCounts: Map<String, Int> = emptyMap()
)

@Serializable
data class ShopState(
    val shopId: String,
    val itemStates: Map<String, ShopItemState> = emptyMap()
)

@Serializable
data class PlayerSellRecord(
    val shopTotals: Map<String, Long> = emptyMap()
)

@Serializable
data class SellRecords(
    val players: Map<String, PlayerSellRecord> = emptyMap()
)

fun loadAllShopStates(dataFolder: File): Map<String, ShopState> {
    val file = File(dataFolder, "shopState.json")
    if (!file.exists()) return emptyMap()
    return json.decodeFromString(file.readText())
}

fun saveAllShopStates(dataFolder: File, states: Map<String, ShopState>) {
    val file = File(dataFolder, "shopState.json")
    file.parentFile.mkdirs()
    file.writeText(json.encodeToString(states))
}

fun loadSellRecords(dataFolder: File): SellRecords {
    val file = File(dataFolder, "sellRecords.json")
    if (!file.exists()) return SellRecords()
    return json.decodeFromString(file.readText())
}

fun saveSellRecords(dataFolder: File, records: SellRecords) {
    val file = File(dataFolder, "sellRecords.json")
    file.parentFile.mkdirs()
    file.writeText(json.encodeToString(records))
}
