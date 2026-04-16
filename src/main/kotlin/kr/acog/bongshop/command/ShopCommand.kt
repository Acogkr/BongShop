package kr.acog.bongshop.command

import org.bukkit.entity.Player
import java.util.Optional

sealed interface ShopCommand {
    data class Create(val shopType: String, val shopId: String, val shopName: String) : ShopCommand
    data class Open(val shopId: String, val targetPlayer: Optional<Player>) : ShopCommand
    data class Delete(val shopId: String) : ShopCommand
    data class SellHistory(val targetPlayer: Player) : ShopCommand
    data class Manage(val shopId: Optional<String>, val targetPlayer: Optional<Player>) : ShopCommand
    data object Reload : ShopCommand
    data object List : ShopCommand
    data object ForcePriceChange : ShopCommand
    data object ForceStockReset : ShopCommand
}
