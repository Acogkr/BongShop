package kr.acog.bongshop.item

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun countSimilarItems(player: Player, referenceItem: ItemStack): Int {
    return player.inventory.contents
        .filterNotNull()
        .filter { it.isSimilar(referenceItem) }
        .sumOf { it.amount }
}

fun removeSimilarItems(player: Player, referenceItem: ItemStack, amount: Int): Boolean {
    if (amount <= 0) return true
    if (countSimilarItems(player, referenceItem) < amount) return false

    var remaining = amount
    for (slot in player.inventory.contents.indices) {
        val itemStack = player.inventory.getItem(slot) ?: continue
        if (!itemStack.isSimilar(referenceItem)) continue

        if (itemStack.amount <= remaining) {
            remaining -= itemStack.amount
            player.inventory.setItem(slot, null)
        } else {
            itemStack.amount -= remaining
            remaining = 0
        }

        if (remaining <= 0) break
    }

    return true
}

fun depositSimilarItems(player: Player, referenceItem: ItemStack, amount: Int): Boolean {
    if (amount <= 0) return true
    if (!canFitSimilarItems(player, referenceItem, amount)) return false

    var remaining = amount
    val maxStack = referenceItem.maxStackSize

    while (remaining > 0) {
        val stackAmount = minOf(maxStack, remaining)
        val stack = referenceItem.clone().also { it.amount = stackAmount }
        val leftover = player.inventory.addItem(stack)
        if (leftover.isNotEmpty()) {
            return false
        }
        remaining -= stackAmount
    }

    return true
}

internal fun canFitSimilarItems(player: Player, referenceItem: ItemStack, amount: Int): Boolean {
    var capacity = 0
    val maxStack = referenceItem.maxStackSize

    for (itemStack in player.inventory.contents) {
        when {
            itemStack == null -> capacity += maxStack
            itemStack.isSimilar(referenceItem) -> capacity += (maxStack - itemStack.amount)
        }

        if (capacity >= amount) return true
    }

    return capacity >= amount
}

