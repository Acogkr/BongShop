package kr.acog.bongshop.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun resolveItem(itemName: String): ItemStack? =
    Material.matchMaterial(itemName)?.let { ItemStack(it) }
