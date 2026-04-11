package kr.acog.bongshop.view.manager

import io.typst.inventory.bukkit.BukkitItem
import io.typst.inventory.bukkit.BukkitItemStackOps
import io.typst.view.ViewAction
import io.typst.view.ViewControl
import io.typst.view.bukkit.BukkitView
import io.typst.view.bukkit.kotlin.*
import io.typst.view.page.PageViewLayout
import kr.acog.bongshop.domain.ShopType
import kr.acog.bongshop.utils.lore
import kr.acog.bongshop.utils.name
import kr.acog.bongshop.shop.ShopManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.function.Function as JFunction

private val CONTENT_SLOTS = (0 until 18).toList()
private const val PREV_SLOT = 18
private const val NEXT_SLOT = 26

fun openShopListView(
    player: Player,
    plugin: JavaPlugin,
    shopManager: ShopManager
) {
    BukkitView.openView(buildShopListLayout(shopManager, plugin).toView(1), player, plugin)
}

private fun buildShopListLayout(
    shopManager: ShopManager,
    plugin: JavaPlugin
): BukkitPageViewLayout {
    val title = "상점 목록"
    val allInstances = shopManager.allInstances().values.toList()

    val elements: List<JFunction<BukkitPageContext, BukkitViewControl>> = allInstances.map { instance ->
        JFunction { _: BukkitPageContext ->
            val itemCount = shopManager.getAllItemsForShop(instance.guiConfig.id).size
            ViewControl.of(buildShopIcon(instance.guiConfig.id, instance.guiConfig.name, instance.guiConfig.shopType, itemCount)) { _ ->
                ViewAction.Open(buildShopManageLayout(instance.guiConfig.id, shopManager, plugin).toView(1))
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
        NEXT_SLOT to JFunction { context: BukkitPageContext ->
            ViewControl.of(buildNextButton(context.page, context.maxPage)) { _ ->
                if (context.page < context.maxPage) ViewAction.Update(context.layout.toView(context.page + 1).contents)
                else ViewAction.nothing()
            }
        }
    )

    return PageViewLayout.of(
        title,
        3,
        elements,
        CONTENT_SLOTS,
        controls,
        { _: BukkitPageContext -> JFunction { _: BukkitCloseEvent -> ViewAction.nothing() } },
        BukkitItemStackOps.INSTANCE
    )
}

private fun buildShopIcon(shopId: String, shopName: String, shopType: ShopType, itemCount: Int): ItemStack {
    val material = if (shopType == ShopType.BUY) Material.GOLD_BLOCK else Material.EMERALD_BLOCK
    val typeLabel = if (shopType == ShopType.BUY) "<yellow>구매 상점" else "<green>판매 상점"
    return BukkitItem.builder().material(material).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name("<white>$shopName"))
            lore(listOf(
                lore("<gray>ID  <white>$shopId"),
                lore("<gray>유형  $typeLabel"),
                lore("<gray>아이템  <white>${itemCount}개"),
                lore(""),
                lore("<yellow>클릭하여 관리 페이지 이동")
            ))
        }}
}

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
