package kr.acog.bongshop.economy

import kr.acog.bongshop.item.countSimilarItems
import kr.acog.bongshop.item.depositSimilarItems
import kr.acog.bongshop.item.removeSimilarItems
import kr.acog.bongshop.item.resolveItem
import org.bukkit.entity.Player

class ItemEconomyProvider(private val currencyItemName: String) {
    fun getBalance(player: Player): Double {
        val referenceItem = resolveItem(currencyItemName) ?: return 0.0
        return countSimilarItems(player, referenceItem).toDouble()
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        val referenceItem = resolveItem(currencyItemName) ?: return false
        return removeSimilarItems(player, referenceItem, amount.toInt())
    }

    fun deposit(player: Player, amount: Double): Boolean {
        val referenceItem = resolveItem(currencyItemName) ?: return false
        return depositSimilarItems(player, referenceItem, amount.toInt())
    }

    fun toOps(): EconomyOps = EconomyOps(
        getBalance = ::getBalance,
        withdraw = ::withdraw,
        deposit = ::deposit
    )
}
