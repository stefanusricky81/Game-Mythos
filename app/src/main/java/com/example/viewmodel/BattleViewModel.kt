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

        val deckAnalysis = SynergyCatalog.analyzeDeck(fullDeck, heroDef)
        val activeSynergy = deckAnalysis.activeSynergies.firstOrNull()?.let { "${it.iconSymbol} ${it.name} Active" }

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
            enemyHand = if (encounterConfig?.isArenaMatch == true && encounterConfig.arenaOpponent != null) {
                encounterConfig.arenaOpponent.deckCardIds.map { CardCatalog.getCard(it) }
            } else {
                DeckFactory.createEnemyDeck()
            },
            activeHeroSynergyText = activeSynergy,
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
            } else if (config?.isWorldBoss == true) {
                val state = _uiState.value
                val bossId = config.worldBossId ?: "world_boss_kronos"
                PlayerEconomyRepository.instance.recordWorldBossBattle(
                    bossId = bossId,
                    damageDealt = state.stats.totalDamageDealt.toLong(),
                    isVictory = true
                )
                PlayerEconomyRepository.instance.recordBattleFinished(isVictory = true, stats = state.stats, isCampaign = false)
                PlayerEconomyRepository.instance.addPlayerXp(_uiState.value.rewards.xp)
                PlayerEconomyRepository.instance.addGold(_uiState.value.rewards.gold)
            } else if (config?.isRaid == true) {
                val state = _uiState.value
                val raidId = config.raidId ?: "raid_olympus"
                PlayerEconomyRepository.instance.recordRaidStageClear(
                    raidId = raidId,
                    stageNumber = config.stageNumber,
                    goldReward = _uiState.value.rewards.gold,
                    tokenReward = config.eventTokensReward
                )
                PlayerEconomyRepository.instance.recordBattleFinished(isVictory = true, stats = state.stats, isCampaign = false)
                PlayerEconomyRepository.instance.addPlayerXp(_uiState.value.rewards.xp)
            } else if (config?.isTrial == true) {
                val state = _uiState.value
                val trialId = config.trialStageId ?: "trial_olympus"
                val hpCond = state.playerHero.currentHp.toFloat() / state.playerHero.maxHp >= 0.5f
                val turnsCond = state.stats.turnsCount <= 8
                val stars = 1 + (if (hpCond) 1 else 0) + (if (turnsCond) 1 else 0)
                PlayerEconomyRepository.instance.recordTrialClear(
                    stageId = trialId,
                    difficulty = config.endgameDifficulty,
                    stars = stars,
                    goldReward = _uiState.value.rewards.gold,
                    xpReward = _uiState.value.rewards.xp,
                    tokenReward = config.eventTokensReward
                )
                PlayerEconomyRepository.instance.recordBattleFinished(isVictory = true, stats = state.stats, isCampaign = false)
            } else if (config?.isArenaMatch == true) {
                val state = _uiState.value
                val opponent = config.arenaOpponent ?: ArenaCatalog.OPPONENT_POOL.first()
                val arenaSummary = PlayerEconomyRepository.instance.recordArenaBattleFinished(
                    opponent = opponent,
                    isVictory = true,
                    stats = state.stats
                )
                _uiState.update {
                    it.copy(
                        arenaBattleResultSummary = arenaSummary,
                        rewards = BattleRewards(
                            gold = arenaSummary.goldAwarded,
                            xp = arenaSummary.xpAwarded,
                            cardRewardName = "Card Shards (${arenaSummary.cardShardsAwarded}x)",
                            cardRewardRarity = CardRarity.RARE
                        )
                    )
                }
            } else {
                val rewardCard = config?.rewardCardId ?: "c_divine_aegis"
                PlayerEconomyRepository.instance.claimBattleVictoryRewards(_uiState.value.rewards, rewardCard)
                val state = _uiState.value
                PlayerEconomyRepository.instance.recordBattleFinished(isVictory = true, stats = state.stats, isCampaign = false)
                PlayerEconomyRepository.instance.addPlayerXp(_uiState.value.rewards.xp)
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
        PlayerEconomyRepository.instance.onCardPlayed()
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
            PlayerEconomyRepository.instance.onDamageDealt(damageResult.modifiedDamage)

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
            eHero = DamageEngine.applyStatusEffect(
                eHero,
                com.example.combat.StatusEffectCatalog.createPoison(card.effect.poisonTurns, card.effect.poisonDamagePerTurn)
            )
            notifText = "${eHero.name} Inflicted with Poison (${card.effect.poisonTurns} turns)"
            notifType = NotificationType.STATUS
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${eHero.name} poisoned for ${card.effect.poisonDamagePerTurn}/turn.", LogType.POISON))
        }

        // 8. Burn / Shock
        if (card.effect.burnTurns > 0) {
            eHero = DamageEngine.applyStatusEffect(
                eHero,
                com.example.combat.StatusEffectCatalog.createBurn(card.effect.burnTurns, card.effect.burnDamagePerTurn)
            )
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "BURN ×${card.effect.burnTurns}", isEnemyTarget = true))
            notifText = "${eHero.name} Inflicted with Burn (${card.effect.burnTurns} turns)"
            notifType = NotificationType.STATUS
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${eHero.name} burns for ${card.effect.burnDamagePerTurn}/turn.", LogType.SPELL))
        }

        // 9. Stun
        if (card.effect.stunTurns > 0) {
            eHero = DamageEngine.applyStatusEffect(
                eHero,
                com.example.combat.StatusEffectCatalog.createStun(card.effect.stunTurns)
            )
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "STUNNED!", isEnemyTarget = true))
            notifText = "${eHero.name} STUNNED (${card.effect.stunTurns} turn)"
            notifType = NotificationType.STATUS
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${eHero.name} is stunned for ${card.effect.stunTurns} turn.", LogType.STATUS))
        }

        // 10. Vulnerable
        if (card.effect.vulnerableTurns > 0) {
            eHero = DamageEngine.applyStatusEffect(
                eHero,
                com.example.combat.StatusEffectCatalog.createVulnerable(card.effect.vulnerableTurns)
            )
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "VULNERABLE +30%", isEnemyTarget = true))
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${eHero.name} is vulnerable (+30% damage taken).", LogType.STATUS))
        }

        // 11. Weaken
        if (card.effect.weakenTurns > 0) {
            eHero = DamageEngine.applyStatusEffect(
                eHero,
                com.example.combat.StatusEffectCatalog.createWeaken(card.effect.weakenTurns)
            )
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "WEAKEN -25%", isEnemyTarget = true))
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${eHero.name} is weakened (-25% damage dealt).", LogType.STATUS))
        }

        // 12. Regeneration
        if (card.effect.regenerationTurns > 0) {
            pHero = DamageEngine.applyStatusEffect(
                pHero,
                com.example.combat.StatusEffectCatalog.createRegeneration(card.effect.regenerationTurns, card.effect.regenerationAmount)
            )
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "+${card.effect.regenerationAmount} Regen/t", isEnemyTarget = false, isPositive = true))
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${pHero.name} gains ${card.effect.regenerationAmount} HP regeneration.", LogType.SPELL))
        }

        // 13. Cleanse
        if (card.effect.isCleanse) {
            pHero = DamageEngine.cleanseDebuffs(pHero)
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "CLEANSED", isEnemyTarget = false, isPositive = true))
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "${pHero.name} cleansed all debuffs!", LogType.SPELL))
        }

        // 14. True Damage (ignores shield)
        if (card.effect.trueDamage > 0) {
            val afterTrueHp = (eHero.currentHp - card.effect.trueDamage).coerceAtLeast(0)
            eHero = eHero.copy(currentHp = afterTrueHp)
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "-${card.effect.trueDamage} True Dmg", isEnemyTarget = true, damageType = "TRUE"))
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "True damage pierces armor for ${card.effect.trueDamage} HP.", LogType.ATTACK))
        }

        // 15. Lifesteal
        if (card.effect.lifestealPercent > 0 && card.effect.damage > 0) {
            val lifesteal = ((card.effect.damage * card.effect.lifestealPercent) / 100).coerceAtLeast(0)
            val (healed, _) = DamageEngine.applyHeal(pHero, lifesteal)
            pHero = healed
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "+$lifesteal Drain", isEnemyTarget = false, isPositive = true))
        }

        // 16. Execute below threshold
        if (card.effect.executeThresholdPercent > 0) {
            if (eHero.hpPercentage <= (card.effect.executeThresholdPercent / 100f)) {
                eHero = eHero.copy(currentHp = 0)
                newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "EXECUTED!", isEnemyTarget = true, isUltimate = true))
                newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Soul Weighing: Enemy executed by divine decree!", LogType.ULTIMATE))
            }
        }

        // 17. Energy Gain
        var pEnergy = curState.playerEnergy
        if (card.effect.energyGain > 0) {
            pEnergy = (pEnergy + card.effect.energyGain).coerceAtMost(curState.maxPlayerEnergy)
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "+${card.effect.energyGain} Energy", isEnemyTarget = false, isPositive = true))
        }

        // 18. Draw Cards
        var pHand = curState.playerHand
        var pDraw = curState.playerDrawPile
        var pDiscard = curState.playerDiscardPile
        if (card.effect.drawCardsCount > 0) {
            val handMutable = pHand.toMutableList()
            val drawMutable = pDraw.toMutableList()
            val discardMutable = pDiscard.toMutableList()
            repeat(card.effect.drawCardsCount) {
                if (drawMutable.isEmpty() && discardMutable.isNotEmpty()) {
                    drawMutable.addAll(discardMutable.shuffled())
                    discardMutable.clear()
                }
                if (drawMutable.isNotEmpty()) {
                    handMutable.add(drawMutable.removeAt(0))
                }
            }
            pHand = handMutable
            pDraw = drawMutable
            pDiscard = discardMutable
        }

        // 19. Relic (Zeus Thunderstone)
        if (card.effect.turnStartLightningDamage > 0) {
            pHero = pHero.copy(hasThunderstoneRelic = true)
            notifText = "Zeus's Thunderstone Equipped"
            notifType = NotificationType.INFO
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "Zeus's Thunderstone ready.", LogType.PASSIVE))
        }

        // Boss Phase 2 Transition Check
        var isBossPhase2 = curState.isBossPhase2Active
        var bossBanner = curState.bossPhaseBannerText
        if (activeEncounterConfig?.isBoss == true && eHero.hpPercentage <= 0.50f && !isBossPhase2) {
            isBossPhase2 = true
            val bossDef = BossCatalog.findBoss(eHero.id)
            bossBanner = bossDef?.phases?.getOrNull(1)?.bannerText ?: "PHASE 2: BOSS ENRAGED!"
            newFloating.add(FloatingCombatText(UUID.randomUUID().toString(), "PHASE 2 TRIGGERED!", isEnemyTarget = true, isUltimate = true))
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "⚠️ $bossBanner ⚠️", LogType.ULTIMATE))
        }

        val newDiscard = pDiscard + card
        val totalDmgDealt = curState.stats.totalDamageDealt + card.effect.damage + card.effect.trueDamage
        val isVictory = !eHero.isAlive

        _uiState.update {
            it.copy(
                playerHero = pHero,
                enemyHero = eHero,
                playerEnergy = pEnergy,
                playerHand = pHand,
                playerDrawPile = pDraw,
                playerDiscardPile = newDiscard,
                combatLogs = newLogs,
                floatingTexts = newFloating,
                isBossPhase2Active = isBossPhase2,
                bossPhaseBannerText = bossBanner,
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
            PlayerEconomyRepository.instance.onDamageDealt(damageResult.modifiedDamage)
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
     * Hero Ultimate Ability (Phase 9 Requirement #1, #7).
     * Differentiates abilities across all mythology champions.
     */
    fun activateTwelveLaborsUltimate() {
        activateHeroUltimate()
    }

    fun activateHeroUltimate() {
        val state = _uiState.value
        if (state.currentTurn != BattleTurn.PLAYER_TURN || state.isExecutingTurn) {
            return
        }

        if (state.playerMythPower < state.maxMythPower) {
            return
        }

        val heroId = state.playerHero.id.lowercase()
        val heroDef = HeroCatalog.findHero(state.playerHero.id) ?: HeroCatalog.HERCULES
        val ultName = heroDef.ultimateName

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    playerMythPower = 0,
                    isExecutingTurn = true,
                    isUltimateCinematicActive = true,
                    screenShakeTrigger = it.screenShakeTrigger + 1,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "⚡ $ultName ⚡", NotificationType.ULTIMATE)
                )
            }

            soundManager?.playUltimateSound()
            delay(1400)

            val curState = _uiState.value
            var pHero = curState.playerHero
            var eHero = curState.enemyHero
            val newFloating = curState.floatingTexts.toMutableList()
            val newLogs = curState.combatLogs.toMutableList()

            var baseDmg = 4500
            when {
                heroId.contains("zeus") -> {
                    baseDmg = 5000
                    eHero = DamageEngine.applyStatusEffect(eHero, com.example.combat.StatusEffectCatalog.createStun(1))
                }
                heroId.contains("athena") -> {
                    baseDmg = 2500
                    pHero = DamageEngine.applyShield(pHero, 3500)
                }
                heroId.contains("ares") -> {
                    baseDmg = 4600
                    val heal = (baseDmg * 0.15).toInt()
                    pHero = DamageEngine.applyHeal(pHero, heal).first
                }
                heroId.contains("medusa") -> {
                    baseDmg = 3800
                    eHero = DamageEngine.applyStatusEffect(eHero, com.example.combat.StatusEffectCatalog.createStun(1))
                }
                heroId.contains("hades") -> {
                    baseDmg = 4400
                    pHero = DamageEngine.applyShield(pHero, 2000)
                }
                heroId.contains("thor") -> {
                    baseDmg = 4800
                }
                heroId.contains("loki") -> {
                    baseDmg = 3600
                    eHero = DamageEngine.applyStatusEffect(eHero, com.example.combat.StatusEffectCatalog.createWeaken(2))
                    eHero = DamageEngine.applyStatusEffect(eHero, com.example.combat.StatusEffectCatalog.createVulnerable(2))
                }
                heroId.contains("anubis") -> {
                    baseDmg = if (eHero.hpPercentage < 0.20f) eHero.currentHp else 4200
                }
                heroId.contains("achilles") -> {
                    baseDmg = 4800
                }
                heroId.contains("merlin") -> {
                    baseDmg = 3500
                }
                else -> {
                    baseDmg = 4500
                }
            }

            val damageResult = DamageEngine.calculateAndApplyDamage(
                attacker = pHero,
                defender = eHero,
                baseDamage = baseDmg,
                isPhysical = false
            )

            val updatedEnemy = damageResult.updatedDefender
            PlayerEconomyRepository.instance.onDamageDealt(damageResult.modifiedDamage)
            newLogs.add(0, CombatLog(UUID.randomUUID().toString(), "$ultName! Deals ${damageResult.modifiedDamage} damage to ${updatedEnemy.name}.", LogType.ULTIMATE))

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
                    playerHero = pHero,
                    enemyHero = updatedEnemy,
                    combatLogs = newLogs,
                    floatingTexts = newFloating,
                    isUltimateCinematicActive = false,
                    activeNotification = CombatNotification(UUID.randomUUID().toString(), "$ultName deals ${damageResult.modifiedDamage} damage!", NotificationType.ULTIMATE),
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
     * Player Turn Ends -> Enemy Turn Begins -> Status effects resolve ->
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

            // Step 1: Start-of-Enemy-Turn Relics & Statuses (Burn, Regen, Poison, Relics)
            var preAiState = _uiState.value
            var eHero = preAiState.enemyHero
            var pHero = preAiState.playerHero
            var pMythPower = preAiState.playerMythPower
            val floating = preAiState.floatingTexts.toMutableList()

            // Thunderstone Relic
            if (pHero.hasThunderstoneRelic) {
                val thunderResult = DamageEngine.calculateAndApplyDamage(null, eHero, 1000)
                eHero = thunderResult.updatedDefender
                pMythPower = (pMythPower + 10).coerceAtMost(preAiState.maxMythPower)
                floating.add(FloatingCombatText(UUID.randomUUID().toString(), "-1000 Lightning", isEnemyTarget = true, damageType = "LIGHTNING"))
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

            // Turn Start Status Effects (Burn, Regen)
            val (turnStartHero, startFloating) = DamageEngine.resolveTurnStartStatuses(eHero, isEnemy = true)
            eHero = turnStartHero
            floating.addAll(startFloating)
            if (startFloating.isNotEmpty()) {
                _uiState.update { it.copy(enemyHero = eHero, floatingTexts = floating) }
                delay(500)
                if (!eHero.isAlive) {
                    _uiState.update { it.copy(currentTurn = BattleTurn.VICTORY, isExecutingTurn = false, activeNotification = null) }
                    triggerVictory()
                    return@launch
                }
            }

            // Stun Check
            if (eHero.isStunned) {
                floating.add(FloatingCombatText(UUID.randomUUID().toString(), "STUNNED! (Turn Skipped)", isEnemyTarget = true))
                _uiState.update {
                    it.copy(
                        enemyHero = eHero,
                        floatingTexts = floating,
                        activeNotification = CombatNotification(UUID.randomUUID().toString(), "${eHero.name} is STUNNED!", NotificationType.STATUS)
                    )
                }
                delay(800)
                // Resolve turn end poison
                val (turnEndHero, endFloating) = DamageEngine.resolveTurnEndStatuses(eHero, isEnemy = true)
                floating.addAll(endFloating)
                _uiState.update { it.copy(enemyHero = turnEndHero, floatingTexts = floating) }
                delay(400)
                finalizeTurnTransition()
                return@launch
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
            availableEnergy = currentState.enemyEnergy,
            isBossPhase2 = currentState.isBossPhase2Active
        )

        for (action in actions) {
            when (action) {
                is AiAction.PlayCard -> {
                    val card = action.card
                    _uiState.update {
                        it.copy(
                            activeNotification = CombatNotification(UUID.randomUUID().toString(), "${currentState.enemyHero.name} → ${card.name}", NotificationType.CARD)
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
                            activeNotification = CombatNotification(UUID.randomUUID().toString(), "${currentState.enemyHero.name} attacks directly", NotificationType.ATTACK)
                        )
                    }
                    soundManager?.playAttackSound()
                    delay(400)

                    resolveEnemyBasicAttack(action.baseDamage)
                    delay(500)
                    _uiState.update { it.copy(isEnemyAttacking = false) }
                }
                is AiAction.BossSpecialAbility -> {
                    _uiState.update {
                        it.copy(
                            isEnemyAttacking = true,
                            screenShakeTrigger = it.screenShakeTrigger + 1,
                            activeNotification = CombatNotification(UUID.randomUUID().toString(), "BOSS: ${action.abilityName}!", NotificationType.ULTIMATE)
                        )
                    }
                    soundManager?.playUltimateSound()
                    delay(500)

                    resolveEnemyBasicAttack(action.damage)
                    delay(500)
                    _uiState.update { it.copy(isEnemyAttacking = false) }
                }
            }

            if (!_uiState.value.playerHero.isAlive) {
                break
            }
        }

        // Resolve end-of-turn Poison
        val preFinal = _uiState.value
        val (turnEndHero, endFloating) = DamageEngine.resolveTurnEndStatuses(preFinal.enemyHero, isEnemy = true)
        val floating = preFinal.floatingTexts.toMutableList()
        floating.addAll(endFloating)
        _uiState.update { it.copy(enemyHero = turnEndHero, floatingTexts = floating) }
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
            if (activeEncounterConfig?.isArenaMatch == true) {
                val opponent = activeEncounterConfig?.arenaOpponent ?: ArenaCatalog.OPPONENT_POOL.first()
                val arenaSummary = PlayerEconomyRepository.instance.recordArenaBattleFinished(
                    opponent = opponent,
                    isVictory = false,
                    stats = state.stats
                )
                _uiState.update {
                    it.copy(
                        arenaBattleResultSummary = arenaSummary,
                        rewards = BattleRewards(
                            gold = arenaSummary.goldAwarded,
                            xp = arenaSummary.xpAwarded,
                            cardRewardName = "Participation Shards",
                            cardRewardRarity = CardRarity.COMMON
                        )
                    )
                }
            } else {
                if (activeEncounterConfig?.isWorldBoss == true) {
                    val bossId = activeEncounterConfig?.worldBossId ?: "world_boss_kronos"
                    PlayerEconomyRepository.instance.recordWorldBossBattle(
                        bossId = bossId,
                        damageDealt = state.stats.totalDamageDealt.toLong(),
                        isVictory = false
                    )
                }
                PlayerEconomyRepository.instance.recordBattleFinished(
                    isVictory = false,
                    stats = state.stats,
                    isCampaign = activeEncounterConfig?.stageId != null
                )
            }
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
