package com.example.data

import com.example.R

/**
 * Definition of a Hero for collection, selection, and battle instantiation.
 */
data class HeroDefinition(
    val id: String,
    val name: String,
    val title: String,
    val faction: String,
    val rarity: CardRarity,
    val combatIdentity: String,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val portraitResId: Int,
    val isPlayableInPrototype: Boolean = true,
    val releaseStatus: String = "AVAILABLE", // "AVAILABLE", "ARCHIVED", "COMING_SOON"
    val maxLevel: Int = 10,
    val defaultDeckCardIds: List<String> = emptyList(),
    val passiveName: String = "LAST STAND",
    val passiveDescription: String = "When HP drops below 30%, gain +25% Attack power.",
    val ultimateName: String = "TWELVE LABORS",
    val ultimateDescription: String = "At 100 Myth Power, unleash a colossal strike dealing 4,500 damage.",
    val unlockCondition: String = "Available by default",
    val role: String = "Balanced Bruiser"
) {
    fun toBattleHero(level: Int = 1): Hero {
        val scaled = HeroProgressionConfig.getScaledStats(this, level)
        if (id == HerculesIdentity.HERO_ID || id == "hercules") {
            return Hero.createHercules(level)
        }
        return Hero(
            id = id,
            name = name,
            title = title,
            currentHp = scaled.hp,
            maxHp = scaled.hp,
            baseAttack = scaled.attack,
            baseDefense = scaled.defense,
            portraitResId = portraitResId,
            level = level
        )
    }
}

/**
 * Centralized Hero Catalog (Phase 9 Requirements #1, #2, #13).
 * Single authoritative source of truth for Hero roster, balance, and metadata.
 */
object HeroCatalog {

    val HERCULES = HeroDefinition(
        id = HerculesIdentity.HERO_ID,
        name = HerculesIdentity.NAME,
        title = HerculesIdentity.TITLE,
        faction = "Greek / Olympus",
        rarity = CardRarity.LEGENDARY,
        combatIdentity = "High durability • Heavy melee damage • Myth Power scaling",
        baseHp = 10000,
        baseAttack = 2800,
        baseDefense = 2600,
        portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Balanced Bruiser",
        unlockCondition = "Unlocked by default",
        defaultDeckCardIds = listOf(
            "c_olympian_guard", "c_olympian_guard",
            "c_titans_wrath",
            "c_power_strike", "c_power_strike",
            "c_heroic_rage", "c_heroic_rage",
            "c_nectar_gods", "c_nectar_gods",
            "c_spartan_phalanx", "c_spartan_phalanx",
            "c_hydra_blade", "c_hydra_blade",
            "c_nemean_hide", "c_nemean_hide",
            "c_divine_challenge", "c_divine_challenge",
            "c_zeus_thunderstone",
            "c_celestial_arrow", "c_celestial_arrow"
        ),
        passiveName = "LAST STAND",
        passiveDescription = "When HP drops below 30%, gain +25% Attack power and divine resilience.",
        ultimateName = "TWELVE LABORS",
        ultimateDescription = "At 100 Myth Power, unleash a colossal strike dealing 4,500 damage."
    )

    val ZEUS = HeroDefinition(
        id = "hero_zeus",
        name = "Zeus",
        title = "King of the Olympians • Lord of the Sky",
        faction = "Greek / Olympus",
        rarity = CardRarity.MYTHIC,
        combatIdentity = "Cataclysmic lightning • Area smite • Overwhelming Myth Power",
        baseHp = 10500,
        baseAttack = 3500,
        baseDefense = 2200,
        portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Burst / Control",
        unlockCondition = "Defeat Ares in Stage 1-5: Wrath of Olympus",
        passiveName = "THUNDER SOVEREIGNTY",
        passiveDescription = "Lightning and storm cards deal +25% damage and shock enemies.",
        ultimateName = "WRATH OF OLYMPUS",
        ultimateDescription = "At 100 Myth Power, strike with cataclysmic lightning dealing 5,000 damage and inflicting Stun."
    )

    val ATHENA = HeroDefinition(
        id = "hero_athena",
        name = "Athena",
        title = "Goddess of Wisdom & Strategic Warfare",
        faction = "Greek / Olympus",
        rarity = CardRarity.EPIC,
        combatIdentity = "Defensive bulwark • Tactical counters • Divine buffs",
        baseHp = 11500,
        baseAttack = 2300,
        baseDefense = 3100,
        portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Defense / Control",
        unlockCondition = "Clear Stage 1-2: Wrath of the Arena",
        passiveName = "DIVINE STRATEGY",
        passiveDescription = "Shield abilities grant +300 bonus shield and counter-attack attackers.",
        ultimateName = "AEGIS OF ATHENA",
        ultimateDescription = "At 100 Myth Power, summon an impenetrable aegis granting 3,500 Shield and +30% Defense."
    )

