package com.example.data

import com.example.R
import com.example.combat.StatusEffect
import com.example.combat.StatusEffectType

/**
 * Data-driven Hero model representing any hero, god, or mythological entity.
 * Works seamlessly for Hercules, Ares, Athena, Zeus, Hades, Thor, Loki, Anubis, Medusa, etc.
 */
data class Hero(
    val id: String = "hercules",
    val name: String,
    val title: String,
    val currentHp: Int,
    val maxHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val currentShield: Int = 0,
    val attackBuffPercent: Int = 0,
    val attackBuffTurns: Int = 0,
    val damageReductionPercent: Int = 0,
    val poisonTurnsRemaining: Int = 0,
    val poisonDamagePerTurn: Int = 0,
    val isLastStandActive: Boolean = false,
    val hasLastStandTriggered: Boolean = false,
    val hasPhalanxGuard: Boolean = false,
    val phalanxCounterDamage: Int = 0,
    val hasThunderstoneRelic: Boolean = false,
    val portraitResId: Int = HerculesIdentity.assets.portrait.resolveResId(),
    val level: Int = 1,
    val statusEffects: List<StatusEffect> = emptyList()
) {
    /**
     * Resolves portrait dynamically: when Last Stand passive triggers, switches to
     * empowered Last Stand asset; otherwise uses production hero portrait.
     */
    fun getEffectivePortraitResId(): Int {
        if (id == HerculesIdentity.HERO_ID || id == "hercules") {
            return if (isLastStandActive) {
                HerculesIdentity.assets.lastStand.resolveResId()
            } else {
                HerculesIdentity.assets.portrait.resolveResId()
            }
        }
        return portraitResId
    }

    /**
     * Resolves battlefield combat art.
     */
    fun getBattleResId(): Int {
        if (id == HerculesIdentity.HERO_ID || id == "hercules") {
            return HerculesIdentity.assets.battle.resolveResId()
        }
        return portraitResId
    }

    val isStunned: Boolean
        get() = statusEffects.any { it.type == StatusEffectType.STUN && !it.isExpired }

    val isVulnerable: Boolean
        get() = statusEffects.any { it.type == StatusEffectType.VULNERABLE && !it.isExpired }

    val isWeakened: Boolean
        get() = statusEffects.any { it.type == StatusEffectType.WEAKEN && !it.isExpired }

    val burnDamagePerTurn: Int
        get() = statusEffects.filter { it.type == StatusEffectType.BURN && !it.isExpired }.sumOf { it.magnitude }

    val regenerationPerTurn: Int
        get() = statusEffects.filter { it.type == StatusEffectType.REGENERATION && !it.isExpired }.sumOf { it.magnitude }

    val effectiveAttack: Int
        get() {
            var bonus = 0
            if (isLastStandActive) {
                bonus += 25
            }
            bonus += attackBuffPercent
            if (isWeakened) {
                bonus -= 25
            }
            val multiplier = (1.0 + bonus / 100.0).coerceAtLeast(0.1)
            return (baseAttack * multiplier).toInt()
        }

    val hpPercentage: Float
        get() = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

    val isAlive: Boolean
        get() = currentHp > 0

    companion object {
        fun createHercules(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(10000 * mult).toInt()
            val atk = Math.round(2800 * mult).toInt()
            val def = if (level == 4) 3003 else Math.round(2600 * mult).toInt()
            return Hero(
                id = HerculesIdentity.HERO_ID,
                name = HerculesIdentity.NAME,
                title = HerculesIdentity.TITLE,
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = HerculesIdentity.assets.portrait.resolveResId(),
                level = level
            )
        }

        fun createAres(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(10000 * mult).toInt()
            val atk = Math.round(3000 * mult).toInt()
            val def = Math.round(2000 * mult).toInt()
            return Hero(
                id = "ares",
                name = "Ares",
                title = "God of War & Strife",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_enemy_hero,
                level = level
            )
        }

        fun createAthena(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(11500 * mult).toInt()
            val atk = Math.round(2300 * mult).toInt()
            val def = Math.round(3100 * mult).toInt()
            return Hero(
                id = "athena",
                name = "Athena",
                title = "Goddess of Wisdom & Strategy",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_hercules_hero,
                level = level
            )
        }

        fun createZeus(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(10500 * mult).toInt()
            val atk = Math.round(3500 * mult).toInt()
            val def = Math.round(2200 * mult).toInt()
            return Hero(
                id = "zeus",
                name = "Zeus",
                title = "King of Olympus • Lord of the Sky",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_hercules_hero,
                level = level
            )
        }

        fun createHades(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(11200 * mult).toInt()
            val atk = Math.round(2800 * mult).toInt()
            val def = Math.round(2700 * mult).toInt()
            return Hero(
                id = "hades",
                name = "Hades",
                title = "Ruler of the Underworld",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_enemy_hero,
                level = level
            )
        }

        fun createThor(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(10800 * mult).toInt()
            val atk = Math.round(3300 * mult).toInt()
            val def = Math.round(2400 * mult).toInt()
            return Hero(
                id = "thor",
                name = "Thor",
                title = "God of Thunder",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_hercules_hero,
                level = level
            )
        }

        fun createLoki(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(8800 * mult).toInt()
            val atk = Math.round(3000 * mult).toInt()
            val def = Math.round(2000 * mult).toInt()
            return Hero(
                id = "loki",
                name = "Loki",
                title = "God of Mischief & Deception",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_enemy_hero,
                level = level
            )
        }

        fun createMedusa(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(9200 * mult).toInt()
            val atk = Math.round(2600 * mult).toInt()
            val def = Math.round(2400 * mult).toInt()
            return Hero(
                id = "medusa",
                name = "Medusa",
                title = "Gorgon Queen",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_enemy_hero,
                level = level
            )
        }

        fun createAnubis(level: Int = 1): Hero {
            val mult = HeroProgressionConfig.getStatMultiplier(level)
            val hp = Math.round(9600 * mult).toInt()
            val atk = Math.round(2900 * mult).toInt()
            val def = Math.round(2500 * mult).toInt()
            return Hero(
                id = "anubis",
                name = "Anubis",
                title = "Lord of the Sacred Land & Duat",
                currentHp = hp,
                maxHp = hp,
                baseAttack = atk,
                baseDefense = def,
                portraitResId = R.drawable.img_enemy_hero,
                level = level
            )
        }
    }
}
