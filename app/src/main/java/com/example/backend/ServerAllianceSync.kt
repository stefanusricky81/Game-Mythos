package com.example.backend

import com.example.data.AllianceRole

/**
 * Server-authoritative Alliance Member Record (Requirement #18).
 */
data class ServerAllianceMember(
    val uid: String,
    val name: String,
    val role: AllianceRole = AllianceRole.MEMBER,
    val contributionScore: Long = 0L,
    val joinedAt: Long = System.currentTimeMillis()
)

/**
 * Server-authoritative Alliance Model (Requirement #18).
 * Represents alliances/{allianceId} in Cloud Firestore.
 */
data class ServerAllianceRecord(
    val allianceId: String,
    val name: String,
    val leaderUid: String,
    val level: Int = 1,
    val totalContribution: Long = 0L,
    val members: List<ServerAllianceMember> = emptyList(),
    val perks: List<String> = listOf("EXP_BOOST_5"),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val memberCount: Int get() = members.size
    val isFull: Boolean get() = members.size >= MAX_MEMBERS

    companion object {
        const val MAX_MEMBERS = 20
    }
}

/**
 * Server Alliance Authority (Requirement #18).
 * Enforces role checks, member limits, and membership consistency on the backend.
 */
class ServerAllianceService(
    initialAlliances: List<ServerAllianceRecord> = emptyList()
) {
    private val _alliances = initialAlliances.associateBy { it.allianceId }.toMutableMap()

    val alliances: Map<String, ServerAllianceRecord> get() = _alliances.toMap()

    fun getAlliance(allianceId: String): ServerAllianceRecord? = _alliances[allianceId]

    /**
     * Authoritatively creates a new Alliance. The creator becomes LEADER.
     */
    fun createAlliance(
        allianceId: String,
        name: String,
        creatorUid: String,
        creatorName: String
    ): ServerAllianceRecord {
        require(allianceId.isNotBlank()) { "Alliance ID cannot be blank" }
        require(name.isNotBlank()) { "Alliance name cannot be blank" }
        check(!_alliances.containsKey(allianceId)) { "Alliance '$allianceId' already exists" }

        val leader = ServerAllianceMember(
            uid = creatorUid,
            name = creatorName,
            role = AllianceRole.LEADER
        )
        val record = ServerAllianceRecord(
            allianceId = allianceId,
            name = name,
            leaderUid = creatorUid,
            members = listOf(leader)
        )
        _alliances[allianceId] = record
        return record
    }

    /**
     * Authoritatively joins an Alliance with MAX 20 members limit check.
     */
    fun joinAlliance(
        allianceId: String,
        uid: String,
        name: String
    ): ServerAllianceRecord {
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance '$allianceId' not found")
        check(alliance.members.none { it.uid == uid }) { "Player is already a member of this alliance" }
        check(!alliance.isFull) { "Alliance is full (${alliance.memberCount}/${ServerAllianceRecord.MAX_MEMBERS})" }

        val newMember = ServerAllianceMember(uid = uid, name = name, role = AllianceRole.MEMBER)
        val updated = alliance.copy(
            members = alliance.members + newMember,
            updatedAt = System.currentTimeMillis()
        )
        _alliances[allianceId] = updated
        return updated
    }

    /**
     * Authoritatively leaves an Alliance.
     */
    fun leaveAlliance(allianceId: String, uid: String): ServerAllianceRecord {
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance '$allianceId' not found")
        val member = alliance.members.find { it.uid == uid } ?: throw IllegalStateException("Player not in alliance")
        check(member.role != AllianceRole.LEADER || alliance.members.size <= 1) {
            "Leader cannot leave without transferring leadership first"
        }

        val updated = alliance.copy(
            members = alliance.members.filter { it.uid != uid },
            updatedAt = System.currentTimeMillis()
        )
        _alliances[allianceId] = updated
        return updated
    }

    /**
     * Authoritatively kicks a member. Caller must be OFFICER or LEADER.
     */
    fun kickMember(
        allianceId: String,
        callerUid: String,
        targetUid: String
    ): ServerAllianceRecord {
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance not found")
        val caller = alliance.members.find { it.uid == callerUid } ?: throw IllegalStateException("Caller not in alliance")
        val target = alliance.members.find { it.uid == targetUid } ?: throw IllegalStateException("Target not in alliance")

        check(caller.role == AllianceRole.LEADER || caller.role == AllianceRole.OFFICER) {
            "Only leaders and officers can kick members"
        }
        check(target.role != AllianceRole.LEADER) { "Cannot kick the alliance leader" }
        if (caller.role == AllianceRole.OFFICER) {
            check(target.role == AllianceRole.MEMBER) { "Officers cannot kick other officers" }
        }

        val updated = alliance.copy(
            members = alliance.members.filter { it.uid != targetUid },
            updatedAt = System.currentTimeMillis()
        )
        _alliances[allianceId] = updated
        return updated
    }

    /**
     * Promotes a member. Caller must be LEADER.
     */
    fun promoteMember(allianceId: String, leaderUid: String, targetUid: String): ServerAllianceRecord {
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance not found")
        check(alliance.leaderUid == leaderUid) { "Only the alliance leader can promote members" }
        val target = alliance.members.find { it.uid == targetUid } ?: throw IllegalStateException("Target not in alliance")
        check(target.role == AllianceRole.MEMBER) { "Member is already an officer or leader" }

        val updatedMembers = alliance.members.map {
            if (it.uid == targetUid) it.copy(role = AllianceRole.OFFICER) else it
        }
        val updated = alliance.copy(members = updatedMembers, updatedAt = System.currentTimeMillis())
        _alliances[allianceId] = updated
        return updated
    }

    /**
     * Demotes an officer. Caller must be LEADER.
     */
    fun demoteMember(allianceId: String, leaderUid: String, targetUid: String): ServerAllianceRecord {
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance not found")
        check(alliance.leaderUid == leaderUid) { "Only the alliance leader can demote officers" }
        val target = alliance.members.find { it.uid == targetUid } ?: throw IllegalStateException("Target not in alliance")
        check(target.role == AllianceRole.OFFICER) { "Target is not an officer" }

        val updatedMembers = alliance.members.map {
            if (it.uid == targetUid) it.copy(role = AllianceRole.MEMBER) else it
        }
        val updated = alliance.copy(members = updatedMembers, updatedAt = System.currentTimeMillis())
        _alliances[allianceId] = updated
        return updated
    }

    /**
     * Transfers leadership to another member. Caller must be LEADER.
     */
    fun transferLeadership(allianceId: String, currentLeaderUid: String, newLeaderUid: String): ServerAllianceRecord {
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance not found")
        check(alliance.leaderUid == currentLeaderUid) { "Only current leader can transfer leadership" }
        check(alliance.members.any { it.uid == newLeaderUid }) { "New leader must be an existing member" }

        val updatedMembers = alliance.members.map {
            when (it.uid) {
                currentLeaderUid -> it.copy(role = AllianceRole.OFFICER)
                newLeaderUid -> it.copy(role = AllianceRole.LEADER)
                else -> it
            }
        }
        val updated = alliance.copy(
            leaderUid = newLeaderUid,
            members = updatedMembers,
            updatedAt = System.currentTimeMillis()
        )
        _alliances[allianceId] = updated
        return updated
    }

    /**
     * Records member contribution to alliance XP and levels up alliance.
     */
    fun recordContribution(allianceId: String, uid: String, points: Long): ServerAllianceRecord {
        require(points > 0) { "Contribution points must be positive" }
        val alliance = _alliances[allianceId] ?: throw IllegalArgumentException("Alliance not found")
        check(alliance.members.any { it.uid == uid }) { "Player is not in alliance" }

        val updatedMembers = alliance.members.map {
            if (it.uid == uid) it.copy(contributionScore = it.contributionScore + points) else it
        }
        val newTotal = alliance.totalContribution + points
        val newLevel = (1 + (newTotal / 10000L)).toInt().coerceIn(1, 20)

        val updated = alliance.copy(
            totalContribution = newTotal,
            level = newLevel,
            members = updatedMembers,
            updatedAt = System.currentTimeMillis()
        )
        _alliances[allianceId] = updated
        return updated
    }
}
