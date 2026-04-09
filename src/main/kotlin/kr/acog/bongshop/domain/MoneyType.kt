package kr.acog.bongshop.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MoneyType { VAULT, COINSENGINE, ITEM }

@Serializable
enum class ShopType { BUY, SELL }

@Serializable
enum class PriceChangeType { RANDOM, DEMAND }
