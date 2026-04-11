package kr.acog.bongshop.plugin

import java.util.UUID

object PendingChatInput {
    private val pending: MutableMap<UUID, (String) -> Unit> = mutableMapOf()

    fun await(playerUuid: UUID, callback: (String) -> Unit) {
        pending[playerUuid] = callback
    }

    fun consume(playerUuid: UUID): ((String) -> Unit)? = pending.remove(playerUuid)
}
