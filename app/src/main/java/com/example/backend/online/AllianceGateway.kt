package com.example.backend.online

import com.example.backend.MythosBackend
import com.example.monetization.PlayerEconomyRepository

/**
 * Alliance operations as the screens use them. LOCAL_DEVELOPMENT delegates to the offline repository
 * (unchanged behaviour); ONLINE_AUTHORITATIVE sends the action to the server, which owns membership,
 * the 20-member cap, roles and contribution. Both return Result<Unit> whose failure message is safe to
 * show to the player, so the existing dialogs work unchanged.
 */
object AllianceGateway {
    private val repository get() = PlayerEconomyRepository.instance

    suspend fun create(name: String, emblem: String, description: String): Result<Unit> =
        if (MythosBackend.isOnline) online { createAlliance(name, emblem, description) }
        else repository.createAlliance(name, emblem, description).map { }

    suspend fun join(allianceId: String): Result<Unit> =
        if (MythosBackend.isOnline) online { joinAlliance(allianceId) } else repository.joinAlliance(allianceId).map { }

    suspend fun leave(): Result<Unit> =
        if (MythosBackend.isOnline) online { leaveAlliance() } else repository.leaveAlliance()

    suspend fun promote(playerId: String): Result<Unit> =
        if (MythosBackend.isOnline) online { promoteMember(playerId) } else repository.promoteMember(playerId).map { }

    suspend fun demote(playerId: String): Result<Unit> =
        if (MythosBackend.isOnline) online { demoteMember(playerId) } else repository.demoteMember(playerId).map { }

    suspend fun kick(playerId: String): Result<Unit> =
        if (MythosBackend.isOnline) online { kickMember(playerId) } else repository.kickMember(playerId).map { }

    suspend fun transferLeadership(playerId: String): Result<Unit> =
        if (MythosBackend.isOnline) online { transferLeadership(playerId) } else repository.transferLeadership(playerId).map { }

    private suspend fun online(call: suspend OnlineBackend.() -> Result<Unit>): Result<Unit> =
        MythosBackend.online.call().fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                Result.failure(Exception((e as? BackendException)?.let(OnlineBackend::userMessage) ?: e.message ?: "Request failed."))
            }
        )
}
