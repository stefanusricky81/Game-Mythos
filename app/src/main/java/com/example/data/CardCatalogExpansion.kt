package com.example.data

/**
 * Phase 9 Card Expansion (32 New Mythology Cards across GOD, MYTHIC, RELIC, SPELL, TRAP, SUMMON).
 * Factions: Olympus, Valhalla, Egypt, Underworld.
 */
object CardCatalogExpansion {

    val NEW_CARDS: List<CardDefinition> = listOf(
        // 1. Zeus: Thunderbolt (GOD / MYTHIC)
        CardDefinition(
            id = "c_zeus_thunderbolt",
            name = "Thunderbolt",
            cost = 3,
            type = CardType.GOD,
            rarity = CardRarity.MYTHIC,
            loreQuote = "Forged in Cyclopean fires to shatter titans and split mountains.",
            iconKey = "lightning",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2800, burnDamagePerTurn = 400, burnTurns = 2), "Deal 2,800 lightning damage and inflict 400 Shock/Burn for 2 turns.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 3100, burnDamagePerTurn = 450, burnTurns = 2), "Deal 3,100 lightning damage and inflict 450 Shock/Burn for 2 turns.", goldCostToNext = 10000, shardCostToNext = 40),
                3 to CardProgressionStep(3, CardEffect(damage = 3450, burnDamagePerTurn = 520, burnTurns = 2), "Deal 3,450 lightning damage and inflict 520 Shock/Burn for 2 turns.", goldCostToNext = 20000, shardCostToNext = 70),
                4 to CardProgressionStep(4, CardEffect(damage = 3850, burnDamagePerTurn = 600, burnTurns = 2), "Deal 3,850 lightning damage and inflict 600 Shock/Burn for 2 turns.", goldCostToNext = 35000, shardCostToNext = 110),
                5 to CardProgressionStep(5, CardEffect(damage = 4350, burnDamagePerTurn = 700, burnTurns = 2), "Deal 4,350 lightning damage and inflict 700 Shock/Burn for 2 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 2. Zeus: Storm of Olympus (SPELL / EPIC)
        CardDefinition(
            id = "c_zeus_storm_olympus",
            name = "Storm of Olympus",
            cost = 4,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "Tempestuous skies rain crackling wrath upon mortal challengers.",
            iconKey = "lightning",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2200, mythPowerGain = 35), "Deal 2,200 damage and generate +35 Myth Power.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(damage = 2450, mythPowerGain = 38), "Deal 2,450 damage and generate +38 Myth Power.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(damage = 2750, mythPowerGain = 42), "Deal 2,750 damage and generate +42 Myth Power.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(damage = 3100, mythPowerGain = 46), "Deal 3,100 damage and generate +46 Myth Power.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(damage = 3500, mythPowerGain = 50), "Deal 3,500 damage and generate +50 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 3. Zeus: Divine Lightning (SPELL / LEGENDARY)
        CardDefinition(
            id = "c_zeus_divine_lightning",
            name = "Divine Lightning",
            cost = 4,
            type = CardType.SPELL,
            rarity = CardRarity.LEGENDARY,
            loreQuote = "Pierces celestial vaults with incandescent, unblockable fury.",
            iconKey = "lightning",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2600, trueDamage = 1200), "Deal 2,600 damage and 1,200 True Damage ignoring all shield.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 2900, trueDamage = 1350), "Deal 2,900 damage and 1,350 True Damage.", goldCostToNext = 10000, shardCostToNext = 50),
                3 to CardProgressionStep(3, CardEffect(damage = 3250, trueDamage = 1500), "Deal 3,250 damage and 1,500 True Damage.", goldCostToNext = 20000, shardCostToNext = 80),
                4 to CardProgressionStep(4, CardEffect(damage = 3650, trueDamage = 1700), "Deal 3,650 damage and 1,700 True Damage.", goldCostToNext = 35000, shardCostToNext = 120),
                5 to CardProgressionStep(5, CardEffect(damage = 4150, trueDamage = 2000), "Deal 4,150 damage and 2,000 True Damage.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 4. Athena: Aegis of Olympus (RELIC / EPIC)
        CardDefinition(
            id = "c_athena_aegis",
            name = "Aegis",
            cost = 3,
            type = CardType.RELIC,
            rarity = CardRarity.EPIC,
            loreQuote = "The divine shield bearing the fearsome gaze of the gorgon.",
            iconKey = "shield",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 2200, damageReductionPercent = 20), "Gain 2,200 Shield and reduce incoming damage by 20%.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(shield = 2450, damageReductionPercent = 22), "Gain 2,450 Shield and reduce damage by 22%.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(shield = 2750, damageReductionPercent = 24), "Gain 2,750 Shield and reduce damage by 24%.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(shield = 3100, damageReductionPercent = 26), "Gain 3,100 Shield and reduce damage by 26%.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(shield = 3500, damageReductionPercent = 30), "Gain 3,500 Shield and reduce damage by 30%.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 5. Athena: Strategic Insight (SPELL / RARE)
        CardDefinition(
            id = "c_athena_strategic_insight",
            name = "Strategic Insight",
            cost = 2,
            type = CardType.SPELL,
            rarity = CardRarity.RARE,
            loreQuote = "Anticipating three moves ahead guarantees victory before battle begins.",
            iconKey = "scroll",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(drawCardsCount = 2, shield = 600), "Draw 2 cards and gain 600 Shield.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(drawCardsCount = 2, shield = 750), "Draw 2 cards and gain 750 Shield.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(drawCardsCount = 2, shield = 900), "Draw 2 cards and gain 900 Shield.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(drawCardsCount = 2, shield = 1100), "Draw 2 cards and gain 1,100 Shield.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(drawCardsCount = 3, shield = 1350), "Draw 3 cards and gain 1,350 Shield.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 6. Athena: Spear of Wisdom (ATTACK / RARE)
        CardDefinition(
            id = "c_athena_spear_wisdom",
            name = "Spear of Wisdom",
            cost = 3,
            type = CardType.ATTACK,
            rarity = CardRarity.RARE,
            loreQuote = "Tempered with cool intellect to pierce heated rage.",
            iconKey = "spears",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1800, weakenTurns = 2), "Deal 1,800 damage and inflict Weaken (-25% damage) for 2 turns.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(damage = 2050, weakenTurns = 2), "Deal 2,050 damage and inflict Weaken for 2 turns.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(damage = 2300, weakenTurns = 2), "Deal 2,300 damage and inflict Weaken for 2 turns.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(damage = 2600, weakenTurns = 2), "Deal 2,600 damage and inflict Weaken for 2 turns.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(damage = 3000, weakenTurns = 3), "Deal 3,000 damage and inflict Weaken for 3 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 7. Ares: Blood Frenzy (SPELL / EPIC)
        CardDefinition(
            id = "c_ares_blood_frenzy",
            name = "Blood Frenzy",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "The hot iron scent of slaughter washes away all restraint.",
            iconKey = "fire",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(attackBuffPercent = 35, attackBuffTurns = 2, lifestealPercent = 20), "Gain +35% Attack for 2 turns and 20% Lifesteal.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(attackBuffPercent = 40, attackBuffTurns = 2, lifestealPercent = 22), "Gain +40% Attack and 22% Lifesteal.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(attackBuffPercent = 45, attackBuffTurns = 2, lifestealPercent = 25), "Gain +45% Attack and 25% Lifesteal.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(attackBuffPercent = 50, attackBuffTurns = 2, lifestealPercent = 28), "Gain +50% Attack and 28% Lifesteal.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(attackBuffPercent = 60, attackBuffTurns = 3, lifestealPercent = 35), "Gain +60% Attack for 3 turns and 35% Lifesteal.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 8. Ares: War Cry (SPELL / COMMON)
        CardDefinition(
            id = "c_ares_war_cry",
            name = "War Cry",
            cost = 2,
            type = CardType.SPELL,
            rarity = CardRarity.COMMON,
            loreQuote = "A roaring battle bellow that chills the marrow of the enemy.",
            iconKey = "fire",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(weakenTurns = 2, energyGain = 1, mythPowerGain = 15), "Inflict Weaken for 2 turns, gain +1 Energy and +15 Myth Power.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(weakenTurns = 2, energyGain = 1, mythPowerGain = 18), "Inflict Weaken, gain +1 Energy and +18 Myth Power.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(weakenTurns = 2, energyGain = 1, mythPowerGain = 22), "Inflict Weaken, gain +1 Energy and +22 Myth Power.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(weakenTurns = 3, energyGain = 1, mythPowerGain = 26), "Inflict Weaken for 3 turns, gain +1 Energy and +26 Myth Power.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(weakenTurns = 3, energyGain = 2, mythPowerGain = 30), "Inflict Weaken for 3 turns, gain +2 Energy and +30 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 9. Ares: Crimson Assault (ATTACK / LEGENDARY)
        CardDefinition(
            id = "c_ares_crimson_assault",
            name = "Crimson Assault",
            cost = 4,
            type = CardType.ATTACK,
            rarity = CardRarity.LEGENDARY,
            loreQuote = "Relentless dual-blade barrage that carves through flesh and bronze.",
            iconKey = "strike",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 3200, bonusDamageAgainstShield = 800), "Deal 3,200 physical damage (+800 bonus against Shield).", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 3550, bonusDamageAgainstShield = 950), "Deal 3,550 damage (+950 bonus against Shield).", goldCostToNext = 10000, shardCostToNext = 50),
                3 to CardProgressionStep(3, CardEffect(damage = 3900, bonusDamageAgainstShield = 1100), "Deal 3,900 damage (+1,100 bonus against Shield).", goldCostToNext = 20000, shardCostToNext = 80),
                4 to CardProgressionStep(4, CardEffect(damage = 4300, bonusDamageAgainstShield = 1300), "Deal 4,300 damage (+1,300 bonus against Shield).", goldCostToNext = 35000, shardCostToNext = 120),
                5 to CardProgressionStep(5, CardEffect(damage = 4800, bonusDamageAgainstShield = 1600), "Deal 4,800 damage (+1,600 bonus against Shield).", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 10. Medusa: Petrifying Gaze (SPELL / EPIC)
        CardDefinition(
            id = "c_medusa_petrifying_gaze",
            name = "Petrifying Gaze",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "Lock eyes with mortal dread, feeling limbs stiffen into cold limestone.",
            iconKey = "poison",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 800, stunTurns = 1, vulnerableTurns = 2), "Deal 800 damage, Stun enemy for 1 turn and inflict Vulnerable (+30% dmg).", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(damage = 950, stunTurns = 1, vulnerableTurns = 2), "Deal 950 damage, Stun for 1 turn and inflict Vulnerable.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(damage = 1150, stunTurns = 1, vulnerableTurns = 2), "Deal 1,150 damage, Stun for 1 turn and inflict Vulnerable.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(damage = 1400, stunTurns = 1, vulnerableTurns = 3), "Deal 1,400 damage, Stun for 1 turn and inflict Vulnerable for 3 turns.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(damage = 1750, stunTurns = 1, vulnerableTurns = 3), "Deal 1,750 damage, Stun for 1 turn and inflict Vulnerable for 3 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 11. Medusa: Serpent Coil (TRAP / RARE)
        CardDefinition(
            id = "c_medusa_serpent_coil",
            name = "Serpent Coil",
            cost = 2,
            type = CardType.TRAP,
            rarity = CardRarity.RARE,
            loreQuote = "Hissing vipers lie coiled in the gloom, waiting to strike unwary ankles.",
            iconKey = "poison",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 800, poisonDamagePerTurn = 500, poisonTurns = 3), "Gain 800 Shield and inflict 500 Poison/turn for 3 turns.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(shield = 950, poisonDamagePerTurn = 570, poisonTurns = 3), "Gain 950 Shield and inflict 570 Poison/turn for 3 turns.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(shield = 1150, poisonDamagePerTurn = 650, poisonTurns = 3), "Gain 1,150 Shield and inflict 650 Poison/turn for 3 turns.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(shield = 1400, poisonDamagePerTurn = 750, poisonTurns = 3), "Gain 1,400 Shield and inflict 750 Poison/turn for 3 turns.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(shield = 1700, poisonDamagePerTurn = 900, poisonTurns = 4), "Gain 1,700 Shield and inflict 900 Poison/turn for 4 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 12. Medusa: Stone Curse (MYTHIC / MYTHIC)
        CardDefinition(
            id = "c_medusa_stone_curse",
            name = "Stone Curse",
            cost = 5,
            type = CardType.GOD,
            rarity = CardRarity.MYTHIC,
            loreQuote = "The irrevocable stillness of the limestone tomb claims all beating hearts.",
            iconKey = "poison",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 3500, stunTurns = 1, poisonDamagePerTurn = 400, poisonTurns = 2), "Deal 3,500 damage, Stun foe for 1 turn, and poison for 400/turn.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 3850, stunTurns = 1, poisonDamagePerTurn = 460, poisonTurns = 2), "Deal 3,850 damage, Stun for 1 turn, and poison for 460/turn.", goldCostToNext = 10000, shardCostToNext = 40),
                3 to CardProgressionStep(3, CardEffect(damage = 4250, stunTurns = 1, poisonDamagePerTurn = 530, poisonTurns = 2), "Deal 4,250 damage, Stun for 1 turn, and poison for 530/turn.", goldCostToNext = 20000, shardCostToNext = 70),
                4 to CardProgressionStep(4, CardEffect(damage = 4700, stunTurns = 1, poisonDamagePerTurn = 620, poisonTurns = 3), "Deal 4,700 damage, Stun for 1 turn, and poison for 620/turn.", goldCostToNext = 35000, shardCostToNext = 110),
                5 to CardProgressionStep(5, CardEffect(damage = 5300, stunTurns = 1, poisonDamagePerTurn = 750, poisonTurns = 3), "Deal 5,300 damage, Stun for 1 turn, and poison for 750/turn.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 13. Hades: Underworld Gate (SUMMON / EPIC)
        CardDefinition(
            id = "c_hades_underworld_gate",
            name = "Underworld Gate",
            cost = 4,
            type = CardType.SUMMON,
            rarity = CardRarity.EPIC,
            loreQuote = "Basalt portals swing wide, disgorging wailing specters into the mortal realm.",
            iconKey = "shadow",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(summonShield = 1800, summonCounterDamage = 900, poisonDamagePerTurn = 300, poisonTurns = 2), "Summon Underworld Shade: +1,800 Shield, 900 counter, and 300 poison.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(summonShield = 2050, summonCounterDamage = 1050, poisonDamagePerTurn = 350, poisonTurns = 2), "Summon Shade: +2,050 Shield, 1,050 counter, 350 poison.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(summonShield = 2350, summonCounterDamage = 1200, poisonDamagePerTurn = 400, poisonTurns = 2), "Summon Shade: +2,350 Shield, 1,200 counter, 400 poison.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(summonShield = 2700, summonCounterDamage = 1400, poisonDamagePerTurn = 470, poisonTurns = 3), "Summon Shade: +2,700 Shield, 1,400 counter, 470 poison.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(summonShield = 3150, summonCounterDamage = 1700, poisonDamagePerTurn = 560, poisonTurns = 3), "Summon Shade: +3,150 Shield, 1,700 counter, 560 poison.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 14. Hades: Soul Drain (SPELL / RARE)
        CardDefinition(
            id = "c_hades_soul_drain",
            name = "Soul Drain",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.RARE,
            loreQuote = "Life essence siphoned to quench the thirst of the Stygian lord.",
            iconKey = "shadow",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1900, healAmount = 1500), "Deal 1,900 damage and siphon 1,500 HP back to your hero.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(damage = 2150, healAmount = 1700), "Deal 2,150 damage and heal 1,700 HP.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(damage = 2450, healAmount = 1950), "Deal 2,450 damage and heal 1,950 HP.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(damage = 2800, healAmount = 2250), "Deal 2,800 damage and heal 2,250 HP.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(damage = 3250, healAmount = 2650), "Deal 3,250 damage and heal 2,650 HP.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 15. Hades: Helm of Darkness (RELIC / LEGENDARY)
        CardDefinition(
            id = "c_hades_helm_darkness",
            name = "Helm of Darkness",
            cost = 3,
            type = CardType.RELIC,
            rarity = CardRarity.LEGENDARY,
            loreQuote = "Cap of pure invisibility that dissolves the wearer into twilight mist.",
            iconKey = "shadow",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 1500, damageReductionPercent = 25), "Gain 1,500 Shield and reduce incoming damage by 25%.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(shield = 1750, damageReductionPercent = 27), "Gain 1,750 Shield and reduce damage by 27%.", goldCostToNext = 10000, shardCostToNext = 50),
                3 to CardProgressionStep(3, CardEffect(shield = 2050, damageReductionPercent = 30), "Gain 2,050 Shield and reduce damage by 30%.", goldCostToNext = 20000, shardCostToNext = 80),
                4 to CardProgressionStep(4, CardEffect(shield = 2400, damageReductionPercent = 32), "Gain 2,400 Shield and reduce damage by 32%.", goldCostToNext = 35000, shardCostToNext = 120),
                5 to CardProgressionStep(5, CardEffect(shield = 2850, damageReductionPercent = 35), "Gain 2,850 Shield and reduce damage by 35%.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 16. Thor: Mjolnir (RELIC / LEGENDARY)
        CardDefinition(
            id = "c_thor_mjolnir",
            name = "Mjolnir",
            cost = 4,
            type = CardType.RELIC,
            rarity = CardRarity.LEGENDARY,
            loreQuote = "Only the worthy can wield the star-metal hammer that commands the lightning.",
            iconKey = "hammer",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2400, stunTurns = 1, turnStartLightningDamage = 800), "Deal 2,400 damage, Stun for 1 turn, and strike with 800 lightning each turn.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 2700, stunTurns = 1, turnStartLightningDamage = 950), "Deal 2,700 damage, Stun, and 950 lightning each turn.", goldCostToNext = 10000, shardCostToNext = 50),
                3 to CardProgressionStep(3, CardEffect(damage = 3050, stunTurns = 1, turnStartLightningDamage = 1100), "Deal 3,050 damage, Stun, and 1,100 lightning each turn.", goldCostToNext = 20000, shardCostToNext = 80),
                4 to CardProgressionStep(4, CardEffect(damage = 3450, stunTurns = 1, turnStartLightningDamage = 1300), "Deal 3,450 damage, Stun, and 1,300 lightning each turn.", goldCostToNext = 35000, shardCostToNext = 120),
                5 to CardProgressionStep(5, CardEffect(damage = 3950, stunTurns = 1, turnStartLightningDamage = 1600), "Deal 3,950 damage, Stun, and 1,600 lightning each turn.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 17. Thor: Thunder Strike (ATTACK / COMMON)
        CardDefinition(
            id = "c_thor_thunder_strike",
            name = "Thunder Strike",
            cost = 2,
            type = CardType.ATTACK,
            rarity = CardRarity.COMMON,
            loreQuote = "A concussive detonation of Asgardian ozone and shattered stone.",
            iconKey = "lightning",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 1600, burnDamagePerTurn = 250, burnTurns = 2), "Deal 1,600 damage and inflict 250 Shock/Burn for 2 turns.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(damage = 1800, burnDamagePerTurn = 290, burnTurns = 2), "Deal 1,800 damage and inflict 290 Shock/Burn for 2 turns.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(damage = 2050, burnDamagePerTurn = 340, burnTurns = 2), "Deal 2,050 damage and inflict 340 Shock/Burn for 2 turns.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(damage = 2350, burnDamagePerTurn = 400, burnTurns = 2), "Deal 2,350 damage and inflict 400 Shock/Burn for 2 turns.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(damage = 2700, burnDamagePerTurn = 480, burnTurns = 2), "Deal 2,700 damage and inflict 480 Shock/Burn for 2 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 18. Thor: Stormbreaker (GOD / MYTHIC)
        CardDefinition(
            id = "c_thor_stormbreaker",
            name = "Stormbreaker",
            cost = 5,
            type = CardType.GOD,
            rarity = CardRarity.MYTHIC,
            loreQuote = "The kingslayer axe forged to sunder titans and channel raging storms.",
            iconKey = "hammer",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 4200, bonusDamageAgainstShield = 1500), "Deal 4,200 physical damage (+1,500 bonus against active Shield).", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 4600, bonusDamageAgainstShield = 1750), "Deal 4,600 damage (+1,750 bonus against Shield).", goldCostToNext = 10000, shardCostToNext = 40),
                3 to CardProgressionStep(3, CardEffect(damage = 5050, bonusDamageAgainstShield = 2050), "Deal 5,050 damage (+2,050 bonus against Shield).", goldCostToNext = 20000, shardCostToNext = 70),
                4 to CardProgressionStep(4, CardEffect(damage = 5550, bonusDamageAgainstShield = 2400), "Deal 5,550 damage (+2,400 bonus against Shield).", goldCostToNext = 35000, shardCostToNext = 110),
                5 to CardProgressionStep(5, CardEffect(damage = 6200, bonusDamageAgainstShield = 2900), "Deal 6,200 damage (+2,900 bonus against Shield).", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 19. Loki: Illusion (TRAP / RARE)
        CardDefinition(
            id = "c_loki_illusion",
            name = "Illusion",
            cost = 2,
            type = CardType.TRAP,
            rarity = CardRarity.RARE,
            loreQuote = "Striking a phantom leaves the foe off-balance and bewildered.",
            iconKey = "mirror",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 1600, summonCounterDamage = 700), "Gain 1,600 Shield and reflect 700 damage to attacker.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(shield = 1850, summonCounterDamage = 820), "Gain 1,850 Shield and reflect 820 damage.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(shield = 2150, summonCounterDamage = 950), "Gain 2,150 Shield and reflect 950 damage.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(shield = 2500, summonCounterDamage = 1120), "Gain 2,500 Shield and reflect 1,120 damage.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(shield = 2950, summonCounterDamage = 1350), "Gain 2,950 Shield and reflect 1,350 damage.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 20. Loki: Trickster's Bargain (SPELL / UNCOMMON)
        CardDefinition(
            id = "c_loki_bargain",
            name = "Trickster's Bargain",
            cost = 1,
            type = CardType.SPELL,
            rarity = CardRarity.UNCOMMON,
            loreQuote = "Take from the enemy's hands to fill your own with chaotic momentum.",
            iconKey = "coin",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(energyGain = 2, mythPowerGain = 20, drawCardsCount = 1), "Gain +2 Energy, +20 Myth Power, and draw 1 card.", goldCostToNext = 1500, shardCostToNext = 10),
                2 to CardProgressionStep(2, CardEffect(energyGain = 2, mythPowerGain = 23, drawCardsCount = 1), "Gain +2 Energy, +23 Myth Power, draw 1 card.", goldCostToNext = 3000, shardCostToNext = 20),
                3 to CardProgressionStep(3, CardEffect(energyGain = 2, mythPowerGain = 26, drawCardsCount = 1), "Gain +2 Energy, +26 Myth Power, draw 1 card.", goldCostToNext = 6000, shardCostToNext = 35),
                4 to CardProgressionStep(4, CardEffect(energyGain = 3, mythPowerGain = 30, drawCardsCount = 1), "Gain +3 Energy, +30 Myth Power, draw 1 card.", goldCostToNext = 12000, shardCostToNext = 55),
                5 to CardProgressionStep(5, CardEffect(energyGain = 3, mythPowerGain = 35, drawCardsCount = 2), "Gain +3 Energy, +35 Myth Power, and draw 2 cards.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 21. Loki: Chaos Rift (SPELL / EPIC)
        CardDefinition(
            id = "c_loki_chaos_rift",
            name = "Chaos Rift",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "A fracture in space where certainty dissolves into unpredictable ruin.",
            iconKey = "shadow",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2200, vulnerableTurns = 2, weakenTurns = 2), "Deal 2,200 chaos damage and inflict both Vulnerable and Weaken for 2 turns.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(damage = 2450, vulnerableTurns = 2, weakenTurns = 2), "Deal 2,450 damage, inflict Vulnerable and Weaken.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(damage = 2750, vulnerableTurns = 2, weakenTurns = 2), "Deal 2,750 damage, inflict Vulnerable and Weaken.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(damage = 3100, vulnerableTurns = 3, weakenTurns = 3), "Deal 3,100 damage, inflict Vulnerable and Weaken for 3 turns.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(damage = 3550, vulnerableTurns = 3, weakenTurns = 3), "Deal 3,550 damage, inflict Vulnerable and Weaken for 3 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 22. Anubis: Judgment (SPELL / EPIC)
        CardDefinition(
            id = "c_anubis_judgment",
            name = "Judgment",
            cost = 4,
            type = CardType.SPELL,
            rarity = CardRarity.EPIC,
            loreQuote = "The balance of Ma'at tips inexorably against unworthy souls.",
            iconKey = "scroll",
            faction = "Egypt",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2700, executeThresholdPercent = 25), "Deal 2,700 damage. Instantly executes enemy if HP is below 25%.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(damage = 3000, executeThresholdPercent = 27), "Deal 3,000 damage. Executes enemy below 27% HP.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(damage = 3350, executeThresholdPercent = 30), "Deal 3,350 damage. Executes enemy below 30% HP.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(damage = 3750, executeThresholdPercent = 32), "Deal 3,750 damage. Executes enemy below 32% HP.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(damage = 4250, executeThresholdPercent = 35), "Deal 4,250 damage. Executes enemy below 35% HP.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 23. Anubis: Soul Weighing (SPELL / RARE)
        CardDefinition(
            id = "c_anubis_soul_weighing",
            name = "Soul Weighing",
            cost = 2,
            type = CardType.SPELL,
            rarity = CardRarity.RARE,
            loreQuote = "Feather and heart weighed in sacred balance upon the golden scales of Duat.",
            iconKey = "chalice",
            faction = "Egypt",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 1400, mythPowerGain = 25), "Gain 1,400 Shield and harvest +25 Myth Power.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(shield = 1650, mythPowerGain = 28), "Gain 1,650 Shield and +28 Myth Power.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(shield = 1950, mythPowerGain = 32), "Gain 1,950 Shield and +32 Myth Power.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(shield = 2300, mythPowerGain = 36), "Gain 2,300 Shield and +36 Myth Power.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(shield = 2750, mythPowerGain = 42), "Gain 2,750 Shield and +42 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 24. Anubis: Tomb's Curse (SPELL / UNCOMMON)
        CardDefinition(
            id = "c_anubis_tomb_curse",
            name = "Tomb's Curse",
            cost = 3,
            type = CardType.SPELL,
            rarity = CardRarity.UNCOMMON,
            loreQuote = "Woe unto mortals who desecrate the quiet slumber of the pharaohs.",
            iconKey = "poison",
            faction = "Egypt",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(poisonDamagePerTurn = 450, poisonTurns = 3, weakenTurns = 2), "Inflict 450 Poison for 3 turns and Weaken for 2 turns.", goldCostToNext = 1500, shardCostToNext = 10),
                2 to CardProgressionStep(2, CardEffect(poisonDamagePerTurn = 510, poisonTurns = 3, weakenTurns = 2), "Inflict 510 Poison for 3 turns and Weaken for 2 turns.", goldCostToNext = 3000, shardCostToNext = 20),
                3 to CardProgressionStep(3, CardEffect(poisonDamagePerTurn = 580, poisonTurns = 3, weakenTurns = 2), "Inflict 580 Poison for 3 turns and Weaken for 2 turns.", goldCostToNext = 6000, shardCostToNext = 35),
                4 to CardProgressionStep(4, CardEffect(poisonDamagePerTurn = 660, poisonTurns = 3, weakenTurns = 3), "Inflict 660 Poison for 3 turns and Weaken for 3 turns.", goldCostToNext = 12000, shardCostToNext = 55),
                5 to CardProgressionStep(5, CardEffect(poisonDamagePerTurn = 770, poisonTurns = 4, weakenTurns = 3), "Inflict 770 Poison for 4 turns and Weaken for 3 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 25. Generic / Cross-Hero: Spartan War Horn (SPELL / COMMON)
        CardDefinition(
            id = "c_spartan_war_horn",
            name = "Spartan War Horn",
            cost = 2,
            type = CardType.SPELL,
            rarity = CardRarity.COMMON,
            loreQuote = "Resounding across Mount Taygetos, rallying hoplites into battle.",
            iconKey = "fire",
            faction = "Olympus",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(attackBuffPercent = 25, attackBuffTurns = 2, mythPowerGain = 15), "Gain +25% Attack for 2 turns and +15 Myth Power.", goldCostToNext = 1000, shardCostToNext = 15),
                2 to CardProgressionStep(2, CardEffect(attackBuffPercent = 28, attackBuffTurns = 2, mythPowerGain = 18), "Gain +28% Attack and +18 Myth Power.", goldCostToNext = 2500, shardCostToNext = 30),
                3 to CardProgressionStep(3, CardEffect(attackBuffPercent = 32, attackBuffTurns = 2, mythPowerGain = 22), "Gain +32% Attack and +22 Myth Power.", goldCostToNext = 5000, shardCostToNext = 50),
                4 to CardProgressionStep(4, CardEffect(attackBuffPercent = 36, attackBuffTurns = 2, mythPowerGain = 26), "Gain +36% Attack and +26 Myth Power.", goldCostToNext = 10000, shardCostToNext = 80),
                5 to CardProgressionStep(5, CardEffect(attackBuffPercent = 42, attackBuffTurns = 3, mythPowerGain = 32), "Gain +42% Attack for 3 turns and +32 Myth Power.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 26. Generic / Cross-Hero: Valkyrie Wings (SUMMON / EPIC)
        CardDefinition(
            id = "c_valkyrie_wings",
            name = "Valkyrie Wings",
            cost = 3,
            type = CardType.SUMMON,
            rarity = CardRarity.EPIC,
            loreQuote = "Choosers of the slain soar on silver feathers, shielding heroes from death.",
            iconKey = "spears",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(summonShield = 1700, summonCounterDamage = 850, healAmount = 800), "Summon Valkyrie: +1,700 Shield, 850 retaliation, and restore 800 HP.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(summonShield = 1950, summonCounterDamage = 980, healAmount = 950), "Summon Valkyrie: +1,950 Shield, 980 counter, restore 950 HP.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(summonShield = 2250, summonCounterDamage = 1120, healAmount = 1150), "Summon Valkyrie: +2,250 Shield, 1,120 counter, restore 1,150 HP.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(summonShield = 2600, summonCounterDamage = 1300, healAmount = 1400), "Summon Valkyrie: +2,600 Shield, 1,300 counter, restore 1,400 HP.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(summonShield = 3050, summonCounterDamage = 1550, healAmount = 1750), "Summon Valkyrie: +3,050 Shield, 1,550 counter, restore 1,750 HP.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 27. Generic / Cross-Hero: Runestone of Odin (RELIC / RARE)
        CardDefinition(
            id = "c_runestone_odin",
            name = "Runestone of Odin",
            cost = 3,
            type = CardType.RELIC,
            rarity = CardRarity.RARE,
            loreQuote = "Elder Futhark markings carved with the wisdom of the Allfather.",
            iconKey = "scroll",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(drawCardsCount = 2, shield = 900), "Draw 2 cards and gain 900 Shield.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(drawCardsCount = 2, shield = 1050), "Draw 2 cards and gain 1,050 Shield.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(drawCardsCount = 2, shield = 1250), "Draw 2 cards and gain 1,250 Shield.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(drawCardsCount = 3, shield = 1500), "Draw 3 cards and gain 1,500 Shield.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(drawCardsCount = 3, shield = 1850), "Draw 3 cards and gain 1,850 Shield.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 28. Generic / Cross-Hero: Eye of Ra (GOD / LEGENDARY)
        CardDefinition(
            id = "c_eye_of_ra",
            name = "Eye of Ra",
            cost = 4,
            type = CardType.GOD,
            rarity = CardRarity.LEGENDARY,
            loreQuote = "The blistering sun disc that cleanses darkness and incinerates foes.",
            iconKey = "fire",
            faction = "Egypt",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2800, burnDamagePerTurn = 500, burnTurns = 2, isCleanse = true), "Deal 2,800 damage, burn for 500/turn, and cleanse all debuffs.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(damage = 3100, burnDamagePerTurn = 560, burnTurns = 2, isCleanse = true), "Deal 3,100 damage, burn for 560/turn, and cleanse.", goldCostToNext = 10000, shardCostToNext = 50),
                3 to CardProgressionStep(3, CardEffect(damage = 3450, burnDamagePerTurn = 630, burnTurns = 2, isCleanse = true), "Deal 3,450 damage, burn for 630/turn, and cleanse.", goldCostToNext = 20000, shardCostToNext = 80),
                4 to CardProgressionStep(4, CardEffect(damage = 3850, burnDamagePerTurn = 720, burnTurns = 2, isCleanse = true), "Deal 3,850 damage, burn for 720/turn, and cleanse.", goldCostToNext = 35000, shardCostToNext = 120),
                5 to CardProgressionStep(5, CardEffect(damage = 4350, burnDamagePerTurn = 850, burnTurns = 3, isCleanse = true), "Deal 4,350 damage, burn for 850/turn, and cleanse all debuffs.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 29. Generic / Cross-Hero: Ankh of Life (RELIC / EPIC)
        CardDefinition(
            id = "c_ankh_of_life",
            name = "Ankh of Life",
            cost = 3,
            type = CardType.RELIC,
            rarity = CardRarity.EPIC,
            loreQuote = "The looped cross representing breath, immortality, and eternal vitality.",
            iconKey = "chalice",
            faction = "Egypt",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(healAmount = 2000, regenerationAmount = 500, regenerationTurns = 3), "Restore 2,000 HP and gain 500 HP Regeneration for 3 turns.", goldCostToNext = 3000, shardCostToNext = 16),
                2 to CardProgressionStep(2, CardEffect(healAmount = 2300, regenerationAmount = 560, regenerationTurns = 3), "Restore 2,300 HP and 560 Regeneration/turn.", goldCostToNext = 6000, shardCostToNext = 35),
                3 to CardProgressionStep(3, CardEffect(healAmount = 2650, regenerationAmount = 630, regenerationTurns = 3), "Restore 2,650 HP and 630 Regeneration/turn.", goldCostToNext = 12000, shardCostToNext = 60),
                4 to CardProgressionStep(4, CardEffect(healAmount = 3050, regenerationAmount = 720, regenerationTurns = 3), "Restore 3,050 HP and 720 Regeneration/turn.", goldCostToNext = 22000, shardCostToNext = 90),
                5 to CardProgressionStep(5, CardEffect(healAmount = 3550, regenerationAmount = 850, regenerationTurns = 4), "Restore 3,550 HP and 850 Regeneration for 4 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 30. Generic / Cross-Hero: River Styx Draught (SPELL / MYTHIC)
        CardDefinition(
            id = "c_river_styx_draught",
            name = "River Styx Draught",
            cost = 4,
            type = CardType.SPELL,
            rarity = CardRarity.MYTHIC,
            loreQuote = "A vial of the boundary waters that transforms mortal skin into adamantine.",
            iconKey = "chalice",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(shield = 2000, damageReductionPercent = 35, isCleanse = true), "Gain 2,000 Shield, reduce damage by 35%, and cleanse all debuffs.", goldCostToNext = 5000, shardCostToNext = 20),
                2 to CardProgressionStep(2, CardEffect(shield = 2300, damageReductionPercent = 37, isCleanse = true), "Gain 2,300 Shield, reduce damage by 37%, and cleanse.", goldCostToNext = 10000, shardCostToNext = 40),
                3 to CardProgressionStep(3, CardEffect(shield = 2650, damageReductionPercent = 40, isCleanse = true), "Gain 2,650 Shield, reduce damage by 40%, and cleanse.", goldCostToNext = 20000, shardCostToNext = 70),
                4 to CardProgressionStep(4, CardEffect(shield = 3050, damageReductionPercent = 42, isCleanse = true), "Gain 3,050 Shield, reduce damage by 42%, and cleanse.", goldCostToNext = 35000, shardCostToNext = 110),
                5 to CardProgressionStep(5, CardEffect(shield = 3600, damageReductionPercent = 45, isCleanse = true), "Gain 3,600 Shield, reduce damage by 45%, and cleanse all debuffs.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 31. Generic / Cross-Hero: Frost Giant Hammer (ATTACK / RARE)
        CardDefinition(
            id = "c_frost_giant_hammer",
            name = "Frost Giant Hammer",
            cost = 4,
            type = CardType.ATTACK,
            rarity = CardRarity.RARE,
            loreQuote = "Carved from glacial rime in the frigid canyons of Jotunheim.",
            iconKey = "hammer",
            faction = "Valhalla",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(damage = 2900, stunTurns = 1), "Deal 2,900 heavy frost damage and Stun enemy for 1 turn.", goldCostToNext = 2000, shardCostToNext = 12),
                2 to CardProgressionStep(2, CardEffect(damage = 3200, stunTurns = 1), "Deal 3,200 damage and Stun for 1 turn.", goldCostToNext = 4000, shardCostToNext = 25),
                3 to CardProgressionStep(3, CardEffect(damage = 3550, stunTurns = 1), "Deal 3,550 damage and Stun for 1 turn.", goldCostToNext = 8000, shardCostToNext = 45),
                4 to CardProgressionStep(4, CardEffect(damage = 3950, stunTurns = 1), "Deal 3,950 damage and Stun for 1 turn.", goldCostToNext = 15000, shardCostToNext = 70),
                5 to CardProgressionStep(5, CardEffect(damage = 4450, stunTurns = 1), "Deal 4,450 damage and Stun for 1 turn.", goldCostToNext = 0, shardCostToNext = 0)
            )
        ),

        // 32. Generic / Cross-Hero: Gorgon Blood Vial (TRAP / UNCOMMON)
        CardDefinition(
            id = "c_gorgon_blood_vial",
            name = "Gorgon Blood Vial",
            cost = 2,
            type = CardType.TRAP,
            rarity = CardRarity.UNCOMMON,
            loreQuote = "Blood harvested from the left vein of Medusa is concentrated, fatal poison.",
            iconKey = "poison",
            faction = "Underworld",
            progression = mapOf(
                1 to CardProgressionStep(1, CardEffect(poisonDamagePerTurn = 400, poisonTurns = 3, vulnerableTurns = 2), "Inflict 400 Poison for 3 turns and Vulnerable (+30% dmg) for 2 turns.", goldCostToNext = 1500, shardCostToNext = 10),
                2 to CardProgressionStep(2, CardEffect(poisonDamagePerTurn = 460, poisonTurns = 3, vulnerableTurns = 2), "Inflict 460 Poison for 3 turns and Vulnerable.", goldCostToNext = 3000, shardCostToNext = 20),
                3 to CardProgressionStep(3, CardEffect(poisonDamagePerTurn = 530, poisonTurns = 3, vulnerableTurns = 2), "Inflict 530 Poison for 3 turns and Vulnerable.", goldCostToNext = 6000, shardCostToNext = 35),
                4 to CardProgressionStep(4, CardEffect(poisonDamagePerTurn = 610, poisonTurns = 3, vulnerableTurns = 2), "Inflict 610 Poison for 3 turns and Vulnerable.", goldCostToNext = 12000, shardCostToNext = 55),
                5 to CardProgressionStep(5, CardEffect(poisonDamagePerTurn = 720, poisonTurns = 4, vulnerableTurns = 3), "Inflict 720 Poison for 4 turns and Vulnerable for 3 turns.", goldCostToNext = 0, shardCostToNext = 0)
            )
        )
    )
}
