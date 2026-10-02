package com.example.backend

import java.util.concurrent.ConcurrentHashMap

/**
 * Audit Operation Types (Requirement #28).
 */
enum class AuditOperationType {
    ARENA_RESULT,
    REWARD_GRANT,
    CURRENCY_MUTATION,
    DECK_VALIDATION_FAILURE,
    ALLIANCE_ROLE_CHANGE,
    EVENT_REWARD_CLAIM,
    WORLD_BOSS_CONTRIBUTION,
    RAID_REWARD
}

/**
 * Server Audit Log Entry (Requirement #28).
 */
data class ServerAuditRecord(
    val auditId: String,
    val type: AuditOperationType,
    val uid: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Server Audit Logger.
 * Records sensitive events without logging secrets or payment tokens.
 */
object ServerAuditLogger {
    private val _logs = mutableListOf<ServerAuditRecord>()
    val logs: List<ServerAuditRecord> get() = synchronized(_logs) { _logs.toList() }

    fun log(type: AuditOperationType, uid: String, details: String) {
        val record = ServerAuditRecord(
            auditId = "audit_${System.currentTimeMillis()}_${_logs.size}",
            type = type,
            uid = uid,
            details = details
        )
        synchronized(_logs) {
            _logs.add(record)
        }
    }

    fun clear() {
        synchronized(_logs) {
            _logs.clear()
        }
    }
}

/**
 * Server Idempotency Manager (Requirement #33).
 * Protects reward grants, matches, and transactions from duplicate executions.
 */
class ServerIdempotencyManager {
    private val _processedTokens = ConcurrentHashMap<String, Long>()

    /**
     * Executes the block only if the token has not been processed yet.
     * If already processed, returns false or throws.
     */
    fun <T> executeIdempotent(
        idempotencyToken: String,
        block: () -> T
    ): Pair<T?, Boolean> {
        require(idempotencyToken.isNotBlank()) { "Idempotency token cannot be blank" }

        if (_processedTokens.containsKey(idempotencyToken)) {
            // Already processed: prevent duplicate grant/execution
            return Pair(null, false)
        }

        val result = block()
        _processedTokens[idempotencyToken] = System.currentTimeMillis()
        return Pair(result, true)
    }

    fun isProcessed(token: String): Boolean = _processedTokens.containsKey(token)
}

/**
 * Lightweight Server-side Rate Limiter (Requirement #27).
 * Prevents spamming on sensitive endpoints (reward claim, match submission, etc.).
 */
class ServerRateLimiter(
    private val maxRequestsPerWindow: Int = 10,
    private val windowMs: Long = 1000L
) {
    private val _requestTimestamps = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Checks if the caller with [uid] can execute an action.
     * Returns true if allowed, false if rate limited.
     */
    fun acquirePermission(uid: String, now: Long = System.currentTimeMillis()): Boolean {
        val timestamps = _requestTimestamps.computeIfAbsent(uid) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeAll { now - it > windowMs }
            if (timestamps.size >= maxRequestsPerWindow) {
                return false
            }
            timestamps.add(now)
            return true
        }
    }
}

/**
 * Security Context Validator (Requirement #26).
 * Enforces authenticated UID matching and prevents client-spoofed identities.
 */
object ServerSecurityValidator {

    /**
     * Validates that the request origin matches the authenticated user.
     */
    fun validateAuthContext(authenticatedUid: String?, requestedUid: String) {
        if (authenticatedUid.isNullOrBlank()) {
            throw SecurityException("Unauthenticated request: missing caller identity")
        }
        if (authenticatedUid != requestedUid) {
            throw SecurityException("Unauthorized access: caller '$authenticatedUid' cannot mutate player '$requestedUid'")
        }
    }

    /**
     * Validates currency mutation bounds to catch overflows or negative tampering.
     */
    fun validateCurrencyBounds(amount: Long, maxPerTransaction: Long = 1_000_000L) {
        if (amount <= 0) {
            throw IllegalArgumentException("Amount must be positive, got: $amount")
        }
        if (amount > maxPerTransaction) {
            throw IllegalArgumentException("Amount $amount exceeds single transaction limit ($maxPerTransaction)")
        }
    }
}
