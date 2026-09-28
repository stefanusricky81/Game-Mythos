package com.example.data

/**
 * Member roles within an Alliance (Phase 8 Section 1).
 */
enum class AllianceRole {
    LEADER,
    OFFICER,
    MEMBER;

    val title: String
        get() = when (this) {
            LEADER -> "Leader"
            OFFICER -> "Officer"
            MEMBER -> "Member"
        }

    val canPromoteDemote: Boolean
        get() = this == LEADER

    val canTransferLeadership: Boolean
        get() = this == LEADER

    val canDisband: Boolean
        get() = this == LEADER

    val canKick: Boolean
        get() = this == LEADER || this == OFFICER

    val canEditDescription: Boolean
        get() = this == LEADER || this == OFFICER
}

/**
 * Individual Alliance Member model (Phase 8 Section 1, 4).
 */
data class AllianceMember(
    val playerId: String,
    val name: String,
    val playerLevel: Int = 1,
    val heroId: String = "hero_hercules",
    val combatPower: Int = 1200,
    val role: AllianceRole = AllianceRole.MEMBER,
    val joinedDate: String = "2026-09-20",
    val battlesWon: Int = 0,
    val damageDealt: Long = 0L,
    val campaignClears: Int = 0,
    val questsCompleted: Int = 0,
    val contributionScore: Long = 0L
) {
    fun calculateContribution(): Long {
        return (battlesWon * 50L) + (campaignClears * 100L) + (questsCompleted * 30L) + (damageDealt / 200L)
    }
}

/**
 * Authoritative Alliance Model (Phase 8 Section 1).
 */
data class Alliance(
    val allianceId: String,
    val name: String,
    val emblem: String = "emblem_gold_eagle",
    val description: String = "Warriors of Mount Olympus united in divine glory.",
    val level: Int = 1,
    val xp: Int = 0,
    val createdDate: String = "2026-09-01",
    val members: List<AllianceMember> = emptyList()
) {
    companion object {
        const val MAX_MEMBERS = 20
        const val CREATE_GOLD_COST = 5_000
    }

    val memberCount: Int
        get() = members.size

    val isFull: Boolean
        get() = members.size >= MAX_MEMBERS

    val totalPower: Long
        get() = members.sumOf { it.combatPower.toLong() }

    val xpRequiredForNextLevel: Int
        get() = AllianceProgressionConfig.getXpRequiredForNextLevel(level)

    val xpProgressFraction: Float
        get() = if (level >= AllianceProgressionConfig.MAX_ALLIANCE_LEVEL) 1f
        else (xp.toFloat() / xpRequiredForNextLevel.toFloat()).coerceIn(0f, 1f)

    val leader: AllianceMember?
        get() = members.find { it.role == AllianceRole.LEADER }

    val sortedByContribution: List<AllianceMember>
        get() = members.sortedByDescending { it.contributionScore }
}

/**
 * Centralized Alliance Progression and XP requirements (Phase 8 Section 3).
 */
object AllianceProgressionConfig {
    const val MAX_ALLIANCE_LEVEL = 10

    private val XP_TABLE = mapOf(
        1 to 1_000,
        2 to 2_500,
        3 to 5_000,
        4 to 8_000,
        5 to 12_000,
        6 to 17_000,
        7 to 23_000,
        8 to 30_000,
        9 to 40_000
    )

    fun getXpRequiredForNextLevel(level: Int): Int {
        if (level >= MAX_ALLIANCE_LEVEL) return 0
        return XP_TABLE[level] ?: (level * 5_000)
    }

    fun getPerkDescription(level: Int): String = when (level) {
        1 -> "Olympus Fellowship: +2% Battle Gold Bonus"
        2 -> "Divine Camaraderie: +3% Battle Gold Bonus"
        3 -> "Pantheon Shield: +5% Defense in Campaign"
        4 -> "Olympian Might: +5% Attack in Battles"
        5 -> "Nectar of the Gods: +5% Extra XP for Members"
        else -> "Ascended Alliance: +10% Overall Rewards"
    }
}

/**
 * Emblem choices for Alliance creation.
 */
