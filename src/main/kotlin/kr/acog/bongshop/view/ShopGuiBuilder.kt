package kr.acog.bongshop.view

import io.typst.inventory.bukkit.BukkitItem
import io.typst.view.ViewAction
import io.typst.view.ViewContents
import io.typst.view.ViewControl
import io.typst.view.bukkit.BukkitView
import io.typst.view.bukkit.kotlin.*
import kr.acog.bongshop.ColorUtils
import kr.acog.bongshop.config.ShopItemConfig
import kr.acog.bongshop.config.TimerButtonConfig
import kr.acog.bongshop.domain.*
import kr.acog.bongshop.item.buildBuyDisplayItem
import kr.acog.bongshop.item.buildSellDisplayItem
import kr.acog.bongshop.item.resolveItem
import kr.acog.bongshop.shop.getPriceChangeRemaining
import kr.acog.bongshop.shop.ShopInstance
import kr.acog.bongshop.shop.ShopManager
import kr.acog.bongshop.state.ShopItemState
import kr.acog.bongshop.utils.lore
import kr.acog.bongshop.utils.name
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

fun openShopView(
    shopInstance: ShopInstance,
    player: Player,
    plugin: JavaPlugin,
    shopManager: ShopManager,
    page: Int = 0
) {
    val backgroundItem = resolveBackgroundItem(shopInstance.guiConfig.backgroundMaterial)
    val view = chestViewBuilder()
        .title(ColorUtils.colorize(shopInstance.guiConfig.title))
        .row(shopInstance.guiConfig.rows)
        .contents(buildShopContents(shopInstance, player, shopManager, plugin, backgroundItem, page))
        .build()
    BukkitView.openView(view, player, plugin)
}

private fun buildShopContents(
    shopInstance: ShopInstance,
    player: Player,
    shopManager: ShopManager,
    plugin: JavaPlugin,
    backgroundItem: ItemStack,
    page: Int
): BukkitViewContents {
    return ViewContents.ofControls(buildControls(shopInstance, player, shopManager, plugin, backgroundItem, page))
}

private fun buildControls(
    shopInstance: ShopInstance,
    player: Player,
    shopManager: ShopManager,
    plugin: JavaPlugin,
    backgroundItem: ItemStack,
    page: Int
): Map<Int, BukkitViewControl> {
    val guiConfig = shopInstance.guiConfig
    val totalSlots = guiConfig.rows * 9
    val playerId = player.uniqueId.toString()
    val isBuyShop = guiConfig.shopType == ShopType.BUY

    val reservedSlots = mutableSetOf<Int>()
    guiConfig.prevPageButton?.let { reservedSlots.add(it.slot) }
    guiConfig.nextPageButton?.let { reservedSlots.add(it.slot) }
    guiConfig.timerButton?.let { reservedSlots.add(it.slot) }

    val itemsWithSlots = shopInstance.items.filter { it.slot != null }
    val itemsWithoutSlots = shopInstance.items.filter { it.slot == null }

    val availableSlots = (0 until totalSlots).filter { it !in reservedSlots }
    val freeSlots = availableSlots.filter { slot -> itemsWithSlots.none { it.slot == slot } }
    val pageSize = freeSlots.size
    val pagedItems = if (itemsWithoutSlots.isNotEmpty() && pageSize > 0) {
        val start = page * pageSize
        itemsWithoutSlots.drop(start).take(pageSize)
    } else emptyList()

    val totalPages = if (itemsWithoutSlots.isNotEmpty() && pageSize > 0) {
        (itemsWithoutSlots.size + pageSize - 1) / pageSize
    } else 1

    val backgroundControls: Map<Int, BukkitViewControl> = (0 until totalSlots)
        .associateWith { ViewControl.just(backgroundItem) }

    val fixedItemControls: Map<Int, BukkitViewControl> = itemsWithSlots
        .mapNotNull { itemConfig ->
            val slot = itemConfig.slot ?: return@mapNotNull null
            if (slot !in 0 until totalSlots) return@mapNotNull null
            slot to buildItemControl(itemConfig, shopInstance, player, shopManager, plugin, isBuyShop, backgroundItem, page)
        }
        .toMap()

    val pagedItemControls: Map<Int, BukkitViewControl> = pagedItems
        .zip(freeSlots)
        .map { (itemConfig, slot) ->
            slot to buildItemControl(itemConfig, shopInstance, player, shopManager, plugin, isBuyShop, backgroundItem, page)
        }
        .toMap()

    val navigationControls = buildNavigationControls(guiConfig, shopInstance, player, shopManager, plugin, backgroundItem, page, totalPages)
    val timerControls = buildTimerControl(guiConfig.timerButton, shopManager)

    return backgroundControls + fixedItemControls + pagedItemControls + navigationControls + timerControls
}

