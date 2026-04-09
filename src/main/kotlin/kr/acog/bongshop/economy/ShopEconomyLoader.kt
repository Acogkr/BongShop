package kr.acog.bongshop.economy

import kr.acog.bongshop.domain.MoneyType
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import su.nightexpress.coinsengine.api.CoinsEngineAPI
import java.util.logging.Logger

fun loadEconomyProviders(plugin: JavaPlugin, logger: Logger): Map<MoneyType, Map<String, EconomyOps>> {
    val vaultProviders = loadVaultProviders(plugin, logger)

    return buildMap {
        if (vaultProviders.isNotEmpty()) put(MoneyType.VAULT, vaultProviders)
        if (isCoinsEngineAvailable()) put(MoneyType.COINSENGINE, emptyMap())
    }
}

fun coinsEngineOps(coinName: String): EconomyOps? {
    if (!isCoinsEngineAvailable() || !CoinsEngineAPI.hasCurrency(coinName)) return null

    return EconomyOps(
        getBalance = { player -> CoinsEngineAPI.getBalance(player.uniqueId, coinName) },
        withdraw = { player, amount -> CoinsEngineAPI.removeBalance(player.uniqueId, coinName, amount) },
        deposit = { player, amount -> CoinsEngineAPI.addBalance(player.uniqueId, coinName, amount) }
    )
}

private fun loadVaultProviders(plugin: JavaPlugin, logger: Logger): Map<String, EconomyOps> {
    if (Bukkit.getPluginManager().getPlugin("Vault") == null) return emptyMap()

    val registration = plugin.server.servicesManager.getRegistration(Economy::class.java)
    if (registration == null) {
        logger.warning("Vault was detected but no Economy provider is registered.")
        return emptyMap()
    }

    val economy = registration.provider
    return mapOf(
        "vault" to EconomyOps(
            getBalance = economy::getBalance,
            withdraw = { player, amount -> economy.withdrawPlayer(player, amount).transactionSuccess() },
            deposit = { player, amount -> economy.depositPlayer(player, amount).transactionSuccess() }
        )
    )
}

private fun isCoinsEngineAvailable(): Boolean {
    return Bukkit.getPluginManager().getPlugin("CoinsEngine") != null && CoinsEngineAPI.isLoaded()
}
