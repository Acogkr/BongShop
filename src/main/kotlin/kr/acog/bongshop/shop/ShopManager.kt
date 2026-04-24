package kr.acog.bongshop.shop

import kr.acog.bongshop.config.*
import kr.acog.bongshop.domain.*
import kr.acog.bongshop.economy.EconomyOps
import kr.acog.bongshop.economy.ItemEconomyProvider
import kr.acog.bongshop.economy.coinsEngineOps
import kr.acog.bongshop.item.canFitSimilarItems
import kr.acog.bongshop.item.countSimilarItems
import kr.acog.bongshop.item.removeSimilarItems
import kr.acog.bongshop.item.resolveItem
import kr.acog.bongshop.state.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.time.Instant
import java.util.logging.Logger

class ShopManager(
    private val dataFolder: File,
    private val economyProviders: Map<MoneyType, Map<String, EconomyOps>>,
    private val logger: Logger,
    private val plugin: JavaPlugin
) {
    private var instances: Map<String, ShopInstance> = emptyMap()
    private var pluginConfig: PluginConfig = PluginConfig()
    private var shopsConfig: ShopsConfig = ShopsConfig()
    private var shopItemsConfig: ShopItemsConfig = ShopItemsConfig()
    private var states: Map<String, ShopState> = emptyMap()
    private var sellRecords: SellRecords = SellRecords()
    private var priceChangeScheduler: PriceChangeScheduler? = null

    var lastPriceChangeTime: Instant = Instant.now()
        private set

    fun initialize(onComplete: () -> Unit = {}) {
        object : BukkitRunnable() {
            override fun run() {
                val loadedPluginConfig = loadPluginConfig(dataFolder)
                val loadedShopsConfig = loadShopsConfig(dataFolder)
                val loadedShopItemsConfig = loadShopItemsConfig(dataFolder)
                val loadedStates = loadAllShopStates(dataFolder)
                val loadedSellRecords = loadSellRecords(dataFolder)

                val validated = validateShopData(loadedPluginConfig, loadedShopsConfig, loadedShopItemsConfig, logger)
                val newInstances = buildNewInstances(validated, loadedStates)
                val newStates = loadedStates + newInstances.mapValues { it.value.state }

                object : BukkitRunnable() {
                    override fun run() {
                        pluginConfig = loadedPluginConfig
                        shopsConfig = validated.shopsConfig
                        shopItemsConfig = validated.shopItemsConfig
                        states = newStates
                        sellRecords = loadedSellRecords
                        instances = newInstances
                        lastPriceChangeTime = Instant.now()
                        priceChangeScheduler?.stop()
                        priceChangeScheduler = PriceChangeScheduler(this@ShopManager, plugin).also { it.start() }
                        persistStateAsync()
                        onComplete()
                    }
                }.runTask(plugin)
            }
        }.runTaskAsynchronously(plugin)
    }

    fun persistStateAsync() {
        val statesSnapshot = states
        val recordsSnapshot = sellRecords
        object : BukkitRunnable() {
            override fun run() {
                saveAllShopStates(dataFolder, statesSnapshot)
                saveSellRecords(dataFolder, recordsSnapshot)
            }
        }.runTaskAsynchronously(plugin)
    }

    fun persistStateSync() {
        saveAllShopStates(dataFolder, states)
        saveSellRecords(dataFolder, sellRecords)
    }

    fun reloadAll(onComplete: () -> Unit = {}) = initialize(onComplete)

    fun addShop(shopConfig: ShopGuiConfig, onComplete: () -> Unit = {}) {
        val updated = shopsConfig.copy(shops = shopsConfig.shops + shopConfig)
        object : BukkitRunnable() {
            override fun run() {
                saveShopsConfig(dataFolder, updated)
                createShopItemsFile(dataFolder, shopConfig.id)
                object : BukkitRunnable() {
                    override fun run() { initialize(onComplete) }
                }.runTask(plugin)
            }
        }.runTaskAsynchronously(plugin)
    }

    fun removeShop(shopId: String, onComplete: () -> Unit = {}) {
        val updated = shopsConfig.copy(shops = shopsConfig.shops.filter { it.id != shopId })
        object : BukkitRunnable() {
            override fun run() {
                saveShopsConfig(dataFolder, updated)
                deleteShopItemsFile(dataFolder, shopId)
                object : BukkitRunnable() {
                    override fun run() { initialize(onComplete) }
                }.runTask(plugin)
            }
        }.runTaskAsynchronously(plugin)
    }

    // --- Management methods ---

    fun getAllItemsForShop(shopId: String): List<ShopItemConfig> =
        shopItemsConfig.items.filter { it.shopId == shopId }

    fun getItem(shopId: String, itemId: String): ShopItemConfig? =
        shopItemsConfig.items.find { it.shopId == shopId && it.id == itemId }

    fun addItem(shopId: String, item: ItemStack) {
        val base = item.type.name.lowercase()
        val id = generateItemId(base, shopId)
        val newItem = ShopItemConfig(
            id = id,
            shopId = shopId,
            itemName = base,
            item = item.clone().also { it.amount = 1 },
            basePrice = 0
        )
        shopItemsConfig = shopItemsConfig.copy(items = shopItemsConfig.items + newItem)
        val instance = instances[shopId]
        if (instance != null) {
            val newItemState = ShopItemState(
                itemId = id,
                stockRemaining = newItem.stock,
                currentPrice = newItem.basePrice
            )
            val updatedState = instance.state.copy(
                itemStates = instance.state.itemStates + (id to newItemState)
            )
            instances = instances + (shopId to ShopInstance(instance.guiConfig, instance.items + newItem, updatedState))
            states = states + (shopId to updatedState)
        }
        saveItemsForShopAsync(shopId)
        persistStateAsync()
    }

    fun updateItem(shopId: String, itemId: String, updater: (ShopItemConfig) -> ShopItemConfig) {
        val oldItem = shopItemsConfig.items.find { it.shopId == shopId && it.id == itemId }
        val updatedItems = shopItemsConfig.items.map {
            if (it.shopId == shopId && it.id == itemId) updater(it) else it
        }
        shopItemsConfig = shopItemsConfig.copy(items = updatedItems)
        val newItem = updatedItems.find { it.shopId == shopId && it.id == itemId }
        val instance = instances[shopId]
        if (instance != null) {
            val newItems = instance.items.map { item ->
                if (item.id == itemId) newItem ?: item else item
            }
            var updatedState = instance.state
            if (oldItem != null && newItem != null && oldItem.stock != newItem.stock) {
                val currentItemState = updatedState.itemStates[itemId]
                if (currentItemState != null) {
                    val newStockRemaining = when {
                        newItem.stock == null -> null
                        oldItem.stock == null -> newItem.stock
                        else -> {
                            val diff = newItem.stock - oldItem.stock
                            val currentRemaining = currentItemState.stockRemaining ?: oldItem.stock
                            (currentRemaining + diff).coerceAtLeast(0)
                        }
                    }
                    updatedState = updatedState.copy(
                        itemStates = updatedState.itemStates + (itemId to currentItemState.copy(stockRemaining = newStockRemaining))
                    )
                }
            }
            instances = instances + (shopId to ShopInstance(instance.guiConfig, newItems, updatedState))
            states = states + (shopId to updatedState)
        }
        saveItemsForShopAsync(shopId)
        persistStateAsync()
    }

    fun removeItem(shopId: String, itemId: String) {
        shopItemsConfig = shopItemsConfig.copy(
            items = shopItemsConfig.items.filter { !(it.shopId == shopId && it.id == itemId) }
        )
        val instance = instances[shopId]
        if (instance != null) {
            instances = instances + (shopId to ShopInstance(instance.guiConfig, instance.items.filter { it.id != itemId }, instance.state))
        }
        saveItemsForShopAsync(shopId)
    }

    private fun generateItemId(base: String, shopId: String): String {
        val existing = shopItemsConfig.items.filter { it.shopId == shopId }.map { it.id }.toSet()
        if (base !in existing) return base
        var i = 1
        while ("${base}_$i" in existing) i++
        return "${base}_$i"
    }

    private fun saveItemsForShopAsync(shopId: String) {
        val items = shopItemsConfig.items.filter { it.shopId == shopId }.toList()
        object : BukkitRunnable() {
            override fun run() {
                saveShopItemsFile(dataFolder, shopId, items)
            }
        }.runTaskAsynchronously(plugin)
    }

    // --- Price / Stock ---

    fun refreshPrices() {
        val updatedInstances = mutableMapOf<String, ShopInstance>()
        val updatedStates = mutableMapOf<String, ShopState>()

        for ((shopId, instance) in instances) {
            val newItemStates = instance.state.itemStates.toMutableMap()
            val shopType = instance.guiConfig.shopType

            val maxVolume = instance.items.maxOfOrNull { itemConfig ->
                val state = newItemStates[itemConfig.id] ?: return@maxOfOrNull 0
                if (shopType == ShopType.BUY) {
                    state.totalBought
                } else {
                    state.totalSold
                }
            } ?: 0

            for (itemConfig in instance.items) {
                val itemState = newItemStates[itemConfig.id] ?: continue
                val newPrice = calculateNewPrice(instance.guiConfig, itemConfig, itemState, maxVolume)
                newItemStates[itemConfig.id] = itemState.copy(
                    currentPrice = newPrice,
                    totalBought = 0,
                    totalSold = 0
                )
            }

            val newState = instance.state.copy(itemStates = newItemStates)
            updatedInstances[shopId] = instance.copy(state = newState)
            updatedStates[shopId] = newState
        }

        instances = updatedInstances
        states = updatedStates
        lastPriceChangeTime = Instant.now()
        persistStateAsync()

        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(pluginConfig.messages.priceChanged))
        val sound = pluginConfig.sounds.priceChanged
        if (sound != null) {
            Bukkit.getOnlinePlayers().forEach { player ->
                player.playSound(player.location, sound.sound, sound.volume, sound.pitch)
            }
        }
    }

    fun resetStockAndLimits() {
        val updatedInstances = mutableMapOf<String, ShopInstance>()
        val updatedStates = mutableMapOf<String, ShopState>()

        for ((shopId, instance) in instances) {
            val newItemStates = instance.state.itemStates.mapValues { (itemId, itemState) ->
                val itemConfig = instance.itemsById[itemId]
                itemState.copy(
                    stockRemaining = itemConfig?.stock,
                    playerDailyBuyCounts = emptyMap(),
                    playerSellCounts = emptyMap()
                )
            }

            val newState = instance.state.copy(itemStates = newItemStates)
            updatedInstances[shopId] = instance.copy(state = newState)
            updatedStates[shopId] = newState
        }

        instances = updatedInstances
        states = updatedStates
        persistStateAsync()

        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(pluginConfig.messages.stockRestocked))
        val sound = pluginConfig.sounds.stockRestocked
        if (sound != null) {
            Bukkit.getOnlinePlayers().forEach { player ->
                player.playSound(player.location, sound.sound, sound.volume, sound.pitch)
            }
        }
    }

    // --- Transactions ---

    fun processBuy(player: Player, shopId: String, itemId: String, amount: Int): TransactionResult {
        val instance = instances[shopId] ?: return TransactionResult.ItemUnavailable
        val itemConfig = instance.itemsById[itemId] ?: return TransactionResult.ItemUnavailable
        val itemState = instance.state.itemStates[itemId]
            ?: ShopItemState(itemId, itemConfig.stock, itemConfig.basePrice)

        val ops = resolveEconomyOps(itemConfig) ?: run {
            sendMessage(player, "<red>결제 시스템을 불러올 수 없습니다.")
            return TransactionResult.ProviderUnavailable
        }

        val currentPrice = itemState.currentPrice
        val balance = ops.getBalance(player)
        val result = validateBuyTransaction(itemConfig, itemState, player.uniqueId.toString(), amount, balance, currentPrice)

        when (result) {
            is TransactionResult.OutOfStock -> {
                sendMessage(player, pluginConfig.messages.outOfStock)
                playSound(player, pluginConfig.sounds.outOfStock)
                return result
            }
            is TransactionResult.BuyLimitReached -> {
                sendMessage(player, pluginConfig.messages.buyLimitReached)
                playSound(player, pluginConfig.sounds.buyLimitReached)
                return result
            }
            is TransactionResult.DailyBuyLimitReached -> {
                sendMessage(player, pluginConfig.messages.dailyBuyLimitReached)
                playSound(player, pluginConfig.sounds.buyLimitReached)
                return result
            }
            is TransactionResult.InsufficientFunds -> {
                val totalCost = currentPrice.toDouble() * amount
                val deficit = (totalCost - balance).toInt()
                val formattedDeficit = formatNumber(deficit)
                val msg = when (itemConfig.payment) {
                    is VaultPaymentConfig -> pluginConfig.messages.insufficientFundsVault
                        .replace("<deficit>", formattedDeficit)
                    is CoinsEnginePaymentConfig -> pluginConfig.messages.insufficientFundsCoinsEngine
                        .replace("<deficit>", formattedDeficit)
                        .replace("<currency>", itemConfig.payment.coinName)
                    is ItemPaymentConfig -> pluginConfig.messages.insufficientFundsItem
                        .replace("<deficit>", formattedDeficit)
                        .replace("<currency>", resolveCurrencyDisplayName(itemConfig.payment.currencyItem))
                }
                sendMessage(player, msg)
                playSound(player, pluginConfig.sounds.purchaseFail)
                return result
            }
            is TransactionResult.Success -> { /* continue */ }
            else -> return result
        }

        val resolvedItem = (itemConfig.item?.clone() ?: resolveItem(itemConfig.itemName))?.also { it.amount = amount }
        if (resolvedItem == null) {
            logger.warning("아이템을 찾을 수 없습니다: ${itemConfig.itemName}")
            return TransactionResult.ItemUnavailable
        }

        if (!canFitSimilarItems(player, resolvedItem, amount)) {
            sendMessage(player, pluginConfig.messages.inventoryFull)
            playSound(player, pluginConfig.sounds.inventoryFull)
            return TransactionResult.InventoryFull
        }

        if (!ops.withdraw(player, currentPrice.toDouble() * amount)) {
            sendMessage(player, pluginConfig.messages.insufficientFundsVault)
            return TransactionResult.InsufficientFunds
        }

        player.inventory.addItem(resolvedItem)

        val updatedState = updateBuyState(instance, itemId, player.uniqueId.toString(), amount)
        applyStateUpdate(shopId, instance, updatedState)
        persistStateAsync()

        val successMsg = pluginConfig.messages.purchaseSuccess
            .replace("<item>", itemConfig.itemName)
            .replace("<price>", formatNumber(currentPrice * amount))
            .replace("<amount>", formatNumber(amount))
        sendMessage(player, successMsg)
        playSound(player, pluginConfig.sounds.purchaseSuccess)

        logTransaction(pluginConfig.messages.purchaseLog, player, shopId, itemConfig, currentPrice, amount)
        return result
    }

    fun processSell(player: Player, shopId: String, itemId: String, amount: Int): TransactionResult {
        val instance = instances[shopId] ?: return TransactionResult.ItemUnavailable
        val itemConfig = instance.itemsById[itemId] ?: return TransactionResult.ItemUnavailable
        val itemState = instance.state.itemStates[itemId]
            ?: ShopItemState(itemId, null, itemConfig.basePrice)

        val referenceItem = itemConfig.item?.clone() ?: resolveItem(itemConfig.itemName) ?: return TransactionResult.ItemUnavailable

        val playerItemCount = player.inventory.contents
            .filterNotNull()
            .filter { it.isSimilar(referenceItem) }
            .sumOf { it.amount }

        val result = validateSellTransaction(itemConfig, itemState, player.uniqueId.toString(), amount, playerItemCount)

        when (result) {
            is TransactionResult.InsufficientItems -> {
                sendMessage(player, pluginConfig.messages.insufficientItems)
                playSound(player, pluginConfig.sounds.sellFail)
                return result
            }
            is TransactionResult.LimitReached -> {
                sendMessage(player, pluginConfig.messages.sellLimitReached)
                playSound(player, pluginConfig.sounds.sellFail)
                return result
            }
            is TransactionResult.Success -> { /* continue */ }
            else -> return result
        }

        val totalItemsToRemove = amount * itemConfig.quantity
        if (!removeSimilarItems(player, referenceItem, totalItemsToRemove)) {
            sendMessage(player, pluginConfig.messages.insufficientItems)
            playSound(player, pluginConfig.sounds.sellFail)
            return TransactionResult.InsufficientItems
        }

        val currentPrice = itemState.currentPrice
        val totalEarned = currentPrice.toLong() * amount
        val ops = resolveEconomyOps(itemConfig)
        if (ops != null) {
            ops.deposit(player, totalEarned.toDouble())
        }

        val updatedState = updateSellState(instance, itemId, player.uniqueId.toString(), amount)
        applyStateUpdate(shopId, instance, updatedState)
        recordSell(player.uniqueId.toString(), shopId, totalEarned)
        persistStateAsync()

        val successMsg = pluginConfig.messages.sellSuccess
            .replace("<item>", itemConfig.itemName)
            .replace("<price>", formatNumber(totalEarned))
            .replace("<amount>", formatNumber(amount))
        sendMessage(player, successMsg)
        playSound(player, pluginConfig.sounds.sellSuccess)

        logTransaction(pluginConfig.messages.sellLog, player, shopId, itemConfig, currentPrice, amount)
        return result
    }

    fun getBalance(player: Player, itemConfig: ShopItemConfig): Double {
        return resolveEconomyOps(itemConfig)?.getBalance(player) ?: 0.0
    }

    fun getPlayerItemCount(player: Player, itemConfig: ShopItemConfig): Int {
        val referenceItem = itemConfig.item?.clone() ?: resolveItem(itemConfig.itemName) ?: return 0
        return countSimilarItems(player, referenceItem)
    }

    fun getInstance(shopId: String): ShopInstance? = instances[shopId]
    fun allShopIds(): List<String> = instances.keys.toList()
    fun allInstances(): Map<String, ShopInstance> = instances
    fun getPluginConfig(): PluginConfig = pluginConfig
    fun getSellRecords(): SellRecords = sellRecords

    // --- private ---

    private fun buildNewInstances(validated: ValidatedShopData, loadedStates: Map<String, ShopState>): Map<String, ShopInstance> {
        val newInstances = mutableMapOf<String, ShopInstance>()
        for (shopConfig in validated.shopsConfig.shops) {
            val existingState = loadedStates[shopConfig.id]
            val itemPool = validated.shopItemsConfig.items.filter { it.shopId == shopConfig.id }
            val state = existingState ?: buildInitialState(shopConfig.id, itemPool, shopConfig.priceVolatility)
            newInstances[shopConfig.id] = ShopInstance(shopConfig, itemPool, state)
        }
        return newInstances
    }

    private fun buildInitialState(shopId: String, items: List<ShopItemConfig>, volatility: Double): ShopState {
        val itemStates = items.associate { itemConfig ->
            val price = if (itemConfig.minPrice != null && itemConfig.maxPrice != null) {
                calculateRandomPrice(itemConfig.minPrice, itemConfig.maxPrice, volatility)
            } else {
                itemConfig.basePrice
            }
            itemConfig.id to ShopItemState(
                itemId = itemConfig.id,
                stockRemaining = itemConfig.stock,
                currentPrice = price
            )
        }
        return ShopState(shopId = shopId, itemStates = itemStates)
    }

    private fun calculateNewPrice(guiConfig: ShopGuiConfig, itemConfig: ShopItemConfig, itemState: ShopItemState, maxVolume: Int): Int {
        val min = itemConfig.minPrice ?: return itemConfig.basePrice
        val max = itemConfig.maxPrice ?: return itemConfig.basePrice

        return when (guiConfig.priceChangeType) {
            PriceChangeType.RANDOM -> calculateRandomPrice(min, max, guiConfig.priceVolatility)
            PriceChangeType.DEMAND -> {
                val itemVolume = if (guiConfig.shopType == ShopType.BUY) {
                    itemState.totalBought
                } else {
                    itemState.totalSold
                }
                calculateDemandPrice(min, max, guiConfig.shopType, itemVolume, maxVolume)
            }
        }
    }

    private fun updateBuyState(instance: ShopInstance, itemId: String, playerId: String, amount: Int): ShopState {
        val itemState = instance.state.itemStates[itemId] ?: return instance.state

        val updatedDailyCounts = itemState.playerDailyBuyCounts.toMutableMap()
        updatedDailyCounts[playerId] = (updatedDailyCounts[playerId] ?: 0) + amount

        val updatedCounts = itemState.playerBuyCounts.toMutableMap()
        updatedCounts[playerId] = (updatedCounts[playerId] ?: 0) + amount

        val updatedItemState = itemState.copy(
            stockRemaining = itemState.stockRemaining?.minus(amount),
            playerDailyBuyCounts = updatedDailyCounts,
            playerBuyCounts = updatedCounts,
            totalBought = itemState.totalBought + amount
        )
        val updatedStates = instance.state.itemStates.toMutableMap()
        updatedStates[itemId] = updatedItemState
        return instance.state.copy(itemStates = updatedStates)
    }

    private fun updateSellState(instance: ShopInstance, itemId: String, playerId: String, amount: Int): ShopState {
        val itemState = instance.state.itemStates[itemId] ?: return instance.state

        val updatedCounts = itemState.playerSellCounts.toMutableMap()
        updatedCounts[playerId] = (updatedCounts[playerId] ?: 0) + amount

        val updatedItemState = itemState.copy(
            playerSellCounts = updatedCounts,
            totalSold = itemState.totalSold + amount
        )
        val updatedStates = instance.state.itemStates.toMutableMap()
        updatedStates[itemId] = updatedItemState
        return instance.state.copy(itemStates = updatedStates)
    }

    private fun applyStateUpdate(shopId: String, instance: ShopInstance, updatedState: ShopState) {
        instances = instances + (shopId to instance.copy(state = updatedState))
        states = states + (shopId to updatedState)
    }

    private fun recordSell(playerId: String, shopId: String, amount: Long) {
        val playerRecord = sellRecords.players[playerId] ?: PlayerSellRecord()
        val updatedTotals = playerRecord.shopTotals.toMutableMap()
        updatedTotals[shopId] = (updatedTotals[shopId] ?: 0L) + amount

        val updatedPlayers = sellRecords.players.toMutableMap()
        updatedPlayers[playerId] = playerRecord.copy(shopTotals = updatedTotals)
        sellRecords = sellRecords.copy(players = updatedPlayers)
    }

    private fun resolveEconomyOps(itemConfig: ShopItemConfig): EconomyOps? {
        return when (val payment = itemConfig.payment) {
            is VaultPaymentConfig -> economyProviders[MoneyType.VAULT]?.values?.firstOrNull()
            is CoinsEnginePaymentConfig -> coinsEngineOps(payment.coinName)
            is ItemPaymentConfig -> ItemEconomyProvider(payment.currencyItem).toOps()
        }
    }

    private fun logTransaction(template: String, player: Player, shopId: String, itemConfig: ShopItemConfig, price: Int, amount: Int) {
        val shopName = instances[shopId]?.guiConfig?.name ?: shopId
        val msg = template
            .replace("<displayname>", PlainTextComponentSerializer.plainText().serialize(player.displayName()))
            .replace("<playername>", player.name)
            .replace("<shopname>", shopName)
            .replace("<item>", itemConfig.itemName)
            .replace("<price>", formatNumber(price * amount))
            .replace("<amount>", formatNumber(amount))
        logger.info(msg)
    }

    private fun sendMessage(player: Player, message: String) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(message))
    }

    private fun playSound(player: Player, soundEntry: SoundEntry?) {
        soundEntry ?: return
        player.playSound(player.location, soundEntry.sound, soundEntry.volume, soundEntry.pitch)
    }
}
