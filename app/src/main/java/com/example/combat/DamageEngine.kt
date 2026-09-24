package com.example.combat

import com.example.data.Hero

data class DamageResult(
    val rawBaseDamage: Int,
    val modifiedDamage: Int,
    val shieldAbsorbed: Int,
    val hpDamageDealt: Int,
    val updatedDefender: Hero,
    val counterDamageToAttacker: Int = 0,
    val isFatal: Boolean,
    val didTriggerLastStand: Boolean
)

object DamageEngine {

    /**
     * Reusable combat resolution pipeline:
     * Base Damage -> Attacker Modifiers (Attack Buffs / Last Stand) ->
     * Defender Reductions (Relics / Armor) -> Shield Absorption ->
     * HP Reduction -> Last Stand Check -> Death Check -> Counter Attacks
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
                didTriggerLastStand = false
            )
        }

        // 1. Attacker Modifiers
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

        // 2. Defender Damage Reduction (e.g. Nemean Lion Hide)
        if (defender.damageReductionPercent > 0) {
            val reduction = (damage * (defender.damageReductionPercent / 100.0)).toInt()
            damage = (damage - reduction).coerceAtLeast(1)
        }

        // 3. Shield Absorption
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

        // 4. HP Reduction
        val newHp = (defender.currentHp - hpDamage).coerceAtLeast(0)

        // 5. Last Stand Check (Hercules falls <= 30% max HP)
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

        // 6. Phalanx Counter Attack
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
            didTriggerLastStand = lastStandTriggered
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
}