    val ARES = HeroDefinition(
        id = "hero_ares",
        name = "Ares",
        title = "God of War & Savage Strife",
        faction = "Greek / Olympus",
        rarity = CardRarity.LEGENDARY,
        combatIdentity = "Frenzied assault • Bleed pressure • Berserker rage",
        baseHp = 9800,
        baseAttack = 3400,
        baseDefense = 2100,
        portraitResId = R.drawable.img_enemy_hero,
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Aggressive Damage",
        unlockCondition = "Defeat Ares in Stage 1-1: Trial of Ares",
        passiveName = "BLOODLUST",
        passiveDescription = "Attacks deal +20% bonus damage when the enemy has no active Shield.",
        ultimateName = "GOD OF WAR",
        ultimateDescription = "At 100 Myth Power, unleash a bloodthirsty onslaught dealing 4,600 damage and restoring 15% life."
    )

    val MEDUSA = HeroDefinition(
        id = "hero_medusa",
        name = "Medusa",
        title = "Gorgon Queen of the Abyssal Den",
        faction = "Greek / Underworld",
        rarity = CardRarity.EPIC,
        combatIdentity = "Petrifying gaze • Toxic venom • Debuff suppression",
        baseHp = 9200,
        baseAttack = 2600,
        baseDefense = 2400,
        portraitResId = R.drawable.img_enemy_hero,
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Control / Debuff",
        unlockCondition = "Clear World 4 Underworld Stage 4-3",
        passiveName = "PETRIFYING GAZE",
        passiveDescription = "When attacked, 25% chance to inflict Stun and Vulnerable on the attacker.",
        ultimateName = "STONE CURSE",
        ultimateDescription = "At 100 Myth Power, gaze upon the enemy dealing 3,800 damage and stunning for 1 turn."
    )

    val HADES = HeroDefinition(
        id = "hero_hades",
        name = "Hades",
        title = "Sovereign of the Underworld & Shadows",
        faction = "Greek / Underworld",
        rarity = CardRarity.MYTHIC,
        combatIdentity = "Abyssal siphon • Life drain • Soul reap",
        baseHp = 11200,
        baseAttack = 2800,
        baseDefense = 2700,
        portraitResId = R.drawable.img_enemy_hero,
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Drain / Sustain",
        unlockCondition = "Complete World 4: Underworld",
        passiveName = "LORD OF THE UNDERWORLD",
        passiveDescription = "Heals for 20% of all damage dealt by cards and basic strikes.",
        ultimateName = "UNDERWORLD JUDGMENT",
        ultimateDescription = "At 100 Myth Power, drain 4,400 HP from the enemy and reinforce yourself with 2,000 Shield."
    )

    val THOR = HeroDefinition(
        id = "hero_thor",
        name = "Thor",
        title = "God of Thunder • Champion of Asgard",
        faction = "Norse / Asgard",
        rarity = CardRarity.LEGENDARY,
        combatIdentity = "Hammer shockwaves • Storm surges • Frenzied burst",
        baseHp = 10800,
        baseAttack = 3300,
        baseDefense = 2400,
        portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Heavy Burst",
        unlockCondition = "Clear World 2 Valhalla Stage 2-3",
        passiveName = "THUNDERBORN",
        passiveDescription = "Gain +15% Attack power and +5 Myth Power whenever an Attack or Relic card is played.",
        ultimateName = "MJOLNIR'S WRATH",
        ultimateDescription = "At 100 Myth Power, smash the ground with Mjolnir dealing 4,800 crushing thunder damage."
    )

    val LOKI = HeroDefinition(
        id = "hero_loki",
        name = "Loki",
        title = "God of Mischief & Chaos",
        faction = "Norse / Asgard",
        rarity = CardRarity.RARE,
        combatIdentity = "Decoys & illusions • Trickster traps • Chaos rift",
        baseHp = 8800,
        baseAttack = 3000,
        baseDefense = 2000,
        portraitResId = R.drawable.img_enemy_hero,
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Trick / Control",
        unlockCondition = "Clear World 2 Valhalla Stage 2-1",
        passiveName = "DECEPTION",
        passiveDescription = "Evades 20% of incoming physical damage and reflects 500 damage back to attacker.",
        ultimateName = "CHAOS UNLEASHED",
        ultimateDescription = "At 100 Myth Power, sow total discord dealing 3,600 chaos damage and inflicting Weaken & Vulnerable."
    )