private fun buildItemControl(
    itemConfig: ShopItemConfig,
    shopInstance: ShopInstance,
    player: Player,
    shopManager: ShopManager,
    plugin: JavaPlugin,
    isBuyShop: Boolean,
    backgroundItem: ItemStack,
    page: Int
): BukkitViewControl {
    val playerId = player.uniqueId.toString()
    val baseItem = itemConfig.item?.clone() ?: resolveItem(itemConfig.itemName) ?: return ViewControl.just(resolveMissingItem())
    val itemState = shopInstance.state.itemStates[itemConfig.id]
        ?: ShopItemState(itemConfig.id, itemConfig.stock, itemConfig.basePrice)

    val loreConfig = shopManager.getPluginConfig().lore
    val displayItem = if (isBuyShop) {
        buildBuyDisplayItem(baseItem, itemConfig, itemState, playerId, loreConfig)
    } else {
        buildSellDisplayItem(baseItem, itemConfig, itemState, playerId, loreConfig)
    }

    return ViewControl.of(displayItem) { clickEvent: BukkitClickEvent ->
        val latestInstance = shopManager.getInstance(shopInstance.guiConfig.id)
            ?: return@of ViewAction.nothing()
        val latestState = latestInstance.state.itemStates[itemConfig.id]
            ?: ShopItemState(itemConfig.id, itemConfig.stock, itemConfig.basePrice)

        if (isBuyShop) {
            val balance = shopManager.getBalance(clickEvent.player, itemConfig)
            val amount = when (clickEvent.click) {
                "LEFT" -> 1
                "SHIFT_LEFT" -> calculateMaxBuyAmount(itemConfig, latestState, playerId, balance, latestState.currentPrice)
                "NUMBER_KEY_7" -> 16
                else -> 0
            }
            if (amount > 0) {
                shopManager.processBuy(clickEvent.player, shopInstance.guiConfig.id, itemConfig.id, amount)
            }
        } else {
            val playerItemCount = shopManager.getPlayerItemCount(clickEvent.player, itemConfig)
            val amount = when (clickEvent.click) {
                "LEFT" -> 1
                "SHIFT_LEFT" -> calculateMaxSellAmount(itemConfig, latestState, playerId, playerItemCount, cap = 64)
                "NUMBER_KEY_7" -> calculateMaxSellAmount(itemConfig, latestState, playerId, playerItemCount, cap = Int.MAX_VALUE)
                else -> 0
            }
            if (amount > 0) {
                shopManager.processSell(clickEvent.player, shopInstance.guiConfig.id, itemConfig.id, amount)
            }
        }

        val refreshedInstance = shopManager.getInstance(shopInstance.guiConfig.id)
            ?: return@of ViewAction.nothing()
        ViewAction.Update(buildShopContents(refreshedInstance, clickEvent.player, shopManager, plugin, backgroundItem, page))
    }
}

private fun buildNavigationControls(
    guiConfig: kr.acog.bongshop.config.ShopGuiConfig,
    shopInstance: ShopInstance,
    player: Player,
    shopManager: ShopManager,
    plugin: JavaPlugin,
    backgroundItem: ItemStack,
    page: Int,
    totalPages: Int
): Map<Int, BukkitViewControl> {
    val controls = mutableMapOf<Int, BukkitViewControl>()

    guiConfig.prevPageButton?.let { btn ->
        if (page > 0) {
            val item = buildButtonItem(btn.material, btn.displayName, btn.lore, btn.customModelData)
            controls[btn.slot] = ViewControl.of(item) { _: BukkitClickEvent ->
                openShopView(shopInstance, player, plugin, shopManager, page - 1)
                ViewAction.nothing()
            }
        }
    }

    guiConfig.nextPageButton?.let { btn ->
        if (page < totalPages - 1) {
            val item = buildButtonItem(btn.material, btn.displayName, btn.lore, btn.customModelData)
            controls[btn.slot] = ViewControl.of(item) { _: BukkitClickEvent ->
                openShopView(shopInstance, player, plugin, shopManager, page + 1)
                ViewAction.nothing()
            }
        }
    }

    return controls
}

private fun buildTimerControl(
    timerConfig: TimerButtonConfig?,
    shopManager: ShopManager
): Map<Int, BukkitViewControl> {
    timerConfig ?: return emptyMap()

    val remaining = getPriceChangeRemaining(shopManager)
    val hours = remaining.toHours()
    val minutes = remaining.toMinutesPart()
    val seconds = remaining.toSecondsPart()

    val processedLore = timerConfig.lore.map { line ->
        line.replace("<hour>", hours.toString())
            .replace("<minute>", minutes.toString())
            .replace("<second>", seconds.toString())
    }

    val item = buildButtonItem(timerConfig.material, timerConfig.displayName, processedLore, timerConfig.customModelData)
    return mapOf(timerConfig.slot to ViewControl.just(item))
}

private fun buildButtonItem(material: Material, displayNameStr: String, loreLines: List<String>, customModelData: Int?): ItemStack =
    BukkitItem.builder().material(material).build().create()
        .also { it.itemMeta = it.itemMeta?.apply {
            displayName(name(displayNameStr))
            if (loreLines.isNotEmpty()) lore(loreLines.map { line -> lore(line) })
            if (customModelData != null) setCustomModelData(customModelData)
        }}

private fun resolveBackgroundItem(material: Material): ItemStack =
    BukkitItem.builder().material(material).build().create()
        .also { it.itemMeta = it.itemMeta?.apply { displayName(name(" ")) } }

private fun resolveMissingItem(): ItemStack =
    BukkitItem.builder().material(Material.BARRIER).build().create()
        .also { it.itemMeta = it.itemMeta?.apply { displayName(name("<red>X")) } }
