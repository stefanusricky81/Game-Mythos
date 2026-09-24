package com.example.ai

import com.example.data.Card
import com.example.data.DeckFactory
import com.example.data.Hero

sealed class AiAction {
    data class PlayCard(val card: Card) : AiAction()
    data class BasicAttack(val baseDamage: Int = 1800) : AiAction()
}

object EnemyAi {

    /**
     * Decides the sequence of actions for the enemy AI on its turn
     * Based on current AI energy, AI health, and available cards.
     */
    fun decideTurnActions(
        enemyHero: Hero,
        playerHero: Hero,
        availableEnergy: Int
    ): List<AiAction> {
        val actions = mutableListOf<AiAction>()
        var energy = availableEnergy
        val deck = DeckFactory.createEnemyDeck().shuffled()

        // 1. If enemy health is under 50% or has no shield, prioritize defense
        if ((enemyHero.currentHp < enemyHero.maxHp * 0.5 || enemyHero.currentShield == 0) && energy >= 2) {
            val defCard = deck.firstOrNull { it.effect.shield > 0 && it.cost <= energy }
            if (defCard != null) {
                actions.add(AiAction.PlayCard(defCard))
                energy -= defCard.cost
            }
        }

        // 2. Play offensive or spell cards with remaining energy
        for (card in deck) {
            if (card.cost <= energy && actions.none { it is AiAction.PlayCard && it.card.id == card.id }) {
                actions.add(AiAction.PlayCard(card))
                energy -= card.cost
                if (actions.size >= 2) break
            }
        }

        // 3. If no cards were played and has at least 1 energy, execute a basic attack
        if (actions.isEmpty() && energy >= 1) {
            actions.add(AiAction.BasicAttack())
        }

        return actions
    }
}
