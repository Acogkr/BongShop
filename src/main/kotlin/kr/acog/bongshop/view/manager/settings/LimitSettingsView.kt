package kr.acog.bongshop.view.manager.settings

import io.typst.inventory.bukkit.BukkitItem
import io.typst.view.ViewAction
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.bukkit.kotlin.*
import kr.acog.bongshop.config.ShopItemConfig
import kr.acog.bongshop.utils.lore
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.shop.ShopManager
import kr.acog.bongshop.view.manager.buildShopManageItemView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

fun buildLimitSettingsView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitChestView {
    val title = "제한 설정 - ${itemConfig.id}"
    val controls: MutableMap<Int, BukkitViewControl> = mutableMapOf()
    var isNavigating = false

    controls[4] = ViewControl.just(buildSummary(itemConfig))

    // dailyBuyLimit (row 2, slots 9-17)
    controls[10] = ViewControl.of(toggleButton(itemConfig.dailyBuyLimit != null, "일일 구매 제한")) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            cfg.copy(dailyBuyLimit = if (cfg.dailyBuyLimit != null) null else 1)
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildLimitSettingsView(shopId, updated, shopManager, plugin))
    }
    if (itemConfig.dailyBuyLimit != null) {
        listOf(11 to -100, 12 to -10, 14 to 10, 15 to 100).forEach { (slot, delta) ->
            controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
                shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                    cfg.copy(dailyBuyLimit = ((cfg.dailyBuyLimit ?: 1) + delta).coerceAtLeast(1))
                }
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                isNavigating = true
                ViewAction.Open(buildLimitSettingsView(shopId, updated, shopManager, plugin))
            }
        }
        controls[13] = ViewControl.just(buildValueDisplay(itemConfig.dailyBuyLimit, "일일 구매 제한"))
    }

    // buyLimit (row 3, slots 18-26)
    controls[19] = ViewControl.of(toggleButton(itemConfig.buyLimit != null, "총 구매 제한")) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            cfg.copy(buyLimit = if (cfg.buyLimit != null) null else 1)
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildLimitSettingsView(shopId, updated, shopManager, plugin))
    }
    if (itemConfig.buyLimit != null) {
        listOf(20 to -100, 21 to -10, 23 to 10, 24 to 100).forEach { (slot, delta) ->
            controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
                shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                    cfg.copy(buyLimit = ((cfg.buyLimit ?: 1) + delta).coerceAtLeast(1))
                }
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                isNavigating = true
                ViewAction.Open(buildLimitSettingsView(shopId, updated, shopManager, plugin))
            }
        }
        controls[22] = ViewControl.just(buildValueDisplay(itemConfig.buyLimit, "총 구매 제한"))
    }

    // dailySellLimit (row 4, slots 27-35)
    controls[28] = ViewControl.of(toggleButton(itemConfig.dailySellLimit != null, "일일 판매 제한")) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            cfg.copy(dailySellLimit = if (cfg.dailySellLimit != null) null else 1)
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildLimitSettingsView(shopId, updated, shopManager, plugin))
    }
    if (itemConfig.dailySellLimit != null) {
        listOf(29 to -100, 30 to -10, 32 to 10, 33 to 100).forEach { (slot, delta) ->
            controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
                shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                    cfg.copy(dailySellLimit = ((cfg.dailySellLimit ?: 1) + delta).coerceAtLeast(1))
                }
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                isNavigating = true
                ViewAction.Open(buildLimitSettingsView(shopId, updated, shopManager, plugin))
            }
        }
        controls[31] = ViewControl.just(buildValueDisplay(itemConfig.dailySellLimit, "일일 판매 제한"))
    }

    // Back button (row 5, slot 36)
    controls[36] = ViewControl.of(buildBackButton()) { _ ->
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        ViewAction.Open(buildShopManageItemView(shopId, updated, shopManager, plugin))
    }

    return chestViewBuilder()
        .title(title)
        .row(5)
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

private fun buildSummary(config: ShopItemConfig): ItemStack =
    BukkitItem.builder().material(Material.PAPER).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<gold>제한 설정"))
            lore(buildList {
                add(lore(if (config.dailyBuyLimit != null) "<gray>일일 구매  <yellow>${config.dailyBuyLimit}개" else "<gray>일일 구매  <dark_gray>없음"))
                add(lore(if (config.buyLimit != null) "<gray>총 구매  <yellow>${config.buyLimit}개" else "<gray>총 구매  <dark_gray>없음"))
                add(lore(if (config.dailySellLimit != null) "<gray>일일 판매  <yellow>${config.dailySellLimit}개" else "<gray>일일 판매  <dark_gray>없음"))
            })
        }}
