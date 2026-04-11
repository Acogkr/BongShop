package kr.acog.bongshop.view.manager.settings

import io.typst.inventory.bukkit.BukkitItem
import io.typst.view.ViewAction
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.bukkit.BukkitView
import io.typst.view.bukkit.kotlin.*
import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.MoneyType
import kr.acog.bongshop.utils.lore
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.plugin.PendingChatInput
import kr.acog.bongshop.shop.ShopManager
import kr.acog.bongshop.view.manager.buildShopManageItemView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

fun buildPaymentSettingsView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitChestView {
    val title = "결제 방식 설정 - ${itemConfig.id}"
    val payment = itemConfig.payment
    val controls: MutableMap<Int, BukkitViewControl> = mutableMapOf()
    var isNavigating = false

    controls[4] = ViewControl.just(buildSummary(itemConfig))

    controls[11] = ViewControl.of(typeButton(Material.GOLD_BLOCK, Material.GOLD_INGOT, "Vault (골드)", payment.moneyType == MoneyType.VAULT)) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            cfg.copy(payment = VaultPaymentConfig())
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildPaymentSettingsView(shopId, updated, shopManager, plugin))
    }

    controls[13] = ViewControl.of(typeButton(Material.EMERALD_BLOCK, Material.EMERALD, "CoinsEngine", payment.moneyType == MoneyType.COINSENGINE)) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            val coinName = (cfg.payment as? CoinsEnginePaymentConfig)?.coinName ?: "gems"
            cfg.copy(payment = CoinsEnginePaymentConfig(coinName = coinName))
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildPaymentSettingsView(shopId, updated, shopManager, plugin))
    }

    controls[15] = ViewControl.of(typeButton(Material.DIAMOND_BLOCK, Material.DIAMOND, "아이템 교환", payment.moneyType == MoneyType.ITEM)) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            val currencyItem = (cfg.payment as? ItemPaymentConfig)?.currencyItem ?: "diamond"
            cfg.copy(payment = ItemPaymentConfig(currencyItem = currencyItem))
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildPaymentSettingsView(shopId, updated, shopManager, plugin))
    }

    when (payment) {
        is CoinsEnginePaymentConfig -> {
            controls[22] = ViewControl.of(coinNameButton(payment.coinName)) { click ->
                val player = click.player
                isNavigating = true
                PendingChatInput.await(player.uniqueId) { input ->
                    shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                        cfg.copy(payment = when (cfg.payment) {
                            is CoinsEnginePaymentConfig -> cfg.payment.copy(coinName = input)
                            else -> cfg.payment
                        })
                    }
                    val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@await
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        BukkitView.openView(buildPaymentSettingsView(shopId, updated, shopManager, plugin), player, plugin)
                    })
                }
                plugin.server.scheduler.runTask(plugin, Runnable { player.closeInventory() })
                player.sendMessage(name("<yellow>화폐 이름을 채팅으로 입력해주세요."))
                ViewAction.nothing()
            }
        }
        is ItemPaymentConfig -> {
            controls[22] = ViewControl.of(currencyItemButton(payment.currencyItem)) { click ->
                val cursor = click.player.itemOnCursor
                if (cursor.type.isAir) {
                    click.player.sendMessage(name("<red>교환 아이템을 커서에 올린 후 클릭해주세요."))
                    ViewAction.nothing()
                } else {
                    shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                        cfg.copy(payment = when (cfg.payment) {
                            is ItemPaymentConfig -> cfg.payment.copy(currencyItem = cursor.type.name.lowercase())
                            else -> cfg.payment
                        })
                    }
                    val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                    isNavigating = true
                    ViewAction.Open(buildPaymentSettingsView(shopId, updated, shopManager, plugin))
                }
            }
        }
        else -> Unit
    }

    return chestViewBuilder()
        .title(title)
        .row(3)
        .onClose {
            if (isNavigating) ViewAction.nothing()
            else {
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@onClose ViewAction.nothing()
                ViewAction.Open(buildShopManageItemView(shopId, updated, shopManager, plugin))
            }
        }
        .contents(ViewContents.ofControls(controls))
        .build()
}

private fun buildSummary(config: ShopItemConfig): ItemStack {
    val typeName = when (config.payment.moneyType) {
        MoneyType.VAULT -> "Vault (골드)"
        MoneyType.COINSENGINE -> "CoinsEngine"
        MoneyType.ITEM -> "아이템 교환"
    }
    return BukkitItem.builder().material(Material.GOLD_INGOT).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<gold>결제 방식 설정"))
            lore(buildList {
                add(lore("<gray>현재 방식  <white>$typeName"))
                when (val p = config.payment) {
                    is CoinsEnginePaymentConfig -> add(lore("<gray>화폐 이름  <white>${p.coinName}"))
                    is ItemPaymentConfig -> add(lore("<gray>교환 아이템  <white>${p.currencyItem.ifBlank { "미설정" }}"))
                    else -> Unit
                }
            })
        }}
}

private fun typeButton(selectedMat: Material, unselectedMat: Material, label: String, selected: Boolean): ItemStack =
    BukkitItem.builder().material(if (selected) selectedMat else unselectedMat).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(if (selected) "<green>$label" else "<gray>$label"))
            lore(listOf(lore(if (selected) "<green>선택됨" else "<dark_gray>클릭하여 선택")))
        }}

private fun coinNameButton(coinName: String): ItemStack =
    BukkitItem.builder().material(Material.NAME_TAG).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<yellow>화폐 이름 설정"))
            lore(listOf(
                lore("<gray>현재  <white>$coinName"),
                lore("<dark_gray>클릭 후 채팅으로 입력")
            ))
        }}

private fun currencyItemButton(currencyItem: String): ItemStack =
    BukkitItem.builder().material(Material.CHEST).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<yellow>교환 아이템 설정"))
            lore(listOf(
                lore("<gray>현재  <white>${currencyItem.ifBlank { "미설정" }}"),
                lore("<dark_gray>커서에 아이템을 올린 후 클릭")
            ))
        }}
