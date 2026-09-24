package com.example.data

import androidx.annotation.DrawableRes
import com.example.R

/**
 * Supported mythological character factions in MYTHOS.
 */
enum class CharacterFaction(val displayName: String) {
    OLYMPUS("Greek / Olympus"),
    UNDERWORLD("Greek / Underworld"),
    ATLANTIS("Atlantis"),
    TITAN("Titans")
}

/**
 * Status of an asset in the production asset pipeline.
 */
enum class AssetStatus(val label: String) {
    PRODUCTION("Production"),
    PLACEHOLDER("Placeholder")
}

/**
 * Production Character Asset definition.
 * Couples:
 * 1. Asset key and project relative path in assets/heroes/<hero>/...
 * 2. Android drawable resource ID for high-performance Jetpack Compose rendering
 * 3. Guaranteed fallback resource ID (graceful degradation prevents crashes)
 * 4. Pipeline production status
 */
data class CharacterAsset(
    val assetKey: String,
    val assetPath: String,
    @DrawableRes val resId: Int,
    @DrawableRes val fallbackResId: Int,
    val status: AssetStatus = AssetStatus.PRODUCTION
) {
    /**
     * Resolves the effective drawable resource ID.
     * Guarantees fallback if the primary asset is 0 or missing.
     */
    @DrawableRes
    fun resolveResId(): Int {
        return if (resId != 0) resId else fallbackResId
    }
}

/**
 * Universal Hero Asset Registry:
 * Allows individual assets to be replaced without modifying gameplay logic.
 * Every hero in MYTHOS implements this exact contract.
 */
data class CharacterAssetRegistry(
    val heroId: String,
    val fullBody: CharacterAsset,
    val battle: CharacterAsset,
    val portrait: CharacterAsset,
    val cardArt: CharacterAsset,
    val avatar: CharacterAsset,
    val lastStand: CharacterAsset,
    val twelveLabors: CharacterAsset,
    val victory: CharacterAsset,
    val defeat: CharacterAsset,
    val designReference: CharacterAsset
) {
    /**
     * List of all assets in this registry for inspection/debugging.
     */
    val allAssets: List<CharacterAsset>
        get() = listOf(
            fullBody,
            battle,
            portrait,
            cardArt,
            avatar,
            lastStand,
            twelveLabors,
            victory,
            defeat,
            designReference
        )
}

/**
 * Canonical Character Identity definition.
 * Centralized, non-duplicated metadata contract for MYTHOS heroes.
 */
data class CharacterIdentity(
    val heroId: String,
    val name: String,
    val title: String,
    val faction: CharacterFaction,
    val rarity: CardRarity,
    val loreDescription: String,
    val assetRegistry: CharacterAssetRegistry
)

/**
 * Canonical Hercules Asset Registry (HerculesAssets):
 * Centralized registry for all Hercules assets.
 * Individual assets can be updated or swapped without modifying gameplay logic.
 */
object HerculesAssets {
    private const val ASSET_BASE = "heroes/hercules"
    val DEFAULT_FALLBACK_RES: Int = R.drawable.img_hercules_hero

    val fullBody = CharacterAsset(
        assetKey = "hercules_fullbody",
        assetPath = "$ASSET_BASE/character/hercules_fullbody.jpg",
        resId = R.drawable.img_hercules_fullbody,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val battle = CharacterAsset(
        assetKey = "hercules_battle",
        assetPath = "$ASSET_BASE/character/hercules_battle.jpg",
        resId = R.drawable.img_hercules_battle,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val portrait = CharacterAsset(
        assetKey = "hercules_portrait",
        assetPath = "$ASSET_BASE/portrait/hercules_portrait.jpg",
        resId = R.drawable.img_hercules_portrait,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val cardArt = CharacterAsset(
        assetKey = "hercules_card_art",
        assetPath = "$ASSET_BASE/card/hercules_card_art.jpg",
        resId = R.drawable.img_hercules_card_art,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val avatar = CharacterAsset(
        assetKey = "hercules_avatar",
        assetPath = "$ASSET_BASE/avatar/hercules_avatar.jpg",
        resId = R.drawable.img_hercules_avatar,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val lastStand = CharacterAsset(
        assetKey = "hercules_last_stand",
        assetPath = "$ASSET_BASE/skills/hercules_last_stand.jpg",
        resId = R.drawable.img_hercules_battle,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val twelveLabors = CharacterAsset(
        assetKey = "hercules_twelve_labors",
        assetPath = "$ASSET_BASE/skills/hercules_twelve_labors.jpg",
        resId = R.drawable.img_twelve_labors,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val victory = CharacterAsset(
        assetKey = "hercules_victory",
        assetPath = "$ASSET_BASE/poses/hercules_victory.jpg",
        resId = R.drawable.img_hercules_portrait,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PRODUCTION
    )

    val defeat = CharacterAsset(
        assetKey = "hercules_defeat",
        assetPath = "$ASSET_BASE/poses/hercules_defeat.jpg",
        resId = R.drawable.img_hercules_hero,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PLACEHOLDER
    )

    val designReference = CharacterAsset(
        assetKey = "hercules_design_reference",
        assetPath = "$ASSET_BASE/reference/hercules_design_reference.jpg",
        resId = R.drawable.img_hercules_hero,
        fallbackResId = DEFAULT_FALLBACK_RES,
        status = AssetStatus.PLACEHOLDER
    )

    val registry = CharacterAssetRegistry(
        heroId = "hero_hercules",
        fullBody = fullBody,
        battle = battle,
        portrait = portrait,
        cardArt = cardArt,
        avatar = avatar,
        lastStand = lastStand,
        twelveLabors = twelveLabors,
        victory = victory,
        defeat = defeat,
        designReference = designReference
    )
}

/**
 * CANONICAL HERCULES CHARACTER IDENTITY:
 *
 * hero_id: hero_hercules
 * name: Hercules
 * title: Champion of Olympus
 * faction: Greek / Olympus
 * rarity: LEGENDARY
 *
 * This identity is referenced by:
 * - Battle Screen & Battlefield Hero presentation
 * - HeroStatusPanel
 * - Hercules Hero Cards (Deck & CardFrame)
 * - Avatar
 * - Victory Screen
 * - Defeat Screen
 * - Skills (Twelve Labors, Last Stand)
 * - Hero Collection and Deck Inspection
 */
object HerculesIdentity {
    const val HERO_ID: String = "hero_hercules"
    const val NAME: String = "Hercules"
    const val TITLE: String = "Champion of Olympus"
    val FACTION: CharacterFaction = CharacterFaction.OLYMPUS
    val RARITY: CardRarity = CardRarity.LEGENDARY

    val assets: CharacterAssetRegistry = HerculesAssets.registry

    val definition = CharacterIdentity(
        heroId = HERO_ID,
        name = NAME,
        title = TITLE,
        faction = FACTION,
        rarity = RARITY,
        loreDescription = "Son of Zeus and mortal Alcmene, renowned across Greece for superhuman strength, unwavering resolve, and triumph over the Twelve Labors.",
        assetRegistry = assets
    )
}
