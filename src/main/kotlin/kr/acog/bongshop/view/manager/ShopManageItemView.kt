package kr.acog.bongshop.view.manager

import io.typst.inventory.bukkit.BukkitItem
import io.typst.view.ViewAction
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.bukkit.kotlin.*
import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.MoneyType
import kr.acog.bongshop.item.resolveItem
import kr.acog.bongshop.utils.lore
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.shop.ShopManager
import kr.acog.bongshop.view.manager.settings.*
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

fun buildShopManageItemView(
    shopId: String,
    itemConfig: ShopItemConfig,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitChestView {
    val title = "아이템 관리 - ${itemConfig.id}"
    val controls: MutableMap<Int, BukkitViewControl> = mutableMapOf()
    var isNavigating = false

    controls[4] = ViewControl.just(buildItemPreview(itemConfig))

    controls[10] = ViewControl.of(buildInfoButton(
        material = Material.GOLD_INGOT,
        label = "<gold>결제 설정",
        lines = buildPaymentLines(itemConfig)
    )) { _ ->
        isNavigating = true
        ViewAction.Open(buildPaymentSettingsView(shopId, itemConfig, shopManager, plugin))
    }

    controls[12] = ViewControl.of(buildInfoButton(
        material = Material.GOLD_NUGGET,
        label = "<gold>가격 설정",
        lines = buildPriceLines(itemConfig)
    )) { _ ->
        isNavigating = true
        ViewAction.Open(buildPriceSettingsView(shopId, itemConfig, shopManager, plugin))
    }

    controls[14] = ViewControl.of(buildInfoButton(
        material = Material.CHEST,
        label = "<gold>재고 설정",
        lines = listOf(
            if (itemConfig.stock != null) "<gray>현재  <yellow>${itemConfig.stock}개"
            else "<gray>현재  <dark_gray>무제한"
        )
    )) { _ ->
        isNavigating = true
        ViewAction.Open(buildStockSettingsView(shopId, itemConfig, shopManager, plugin))
    }

    controls[16] = ViewControl.of(buildInfoButton(
        material = Material.PAPER,
        label = "<gold>제한 설정",
        lines = buildLimitLines(itemConfig)
    )) { _ ->
        isNavigating = true
        ViewAction.Open(buildLimitSettingsView(shopId, itemConfig, shopManager, plugin))
    }

    controls[20] = ViewControl.of(buildInfoButton(
        material = Material.REPEATER,
        label = "<gold>수량 설정",
        lines = listOf("<gray>현재  <yellow>${itemConfig.quantity}개")
    )) { _ ->
        isNavigating = true
        ViewAction.Open(buildQuantitySettingsView(shopId, itemConfig, shopManager, plugin))
    }

    controls[22] = ViewControl.of(buildChangeItemButton()) { click ->
        val cursor = click.player.itemOnCursor
        if (cursor.type.isAir) {
            click.player.sendMessage(name("<red>변경할 아이템을 커서에 올린 후 클릭해주세요."))
            ViewAction.nothing()
        } else {
            shopManager.updateItem(shopId, itemConfig.id) { it.copy(item = cursor.clone().also { i -> i.amount = 1 }) }
            val updated = shopManager.getItem(shopId, itemConfig.id) ?: return@of ViewAction.nothing()
            isNavigating = true
            ViewAction.Open(buildShopManageItemView(shopId, updated, shopManager, plugin))
        }
    }

    controls[24] = ViewControl.of(buildDeleteButton(itemConfig)) { _ ->
        shopManager.removeItem(shopId, itemConfig.id)
        isNavigating = true
        ViewAction.Open(buildShopManageLayout(shopId, shopManager, plugin).toView(1))
    }

    return chestViewBuilder()
        .title(title)
        .row(3)
        .onClose {
            if (isNavigating) ViewAction.nothing()
            else ViewAction.Open(buildShopManageLayout(shopId, shopManager, plugin).toView(1))
        }
        .contents(ViewContents.ofControls(controls))
        .build()
}

private fun buildItemPreview(config: ShopItemConfig): ItemStack =
    (config.item?.clone() ?: resolveItem(config.itemName) ?: ItemStack(Material.BARRIER)).clone()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<gold>아이템 미리보기"))
            lore(listOf(
                lore("<gray>ID  <white>${config.id}"),
                lore("<gray>아이템  <white>${config.itemName}"),
                lore(""),
                lore("<dark_gray>상점에 표시될 아이템의 모습입니다.")
            ))
        }}

private fun buildInfoButton(material: Material, label: String, lines: List<String>): ItemStack =
    BukkitItem.builder().material(material).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(label))
            lore(lines.map { lore(it) } + listOf(lore(""), lore("<dark_gray>클릭하여 설정 페이지 이동")))
        }}

private fun buildPaymentLines(config: ShopItemConfig): List<String> = buildList {
    add(when (config.payment.moneyType) {
        MoneyType.VAULT -> "<gray>방식  <white>Vault (골드)"
        MoneyType.COINSENGINE -> "<gray>방식  <white>CoinsEngine"
        MoneyType.ITEM -> "<gray>방식  <white>아이템 교환"
    })
    if (config.payment is CoinsEnginePaymentConfig) add("<gray>화폐  <white>${config.payment.coinName}")
    if (config.payment is ItemPaymentConfig) add("<gray>교환  <white>${config.payment.currencyItem.ifBlank { "미설정" }}")
}

private fun buildPriceLines(config: ShopItemConfig): List<String> = buildList {
    add("<gray>기본 가격  <yellow>${config.basePrice}")
    if (config.minPrice != null && config.maxPrice != null) {
        add("<gray>가격 범위  <yellow>${config.minPrice} ~ ${config.maxPrice}")
    } else {
        add("<gray>가격 변동  <dark_gray>없음")
    }
}

private fun buildLimitLines(config: ShopItemConfig): List<String> = listOf(
    if (config.dailyBuyLimit != null) "<gray>일일 구매  <yellow>${config.dailyBuyLimit}개" else "<gray>일일 구매  <dark_gray>없음",
    if (config.buyLimit != null) "<gray>총 구매  <yellow>${config.buyLimit}개" else "<gray>총 구매  <dark_gray>없음",
    if (config.dailySellLimit != null) "<gray>일일 판매  <yellow>${config.dailySellLimit}개" else "<gray>일일 판매  <dark_gray>없음"
)

private fun buildChangeItemButton(): ItemStack =
    BukkitItem.builder().material(Material.EMERALD).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<green>표시 아이템 변경"))
            lore(listOf(lore("<dark_gray>커서에 아이템을 올린 후 클릭")))
        }}

private fun buildDeleteButton(config: ShopItemConfig): ItemStack =
    BukkitItem.builder().material(Material.BARRIER).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<red>아이템 삭제"))
            lore(listOf(lore("<dark_gray>ID: ${config.id}"), lore(""), lore("<red>이 작업은 되돌릴 수 없습니다.")))
        }}