object AllianceEmblems {
    val ALL_EMBLEMS = listOf(
        AllianceEmblemItem("emblem_gold_eagle", "Golden Eagle", "🦅", 0xFFE0B034),
        AllianceEmblemItem("emblem_lion_nemean", "Nemean Lion", "🦁", 0xFFE57373),
        AllianceEmblemItem("emblem_thunderbolt", "Zeus Bolt", "⚡", 0xFF64B5F6),
        AllianceEmblemItem("emblem_olympus_shield", "Aegis Shield", "🛡️", 0xFF81C784),
        AllianceEmblemItem("emblem_spartan_helmet", "Spartan Helm", "⚔️", 0xFFFFB74D),
        AllianceEmblemItem("emblem_poseidon_trident", "Poseidon Trident", "🔱", 0xFF4DD0E1)
    )

    fun find(id: String): AllianceEmblemItem =
        ALL_EMBLEMS.find { it.id == id } ?: ALL_EMBLEMS[0]
}

data class AllianceEmblemItem(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val colorHex: Long
)

/**
 * Pre-populated Open Alliances for prototype browsing and joining (Phase 8 Section 1).
 */
object AllianceCatalog {
    fun createDefaultAlliances(): List<Alliance> = listOf(
        Alliance(
            allianceId = "alliance_olympus_guard",
            name = "Guardians of Olympus",
            emblem = "emblem_gold_eagle",
            description = "Devoted defenders of Mount Olympus. Seeking brave mythic champions.",
            level = 3,
            xp = 1800,
            createdDate = "2026-08-15",
            members = listOf(
                AllianceMember("bot_zeus_fan", "Aeroch", 14, "hero_achilles", 3400, AllianceRole.LEADER, "2026-08-15", 42, 185000L, 12, 28, 4850L),
                AllianceMember("bot_valkyrie", "Brynhild", 11, "hero_hercules", 2900, AllianceRole.OFFICER, "2026-08-18", 31, 142000L, 9, 21, 3550L),
                AllianceMember("bot_spartan_1", "Leonidas", 9, "hero_achilles", 2300, AllianceRole.MEMBER, "2026-08-25", 22, 98000L, 7, 15, 2500L),
                AllianceMember("bot_athena_priest", "Lysander", 8, "hero_merlin", 2100, AllianceRole.MEMBER, "2026-09-02", 15, 65000L, 5, 12, 1800L),
                AllianceMember("bot_gladiator", "Decimus", 7, "hero_hercules", 1850, AllianceRole.MEMBER, "2026-09-10", 11, 45000L, 4, 8, 1300L)
            )
        ),
        Alliance(
            allianceId = "alliance_titans_bane",
            name = "Titans' Bane",
            emblem = "emblem_spartan_helmet",
            description = "Elite gladiators forged in Tartarus. High combat power preferred.",
            level = 4,
            xp = 4200,
            createdDate = "2026-07-28",
            members = listOf(
                AllianceMember("bot_kratos", "Theron", 18, "hero_hercules", 4500, AllianceRole.LEADER, "2026-07-28", 68, 310000L, 20, 45, 7800L),
                AllianceMember("bot_ares_son", "Crixus", 15, "hero_achilles", 3800, AllianceRole.OFFICER, "2026-08-01", 52, 240000L, 16, 36, 6100L),
                AllianceMember("bot_blade_dancer", "Daphne", 12, "hero_achilles", 3100, AllianceRole.MEMBER, "2026-08-10", 35, 155000L, 10, 24, 4000L)
            )
        ),
        Alliance(
            allianceId = "alliance_arcane_cabal",
            name = "Arcane Cabal",
            emblem = "emblem_thunderbolt",
            description = "Mystics unraveling celestial spells and relics across ancient realms.",
            level = 2,
            xp = 950,
            createdDate = "2026-09-05",
            members = listOf(
                AllianceMember("bot_mage_elder", "Morrigan", 10, "hero_merlin", 2600, AllianceRole.LEADER, "2026-09-05", 25, 110000L, 8, 18, 2900L),
                AllianceMember("bot_elementalist", "Ignis", 8, "hero_merlin", 2050, AllianceRole.MEMBER, "2026-09-12", 14, 58000L, 4, 11, 1650L)
            )
        )
    )
}
