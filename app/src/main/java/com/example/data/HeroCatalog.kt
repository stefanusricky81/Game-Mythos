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
    val isPlayableInPrototype: Boolean = false,
    val releaseStatus: String = "AVAILABLE", // "AVAILABLE", "ARCHIVED", "COMING_SOON"
    val maxLevel: Int = 10,
    val defaultDeckCardIds: List<String> = emptyList()
) {
    fun toBattleHero(level: Int = 1): Hero {
        if (id == HerculesIdentity.HERO_ID || id == "hercules") {
            return Hero.createHercules()
        }
        return Hero(
            id = id,
            name = name,
            title = title,
            currentHp = baseHp,
            maxHp = baseHp,
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            portraitResId = portraitResId
        )
    }
}

/**
 * Centralized Hero Catalog (Requirements #3, #17, #18).
 * Single source of truth for Hero metadata and extensible roster.
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
        )
    )

    // Future Hero architecture (extensible roster as required by Section 3)
    val UPCOMING_HEROES: List<HeroDefinition> = listOf(
        HeroDefinition(
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
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_zeus",
            name = "Zeus",
            title = "King of the Olympians",
            faction = "Greek / Olympus",
            rarity = CardRarity.MYTHIC,
            combatIdentity = "Cataclysmic lightning • Area smite • Overwhelming Myth Power",
            baseHp = 12000,
            baseAttack = 3500,
            baseDefense = 2800,
            portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_athena",
            name = "Athena",
            title = "Goddess of Wisdom & Strategy",
            faction = "Greek / Olympus",
            rarity = CardRarity.EPIC,
            combatIdentity = "Defensive bulwark • Tactical counters • Divine buffs",
            baseHp = 11000,
            baseAttack = 2400,
            baseDefense = 3000,
            portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
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
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_thor",
            name = "Thor",
            title = "God of Thunder",
            faction = "Norse / Asgard",
            rarity = CardRarity.LEGENDARY,
            combatIdentity = "Hammer shockwaves • Storm surges • Frenzied resilience",
            baseHp = 10500,
            baseAttack = 3200,
            baseDefense = 2500,
            portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_anubis",
            name = "Anubis",
            title = "Lord of the Sacred Land",
            faction = "Egyptian / Duat",
            rarity = CardRarity.EPIC,
            combatIdentity = "Soul drain • Curse application • Underworld summons",
            baseHp = 9200,
            baseAttack = 2700,
            baseDefense = 2400,
            portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_medusa",
            name = "Medusa",
            title = "Gorgon Queen",
            faction = "Greek / Underworld",
            rarity = CardRarity.EPIC,
            combatIdentity = "Petrifying gaze • Toxic venom • Retaliation",
            baseHp = 9000,
            baseAttack = 2600,
            baseDefense = 2300,
            portraitResId = R.drawable.img_enemy_hero,
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_hades",
            name = "Hades",
            title = "Ruler of the Underworld",
            faction = "Greek / Underworld",
            rarity = CardRarity.MYTHIC,
            combatIdentity = "Abyssal chains • Life siphon • Undying legions",
            baseHp = 11500,
            baseAttack = 3400,
            baseDefense = 2900,
            portraitResId = R.drawable.img_enemy_hero,
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        ),
        HeroDefinition(
            id = "hero_loki",
            name = "Loki",
            title = "God of Mischief",
            faction = "Norse / Asgard",
            rarity = CardRarity.RARE,
            combatIdentity = "Decoys & illusions • Trickster traps • Backstabs",
            baseHp = 8500,
            baseAttack = 2900,
            baseDefense = 2100,
            portraitResId = R.drawable.img_enemy_hero,
            isPlayableInPrototype = false,
            releaseStatus = "COMING_SOON"
        )
    )

    val ALL_HEROES: List<HeroDefinition> = listOf(HERCULES) + UPCOMING_HEROES

    private val heroMap: Map<String, HeroDefinition> = ALL_HEROES.associateBy { it.id }

    fun findHero(heroId: String): HeroDefinition? = heroMap[heroId] ?: if (heroId == "hercules") HERCULES else null

    fun getHercules(): HeroDefinition = HERCULES
}
