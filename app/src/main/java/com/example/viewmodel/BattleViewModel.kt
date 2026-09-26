package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAction
import com.example.ai.EnemyAi
import com.example.audio.SoundManager
import com.example.combat.DamageEngine
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BattleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var soundManager: SoundManager? = null
    private var hasAwardedVictoryRewards: Boolean = false

    var activeEncounterConfig: BattleEncounterConfig? = null
        private set

    init {
        startNewBattle()
    }

    fun setSoundManager(manager: SoundManager) {
        this.soundManager = manager
    }

    /**
     * Completely resets and starts a fresh new battle using Active Deck (Phase 6C Requirements).
     * Validates active deck before starting; blocks combat and marks invalid state if criteria are unmet.
     */
    fun startNewBattle(
        customDeck: ActiveDeck? = null,
        encounterConfig: BattleEncounterConfig? = null
    ): Boolean {
        hasAwardedVictoryRewards = false
        activeEncounterConfig = encounterConfig
        val economy = PlayerEconomyRepository.instance.economyState.value
        val sourceDeck = customDeck ?: economy.activeDeck

        // Strictly isolate BattleDeck as an independent defensive copy (ROOT REQUIREMENT: Persistent ActiveDeck is READ ONLY)
        val activeDeck = sourceDeck.createDefensiveCopy()

        // Validate ActiveDeck using authoritative DeckValidator
        val validation = DeckValidator.validate(activeDeck, economy.ownedCardCounts)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    isDeckInvalid = true,
                    deckValidationResult = validation,
                    playerHand = emptyList(),
                    playerDrawPile = emptyList()
                )
            }
            return false
        }

        // Resolve Hero from HeroCatalog with current level progression (Phase 7C Section 14)
        val heroDef = HeroCatalog.findHero(activeDeck.heroId) ?: HeroCatalog.HERCULES
        val heroLevel = economy.heroProgression[activeDeck.heroId] ?: 1
        val playerHero = heroDef.toBattleHero(level = heroLevel)

        // Resolve Enemy Hero and Rewards from encounter configuration (Campaign integration)
        val enemyHero = encounterConfig?.enemyHero ?: Hero.createAres()
        val rewards = encounterConfig?.rewards ?: BattleRewards()
        val encounterTitle = encounterConfig?.encounterName ?: "Mount Olympus"

        // Resolve 20 Cards with player's upgraded cardLevels from CardCatalog (authoritative source)
        // Each card gets a unique instanceId to differentiate duplicate copies in hand & deck
        val fullDeck = activeDeck.cardIds.mapIndexed { index, cardId ->
            val level = economy.cardLevels[cardId] ?: 1
            CardCatalog.getCard(cardId, level).copy(
                instanceId = "${cardId}_${index}_${UUID.randomUUID()}"
            )
        }.shuffled()

        val initialHand = fullDeck.take(4).map { it.copy() }
        val drawPile = fullDeck.drop(4).map { it.copy() }

        _uiState.value = BattleUiState(
            playerHero = playerHero,
            enemyHero = enemyHero,
            playerEnergy = 5,
            maxPlayerEnergy = 5,
            playerMythPower = 30,
            maxMythPower = 100,
            enemyEnergy = 5,
            maxEnemyEnergy = 5,
            currentTurn = BattleTurn.PLAYER_TURN,
            playerHand = initialHand,
            playerDrawPile = drawPile,
            playerDiscardPile = emptyList(),
            enemyHand = DeckFactory.createEnemyDeck(),
            combatLogs = listOf(
                CombatLog(UUID.randomUUID().toString(), "Battle commences! ${playerHero.name} faces ${enemyHero.name} in $encounterTitle.", LogType.INFO)
            ),
            floatingTexts = emptyList(),
            stats = BattleStats(),
            rewards = rewards,
            isExecutingTurn = false,
            isPlayerAttacking = false,
            isEnemyAttacking = false,
            isUltimateCinematicActive = false,
            isLastStandBannerActive = false,
            activePlayingCard = null,
            inspectedCard = null,
            userFeedbackMessage = null,
            compactEnergyWarning = null,
            shakingCardId = null,
            isEnergyHighlighted = false,
            activeNotification = null,
            mythPowerGainNotification = null,
            isDebugPanelOpen = false,
            isDeckInvalid = false,
            deckValidationResult = null,
            campaignVictoryResult = null
        )
        return true
    }

    private fun triggerVictory() {
        if (!hasAwardedVictoryRewards) {
            hasAwardedVictoryRewards = true
            val config = activeEncounterConfig
            val stage = config?.stageId?.let { CampaignCatalog.findStage(it) }

            if (stage != null) {
                val state = _uiState.value
                val victoryResult = PlayerEconomyRepository.instance.recordCampaignVictory(
                    stage = stage,
                    playerRemainingHp = state.playerHero.currentHp,
                    playerMaxHp = state.playerHero.maxHp,
                    turnsCount = state.stats.turnsCount
                )
                _uiState.update {
                    it.copy(
                        campaignVictoryResult = victoryResult,
                        rewards = BattleRewards(
                            gold = victoryResult.goldAwarded,
                            xp = victoryResult.xpAwarded,
                            cardRewardName = victoryResult.cardNameAwarded ?: "",
                            cardRewardRarity = victoryResult.cardRarityAwarded ?: CardRarity.COMMON
                        )
                    )
                }
            } else {
                val rewardCard = config?.rewardCardId ?: "c_divine_aegis"
                PlayerEconomyRepository.instance.claimBattleVictoryRewards(_uiState.value.rewards, rewardCard)
            }
        }
        soundManager?.playVictorySound()
    }

    fun inspectCard(card: Card?) {
        _uiState.update { it.copy(inspectedCard = card) }
    }

    fun dismissLastStandBanner() {
        _uiState.update { it.copy(isLastStandBannerActive = false) }
    }

    fun dismissUserFeedback() {
        _uiState.update { it.copy(userFeedbackMessage = null, compactEnergyWarning = null) }
    }

    fun toggleDebugPanel() {
        _uiState.update { it.copy(isDebugPanelOpen = !it.isDebugPanelOpen) }
    }

    /**
     * Card Play with strict sequential feedback and compact notifications (Requirements #1, #4, #5)
     */
    fun playCard(card: Card) {
        val state = _uiState.value
        if (state.currentTurn != BattleTurn.PLAYER_TURN || state.isExecutingTurn || state.isDeckInvalid) {
            return
        }

        // Insufficient Energy handling (Requirement #4)
        if (state.playerEnergy < card.cost) {
            viewModelScope.launch {
                val needed = card.cost - state.playerEnergy
                _uiState.update {
                    it.copy(
                        compactEnergyWarning = "Need $needed Energy",
                        shakingCardId = card.id,
                        isEnergyHighlighted = true
                    )
                }
                delay(1000)
                _uiState.update {
                    it.copy(
                        compactEnergyWarning = null,
                        shakingCardId = null,
                        isEnergyHighlighted = false
                    )
                }
            }
            return
        }

        // Ensure card exists in hand (respecting individual card instances)
        val handIndex = state.playerHand.indexOfFirst {
            if (card.instanceId.isNotEmpty()) it.instanceId == card.instanceId else it === card || it == card
        }
        if (handIndex == -1) {
            return
        }

        viewModelScope.launch {
            // Step 1: Immediate deduction and remove ONLY the played card instance from hand (Requirement #9)
            val newHand = state.playerHand.toMutableList().apply { removeAt(handIndex) }
            val deductedEnergy = state.playerEnergy - card.cost
            val gainedMythPower = (state.playerMythPower + 12 + card.effect.mythPowerGain).coerceAtMost(state.maxMythPower)
            val mythGainDelta = 12 + card.effect.mythPowerGain

            _uiState.update {
                it.copy(
                    playerEnergy = deductedEnergy,
                    playerHand = newHand,
                    playerMythPower = gainedMythPower,
                    mythPowerGainNotification = mythGainDelta,
                    isExecutingTurn = true,
                    activePlayingCard = card,
                    isPlayingCardForPlayer = true,
                    activeNotification = CombatNotification(
                        id = UUID.randomUUID().toString(),
                        text = "Hercules → ${card.name}",
                        type = when (card.type) {
                            CardType.ATTACK -> NotificationType.ATTACK
                            CardType.DEFENSE -> NotificationType.DEFENSE
                            CardType.SPELL -> NotificationType.SPELL
                            else -> NotificationType.INFO
                        }
                    )
                )
            }

            soundManager?.playCardSound()
            delay(350)

            // Step 2: Resolve card effect sequentially
            executeCardEffects(card)

            delay(600)
            _uiState.update {
                it.copy(
                    activePlayingCard = null,
                    activeNotification = null,
                    mythPowerGainNotification = null,
                    isExecutingTurn = false
                )
            }
        }
    }

    private fun executeCardEffects(card: Card) {
        val curState = _uiState.value
        var pHero = curState.playerHero
        var eHero = curState.enemyHero
        val newLogs = curState.combatLogs.toMutableList()
        val newFloating = curState.floatingTexts.toMutableList()
        var notifText = ""
        var notifType = NotificationType.INFO

        // 1. Damage Effect
        if (card.effect.damage > 0) {
            val damageResult = DamageEngine.calculateAndApplyDamage(
                attacker = pHero,
                defender = eHero,
                baseDamage = card.effect.damage,
                isPhysical = true,
                bonusAgainstShield = card.effect.bonusDamageAgainstShield
            )
            eHero = damageResult.updatedDefender

            if (damageResult.shieldAbsorbed > 0) soundManager?.playShieldSound()
            if (damageResult.hpDamageDealt > 0) soundManager?.playAttackSound()

            val text = if (damageResult.shieldAbsorbed > 0 && damageResult.hpDamageDealt == 0) {
                "-${damageResult.shieldAbsorbed} Shield"
            } else {
                "-${damageResult.hpDamageDealt + damageResult.shieldAbsorbed}"
            }

            newFloating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    isEnemyTarget = true
                )
            )
            notifText = "Ares takes ${damageResult.modifiedDamage} damage"
            notifType = NotificationType.ATTACK
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Hercules plays ${card.name}, dealing ${damageResult.modifiedDamage} damage.", LogType.ATTACK))
        }

        // 2. Shield Effect (Olympian Guard)
        if (card.effect.shield > 0) {
            pHero = DamageEngine.applyShield(pHero, card.effect.shield)
            soundManager?.playShieldSound()
            newFloating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = "+${card.effect.shield} Shield",
                    isEnemyTarget = false,
                    isPositive = true,
                    isShield = true
                )
            )
            notifText = "Shield +${card.effect.shield}"
            notifType = NotificationType.DEFENSE
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Hercules casts ${card.name}, gaining +${card.effect.shield} Shield.", LogType.DEFENSE))
        }

        // 3. Attack Buff (Heroic Rage)
        if (card.effect.attackBuffPercent > 0) {
            pHero = pHero.copy(
                attackBuffPercent = pHero.attackBuffPercent + card.effect.attackBuffPercent,
                attackBuffTurns = card.effect.attackBuffTurns
            )
            notifText = "+${card.effect.attackBuffPercent}% Attack Boost"
            notifType = NotificationType.SPELL
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Hercules channels ${card.name}! +${card.effect.attackBuffPercent}% Attack.", LogType.SPELL))
        }

        // 4. Relic (Nemean Lion Hide)
        if (card.effect.damageReductionPercent > 0) {
            pHero = pHero.copy(
                damageReductionPercent = (pHero.damageReductionPercent + card.effect.damageReductionPercent).coerceAtMost(40)
            )
            notifText = "Nemean Hide: -${card.effect.damageReductionPercent}% Damage"
            notifType = NotificationType.INFO
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Nemean Lion Hide equipped (-${card.effect.damageReductionPercent}% damage taken).", LogType.PASSIVE))
        }

        // 5. Summon (Spartan Phalanx)
        if (card.effect.summonShield > 0) {
            pHero = DamageEngine.applyShield(pHero, card.effect.summonShield).copy(
                hasPhalanxGuard = true,
                phalanxCounterDamage = card.effect.summonCounterDamage
            )
            newFloating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = "+${card.effect.summonShield} Phalanx",
                    isEnemyTarget = false,
                    isPositive = true,
                    isShield = true
                )
            )
            notifText = "Spartan Phalanx: +${card.effect.summonShield} Shield & Counter"
            notifType = NotificationType.DEFENSE
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Spartan Phalanx assembled! +${card.effect.summonShield} Shield and 800 retaliation.", LogType.DEFENSE))
        }

        // 6. Heal & Cleanse (Nectar of Gods)
        if (card.effect.healAmount > 0) {
            val (healedHero, amount) = DamageEngine.applyHeal(pHero, card.effect.healAmount)
            pHero = healedHero.copy(poisonTurnsRemaining = 0, poisonDamagePerTurn = 0)
            newFloating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = "+$amount HP",
                    isEnemyTarget = false,
                    isPositive = true
                )
            )
            notifText = "Healed +$amount HP"
            notifType = NotificationType.SPELL
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Nectar of the Gods heals Hercules for $amount HP.", LogType.SPELL))
        }

        // 7. Poison (Hydra Venom Blade)
        if (card.effect.poisonTurns > 0) {
            eHero = eHero.copy(
                poisonTurnsRemaining = card.effect.poisonTurns,
                poisonDamagePerTurn = card.effect.poisonDamagePerTurn
            )
            notifText = "Ares Inflicted with Poison (${card.effect.poisonTurns} turns)"
            notifType = NotificationType.STATUS
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Ares poisoned for ${card.effect.poisonDamagePerTurn}/turn.", LogType.POISON))
        }

        // 8. Relic (Zeus Thunderstone)
        if (card.effect.turnStartLightningDamage > 0) {
            pHero = pHero.copy(hasThunderstoneRelic = true)
            notifText = "Zeus's Thunderstone Equipped"
            notifType = NotificationType.INFO
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Zeus's Thunderstone ready.", LogType.PASSIVE))
        }

        val newDiscard = curState.playerDiscardPile + card
        val totalDmgDealt = curState.stats.totalDamageDealt + card.effect.damage
        val isVictory = !eHero.isAlive

        _uiState.update {
            it.copy(
                playerHero = pHero,
                enemyHero = eHero,
                playerDiscardPile = newDiscard,
                combatLogs = newLogs,
                floatingTexts = newFloating,
                activeNotification = if (notifText.isNotEmpty()) CombatNotification(UUID.randomUUID().toString(), notifText, notifType) else it.activeNotification,
                stats = it.stats.copy(
                    totalDamageDealt = totalDmgDealt,
                    cardsPlayedCount = it.stats.cardsPlayedCount + 1
                ),
                currentTurn = if (isVictory) BattleTurn.VICTORY else it.currentTurn
            )
        }

        if (isVictory) {
            triggerVictory()
        }
    }

    /**
     * Hero Basic Attack (Requirements #1, #2, #3)
     */
    fun performHeroAttack() {
        val state = _uiState.value
        if (state.currentTurn != BattleTurn.PLAYER_TURN || state.isExecutingTurn) {
            return
        }

        if (state.playerEnergy < 1) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        compactEnergyWarning = "Need 1 Energy",
                        isEnergyHighlighted = true
                    )
                }
                delay(1000)
                _uiState.update {
                    it.copy(
                        compactEnergyWarning = null,
                        isEnergyHighlighted = false
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            val deductedEnergy = state.playerEnergy - 1
            val gainedMythPower = (state.playerMythPower + 15).coerceAtMost(state.maxMythPower)

            _uiState.update {
                it.copy(
                    playerEnergy = deductedEnergy,
                    playerMythPower = gainedMythPower,
                    mythPowerGainNotification = 15,
                    isPlayerAttacking = true,
                    isExecutingTurn = true,
                    screenShakeTrigger = it.screenShakeTrigger + 1,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "Hercules → War Club Strike", NotificationType.ATTACK)
                )
            }

            soundManager?.playAttackSound()
            delay(350)

            val curState = _uiState.value
            val damageResult = DamageEngine.calculateAndApplyDamage(
                attacker = curState.playerHero,
                defender = curState.enemyHero,
                baseDamage = curState.playerHero.baseAttack,
                isPhysical = true
            )

            val updatedEnemy = damageResult.updatedDefender
            val newLogs = curState.combatLogs.toMutableList()
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Hercules strikes Ares for ${damageResult.modifiedDamage} damage.", LogType.ATTACK))

            val newFloating = curState.floatingTexts.toMutableList()
            newFloating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = "-${damageResult.hpDamageDealt + damageResult.shieldAbsorbed}",
                    isEnemyTarget = true
                )
            )

            val isVictory = !updatedEnemy.isAlive

            _uiState.update {
                it.copy(
                    enemyHero = updatedEnemy,
                    combatLogs = newLogs,
                    floatingTexts = newFloating,
                    isPlayerAttacking = false,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "Ares takes ${damageResult.modifiedDamage} damage", NotificationType.ATTACK),
                    stats = it.stats.copy(
                        totalDamageDealt = it.stats.totalDamageDealt + damageResult.modifiedDamage
                    ),
                    currentTurn = if (isVictory) BattleTurn.VICTORY else it.currentTurn
                )
            }

            if (isVictory) {
                triggerVictory()
            }

            delay(600)
            _uiState.update {
                it.copy(
                    activeNotification = null,
                    mythPowerGainNotification = null,
                    isExecutingTurn = false
                )
            }
        }
    }

    /**
     * Hercules Ultimate: TWELVE LABORS (Requirements #1, #7)
     */
    fun activateTwelveLaborsUltimate() {
        val state = _uiState.value
        if (state.currentTurn != BattleTurn.PLAYER_TURN || state.isExecutingTurn) {
            return
        }

        if (state.playerMythPower < state.maxMythPower) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    playerMythPower = 0,
                    isExecutingTurn = true,
                    isUltimateCinematicActive = true,
                    screenShakeTrigger = it.screenShakeTrigger + 1,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "⚡ TWELVE LABORS UNLEASHED ⚡", NotificationType.ULTIMATE)
                )
            }

            soundManager?.playUltimateSound()
            delay(1400)

            val curState = _uiState.value
            val damageResult = DamageEngine.calculateAndApplyDamage(
                attacker = curState.playerHero,
                defender = curState.enemyHero,
                baseDamage = 4500,
                isPhysical = false
            )

            val updatedEnemy = damageResult.updatedDefender
            val newLogs = curState.combatLogs.toMutableList()
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "TWELVE LABORS! Devastating blow deals ${damageResult.modifiedDamage} damage to Ares.", LogType.ULTIMATE))

            val newFloating = curState.floatingTexts.toMutableList()
            newFloating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = "CRITICAL -${damageResult.hpDamageDealt + damageResult.shieldAbsorbed}",
                    isEnemyTarget = true,
                    isUltimate = true
                )
            )

            val isVictory = !updatedEnemy.isAlive

            _uiState.update {
                it.copy(
                    enemyHero = updatedEnemy,
                    combatLogs = newLogs,
                    floatingTexts = newFloating,
                    isUltimateCinematicActive = false,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "Twelve Labors deals ${damageResult.modifiedDamage} damage!", NotificationType.ULTIMATE),
                    stats = it.stats.copy(
                        totalDamageDealt = it.stats.totalDamageDealt + damageResult.modifiedDamage,
                        ultimateUses = it.stats.ultimateUses + 1
                    ),
                    currentTurn = if (isVictory) BattleTurn.VICTORY else it.currentTurn
                )
            }

            if (isVictory) {
                triggerVictory()
            }

            delay(700)
            _uiState.update {
                it.copy(
                    activeNotification = null,
                    isExecutingTurn = false
                )
            }
        }
    }

    /**
     * Sequential Turn Progression (Requirements #8, #9):
     * Player Turn Ends -> Enemy Turn Begins -> Poison & Status resolves with floating text ->
     * AI actions sequentially -> Enemy Turn Ends -> Player Turn Begins -> Resources Refresh -> Cards Draw
     */
    fun endTurn() {
        val state = _uiState.value
        if (state.currentTurn != BattleTurn.PLAYER_TURN || state.isExecutingTurn) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentTurn = BattleTurn.ENEMY_TURN,
                    isExecutingTurn = true,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "ENEMY TURN", NotificationType.INFO)
                )
            }

            delay(500)

            // Step 1: Start-of-Enemy-Turn Relics & Poison (Requirement #8: Poison activates -> Damage number -> HP updates -> Counter updates)
            var preAiState = _uiState.value
            var eHero = preAiState.enemyHero
            var pHero = preAiState.playerHero
            var pMythPower = preAiState.playerMythPower
            val logs = preAiState.combatLogs.toMutableList()
            val floating = preAiState.floatingTexts.toMutableList()

            // Thunderstone Relic
            if (pHero.hasThunderstoneRelic) {
                val thunderResult = DamageEngine.calculateAndApplyDamage(null, eHero, 1000)
                eHero = thunderResult.updatedDefender
                pMythPower = (pMythPower + 10).coerceAtMost(preAiState.maxMythPower)
                floating.add(FloatingCombatText(UUID.randomUUID().toString(), "-1000 Lightning", isEnemyTarget = true))
                _uiState.update {
                    it.copy(
                        enemyHero = eHero,
                        playerMythPower = pMythPower,
                        floatingTexts = floating,
                        activeNotification = CombatNotification(UUID.randomUUID().toString(), "Zeus's Thunderstone: -1,000 Lightning", NotificationType.SPELL)
                    )
                }
                delay(600)
                if (!eHero.isAlive) {
                    _uiState.update { it.copy(currentTurn = BattleTurn.VICTORY, isExecutingTurn = false, activeNotification = null) }
                    triggerVictory()
                    return@launch
                }
            }

            // Poison Resolution (Requirement #8)
            if (eHero.poisonTurnsRemaining > 0) {
                val poisonDmg = eHero.poisonDamagePerTurn
                val poisonResult = DamageEngine.calculateAndApplyDamage(null, eHero, poisonDmg)
                val remainingTurns = eHero.poisonTurnsRemaining - 1
                eHero = poisonResult.updatedDefender.copy(poisonTurnsRemaining = remainingTurns)

                floating.add(FloatingCombatText(UUID.randomUUID().toString(), "-$poisonDmg Poison", isEnemyTarget = true))
                _uiState.update {
                    it.copy(
                        enemyHero = eHero,
                        floatingTexts = floating,
                        activeNotification = CombatNotification(UUID.randomUUID().toString(), "Poison Tick: Ares takes $poisonDmg damage", NotificationType.STATUS)
                    )
                }
                delay(700)
                if (!eHero.isAlive) {
                    _uiState.update { it.copy(currentTurn = BattleTurn.VICTORY, isExecutingTurn = false, activeNotification = null) }
                    triggerVictory()
                    return@launch
                }
            }

            // Step 2: AI Actions sequentially (Requirement #9)
            runSequentialAiTurn()
        }
    }

    private suspend fun runSequentialAiTurn() {
        val currentState = _uiState.value
        val actions = EnemyAi.decideTurnActions(
            enemyHero = currentState.enemyHero,
            playerHero = currentState.playerHero,
            availableEnergy = currentState.enemyEnergy
        )

        for (action in actions) {
            when (action) {
                is AiAction.PlayCard -> {
                    val card = action.card
                    _uiState.update {
                        it.copy(
                            activeNotification = CombatNotification(UUID.randomUUID().toString(), "Ares → ${card.name}", NotificationType.CARD)
                        )
                    }
                    soundManager?.playCardSound()
                    delay(450)

                    resolveEnemyCardPlay(card)
                    delay(550)
                }
                is AiAction.BasicAttack -> {
                    _uiState.update {
                        it.copy(
                            isEnemyAttacking = true,
                            activeNotification = CombatNotification(UUID.randomUUID().toString(), "Ares attacks Hercules directly", NotificationType.ATTACK)
                        )
                    }
                    soundManager?.playAttackSound()
                    delay(400)

                    resolveEnemyBasicAttack(action.baseDamage)
                    delay(500)
                    _uiState.update { it.copy(isEnemyAttacking = false) }
                }
            }

            if (!_uiState.value.playerHero.isAlive) {
                break
            }
        }

        delay(400)
        finalizeTurnTransition()
    }

    private fun resolveEnemyCardPlay(card: Card) {
        val curState = _uiState.value
        var pHero = curState.playerHero
        var eHero = curState.enemyHero
        val logs = curState.combatLogs.toMutableList()
        val floating = curState.floatingTexts.toMutableList()

        if (card.effect.shield > 0) {
            eHero = DamageEngine.applyShield(eHero, card.effect.shield)
            floating.add(FloatingCombatText(UUID.randomUUID().toString(), "+${card.effect.shield} Shield", isEnemyTarget = true, isPositive = true, isShield = true))
            _uiState.update {
                it.copy(
                    enemyHero = eHero,
                    floatingTexts = floating,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "Ares gains +${card.effect.shield} Shield", NotificationType.DEFENSE)
                )
            }
        }

        if (card.effect.damage > 0) {
            val damageResult = DamageEngine.calculateAndApplyDamage(
                attacker = eHero,
                defender = pHero,
                baseDamage = card.effect.damage
            )
            pHero = damageResult.updatedDefender

            if (damageResult.shieldAbsorbed > 0) soundManager?.playShieldSound()
            if (damageResult.hpDamageDealt > 0) soundManager?.playAttackSound()

            floating.add(
                FloatingCombatText(
                    id = UUID.randomUUID().toString(),
                    text = "-${damageResult.hpDamageDealt + damageResult.shieldAbsorbed}",
                    isEnemyTarget = false
                )
            )

            // Phalanx Counter
            if (damageResult.counterDamageToAttacker > 0) {
                eHero = DamageEngine.calculateAndApplyDamage(null, eHero, damageResult.counterDamageToAttacker).updatedDefender
                floating.add(FloatingCombatText(UUID.randomUUID().toString(), "-${damageResult.counterDamageToAttacker} Counter", isEnemyTarget = true))
            }

            // Hercules Myth Power Gain on taking damage (+15 MP)
            val updatedMyth = (curState.playerMythPower + 15).coerceAtMost(curState.maxMythPower)

            _uiState.update {
                it.copy(
                    playerHero = pHero,
                    enemyHero = eHero,
                    playerMythPower = updatedMyth,
                    mythPowerGainNotification = 15,
                    floatingTexts = floating,
                    isLastStandBannerActive = damageResult.didTriggerLastStand || it.isLastStandBannerActive,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "Hercules takes ${damageResult.modifiedDamage} damage", NotificationType.ATTACK),
                    stats = it.stats.copy(totalDamageTaken = it.stats.totalDamageTaken + damageResult.modifiedDamage)
                )
            }
        }
    }

    private fun resolveEnemyBasicAttack(baseDamage: Int) {
        val curState = _uiState.value
        val damageResult = DamageEngine.calculateAndApplyDamage(
            attacker = curState.enemyHero,
            defender = curState.playerHero,
            baseDamage = baseDamage
        )

        var pHero = damageResult.updatedDefender
        var eHero = curState.enemyHero
        val floating = curState.floatingTexts.toMutableList()

        floating.add(
            FloatingCombatText(
                id = UUID.randomUUID().toString(),
                text = "-${damageResult.hpDamageDealt + damageResult.shieldAbsorbed}",
                isEnemyTarget = false
            )
        )

        if (damageResult.counterDamageToAttacker > 0) {
            eHero = DamageEngine.calculateAndApplyDamage(null, eHero, damageResult.counterDamageToAttacker).updatedDefender
            floating.add(FloatingCombatText(UUID.randomUUID().toString(), "-${damageResult.counterDamageToAttacker} Counter", isEnemyTarget = true))
        }

        val updatedMyth = (curState.playerMythPower + 15).coerceAtMost(curState.maxMythPower)

        _uiState.update {
            it.copy(
                playerHero = pHero,
                enemyHero = eHero,
                playerMythPower = updatedMyth,
                mythPowerGainNotification = 15,
                floatingTexts = floating,
                isLastStandBannerActive = damageResult.didTriggerLastStand || it.isLastStandBannerActive,
                activeNotification = CombatNotification(UUID.randomUUID().toString(), "Ares strikes for ${damageResult.modifiedDamage} damage", NotificationType.ATTACK),
                stats = it.stats.copy(totalDamageTaken = it.stats.totalDamageTaken + damageResult.modifiedDamage)
            )
        }
    }

    private fun finalizeTurnTransition() {
        val state = _uiState.value
        if (!state.playerHero.isAlive) {
            _uiState.update {
                it.copy(
                    currentTurn = BattleTurn.DEFEAT,
                    isExecutingTurn = false,
                    activeNotification = null
                )
            }
            soundManager?.playDefeatSound()
            return
        }

        // Draw new cards for player up to 4
        var drawPile = state.playerDrawPile.toMutableList()
        var discardPile = state.playerDiscardPile.toMutableList()
        val currentHand = state.playerHand.toMutableList()

        while (currentHand.size < 4) {
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) break
                drawPile.addAll(discardPile.shuffled())
                discardPile.clear()
            }
            if (drawPile.isNotEmpty()) {
                currentHand.add(drawPile.removeAt(0))
            }
        }

        // Energy increment rule: +1 Max Energy up to 10, refreshed to max
        val nextMaxEnergy = (state.maxPlayerEnergy + 1).coerceAtMost(10)
        // Myth power passive gain +10
        val nextMythPower = (state.playerMythPower + 10).coerceAtMost(state.maxMythPower)

        // Decrement attack buffs
        var pHero = state.playerHero
        if (pHero.attackBuffTurns > 0) {
            val remainingTurns = pHero.attackBuffTurns - 1
            pHero = pHero.copy(
                attackBuffTurns = remainingTurns,
                attackBuffPercent = if (remainingTurns == 0) 0 else pHero.attackBuffPercent
            )
        }

        val logs = state.combatLogs.toMutableList()
        logs.add(0, CombatLog(UUID.randomUUID().toString(), "Turn ${state.stats.turnsCount + 1}: Player Turn. Energy restored to $nextMaxEnergy.", LogType.INFO))

        _uiState.update {
            it.copy(
                playerHero = pHero,
                playerEnergy = nextMaxEnergy,
                maxPlayerEnergy = nextMaxEnergy,
                playerMythPower = nextMythPower,
                playerHand = currentHand,
                playerDrawPile = drawPile,
                playerDiscardPile = discardPile,
                currentTurn = BattleTurn.PLAYER_TURN,
                isExecutingTurn = false,
                activeNotification = CombatNotification(UUID.randomUUID().toString(), "PLAYER TURN • $nextMaxEnergy ENERGY", NotificationType.INFO),
                combatLogs = logs,
                stats = it.stats.copy(turnsCount = it.stats.turnsCount + 1)
            )
        }

        viewModelScope.launch {
            delay(1000)
            _uiState.update {
                it.copy(
                    activeNotification = null,
                    mythPowerGainNotification = null
                )
            }
        }
    }

    // ==========================================
    // DEVELOPMENT-ONLY DEBUG PANEL ACTIONS
    // ==========================================

    fun debugAddEnergy(amount: Int = 5) {
        _uiState.update {
            val newEnergy = (it.playerEnergy + amount).coerceAtMost(it.maxPlayerEnergy)
            it.copy(playerEnergy = newEnergy)
        }
    }

    fun debugAddMythPower(amount: Int = 50) {
        _uiState.update {
            val newMyth = (it.playerMythPower + amount).coerceAtMost(it.maxMythPower)
            it.copy(playerMythPower = newMyth, mythPowerGainNotification = amount)
        }
    }

    fun debugDamageEnemy(amount: Int = 3000) {
        val curState = _uiState.value
        val result = DamageEngine.calculateAndApplyDamage(null, curState.enemyHero, amount)
        val isVictory = !result.updatedDefender.isAlive
        _uiState.update {
            it.copy(
                enemyHero = result.updatedDefender,
                currentTurn = if (isVictory) BattleTurn.VICTORY else it.currentTurn,
                activeNotification = CombatNotification(UUID.randomUUID().toString(), "Debug: Ares takes $amount damage", NotificationType.ATTACK)
            )
        }
        if (isVictory) triggerVictory()
    }

    fun debugSetHerculesLowHp(targetHp: Int = 2500) {
        _uiState.update {
            val updated = it.playerHero.copy(
                currentHp = targetHp,
                isLastStandActive = true,
                hasLastStandTriggered = true
            )
            it.copy(
                playerHero = updated,
                isLastStandBannerActive = true,
                activeNotification = CombatNotification(UUID.randomUUID().toString(), "Debug: Last Stand Activated!", NotificationType.STATUS)
            )
        }
    }

    fun debugHealHercules(amount: Int = 3000) {
        _uiState.update {
            val (healed, actual) = DamageEngine.applyHeal(it.playerHero, amount)
            it.copy(
                playerHero = healed,
                activeNotification = CombatNotification(UUID.randomUUID().toString(), "Debug: Healed Hercules +$actual HP", NotificationType.SPELL)
            )
        }
    }

    fun debugForceVictory() {
        _uiState.update {
            it.copy(
                enemyHero = it.enemyHero.copy(currentHp = 0),
                currentTurn = BattleTurn.VICTORY,
                activeNotification = null
            )
        }
        triggerVictory()
    }

    fun debugForceDefeat() {
        _uiState.update {
            it.copy(
                playerHero = it.playerHero.copy(currentHp = 0),
                currentTurn = BattleTurn.DEFEAT,
                activeNotification = null
            )
        }
        soundManager?.playDefeatSound()
    }
}
