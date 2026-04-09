package kr.acog.bongshop.economy

import org.bukkit.entity.Player

data class EconomyOps(
    val getBalance: (Player) -> Double,
    val withdraw: (Player, Double) -> Boolean,
    val deposit: (Player, Double) -> Boolean
)
