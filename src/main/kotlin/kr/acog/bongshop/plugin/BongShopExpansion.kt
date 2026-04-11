package kr.acog.bongshop.plugin

import kr.acog.bongshop.domain.formatNumber
import kr.acog.bongshop.shop.getPriceChangeRemaining
import kr.acog.bongshop.shop.ShopManager
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import java.time.Duration

class BongShopExpansion(
    private val shopManager: ShopManager
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "bongshop"
    override fun getAuthor(): String = "Acogkr"
    override fun getVersion(): String = "1.0.0"
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null

        val playerId = player.uniqueId.toString()
        val records = shopManager.getSellRecords()
        val playerRecord = records.players[playerId]

        // 점(.)과 언더스코어(_) 형식을 모두 지원
        // 예: playerselltotal.all / playerselltotal_all
        //     pricechange.time.second / pricechange_time_second
        return when {
            params == "playerselltotal.all" || params == "playerselltotal_all" -> {
                val total = playerRecord?.shopTotals?.values?.sum() ?: 0L
                formatNumber(total)
            }

            params.startsWith("playerselltotal.") -> {
                val shopId = params.removePrefix("playerselltotal.")
                val total = playerRecord?.shopTotals?.get(shopId) ?: 0L
                formatNumber(total)
            }

            params.startsWith("playerselltotal_") -> {
                val shopId = params.removePrefix("playerselltotal_")
                val total = playerRecord?.shopTotals?.get(shopId) ?: 0L
                formatNumber(total)
            }

            params == "pricechange.time.second" || params == "pricechange_time_second" -> {
                getRemainingTime().toSecondsPart().toString()
            }

            params == "pricechange.time.minute" || params == "pricechange_time_minute" -> {
                getRemainingTime().toMinutesPart().toString()
            }

            params == "pricechange.time.hour" || params == "pricechange_time_hour" -> {
                getRemainingTime().toHours().toString()
            }

            else -> null
        }
    }

    private fun getRemainingTime(): Duration = getPriceChangeRemaining(shopManager)
}
