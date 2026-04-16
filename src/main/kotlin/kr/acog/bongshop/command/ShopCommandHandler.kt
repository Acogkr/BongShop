package kr.acog.bongshop.command

import kr.acog.bongshop.config.ShopGuiConfig
import kr.acog.bongshop.domain.ShopType
import kr.acog.bongshop.domain.formatNumber
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.view.openShopView
import kr.acog.bongshop.view.manager.openShopListView
import kr.acog.bongshop.view.manager.openShopManageView
import kr.acog.bongshop.shop.ShopManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.jvm.optionals.getOrElse

class ShopCommandHandler(
    private val shopManager: ShopManager,
    private val plugin: JavaPlugin
) {

    fun handle(sender: CommandSender, command: ShopCommand) {
        when (command) {
            is ShopCommand.Create -> {
                val shopType = when (command.shopType) {
                    "구매" -> ShopType.BUY
                    "판매" -> ShopType.SELL
                    else -> {
                        sender.sendMessage(name("<red>유형은 '구매' 또는 '판매'만 가능합니다."))
                        return
                    }
                }

                if (shopManager.allShopIds().contains(command.shopId)) {
                    sender.sendMessage(name("<red>이미 존재하는 상점입니다: ${command.shopId}"))
                    return
                }

                shopManager.addShop(ShopGuiConfig(id = command.shopId, name = command.shopName, shopType = shopType)) {
                    sender.sendMessage(name("<green>상점이 생성되었습니다: ${command.shopName} (${command.shopId})"))
                }
            }

            is ShopCommand.Open -> {
                val target = command.targetPlayer.getOrElse { sender as? Player }
                if (target == null) {
                    sender.sendMessage(name("<red>콘솔에서는 플레이어를 지정해야 합니다."))
                    return
                }
                val instance = shopManager.getInstance(command.shopId)
                if (instance == null) {
                    sender.sendMessage(name("<red>상점을 찾을 수 없습니다: ${command.shopId}"))
                    return
                }
                openShopView(instance, target, plugin, shopManager)
                if (sender != target) {
                    sender.sendMessage(name("<green>${target.name}에게 ${instance.guiConfig.name} 상점을 열었습니다."))
                }
            }

            is ShopCommand.Delete -> {
                if (shopManager.getInstance(command.shopId) == null) {
                    sender.sendMessage(name("<red>상점을 찾을 수 없습니다: ${command.shopId}"))
                    return
                }
                shopManager.removeShop(command.shopId) {
                    sender.sendMessage(name("<green>상점이 삭제되었습니다: ${command.shopId}"))
                }
            }

            is ShopCommand.SellHistory -> {
                val target = command.targetPlayer
                val playerId = target.uniqueId.toString()
                val records = shopManager.getSellRecords()
                val playerRecord = records.players[playerId]

                val messages = shopManager.getPluginConfig().messages
                val displayName = PlainTextComponentSerializer.plainText().serialize(target.displayName())

                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.sellHistory.replace("<player_displayname>", displayName)
                ))

                if (playerRecord == null || playerRecord.shopTotals.isEmpty()) {
                    sender.sendMessage(name("<gray>판매기록이 없습니다."))
                    return
                }

                val allTotal = playerRecord.shopTotals.values.sum()
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.sellHistoryTotal.replace("<amount>", formatNumber(allTotal))
                ))

                for ((shopId, total) in playerRecord.shopTotals) {
                    val shopName = shopManager.getInstance(shopId)?.guiConfig?.name ?: shopId
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        messages.sellHistoryShop
                            .replace("<shopname>", shopName)
                            .replace("<amount>", formatNumber(total))
                    ))
                }
            }

            is ShopCommand.Manage -> {
                val target = command.targetPlayer.getOrElse { sender as? Player }
                if (target == null) {
                    sender.sendMessage(name("<red>콘솔에서는 플레이어를 지정해야 합니다."))
                    return
                }
                val shopId = command.shopId.orElse(null)
                if (shopId != null) {
                    if (shopManager.getInstance(shopId) == null) {
                        sender.sendMessage(name("<red>상점을 찾을 수 없습니다: $shopId"))
                        return
                    }
                    openShopManageView(shopId, target, plugin, shopManager)
                } else {
                    openShopListView(target, plugin, shopManager)
                }
            }

            ShopCommand.Reload -> {
                shopManager.reloadAll {
                    sender.sendMessage(name("<green>BongShop 설정을 리로드했습니다."))
                }
            }

            ShopCommand.List -> {
                val instances = shopManager.allInstances()
                if (instances.isEmpty()) {
                    sender.sendMessage(name("<gray>등록된 상점이 없습니다."))
                    return
                }
                sender.sendMessage(name("<green>=== 상점 목록 (${instances.size}개) ==="))
                instances.values.forEach { instance ->
                    val typeLabel = if (instance.guiConfig.shopType == ShopType.BUY) "구매" else "판매"
                    sender.sendMessage(name(
                        "<white>[${instance.guiConfig.id}] ${instance.guiConfig.name} <gray>($typeLabel / 아이템 ${instance.items.size}개)"
                    ))
                }
            }

            ShopCommand.ForcePriceChange -> {
                shopManager.refreshPrices()
                sender.sendMessage(name("<green>가격이 강제로 변동되었습니다."))
            }

            ShopCommand.ForceStockReset -> {
                shopManager.resetStockAndLimits()
                sender.sendMessage(name("<green>재고가 강제로 입고되었습니다."))
            }
        }
    }
}
