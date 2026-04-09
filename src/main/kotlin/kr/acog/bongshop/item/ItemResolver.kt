package kr.acog.bongshop.item

import com.nexomc.nexo.api.NexoItems
import dev.lone.itemsadder.api.CustomStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun resolveItem(itemName: String): ItemStack? {
    return when {
        itemName.startsWith("nexo:") -> resolveNexoItem(itemName.removePrefix("nexo:"))
        itemName.startsWith("itemsadder:") -> resolveItemsAdderItem(itemName.removePrefix("itemsadder:"))
        else -> resolveBukkitItem(itemName)
    }
}

private fun resolveNexoItem(id: String): ItemStack? {
    if (Bukkit.getPluginManager().getPlugin("Nexo") == null) return null
    return NexoItems.itemFromId(id)?.build()
}

private fun resolveItemsAdderItem(id: String): ItemStack? {
    if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) return null
    return CustomStack.getInstance(id)?.itemStack
}

private fun resolveBukkitItem(name: String): ItemStack? {
    val material = Material.matchMaterial(name) ?: return null
    return ItemStack(material)
}
