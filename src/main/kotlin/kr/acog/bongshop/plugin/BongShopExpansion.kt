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

        return when {
            params == "playerselltotal.all" -> {
                val total = playerRecord?.shopTotals?.values?.sum() ?: 0L
                formatNumber(total)
            }

            params.startsWith("playerselltotal.") -> {
                val shopId = params.removePrefix("playerselltotal.")
                val total = playerRecord?.shopTotals?.get(shopId) ?: 0L
                formatNumber(total)
            }

            params == "pricechange.time.second" -> {
                val remaining = getRemainingTime()
                remaining.toSecondsPart().toString()
            }

            params == "pricechange.time.minute" -> {
                val remaining = getRemainingTime()
                remaining.toMinutesPart().toString()
            }

            params == "pricechange.time.hour" -> {
                val remaining = getRemainingTime()
                remaining.toHours().toString()
            }

            else -> null
        }
    }

    private fun getRemainingTime(): Duration {
        return getPriceChangeRemaining(shopManager)
    }
}
