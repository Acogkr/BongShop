package kr.acog.bongshop.config

import io.typst.bukkit.kotlin.serialization.ItemStackSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kr.acog.bongshop.domain.MoneyType
import kr.acog.bongshop.domain.PriceChangeType
import kr.acog.bongshop.domain.ShopType
import org.bukkit.Material

object IntOrIntListSerializer : KSerializer<List<Int>> {
    private val delegate = ListSerializer(Int.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<Int> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonArray -> element.map { it.jsonPrimitive.int }
            else -> listOf(element.jsonPrimitive.int)
        }
    }

    override fun serialize(encoder: Encoder, value: List<Int>) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            jsonEncoder.encodeJsonElement(JsonArray(value.map { JsonPrimitive(it) }))
        } else {
            delegate.serialize(encoder, value)
        }
    }
}

@Serializable
data class ShopGuiConfig(
    val id: String,
    val name: String,
    val shopType: ShopType = ShopType.BUY,
    val title: String = "상점",
    val rows: Int = 6,
    val priceChangeType: PriceChangeType = PriceChangeType.RANDOM,
    val priceVolatility: Double = 1.0,
    val backgroundMaterial: Material = Material.AIR,
    val prevPageButton: PageButtonConfig? = null,
    val nextPageButton: PageButtonConfig? = null,
    val timerButton: TimerButtonConfig? = null,
    val infoButton: InfoButtonConfig? = null,
    val showBalanceLore: Boolean = true
)

@Serializable
data class PageButtonConfig(
    @Serializable(with = IntOrIntListSerializer::class) val slots: List<Int>,
    val material: Material = Material.ARROW,
    val displayName: String = "<white>이전 페이지",
    val lore: List<String> = emptyList(),
    val customModelData: Int? = null
)

@Serializable
data class TimerButtonConfig(
    @Serializable(with = IntOrIntListSerializer::class) val slots: List<Int>,
    val material: Material = Material.CLOCK,
    val displayName: String = "<yellow>가격 변동까지",
    val lore: List<String> = listOf("<white><hour>시간 <minute>분 <second>초"),
    val customModelData: Int? = null
)

@Serializable
data class InfoButtonConfig(
    @Serializable(with = IntOrIntListSerializer::class) val slots: List<Int>,
    val material: Material = Material.BOOK,
    val displayName: String = "<yellow>상점 정보",
    val lore: List<String> = emptyList(),
    val customModelData: Int? = null
)

@Serializable
data class ShopItemConfig(
    val id: String,
    val shopId: String,
    val itemName: String,
    @Transient val item: ItemStackSerializable? = null,
    val payment: PaymentConfig = VaultPaymentConfig(),
    val quantity: Int = 1,
    val basePrice: Int = 0,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val slot: Int? = null,
    val stock: Int? = null,
    val dailyBuyLimit: Int? = null,
    val buyLimit: Int? = null,
    val dailySellLimit: Int? = null,
    val page: Int = 1,
    val showPriceChange: Boolean = true
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
    val item: ItemStackSerializable? = null,
    val payment: PaymentConfig = VaultPaymentConfig(),
    val quantity: Int = 1,
    val basePrice: Int = 0,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val slot: Int? = null,
    val stock: Int? = null,
    val dailyBuyLimit: Int? = null,
    val buyLimit: Int? = null,
    val dailySellLimit: Int? = null,
    val page: Int = 1,
    val showPriceChange: Boolean = true
)
