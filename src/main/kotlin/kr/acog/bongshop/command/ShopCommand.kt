package kr.acog.bongshop.command

import kr.acog.bongshop.domain.ShopType
import org.bukkit.entity.Player
import java.util.Optional

sealed interface ShopCommand {
    data class Create(val shopType: String, val shopName: String, val shopId: String) : ShopCommand
    data class Open(val shopId: String, val targetPlayer: Optional<Player>) : ShopCommand
    data class Delete(val shopId: String) : ShopCommand
    data class SellHistory(val targetPlayer: Player) : ShopCommand
    data object Reload : ShopCommand
}
