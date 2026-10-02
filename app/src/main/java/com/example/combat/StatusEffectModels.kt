package com.example.combat

/**
 * Supported Status Effect Types across MYTHOS (Phase 9 Requirements #4, #5).
 */
enum class StatusEffectType(
    val displayName: String,
    val isDebuff: Boolean,
    val iconSymbol: String,
    val description: String
) {
    POISON("Poison", true, "🧪", "Deals damage at the end of each turn."),
    BURN("Burn", true, "🔥", "Deals flat fire damage at turn start."),
    STUN("Stun", true, "⚡", "Prevents taking basic attacks or card actions for 1 turn."),
    VULNERABLE("Vulnerable", true, "💔", "Takes +30% increased damage from all attacks."),
    WEAKEN("Weaken", true, "📉", "Deals -25% reduced damage with all attacks and abilities."),
    SHIELD("Shield", false, "🛡️", "Absorbs incoming damage before HP is reduced."),
    REGENERATION("Regeneration", false, "🌿", "Restores HP at the beginning of each turn.")
}

/**
 * Execution timing trigger for status effects.
 */
enum class StatusTiming {
    TURN_START,
    TURN_END,
    ON_DAMAGE_DEALT,
    ON_DAMAGE_TAKEN
}

/**
 * Reusable, centralized Status Effect instance attached to a Hero.
 */
data class StatusEffect(
    val id: String,
    val type: StatusEffectType,
    val displayName: String = type.displayName,
    val durationTurns: Int,
    val stacks: Int = 1,
    val magnitude: Int = 0,
    val sourceName: String = "",
    val timing: StatusTiming = when (type) {
        StatusEffectType.BURN, StatusEffectType.REGENERATION, StatusEffectType.STUN -> StatusTiming.TURN_START
        StatusEffectType.POISON -> StatusTiming.TURN_END
        StatusEffectType.WEAKEN -> StatusTiming.ON_DAMAGE_DEALT
        StatusEffectType.VULNERABLE -> StatusTiming.ON_DAMAGE_TAKEN
        StatusEffectType.SHIELD -> StatusTiming.ON_DAMAGE_TAKEN
    }
) {
    val isExpired: Boolean get() = durationTurns <= 0
}

/**
 * Catalog of standard status effect definitions and helper factories.
 */
object StatusEffectCatalog {

    fun createPoison(turns: Int, damagePerTurn: Int, source: String = "Poison"): StatusEffect {
        return StatusEffect(
            id = "status_poison_${System.currentTimeMillis()}",
            type = StatusEffectType.POISON,
            displayName = "Poison",
            durationTurns = turns,
            magnitude = damagePerTurn,
            sourceName = source,
            timing = StatusTiming.TURN_END
        )
    }

    fun createBurn(turns: Int, damagePerTurn: Int, source: String = "Burn"): StatusEffect {
        return StatusEffect(
            id = "status_burn_${System.currentTimeMillis()}",
            type = StatusEffectType.BURN,
            displayName = "Burn",
            durationTurns = turns,
            magnitude = damagePerTurn,
            sourceName = source,
            timing = StatusTiming.TURN_START
        )
    }

    fun createStun(turns: Int = 1, source: String = "Stun"): StatusEffect {
        return StatusEffect(
            id = "status_stun_${System.currentTimeMillis()}",
            type = StatusEffectType.STUN,
            displayName = "Stunned",
            durationTurns = turns,
            magnitude = 0,
            sourceName = source,
            timing = StatusTiming.TURN_START
        )
    }

    fun createVulnerable(turns: Int, bonusDamagePercent: Int = 30, source: String = "Vulnerable"): StatusEffect {
        return StatusEffect(
            id = "status_vuln_${System.currentTimeMillis()}",
            type = StatusEffectType.VULNERABLE,
            displayName = "Vulnerable",
            durationTurns = turns,
            magnitude = bonusDamagePercent,
            sourceName = source,
            timing = StatusTiming.ON_DAMAGE_TAKEN
        )
    }

    fun createWeaken(turns: Int, reductionPercent: Int = 25, source: String = "Weaken"): StatusEffect {
        return StatusEffect(
            id = "status_weaken_${System.currentTimeMillis()}",
            type = StatusEffectType.WEAKEN,
            displayName = "Weakened",
            durationTurns = turns,
            magnitude = reductionPercent,
            sourceName = source,
            timing = StatusTiming.ON_DAMAGE_DEALT
        )
    }

    fun createRegeneration(turns: Int, healPerTurn: Int, source: String = "Regeneration"): StatusEffect {
        return StatusEffect(
            id = "status_regen_${System.currentTimeMillis()}",
            type = StatusEffectType.REGENERATION,
            displayName = "Regen",
            durationTurns = turns,
            magnitude = healPerTurn,
            sourceName = source,
            timing = StatusTiming.TURN_START
        )
    }
}
