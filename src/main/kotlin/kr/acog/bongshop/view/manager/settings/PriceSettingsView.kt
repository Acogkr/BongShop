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

fun buildPriceSettingsView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitChestView {
    val title = "가격 설정 - ${itemConfig.id}"
    val controls: MutableMap<Int, BukkitViewControl> = mutableMapOf()
    var isNavigating = false
    val fluctuationEnabled = itemConfig.minPrice != null && itemConfig.maxPrice != null

    controls[4] = ViewControl.just(buildSummary(itemConfig))

    // basePrice adjusters (row 2, slots 9-17)
    listOf(10 to -1000, 11 to -100, 12 to -10, 14 to 10, 15 to 100, 16 to 1000).forEach { (slot, delta) ->
        controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
            shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                cfg.copy(basePrice = (cfg.basePrice + delta).coerceAtLeast(0))
            }
            val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
            isNavigating = true
            ViewAction.Open(buildPriceSettingsView(shopId, updated, shopManager, plugin))
        }
    }
    controls[13] = ViewControl.just(buildValueDisplay(itemConfig.basePrice, "기본 가격"))

    // fluctuation toggle (row 3, slot 19)
    controls[19] = ViewControl.of(toggleButton(fluctuationEnabled, "가격 변동")) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            if (fluctuationEnabled) {
                cfg.copy(minPrice = null, maxPrice = null)
            } else {
                val base = cfg.basePrice
                cfg.copy(
                    minPrice = (base - 100).coerceAtLeast(0),
                    maxPrice = base + 100
                )
            }
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        isNavigating = true
        ViewAction.Open(buildPriceSettingsView(shopId, updated, shopManager, plugin))
    }

    // minPrice adjusters (row 3, when enabled)
    if (fluctuationEnabled && itemConfig.minPrice != null) {
        listOf(20 to -100, 21 to -10, 23 to 10, 24 to 100).forEach { (slot, delta) ->
            controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
                shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                    cfg.copy(minPrice = ((cfg.minPrice ?: 0) + delta).coerceAtLeast(0))
                }
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                isNavigating = true
                ViewAction.Open(buildPriceSettingsView(shopId, updated, shopManager, plugin))
            }
        }
        controls[22] = ViewControl.just(buildValueDisplay(itemConfig.minPrice, "최소 가격"))
    }

    // maxPrice adjusters (row 4, when enabled)
    if (fluctuationEnabled && itemConfig.maxPrice != null) {
        listOf(29 to -100, 30 to -10, 32 to 10, 33 to 100).forEach { (slot, delta) ->
            controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
                shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                    cfg.copy(maxPrice = ((cfg.maxPrice ?: 0) + delta).coerceAtLeast(0))
                }
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                isNavigating = true
                ViewAction.Open(buildPriceSettingsView(shopId, updated, shopManager, plugin))
            }
        }
        controls[31] = ViewControl.just(buildValueDisplay(itemConfig.maxPrice, "최대 가격"))
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
    BukkitItem.builder().material(Material.GOLD_NUGGET).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<gold>가격 설정"))
            lore(buildList {
                add(lore("<gray>기본 가격  <yellow>${config.basePrice}"))
                if (config.minPrice != null && config.maxPrice != null) {
                    add(lore("<gray>가격 범위  <yellow>${config.minPrice} ~ ${config.maxPrice}"))
                } else {
                    add(lore("<gray>가격 변동  <dark_gray>없음 (고정 가격)"))
                }
            })
        }}
