package com.example.ai

import com.example.data.BossCatalog
import com.example.data.Card
import com.example.data.DeckFactory
import com.example.data.EnemyBehaviorType
import com.example.data.EnemyCatalog
import com.example.data.Hero

sealed class AiAction {
    data class PlayCard(val card: Card) : AiAction()
    data class BasicAttack(val baseDamage: Int = 1800, val isMultiHit: Boolean = false) : AiAction()
    data class BossSpecialAbility(val abilityName: String, val damage: Int, val description: String) : AiAction()
}

object EnemyAi {

    /**
     * Decides the sequence of actions for the enemy AI on its turn (Requirement #6 & #7).
     * Differentiates AI behaviors based on Enemy identity:
     * - Minotaur: aggressive melee bursts, heavy basic attacks
     * - Hydra: venom spitting, regeneration, multi-head strikes
     * - Medusa: control debuffs, gaze petrification
     * - Cerberus: triple-fanged multi-hit attacks
     * - Frost Giant: slow heavy crushing strikes
     * - Spartan Champion: defensive shields and retaliation
     * - Valkyrie: defensive tactical play and execute on wounded targets
     * - Egyptian Guardian: curse drain and soul execution
     */
    fun decideTurnActions(
        enemyHero: Hero,
        playerHero: Hero,
        availableEnergy: Int,
        isBossPhase2: Boolean = false
    ): List<AiAction> {
        val actions = mutableListOf<AiAction>()
        var energy = availableEnergy
        val enemyDef = EnemyCatalog.findEnemy(enemyHero.id)
        val bossDef = BossCatalog.findBoss(enemyHero.id)
        val behavior = enemyDef?.behaviorType ?: bossDef?.behaviorType ?: EnemyBehaviorType.AGGRESSIVE_MELEE
        val deck = DeckFactory.createEnemyDeck().shuffled()

        // 1. Boss Phase 2 Special Ability Trigger
        if (isBossPhase2 && energy >= 3) {
            val specialDmg = when {
                enemyHero.id.contains("minotaur") -> 3500
                enemyHero.id.contains("hydra") -> 3200
                enemyHero.id.contains("medusa") -> 3000
                else -> 3100
            }
            val specialName = when {
                enemyHero.id.contains("minotaur") -> "Cataclysmic Gore"
                enemyHero.id.contains("hydra") -> "Nine-Fanged Barrage"
                enemyHero.id.contains("medusa") -> "Stone-Eye Petrification"
                else -> "Mythic Enrage Surge"
            }
            val specialDesc = bossDef?.phase2AbilityDescription ?: "Unleashes second-phase mythic assault!"
            actions.add(AiAction.BossSpecialAbility(specialName, specialDmg, specialDesc))
            energy -= 3
        }

        when (behavior) {
            EnemyBehaviorType.AGGRESSIVE_MELEE -> {
                // Minotaur / Aggressive: prioritize high-damage attack cards, then heavy basic attacks
                for (card in deck.filter { it.effect.damage > 0 }) {
                    if (card.cost <= energy && actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                        actions.add(AiAction.PlayCard(card))
                        energy -= card.cost
                        if (actions.size >= 2) break
                    }
                }
                if (energy >= 1) {
                    val bonusDmg = if (enemyHero.hpPercentage < 0.5f) 2400 else 1800
                    actions.add(AiAction.BasicAttack(baseDamage = bonusDmg))
                }
            }

            EnemyBehaviorType.REGENERATION -> {
                // Hydra: spit poison / status cards first, then attack
                val poisonCard = deck.firstOrNull { it.effect.poisonTurns > 0 && it.cost <= energy }
                if (poisonCard != null) {
                    actions.add(AiAction.PlayCard(poisonCard))
                    energy -= poisonCard.cost
                }
                for (card in deck.filter { it.cost <= energy }) {
                    if (actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                        actions.add(AiAction.PlayCard(card))
                        energy -= card.cost
                        break
                    }
                }
                if (actions.isEmpty() && energy >= 1) {
                    actions.add(AiAction.BasicAttack(baseDamage = 1900))
                }
            }

            EnemyBehaviorType.CONTROL_DEBUFF -> {
                // Medusa / Control: apply debuffs, then strike
                val debuffCard = deck.firstOrNull { (it.effect.poisonTurns > 0 || it.effect.damageReductionPercent > 0) && it.cost <= energy }
                if (debuffCard != null) {
                    actions.add(AiAction.PlayCard(debuffCard))
                    energy -= debuffCard.cost
                }
                for (card in deck) {
                    if (card.cost <= energy && actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                        actions.add(AiAction.PlayCard(card))
                        energy -= card.cost
                        if (actions.size >= 2) break
                    }
                }
                if (actions.isEmpty() && energy >= 1) {
                    actions.add(AiAction.BasicAttack(baseDamage = 1700))
                }
            }

            EnemyBehaviorType.MULTI_HIT -> {
                // Cerberus: multi-bite attack
                for (card in deck.take(1)) {
                    if (card.cost <= energy) {
                        actions.add(AiAction.PlayCard(card))
                        energy -= card.cost
                    }
                }
                if (energy >= 1) {
                    actions.add(AiAction.BasicAttack(baseDamage = 2200, isMultiHit = true))
                }
            }

            EnemyBehaviorType.HEAVY_SMASH -> {
                // Frost Giant: slow heavy crushing strikes
                val heavyCard = deck.maxByOrNull { it.effect.damage }
                if (heavyCard != null && heavyCard.cost <= energy) {
                    actions.add(AiAction.PlayCard(heavyCard))
                    energy -= heavyCard.cost
                }
                if (energy >= 1) {
                    actions.add(AiAction.BasicAttack(baseDamage = 2700))
                }
            }

            EnemyBehaviorType.DEFENSIVE_RETALIATION -> {
                // Spartan Champion: raises shield first, then counter-strikes
                val shieldCard = deck.firstOrNull { it.effect.shield > 0 && it.cost <= energy }
                if (shieldCard != null && enemyHero.currentShield < 500) {
                    actions.add(AiAction.PlayCard(shieldCard))
                    energy -= shieldCard.cost
                }
                for (card in deck) {
                    if (card.cost <= energy && actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                        actions.add(AiAction.PlayCard(card))
                        energy -= card.cost
                        break
                    }
                }
                if (actions.isEmpty() && energy >= 1) {
                    actions.add(AiAction.BasicAttack(baseDamage = 2000))
                }
            }

            EnemyBehaviorType.EXECUTE_BURST -> {
                // Valkyrie: if player HP < 35%, strike with full power
                if (playerHero.hpPercentage < 0.35f) {
                    for (card in deck.sortedByDescending { it.effect.damage }) {
                        if (card.cost <= energy) {
                            actions.add(AiAction.PlayCard(card))
                            energy -= card.cost
                            if (actions.size >= 2) break
                        }
                    }
                    if (energy >= 1) {
                        actions.add(AiAction.BasicAttack(baseDamage = 2600))
                    }
                } else {
                    // Standard tactical
                    if (enemyHero.currentShield == 0 && energy >= 2) {
                        val defCard = deck.firstOrNull { it.effect.shield > 0 && it.cost <= energy }
                        if (defCard != null) {
                            actions.add(AiAction.PlayCard(defCard))
                            energy -= defCard.cost
                        }
                    }
                    for (card in deck) {
                        if (card.cost <= energy && actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                            actions.add(AiAction.PlayCard(card))
                            energy -= card.cost
                            if (actions.size >= 2) break
                        }
                    }
                    if (actions.isEmpty() && energy >= 1) {
                        actions.add(AiAction.BasicAttack(baseDamage = 1800))
                    }
                }
            }

            EnemyBehaviorType.CURSE_DRAIN -> {
                // Egyptian Guardian: curse and drain vitality
                val curseCard = deck.firstOrNull { it.effect.damageReductionPercent > 0 || it.effect.poisonTurns > 0 }
                if (curseCard != null && curseCard.cost <= energy) {
                    actions.add(AiAction.PlayCard(curseCard))
                    energy -= curseCard.cost
                }
                for (card in deck.filter { it.effect.healAmount > 0 || it.effect.damage > 0 }) {
                    if (card.cost <= energy && actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                        actions.add(AiAction.PlayCard(card))
                        energy -= card.cost
                        break
                    }
                }
                if (energy >= 1) {
                    actions.add(AiAction.BasicAttack(baseDamage = 1950))
                }
            }
        }

        return actions
    }
}
