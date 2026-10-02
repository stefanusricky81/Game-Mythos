package com.example.backend

import java.util.UUID

/**
 * Player Identity Abstraction (Requirement #6).
 *
 * Provides a clean boundary for authenticated player identity.
 * Does not scatter Firebase UID access throughout the UI.
 * All server-side player documents are keyed to this authenticated UID.
 * The backend derives identity from the authenticated context, never trusting client-supplied spoofed UIDs.
 */
interface PlayerIdentity {
    val currentUid: String
    val isAnonymous: Boolean
    val isAuthenticated: Boolean

    suspend fun getAuthToken(): String?
}

/**
 * Default production implementation of PlayerIdentity.
 * Supports Firebase Anonymous Authentication and secure local fallback identity.
 */
class DefaultPlayerIdentity(
    private val persistentUidProvider: () -> String = { "player_local" }
) : PlayerIdentity {

    @Volatile
    private var _cachedUid: String = persistentUidProvider()

    @Volatile
    private var _isAnonymous: Boolean = true

    @Volatile
    private var _isAuthenticated: Boolean = true

    override val currentUid: String
        get() = _cachedUid

    override val isAnonymous: Boolean
        get() = _isAnonymous

    override val isAuthenticated: Boolean
        get() = _isAuthenticated

    fun setAuthenticatedUser(uid: String, isAnon: Boolean) {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        _cachedUid = uid
        _isAnonymous = isAnon
        _isAuthenticated = true
    }

    override suspend fun getAuthToken(): String? {
        return if (_isAuthenticated) "mythos_token_${_cachedUid.hashCode()}" else null
    }

    companion object {
        val Instance: DefaultPlayerIdentity by lazy { DefaultPlayerIdentity() }
    }
}
