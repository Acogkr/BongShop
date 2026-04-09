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

        val commandTree = Command.mapping(
            Command.pair("생성", Command.argument(ShopCommand::Create, shopTypeArg, strArg, strArg)),
            Command.pair("열기", Command.argument(ShopCommand::Open, shopIdArg, playerArg.asOptional())),
            Command.pair("삭제", Command.argument(ShopCommand::Delete, shopIdArg)),
            Command.pair("판매기록", Command.argument(ShopCommand::SellHistory, playerArg)),
            Command.pair("리로드", Command.present(ShopCommand.Reload))
        )

        BukkitCommands.register("상점", commandTree, { sender, command ->
            handler.handle(sender, command)
        }, plugin)
    }
}
