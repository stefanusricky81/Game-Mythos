package com.example.combat

import com.example.data.FloatingCombatText
import com.example.data.Hero
import java.util.UUID

data class DamageResult(
    val rawBaseDamage: Int,
    val modifiedDamage: Int,
    val shieldAbsorbed: Int,
    val hpDamageDealt: Int,
    val updatedDefender: Hero,
    val counterDamageToAttacker: Int = 0,
    val isFatal: Boolean,
    val didTriggerLastStand: Boolean,
    val wasVulnerableHit: Boolean = false
)

object DamageEngine {

    /**
     * Reusable combat resolution pipeline (Phase 9 Requirements #4, #5):
     * Base Damage -> Attacker Modifiers (Attack Buffs / Last Stand / Weaken) ->
     * Defender Vulnerability (+30% if vulnerable) -> Defender Reductions (Relics / Armor) ->
     * Shield Absorption -> HP Reduction -> Last Stand Check -> Death Check -> Counter Attacks
     */
    fun calculateAndApplyDamage(
        attacker: Hero?,
        defender: Hero,
        baseDamage: Int,
        isPhysical: Boolean = true,
        bonusAgainstShield: Int = 0
    ): DamageResult {
        if (baseDamage <= 0) {
            return DamageResult(
                rawBaseDamage = 0,
                modifiedDamage = 0,
                shieldAbsorbed = 0,
                hpDamageDealt = 0,
                updatedDefender = defender,
                counterDamageToAttacker = 0,
                isFatal = !defender.isAlive,
                didTriggerLastStand = false,
                wasVulnerableHit = false
            )
        }

        // 1. Attacker Modifiers (Attack stat scaling + buffs / debuffs)
        var damage = baseDamage
        if (attacker != null && attacker.baseAttack > 0) {
            val attackMultiplier = attacker.effectiveAttack.toDouble() / attacker.baseAttack.toDouble()
            damage = (damage * attackMultiplier).toInt()
        }

        // Bonus if defender has shield
        if (defender.currentShield > 0 && bonusAgainstShield > 0) {
            val bonusMult = if (attacker != null && attacker.baseAttack > 0) {
                attacker.effectiveAttack.toDouble() / attacker.baseAttack.toDouble()
            } else 1.0
            damage += (bonusAgainstShield * bonusMult).toInt()
        }

        // 2. Defender Vulnerability (+30% damage amplification)
        val wasVulnerable = defender.isVulnerable
        if (wasVulnerable) {
            damage = (damage * 1.30).toInt()
        }

        // 3. Defender Damage Reduction (e.g. Nemean Lion Hide)
        if (defender.damageReductionPercent > 0) {
            val reduction = (damage * (defender.damageReductionPercent / 100.0)).toInt()
            damage = (damage - reduction).coerceAtLeast(1)
        }

        // 4. Shield Absorption
        val currentShield = defender.currentShield
        var shieldAbsorbed = 0
        var hpDamage = 0
        var remainingShield = currentShield

        if (currentShield > 0) {
            if (currentShield >= damage) {
                shieldAbsorbed = damage
                remainingShield = currentShield - damage
                hpDamage = 0
            } else {
                shieldAbsorbed = currentShield
                remainingShield = 0
                hpDamage = damage - currentShield
            }
        } else {
            hpDamage = damage
        }

        // 5. HP Reduction
        val newHp = (defender.currentHp - hpDamage).coerceAtLeast(0)

        // 6. Last Stand Check (Hercules falls <= 30% max HP)
        var lastStandTriggered = false
        var isLastStandActive = defender.isLastStandActive
        var hasLastStandTriggered = defender.hasLastStandTriggered

        if (defender.name == "Hercules" && !hasLastStandTriggered) {
            val threshold = (defender.maxHp * 0.30).toInt() // 3,000 HP
            if (newHp in 1..threshold) {
                lastStandTriggered = true
                isLastStandActive = true
                hasLastStandTriggered = true
            }
        }

        // 7. Phalanx Counter Attack
        var counterDmg = 0
        if (defender.hasPhalanxGuard && attacker != null && hpDamage > 0) {
            counterDmg = defender.phalanxCounterDamage
        }

        val updatedDefender = defender.copy(
            currentHp = newHp,
            currentShield = remainingShield,
            isLastStandActive = isLastStandActive,
            hasLastStandTriggered = hasLastStandTriggered
        )

        return DamageResult(
            rawBaseDamage = baseDamage,
            modifiedDamage = damage,
            shieldAbsorbed = shieldAbsorbed,
            hpDamageDealt = hpDamage,
            updatedDefender = updatedDefender,
            counterDamageToAttacker = counterDmg,
            isFatal = (newHp <= 0),
            didTriggerLastStand = lastStandTriggered,
            wasVulnerableHit = wasVulnerable
        )
    }

    /**
     * Apply Shield to a Hero
     */
    fun applyShield(target: Hero, shieldAmount: Int): Hero {
        val newShield = target.currentShield + shieldAmount
        return target.copy(currentShield = newShield)
    }

    /**
     * Apply Heal to a Hero (capped at maxHp)
     */
    fun applyHeal(target: Hero, healAmount: Int): Pair<Hero, Int> {
        val newHp = (target.currentHp + healAmount).coerceAtMost(target.maxHp)
        val actualHealed = newHp - target.currentHp
        return Pair(target.copy(currentHp = newHp), actualHealed)
    }

