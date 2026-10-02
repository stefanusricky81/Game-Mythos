package com.example.data

import com.example.R

/**
 * Enemy archetypes and behavior styles.
 */
enum class EnemyBehaviorType {
    AGGRESSIVE_MELEE,
    REGENERATION,
    CONTROL_DEBUFF,
    MULTI_HIT,
    HEAVY_SMASH,
    DEFENSIVE_RETALIATION,
    EXECUTE_BURST,
    CURSE_DRAIN
}

/**
 * Definitive Enemy Model representing non-boss and elite mythic adversaries (Requirement #6).
 */
data class EnemyDefinition(
    val id: String,
    val name: String,
    val title: String,
    val faction: String,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val behaviorType: EnemyBehaviorType,
    val description: String,
    val specialAbilityName: String,
    val specialAbilityDescription: String,
    val portraitResId: Int = R.drawable.img_enemy_hero,
    val rewardGold: Int = 1000,
    val rewardXp: Int = 1500
) {
    fun toBattleHero(): Hero {
        return Hero(
            id = id,
            name = name,
            title = title,
            currentHp = hp,
            maxHp = hp,
            baseAttack = attack,
            baseDefense = defense,
            portraitResId = portraitResId
        )
    }
}

/**
 * Centralized Enemy Catalog (Requirement #6, #13).
 */
object EnemyCatalog {

    val MINOTAUR = EnemyDefinition(
        id = "enemy_minotaur",
        name = "Minotaur",
        title = "Beast of the Labyrinth",
        faction = "Greek / Crete",
        hp = 11000,
        attack = 2600,
        defense = 2000,
        behaviorType = EnemyBehaviorType.AGGRESSIVE_MELEE,
        description = "Bovine horror that charges with relentless, reckless aggression.",
        specialAbilityName = "Labyrinth Gore",
        specialAbilityDescription = "Charges headlong, dealing massive physical damage.",
        rewardGold = 1200,
        rewardXp = 1800
    )

    val HYDRA = EnemyDefinition(
        id = "enemy_hydra",
        name = "Lernaean Hydra",
        title = "Serpent of the Stagnant Swamps",
        faction = "Greek / Swamps",
        hp = 13000,
        attack = 2300,
        defense = 2200,
        behaviorType = EnemyBehaviorType.REGENERATION,
        description = "Multi-headed terror that regenerates severed flesh each round.",
        specialAbilityName = "Hydra Regrowth",
        specialAbilityDescription = "Heals 800 HP at the start of each turn and spits corrosive venom.",
        rewardGold = 1400,
        rewardXp = 2100
    )

    val MEDUSA = EnemyDefinition(
        id = "enemy_medusa",
        name = "Medusa",
        title = "Gorgon Queen",
        faction = "Greek / Underworld",
        hp = 9500,
        attack = 2700,
        defense = 2100,
        behaviorType = EnemyBehaviorType.CONTROL_DEBUFF,
        description = "Cursed priestess whose gaze petrifies the blood in mortal veins.",
        specialAbilityName = "Stone Glare",
        specialAbilityDescription = "Inflicts Stun and Vulnerable, neutralizing player actions.",
        rewardGold = 1500,
        rewardXp = 2200
    )

    val CERBERUS = EnemyDefinition(
        id = "enemy_cerberus",
        name = "Cerberus",
        title = "Hound of Tartarus",
        faction = "Greek / Underworld",
        hp = 12000,
        attack = 2800,
        defense = 2400,
        behaviorType = EnemyBehaviorType.MULTI_HIT,
        description = "Three ravenous fanged maws that rip into armor simultaneously.",
        specialAbilityName = "Triple Rend",
        specialAbilityDescription = "Strikes multiple times in a single furious turn.",
        rewardGold = 1600,
        rewardXp = 2400
    )

    val CYCLOPS = EnemyDefinition(
        id = "enemy_cyclops",
        name = "Cyclops",
        title = "Brute of Mount Etna",
        faction = "Greek / Olympus",
        hp = 14000,
        attack = 3000,
        defense = 1800,
        behaviorType = EnemyBehaviorType.HEAVY_SMASH,
        description = "One-eyed giant wielding heavy boulders that pulverize divine shields.",
        specialAbilityName = "Colossal Crush",
        specialAbilityDescription = "Slow wind-up followed by an earth-shattering blow.",
        rewardGold = 1300,
        rewardXp = 2000
    )

    val SPARTAN_CHAMPION = EnemyDefinition(
        id = "enemy_spartan_champion",
        name = "Spartan Champion",
        title = "Vanguard of the 300",
        faction = "Greek / Sparta",
        hp = 10500,
        attack = 2500,
        defense = 2800,
        behaviorType = EnemyBehaviorType.DEFENSIVE_RETALIATION,
        description = "Disciplined master of the bronze shield who retaliates against every strike.",
        specialAbilityName = "Phalanx Counter",
        specialAbilityDescription = "Raises high-defense shield and counters incoming damage.",
        rewardGold = 1100,
        rewardXp = 1700
    )

    val VALKYRIE = EnemyDefinition(
        id = "enemy_valkyrie",
        name = "Valkyrie",
        title = "Chooser of the Slain",
        faction = "Norse / Valhalla",
        hp = 10000,
        attack = 2900,
        defense = 2500,
        behaviorType = EnemyBehaviorType.EXECUTE_BURST,
        description = "Asgardian shieldmaiden who delivers righteous judgment to the dying.",
        specialAbilityName = "Swan Dive Execute",
        specialAbilityDescription = "Deals +50% damage when target is below 35% health.",
        rewardGold = 1500,
        rewardXp = 2300
    )

    val FROST_GIANT = EnemyDefinition(
        id = "enemy_frost_giant",
        name = "Frost Giant",
        title = "Warrior of Jotunheim",
        faction = "Norse / Jotunheim",
        hp = 15000,
        attack = 3200,
        defense = 2200,
        behaviorType = EnemyBehaviorType.HEAVY_SMASH,
        description = "Ancient glacial colossus swinging frozen rime pillars.",
        specialAbilityName = "Glacial Slam",
        specialAbilityDescription = "Heavy cold damage that chills and slows energy recovery.",
        rewardGold = 1700,
        rewardXp = 2500
    )

    val EGYPTIAN_GUARDIAN = EnemyDefinition(
        id = "enemy_egyptian_guardian",
        name = "Egyptian Guardian",
        title = "Sentinel of the Pharaoh's Tomb",
        faction = "Egyptian / Duat",
        hp = 11500,
        attack = 2700,
        defense = 2600,
        behaviorType = EnemyBehaviorType.CURSE_DRAIN,
        description = "Golden ankh warrior channeling the eternal curses of the Duat.",
        specialAbilityName = "Ankh Soul Siphon",
        specialAbilityDescription = "Drains life and inflicts long-lasting debilitating curses.",
        rewardGold = 1600,
        rewardXp = 2400
    )

    val ALL_ENEMIES: List<EnemyDefinition> = listOf(
        MINOTAUR,
        HYDRA,
        MEDUSA,
        CERBERUS,
        CYCLOPS,
        SPARTAN_CHAMPION,
        VALKYRIE,
        FROST_GIANT,
        EGYPTIAN_GUARDIAN
    )

    private val enemyMap: Map<String, EnemyDefinition> = ALL_ENEMIES.associateBy { it.id }

    fun findEnemy(enemyId: String): EnemyDefinition? = enemyMap[enemyId]
}
