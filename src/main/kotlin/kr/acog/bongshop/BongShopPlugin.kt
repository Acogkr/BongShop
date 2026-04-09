package kr.acog.bongshop

import io.typst.view.bukkit.BukkitView
import kr.acog.bongshop.command.ShopCommandRegistrar
import kr.acog.bongshop.economy.loadEconomyProviders
import kr.acog.bongshop.plugin.BongShopExpansion
import kr.acog.bongshop.plugin.generateDefaultConfigs
import kr.acog.bongshop.shop.PriceChangeScheduler
import kr.acog.bongshop.shop.ShopManager
import kr.acog.bongshop.shop.StockResetScheduler
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BongShopPlugin : JavaPlugin() {
    lateinit var shopManager: ShopManager
        private set

    override fun onEnable() {
        BukkitView.register(this)
        generateDefaultConfigs(dataFolder)

        shopManager = ShopManager(dataFolder, loadEconomyProviders(this, logger), logger)
        shopManager.initialize()

        PriceChangeScheduler(shopManager, this).start()
        StockResetScheduler(shopManager, this).start()
        ShopCommandRegistrar(shopManager, this).register()

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            BongShopExpansion(shopManager).register()
        }

        logger.info("BongShop 활성화 완료")
    }

    override fun onDisable() {
        shopManager.persistState()
        logger.info("BongShop 비활성화 완료")
    }
}