    /**
     * Attaches a status effect to the Hero, refreshing duration or stacking if same type exists.
     */
    fun applyStatusEffect(target: Hero, effect: StatusEffect): Hero {
        val currentList = target.statusEffects.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.type == effect.type }
        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(
                durationTurns = maxOf(existing.durationTurns, effect.durationTurns),
                stacks = existing.stacks + effect.stacks,
                magnitude = maxOf(existing.magnitude, effect.magnitude)
            )
        } else {
            currentList.add(effect)
        }

        // Backwards compatibility sync for legacy poison fields
        var poisonTurns = target.poisonTurnsRemaining
        var poisonDmg = target.poisonDamagePerTurn
        if (effect.type == StatusEffectType.POISON) {
            poisonTurns = maxOf(poisonTurns, effect.durationTurns)
            poisonDmg = maxOf(poisonDmg, effect.magnitude)
        }

        return target.copy(
            statusEffects = currentList,
            poisonTurnsRemaining = poisonTurns,
            poisonDamagePerTurn = poisonDmg
        )
    }

    /**
     * Cleanses all negative debuffs (Poison, Burn, Stun, Vulnerable, Weaken).
     */
    fun cleanseDebuffs(target: Hero): Hero {
        val cleansed = target.statusEffects.filterNot { it.type.isDebuff }
        return target.copy(
            statusEffects = cleansed,
            poisonTurnsRemaining = 0,
            poisonDamagePerTurn = 0
        )
    }

    /**
     * Resolves start-of-turn statuses (Burn damage, Regeneration, decrementing Stun).
     */
    fun resolveTurnStartStatuses(hero: Hero, isEnemy: Boolean): Pair<Hero, List<FloatingCombatText>> {
        val floating = mutableListOf<FloatingCombatText>()
        var currentHero = hero
        val updatedEffects = mutableListOf<StatusEffect>()

        for (effect in currentHero.statusEffects) {
            if (effect.isExpired) continue

            when (effect.type) {
                StatusEffectType.BURN -> {
                    val burnDmg = effect.magnitude
                    val newHp = (currentHero.currentHp - burnDmg).coerceAtLeast(0)
                    currentHero = currentHero.copy(currentHp = newHp)
                    floating.add(
                        FloatingCombatText(
                            id = UUID.randomUUID().toString(),
                            text = "-$burnDmg Burn",
                            isEnemyTarget = isEnemy,
                            isPositive = false
                        )
                    )
                    val remainingTurns = effect.durationTurns - 1
                    if (remainingTurns > 0) {
                        updatedEffects.add(effect.copy(durationTurns = remainingTurns))
                    }
                }
                StatusEffectType.REGENERATION -> {
                    val (healedHero, amount) = applyHeal(currentHero, effect.magnitude)
                    currentHero = healedHero
                    floating.add(
                        FloatingCombatText(
                            id = UUID.randomUUID().toString(),
                            text = "+$amount Regen",
                            isEnemyTarget = isEnemy,
                            isPositive = true
                        )
                    )
                    val remainingTurns = effect.durationTurns - 1
                    if (remainingTurns > 0) {
                        updatedEffects.add(effect.copy(durationTurns = remainingTurns))
                    }
                }
                StatusEffectType.STUN -> {
                    val remainingTurns = effect.durationTurns - 1
                    if (remainingTurns > 0) {
                        updatedEffects.add(effect.copy(durationTurns = remainingTurns))
                    }
                }
                else -> {
                    // Turn-end or on-hit effects stay unchanged at turn start
                    updatedEffects.add(effect)
                }
            }
        }

        return Pair(currentHero.copy(statusEffects = updatedEffects), floating)
    }

    /**
     * Resolves end-of-turn statuses (Poison damage, decrementing Vulnerable and Weaken).
     */
    fun resolveTurnEndStatuses(hero: Hero, isEnemy: Boolean): Pair<Hero, List<FloatingCombatText>> {
        val floating = mutableListOf<FloatingCombatText>()
        var currentHero = hero
        val updatedEffects = mutableListOf<StatusEffect>()

        for (effect in currentHero.statusEffects) {
            if (effect.isExpired) continue

            when (effect.type) {
                StatusEffectType.POISON -> {
                    val poisonDmg = effect.magnitude
                    val newHp = (currentHero.currentHp - poisonDmg).coerceAtLeast(0)
                    currentHero = currentHero.copy(currentHp = newHp)
                    floating.add(
                        FloatingCombatText(
                            id = UUID.randomUUID().toString(),
                            text = "-$poisonDmg Poison",
                            isEnemyTarget = isEnemy,
                            isPositive = false
                        )
                    )
                    val remainingTurns = effect.durationTurns - 1
                    if (remainingTurns > 0) {
                        updatedEffects.add(effect.copy(durationTurns = remainingTurns))
                    }
                }
                StatusEffectType.VULNERABLE, StatusEffectType.WEAKEN -> {
                    val remainingTurns = effect.durationTurns - 1
                    if (remainingTurns > 0) {
                        updatedEffects.add(effect.copy(durationTurns = remainingTurns))
                    }
                }
                else -> {
                    updatedEffects.add(effect)
                }
            }
        }

        // Backwards compatibility for legacy poison field
        val poisonRemaining = updatedEffects.find { it.type == StatusEffectType.POISON }?.durationTurns ?: 0
        val poisonDamage = updatedEffects.find { it.type == StatusEffectType.POISON }?.magnitude ?: 0

        return Pair(
            currentHero.copy(
                statusEffects = updatedEffects,
                poisonTurnsRemaining = poisonRemaining,
                poisonDamagePerTurn = poisonDamage
            ),
            floating
        )
    }
}
