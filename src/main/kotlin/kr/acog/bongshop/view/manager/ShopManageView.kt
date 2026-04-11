package kr.acog.bongshop.view.manager

import io.typst.inventory.bukkit.BukkitItem
import io.typst.inventory.bukkit.BukkitItemStackOps
import io.typst.view.ViewAction
import io.typst.view.ViewControl
import io.typst.view.bukkit.BukkitView
import io.typst.view.bukkit.kotlin.*
import io.typst.view.page.PageViewLayout
import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.MoneyType
import kr.acog.bongshop.item.resolveItem
import kr.acog.bongshop.utils.lore
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.shop.ShopManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.function.Function as JFunction

private val CONTENT_SLOTS = (0 until 45).toList()
private const val PREV_SLOT = 45
private const val ADD_ITEM_SLOT = 49
private const val NEXT_SLOT = 53

fun openShopManageView(
    shopId: String,
    player: Player,
    plugin: JavaPlugin,
    shopManager: ShopManager,
    page: Int = 1
) {
    BukkitView.openView(buildShopManageLayout(shopId, shopManager, plugin).toView(page), player, plugin)
}

internal fun buildShopManageLayout(
    shopId: String,
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitPageViewLayout {
    val allItems = shopManager.getAllItemsForShop(shopId)
    val shopDisplayName = shopManager.getInstance(shopId)?.guiConfig?.name ?: shopId
    val title = "상점 관리 - $shopDisplayName"

    val elements: List<JFunction<BukkitPageContext, BukkitViewControl>> = allItems.map { itemConfig ->
        JFunction { _: BukkitPageContext ->
            val baseItem = itemConfig.item?.clone() ?: resolveItem(itemConfig.itemName) ?: buildFallbackItem(itemConfig)
            ViewControl.of(buildManageItemDisplay(baseItem, itemConfig)) { _ ->
                ViewAction.Open(buildShopManageItemView(shopId, itemConfig, shopManager, plugin))
            }
        }
    }

    val controls: Map<Int, JFunction<BukkitPageContext, BukkitViewControl>> = mapOf(
        PREV_SLOT to JFunction { context: BukkitPageContext ->
            ViewControl.of(buildPrevButton(context.page, context.maxPage)) { _ ->
                if (context.page > 1) ViewAction.Update(context.layout.toView(context.page - 1).contents)
                else ViewAction.nothing()
            }
        },
        ADD_ITEM_SLOT to JFunction { _: BukkitPageContext ->
            ViewControl.of(buildAddItemButton()) { click ->
                val cursor = click.player.itemOnCursor
                if (cursor.type.isAir) {
                    click.player.sendMessage(name("<red>등록할 아이템을 커서에 올린 후 클릭해주세요."))
                    ViewAction.nothing()
                } else {
                    shopManager.addItem(shopId, cursor)
                    ViewAction.Open(buildShopManageLayout(shopId, shopManager, plugin).toView(1))
                }
            }
        },
        NEXT_SLOT to JFunction { context: BukkitPageContext ->
            ViewControl.of(buildNextButton(context.page, context.maxPage)) { _ ->
                if (context.page < context.maxPage) ViewAction.Update(context.layout.toView(context.page + 1).contents)
                else ViewAction.nothing()
            }
        }
    )

    return PageViewLayout.of(
        title,
        6,
        elements,
        CONTENT_SLOTS,
        controls,
        { _: BukkitPageContext -> JFunction { _: BukkitCloseEvent -> ViewAction.nothing() } },
        BukkitItemStackOps.INSTANCE
    )
}

private fun buildManageItemDisplay(base: ItemStack, config: ShopItemConfig): ItemStack {
    val paymentLine = when (config.payment.moneyType) {
        MoneyType.VAULT -> "<gray>결제  <white>Vault (골드)"
        MoneyType.COINSENGINE -> "<gray>결제  <white>CoinsEngine"
        MoneyType.ITEM -> "<gray>결제  <white>아이템 교환"
    }
    return base.clone().also { it.itemMeta = it.itemMeta?.apply {
        lore(buildList {
            add(lore("<gray>ID: <white>${config.id}"))
            add(lore("<gray>원본: <white>${config.itemName}"))
            add(lore(""))
            add(lore(paymentLine))
            add(lore("<gray>기본 가격: <yellow>${config.basePrice}"))
            add(lore(if (config.stock != null) "<gray>재고: <white>${config.stock}개" else "<gray>재고: <dark_gray>무제한"))
            if (config.dailyBuyLimit != null || config.buyLimit != null || config.dailySellLimit != null) {
                if (config.dailyBuyLimit != null) add(lore("<gray>일일 구매: <white>${config.dailyBuyLimit}개"))
                if (config.buyLimit != null) add(lore("<gray>총 구매: <white>${config.buyLimit}개"))
                if (config.dailySellLimit != null) add(lore("<gray>일일 판매: <white>${config.dailySellLimit}개"))
            }
            add(lore(""))
            add(lore("<yellow>클릭하여 상세 설정 이동"))
        })
    }}
}

private fun buildFallbackItem(config: ShopItemConfig): ItemStack =
    BukkitItem.builder().material(Material.BARRIER).build().create()
        .also { it.itemMeta = it.itemMeta?.apply { displayName(name("<red>아이템 정보 누락: ${config.itemName}")) } }

private fun buildPrevButton(currentPage: Int, maxPage: Int): ItemStack {
    val enabled = currentPage > 1
    return BukkitItem.builder()
        .material(if (enabled) Material.ARROW else Material.GRAY_STAINED_GLASS_PANE)
        .build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(if (enabled) "<gray>이전 페이지 <white>($currentPage/$maxPage)" else "<dark_gray>이전 페이지 없음"))
        }}
}

private fun buildNextButton(currentPage: Int, maxPage: Int): ItemStack {
    val enabled = currentPage < maxPage
    return BukkitItem.builder()
        .material(if (enabled) Material.ARROW else Material.GRAY_STAINED_GLASS_PANE)
        .build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(if (enabled) "<gray>다음 페이지 <white>($currentPage/$maxPage)" else "<dark_gray>다음 페이지 없음"))
        }}
}

private fun buildAddItemButton(): ItemStack =
    BukkitItem.builder().material(Material.EMERALD).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<green>새 아이템 등록"))
            lore(listOf(lore("<dark_gray>커서에 아이템을 올린 후 클릭")))
        }}
