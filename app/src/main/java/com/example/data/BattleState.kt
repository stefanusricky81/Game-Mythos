package com.example.data

enum class BattleTurn {
    PLAYER_TURN,
    ENEMY_TURN,
    VICTORY,
    DEFEAT
}

enum class LogType {
    INFO,
    ATTACK,
    DEFENSE,
    SPELL,
    PASSIVE,
    ULTIMATE,
    POISON
}

enum class NotificationType {
    ATTACK,
    DEFENSE,
    SPELL,
    CARD,
    ULTIMATE,
    STATUS,
    INFO
}

data class CombatNotification(
    val id: String,
    val text: String,
    val type: NotificationType
)

data class CombatLog(
    val id: String,
    val message: String,
    val type: LogType
)

data class FloatingCombatText(
    val id: String,
    val text: String,
    val isEnemyTarget: Boolean,
    val isPositive: Boolean = false,
    val isUltimate: Boolean = false,
    val isShield: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BattleStats(
    val turnsCount: Int = 1,
    val totalDamageDealt: Int = 0,
    val totalDamageTaken: Int = 0,
    val ultimateUses: Int = 0,
    val cardsPlayedCount: Int = 0
)

data class BattleRewards(
    val gold: Int = 750,
    val xp: Int = 1200,
    val cardRewardName: String = "Divine Aegis of Olympus",
    val cardRewardRarity: CardRarity = CardRarity.EPIC
)

data class BattleUiState(
    val playerHero: Hero = Hero.createHercules(),
    val enemyHero: Hero = Hero.createAres(),
    val playerEnergy: Int = 5,
    val maxPlayerEnergy: Int = 5,
    val playerMythPower: Int = 30,
    val maxMythPower: Int = 100,
    val enemyEnergy: Int = 5,
    val maxEnemyEnergy: Int = 5,
    val currentTurn: BattleTurn = BattleTurn.PLAYER_TURN,
    val playerHand: List<Card> = emptyList(),
    val playerDrawPile: List<Card> = emptyList(),
    val playerDiscardPile: List<Card> = emptyList(),
    val enemyHand: List<Card> = emptyList(),
    val combatLogs: List<CombatLog> = emptyList(),
    val floatingTexts: List<FloatingCombatText> = emptyList(),
    val stats: BattleStats = BattleStats(),
    val rewards: BattleRewards = BattleRewards(),
    val isExecutingTurn: Boolean = false,
    val isPlayerAttacking: Boolean = false,
    val isEnemyAttacking: Boolean = false,
    val isUltimateCinematicActive: Boolean = false,
    val isLastStandBannerActive: Boolean = false,
    val activePlayingCard: Card? = null,
    val isPlayingCardForPlayer: Boolean = true,
    val inspectedCard: Card? = null,
    val screenShakeTrigger: Int = 0,
    val userFeedbackMessage: String? = null,
    val compactEnergyWarning: String? = null,
    val shakingCardId: String? = null,
    val isEnergyHighlighted: Boolean = false,
    val activeNotification: CombatNotification? = null,
    val mythPowerGainNotification: Int? = null,
    val isDebugPanelOpen: Boolean = false
)
