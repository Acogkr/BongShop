package kr.acog.bongshop.plugin

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin

@Suppress("DEPRECATION")
class PlayerChatListener(private val plugin: JavaPlugin) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncPlayerChatEvent) {
        val callback = PendingChatInput.consume(event.player.uniqueId) ?: return
        event.isCancelled = true
        plugin.server.scheduler.runTask(plugin, Runnable { callback(event.message) })
    }
}
