package kr.acog.bongshop.shop

import kr.acog.bongshop.config.ShopGuiConfig
import kr.acog.bongshop.config.ShopItemConfig
import kr.acog.bongshop.state.ShopState

data class ShopInstance(
    val guiConfig: ShopGuiConfig,
    val items: List<ShopItemConfig>,
    val state: ShopState
) {
    val itemsById: Map<String, ShopItemConfig> = items.associateBy { it.id }
}
