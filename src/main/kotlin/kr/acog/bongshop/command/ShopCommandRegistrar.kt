package kr.acog.bongshop.command

import io.typst.command.Command
import io.typst.command.StandardArguments.strArg
import io.typst.command.bukkit.BukkitArguments.playerArg
import io.typst.command.bukkit.BukkitCommands
import kr.acog.bongshop.shop.ShopManager
import org.bukkit.plugin.java.JavaPlugin

class ShopCommandRegistrar(
    private val shopManager: ShopManager,
    private val plugin: JavaPlugin
) {
    fun register() {
        val handler = ShopCommandHandler(shopManager, plugin)
        val shopIdArg = strArg.withTabCompletes { shopManager.allShopIds() }
        val shopTypeArg = strArg.withTabCompletes { listOf("구매", "판매") }
        val createIdArg = strArg.withTabCompletes { listOf("상점ID") }
        val createDisplayNameArg = strArg.withTabCompletes { listOf("상점이름") }

        val commandTree = Command.mapping(
            Command.pair("생성", Command.argument(ShopCommand::Create, shopTypeArg, createIdArg, createDisplayNameArg)),
            Command.pair("열기", Command.argument(ShopCommand::Open, shopIdArg, playerArg.asOptional())),
            Command.pair("삭제", Command.argument(ShopCommand::Delete, shopIdArg)),
            Command.pair("판매기록", Command.argument(ShopCommand::SellHistory, playerArg)),
            Command.pair("관리", Command.argument(ShopCommand::Manage, shopIdArg.asOptional(), playerArg.asOptional())),
            Command.pair("리로드", Command.present(ShopCommand.Reload)),
            Command.pair("목록", Command.present(ShopCommand.List)),
            Command.pair("가격변동", Command.present(ShopCommand.ForcePriceChange)),
            Command.pair("재고입고", Command.present(ShopCommand.ForceStockReset))
        )

        BukkitCommands.register("상점", commandTree, { sender, command ->
            handler.handle(sender, command)
        }, plugin)
    }
}
