package kr.acog.bongshop.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kr.acog.bongshop.domain.MoneyType
import kr.acog.bongshop.domain.PriceChangeType
import kr.acog.bongshop.domain.ShopType
import org.bukkit.Material

@Serializable
data class ShopGuiConfig(
    val id: String,
    val name: String,
    val shopType: ShopType = ShopType.BUY,
    val title: String = "&6상점",
    val rows: Int = 6,
    val priceChangeType: PriceChangeType = PriceChangeType.RANDOM,
    val backgroundMaterial: Material = Material.AIR,
    val prevPageButton: PageButtonConfig? = null,
    val nextPageButton: PageButtonConfig? = null,
    val timerButton: TimerButtonConfig? = null
)

@Serializable
data class PageButtonConfig(
    val slot: Int,
    val material: Material = Material.ARROW,
    val displayName: String = "<white>이전 페이지",
    val lore: List<String> = emptyList(),
    val customModelData: Int? = null
)

@Serializable
data class TimerButtonConfig(
    val slot: Int,
    val material: Material = Material.CLOCK,
    val displayName: String = "<yellow>가격 변동까지",
    val lore: List<String> = listOf("<white><hour>시간 <minute>분 <second>초"),
    val customModelData: Int? = null
)

@Serializable
data class ShopItemConfig(
    val id: String,
    val shopId: String,
    val itemName: String,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    val payment: PaymentConfig = VaultPaymentConfig(),
    val quantity: Int = 1,
    val basePrice: Int = 0,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val slot: Int? = null,
    val stock: Int? = null,
    val dailyBuyLimit: Int? = null,
    val buyLimit: Int? = null,
    val dailySellLimit: Int? = null
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface PaymentConfig {
    val moneyType: MoneyType
}

@Serializable
@SerialName("vault")
data class VaultPaymentConfig(
    val dummy: Boolean = true
) : PaymentConfig {
    override val moneyType: MoneyType = MoneyType.VAULT
}

@Serializable
@SerialName("coinsEngine")
data class CoinsEnginePaymentConfig(
    val coinName: String
) : PaymentConfig {
    override val moneyType: MoneyType = MoneyType.COINSENGINE
}

@Serializable
@SerialName("item")
data class ItemPaymentConfig(
    val currencyItem: String
) : PaymentConfig {
    override val moneyType: MoneyType = MoneyType.ITEM
}

@Serializable
data class ShopsConfig(
    val shops: List<ShopGuiConfig> = emptyList()
)

@Serializable
data class ShopItemsFileConfig(
    val items: List<ShopItemFileEntry> = emptyList()
)

@Serializable
data class ShopItemFileEntry(
    val id: String,
    val itemName: String,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    val payment: PaymentConfig = VaultPaymentConfig(),
    val quantity: Int = 1,
    val basePrice: Int = 0,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val slot: Int? = null,
    val stock: Int? = null,
    val dailyBuyLimit: Int? = null,
    val buyLimit: Int? = null,
    val dailySellLimit: Int? = null
)