    val ANUBIS = HeroDefinition(
        id = "hero_anubis",
        name = "Anubis",
        title = "Lord of the Sacred Land • Weigher of Souls",
        faction = "Egyptian / Duat",
        rarity = CardRarity.EPIC,
        combatIdentity = "Soul weighing • Execution • Tomb curses",
        baseHp = 9600,
        baseAttack = 2900,
        baseDefense = 2500,
        portraitResId = R.drawable.img_enemy_hero,
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Execution / Death Control",
        unlockCondition = "Complete World 3: Kingdom of Egypt",
        passiveName = "WEIGHER OF SOULS",
        passiveDescription = "Deals +50% amplified damage against enemies whose HP is below 35%.",
        ultimateName = "JUDGMENT OF THE DEAD",
        ultimateDescription = "At 100 Myth Power, weigh the foe's heart dealing 4,200 damage. Instantly executes targets below 20% HP."
    )

    val ACHILLES = HeroDefinition(
        id = "hero_achilles",
        name = "Achilles",
        title = "Hero of the Trojan War",
        faction = "Greek / Myrmidons",
        rarity = CardRarity.LEGENDARY,
        combatIdentity = "Invulnerability stance • Piercing lance • Critical strikes",
        baseHp = 9500,
        baseAttack = 3100,
        baseDefense = 2200,
        portraitResId = R.drawable.img_enemy_hero,
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Critical / Pierce",
        unlockCondition = "Clear Stage 1-3: Champion's Trial",
        passiveName = "RIVER STYX BLESSING",
        passiveDescription = "Immune to critical strikes and absorbs 20% incoming damage.",
        ultimateName = "SPEAR OF PELION",
        ultimateDescription = "At 100 Myth Power, pierces all enemy armor and deals 4,800 true damage."
    )

    val MERLIN = HeroDefinition(
        id = "hero_merlin",
        name = "Merlin",
        title = "Archmage of Avalon",
        faction = "Celtic / Arthurian",
        rarity = CardRarity.MYTHIC,
        combatIdentity = "Spell weaving • Time manipulation • Arcane shields",
        baseHp = 8800,
        baseAttack = 3300,
        baseDefense = 2000,
        portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
        isPlayableInPrototype = true,
        releaseStatus = "AVAILABLE",
        role = "Spell Weaving / Control",
        unlockCondition = "Reach Player Level 5",
        passiveName = "ARCANE RESONANCE",
        passiveDescription = "Every spell card played generates +5 bonus Myth Power.",
        ultimateName = "TIME DILATION",
        ultimateDescription = "At 100 Myth Power, freezes enemy turn meter and grants 3 bonus draw cards."
    )

    /**
     * All playable and selectable heroes for HeroSelectionScreen.
     */
    val SELECTION_HEROES: List<HeroDefinition> = listOf(
        HERCULES,
        ZEUS,
        ATHENA,
        ARES,
        MEDUSA,
        HADES,
        THOR,
        LOKI,
        ANUBIS,
        ACHILLES,
        MERLIN
    )

    val UPCOMING_HEROES: List<HeroDefinition> = emptyList()

    val ALL_HEROES: List<HeroDefinition> = SELECTION_HEROES

    private val heroMap: Map<String, HeroDefinition> = ALL_HEROES.associateBy { it.id }

    fun findHero(heroId: String): HeroDefinition? {
        val canonical = when (heroId) {
            "hercules", HerculesIdentity.HERO_ID -> HERCULES
            "ares", "hero_ares" -> ARES
            "athena", "hero_athena" -> ATHENA
            "zeus", "hero_zeus" -> ZEUS
            "hades", "hero_hades" -> HADES
            "thor", "hero_thor" -> THOR
            "loki", "hero_loki" -> LOKI
            "medusa", "hero_medusa" -> MEDUSA
            "anubis", "hero_anubis" -> ANUBIS
            "achilles", "hero_achilles" -> ACHILLES
            "merlin", "hero_merlin" -> MERLIN
            else -> heroMap[heroId]
        }
        return canonical ?: HERCULES
    }

    fun getHercules(): HeroDefinition = HERCULES
}
