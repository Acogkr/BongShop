package kr.acog.bongshop.view.manager.settings

import io.typst.view.ViewAction
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.bukkit.kotlin.*
import kr.acog.bongshop.config.ShopItemConfig
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.shop.ShopManager
import kr.acog.bongshop.view.manager.buildShopManageItemView
import io.typst.inventory.bukkit.BukkitItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

fun buildQuantitySettingsView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitChestView {
    val title = "수량 설정 - ${itemConfig.id}"
    val controls: MutableMap<Int, BukkitViewControl> = mutableMapOf()
    var isNavigating = false
    val quantity = itemConfig.quantity

    controls[4] = ViewControl.just(buildSummary(quantity))

    val deltas = listOf(11 to -10, 12 to -1, 14 to 1, 15 to 10)
    for ((slot, delta) in deltas) {
        controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
            shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                cfg.copy(quantity = (cfg.quantity + delta).coerceAtLeast(1))
            }
            val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
            isNavigating = true
            ViewAction.Open(buildQuantitySettingsView(shopId, updated, shopManager, plugin))
        }
    }
    controls[13] = ViewControl.just(buildValueDisplay(quantity, "판매 수량"))

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

private fun buildSummary(quantity: Int): ItemStack =
    BukkitItem.builder().material(Material.REPEATER).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<gold>판매 수량 설정"))
            lore(listOf(
                kr.acog.bongshop.utils.lore("<gray>현재 판매 1회당 차감되는 아이템 수량입니다."),
                kr.acog.bongshop.utils.lore("<gray>현재  <yellow>${quantity}개")
            ))
        }}
