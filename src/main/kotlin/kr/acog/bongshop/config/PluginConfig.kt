package kr.acog.bongshop.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Sound

object SoundSerializer : KSerializer<Sound> {
    override val descriptor = PrimitiveSerialDescriptor("Sound", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Sound) {
        encoder.encodeString((value as Enum<*>).name)
    }
    override fun deserialize(decoder: Decoder): Sound {
        return Sound.valueOf(decoder.decodeString())
    }
}

@Serializable
data class PluginConfig(
    val priceChangeIntervalMinutes: Int = 60,
    val stockResetTime: String = "00:00",
    val messages: MessagesConfig = MessagesConfig(),
    val sounds: SoundsConfig = SoundsConfig()
)

@Serializable
data class MessagesConfig(
    val insufficientFundsVault: String = "<red>총 <deficit> 골드 만큼 부족하여 아이템 구매가 불가능합니다.",
    val insufficientFundsCoinsEngine: String = "<red>총 <deficit> <currency> 만큼 부족하여 아이템 구매가 불가능합니다.",
    val insufficientFundsItem: String = "<red>총 <deficit> <currency> 만큼 부족하여 아이템 구매가 불가능합니다.",
    val purchaseSuccess: String = "<green><item>을 <price>원에 <amount>개 구매했습니다.",
    val sellSuccess: String = "<green><item>을 <price>원에 <amount>개 판매했습니다.",
    val insufficientItems: String = "<red>충분한 아이템을 가지고 있지 않습니다.",
    val outOfStock: String = "<red>해당 아이템은 품절되었습니다.",
    val buyLimitReached: String = "<red>이 아이템의 최대 구매 갯수를 초과하였습니다.",
    val dailyBuyLimitReached: String = "<red>이 아이템의 하루 구매 갯수를 초과하였습니다.",
    val sellLimitReached: String = "<red>이 아이템의 하루 판매 갯수를 초과하였습니다.",
    val inventoryFull: String = "<red>인벤토리 공간이 없어 구매가 취소되었습니다.",
    val priceChanged: String = "<gold>상점 가격이 업데이트 되었습니다!",
    val stockRestocked: String = "<gold>상점 재고가 입고되었습니다!",
    val purchaseLog: String = "BongShopLog >> <displayname>(<playername>)님이 [<shopname>]에서 <item>을 <price>원에 <amount>개 구매했습니다.",
    val sellLog: String = "BongShopLog >> <displayname>(<playername>)님이 [<shopname>]에서 <item>을 <price>원에 <amount>개 판매했습니다.",
    val sellHistory: String = "<white><player_displayname>님의 판매기록",
    val sellHistoryTotal: String = "<white>모든 상점 판매 토탈 : <amount>원",
    val sellHistoryShop: String = "<white><shopname> 판매 토탈 : <amount>원"
)

@Serializable
data class SoundEntry(
    @Serializable(with = SoundSerializer::class) val sound: Sound,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)

@Serializable
data class SoundsConfig(
    val purchaseFail: SoundEntry? = null,
    val purchaseSuccess: SoundEntry? = null,
    val sellFail: SoundEntry? = null,
    val sellSuccess: SoundEntry? = null,
    val outOfStock: SoundEntry? = null,
    val buyLimitReached: SoundEntry? = null,
    val inventoryFull: SoundEntry? = null,
    val stockRestocked: SoundEntry? = null,
    val priceChanged: SoundEntry? = null
)

@Serializable
data class ShopItemsConfig(
    val items: List<ShopItemConfig> = emptyList()
)
