package kr.acog.bongshop.view.manager.settings

import io.typst.view.bukkit.kotlin.BukkitChestView
import kr.acog.bongshop.config.ShopItemConfig
import kr.acog.bongshop.shop.ShopManager
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

fun buildStockSettingsView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitChestView = buildNullableIntSettingsView(
    shopId, itemConfig, shopManager, plugin,
    title = "재고 설정 - ${itemConfig.id}",
    summaryMaterial = Material.CHEST,
    summaryLabel = "<gold>아이템 재고 설정",
    toggleLabel = "재고",
    nullLabel = "<dark_gray>무제한",
    valueLabel = "재고",
    getValue = { it.stock },
    applyValue = { cfg, v -> cfg.copy(stock = v) },
    buildSelf = ::buildStockSettingsView
)
