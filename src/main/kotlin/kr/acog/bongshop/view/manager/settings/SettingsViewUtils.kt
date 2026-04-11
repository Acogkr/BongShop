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

internal fun buildAdjustButton(delta: Int): ItemStack {
    val positive = delta > 0
    return BukkitItem.builder()
        .material(if (positive) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE)
        .build().create()
        .also { it.itemMeta = it.itemMeta?.apply { displayName(name(if (positive) "<green>+$delta" else "<red>$delta")) } }
}

internal fun buildValueDisplay(value: Int, label: String): ItemStack =
    BukkitItem.builder().material(Material.PAPER).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<white>$label  <yellow>$value"))
            lore(listOf(lore("<dark_gray>좌우 버튼으로 조절")))
        }}

internal fun buildBackButton(): ItemStack =
    BukkitItem.builder().material(Material.ARROW).build().create()
        .also { it.itemMeta = it.itemMeta?.apply { displayName(name("<gray>돌아가기")) } }

internal fun toggleButton(enabled: Boolean, label: String): ItemStack =
    BukkitItem.builder()
        .material(if (enabled) Material.LIME_DYE else Material.GRAY_DYE)
        .build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(if (enabled) "<green>$label 활성화" else "<gray>$label 없음"))
            lore(listOf(lore(if (enabled) "<dark_gray>클릭하여 비활성화" else "<dark_gray>클릭하여 활성화")))
        }}

internal fun buildNullableIntSettingsView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin,
    title: String,
    summaryMaterial: Material,
    summaryLabel: String,
    toggleLabel: String,
    nullLabel: String,
    valueLabel: String,
    getValue: (ShopItemConfig) -> Int?,
    applyValue: (ShopItemConfig, Int?) -> ShopItemConfig,
    buildSelf: (String, ShopItemConfig, ShopManager, JavaPlugin) -> BukkitChestView
): BukkitChestView {
    val controls: MutableMap<Int, BukkitViewControl> = mutableMapOf()
    val currentValue = getValue(itemConfig)

    controls[4] = ViewControl.just(buildNullableIntSummary(currentValue, summaryMaterial, summaryLabel, nullLabel))

    controls[13] = ViewControl.of(toggleButton(currentValue != null, toggleLabel)) { _ ->
        shopManager.updateItem(shopId, itemConfig.id) { cfg ->
            applyValue(cfg, if (getValue(cfg) != null) null else 1)
        }
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        ViewAction.Open(buildSelf(shopId, updated, shopManager, plugin))
    }

    if (currentValue != null) {
        val deltas = listOf(18 to -1000, 19 to -100, 20 to -10, 21 to -1, 23 to 1, 24 to 10, 25 to 100, 26 to 1000)
        for ((slot, delta) in deltas) {
            controls[slot] = ViewControl.of(buildAdjustButton(delta)) { _ ->
                shopManager.updateItem(shopId, itemConfig.id) { cfg ->
                    applyValue(cfg, ((getValue(cfg) ?: 1) + delta).coerceAtLeast(1))
                }
                val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
                ViewAction.Open(buildSelf(shopId, updated, shopManager, plugin))
            }
        }
        controls[22] = ViewControl.just(buildValueDisplay(currentValue, valueLabel))
    }

    controls[27] = ViewControl.of(buildBackButton()) { _ ->
        val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
        ViewAction.Open(buildShopManageItemView(shopId, updated, shopManager, plugin))
    }

    return chestViewBuilder()
        .title(title)
        .row(4)
        .contents(ViewContents.ofControls(controls))
        .build()
}

private fun buildNullableIntSummary(value: Int?, material: Material, label: String, nullLabel: String): ItemStack =
    BukkitItem.builder().material(material).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(label))
            lore(listOf(lore(if (value != null) "<gray>현재  <yellow>${value}개" else "<gray>현재  $nullLabel")))
        }}
