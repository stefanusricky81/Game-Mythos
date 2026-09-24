package com.example.data

import com.example.R

/**
 * Data-driven Hero model representing any hero, god, or mythological entity.
 * Works seamlessly for Hercules, Ares, Athena, Zeus, Hades, etc.
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
    val portraitResId: Int = HerculesIdentity.assets.portrait.resolveResId()
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

    val effectiveAttack: Int
        get() {
            var bonus = 0
            if (isLastStandActive) {
                bonus += 25
            }
            bonus += attackBuffPercent
            return (baseAttack * (1.0 + bonus / 100.0)).toInt()
        }

    val hpPercentage: Float
        get() = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

    val isAlive: Boolean
        get() = currentHp > 0

    companion object {
        fun createHercules(): Hero = Hero(
            id = HerculesIdentity.HERO_ID,
            name = HerculesIdentity.NAME,
            title = HerculesIdentity.TITLE,
            currentHp = 10000,
            maxHp = 10000,
            baseAttack = 2800,
            baseDefense = 2600,
            portraitResId = HerculesIdentity.assets.portrait.resolveResId()
        )

        fun createAres(): Hero = Hero(
            id = "ares",
            name = "Ares",
            title = "God of War & Strife",
            currentHp = 10000,
            maxHp = 10000,
            baseAttack = 2400,
            baseDefense = 2200,
            portraitResId = R.drawable.img_enemy_hero
        )

        fun createAthena(): Hero = Hero(
            id = "athena",
            name = "Athena",
            title = "Goddess of Wisdom & Strategy",
            currentHp = 11000,
            maxHp = 11000,
            baseAttack = 2200,
            baseDefense = 3000,
            portraitResId = R.drawable.img_hercules_hero
        )

        fun createZeus(): Hero = Hero(
            id = "zeus",
            name = "Zeus",
            title = "King of the Gods • Lord of the Sky",
            currentHp = 12000,
            maxHp = 12000,
            baseAttack = 3200,
            baseDefense = 2400,
            portraitResId = R.drawable.img_hercules_hero
        )

        fun createHades(): Hero = Hero(
            id = "hades",
            name = "Hades",
            title = "Ruler of the Underworld",
            currentHp = 11500,
            maxHp = 11500,
            baseAttack = 2600,
            baseDefense = 2800,
            portraitResId = R.drawable.img_enemy_hero
        )
    }
}
