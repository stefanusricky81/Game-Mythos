package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.monetization.PlayerEconomyRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 8 — Alliance, Social, Events & Retention Foundation Unit Tests (Section 10).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AllianceAndRetentionSystemTest {

    private lateinit var context: Context
    private lateinit var repository: PlayerEconomyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mythos_player_economy", Context.MODE_PRIVATE).edit().clear().commit()
        repository = PlayerEconomyRepository()
        repository.initPersistence(context)
        // Ensure starting gold is sufficient for creation tests
        repository.addGold(10_000)
    }

    @Test
    fun test1_allianceCreation() {
        val initialGold = repository.economyState.value.gold
        val result = repository.createAlliance(
            name = "Sons of Olympus",
            emblem = "emblem_gold_eagle",
            description = "Heroes of divine blood unite."
        )

        assertTrue("Alliance creation should succeed", result.isSuccess)
        val created = result.getOrNull()!!
        assertEquals("Sons of Olympus", created.name)
        assertEquals("emblem_gold_eagle", created.emblem)
        assertEquals(1, created.level)
        assertEquals(1, created.members.size)
        assertEquals(AllianceRole.LEADER, created.members[0].role)
        assertEquals("player_local", created.members[0].playerId)

        // Verify gold deducted
        assertEquals(initialGold - Alliance.CREATE_GOLD_COST, repository.economyState.value.gold)

        // Verify PlayerProgress
        val progress = repository.economyState.value.playerProgress
        assertEquals(created.allianceId, progress.allianceId)
        assertEquals("Sons of Olympus", progress.allianceName)
        assertEquals("Leader", progress.allianceRole)
    }

    @Test
    fun test2_allianceJoining() {
        val defaultAlliance = repository.economyState.value.alliances.first()
        val initialMembersCount = defaultAlliance.members.size

        val result = repository.joinAlliance(defaultAlliance.allianceId)
        assertTrue("Joining alliance should succeed", result.isSuccess)

        val updatedAlliance = repository.economyState.value.playerAlliance
        assertNotNull("Player should now belong to an alliance", updatedAlliance)
        assertEquals(initialMembersCount + 1, updatedAlliance!!.members.size)

        val localMember = updatedAlliance.members.find { it.playerId == "player_local" }
        assertNotNull(localMember)
        assertEquals(AllianceRole.MEMBER, localMember!!.role)

        val progress = repository.economyState.value.playerProgress
        assertEquals(defaultAlliance.allianceId, progress.allianceId)
        assertEquals(defaultAlliance.name, progress.allianceName)
        assertEquals("Member", progress.allianceRole)
    }

    @Test
    fun test3_twentyMemberMaximumEnforced() {
        // Construct a full alliance with 20 members
        val fullMembers = (1..20).map { i ->
            AllianceMember(
                playerId = "bot_member_$i",
                name = "Champion $i",
                playerLevel = 5,
                combatPower = 1500,
                role = if (i == 1) AllianceRole.LEADER else AllianceRole.MEMBER
            )
        }
        val fullAlliance = Alliance(
            allianceId = "alliance_full_pantheon",
            name = "Full Pantheon",
            members = fullMembers
        )
        repository.setAllianceForTesting(fullAlliance)

        assertTrue("Alliance must report full when 20 members", fullAlliance.isFull)

        // Attempting to join must fail
        val joinResult = repository.joinAlliance("alliance_full_pantheon")
        assertTrue("Joining a full 20-member alliance must fail", joinResult.isFailure)
        assertTrue(
            joinResult.exceptionOrNull()?.message?.contains("full", ignoreCase = true) == true
        )
    }

    @Test
    fun test4_memberRolesAndPermissions() {
        // Setup alliance where local player is LEADER and has an OFFICER and a MEMBER
        val testAlliance = Alliance(
            allianceId = "alliance_perms_test",
            name = "Permissions Test Alliance",
            members = listOf(
                AllianceMember("player_local", "Leader Player", role = AllianceRole.LEADER),
                AllianceMember("officer_1", "Officer Spartan", role = AllianceRole.OFFICER),
                AllianceMember("member_1", "Cadet Trojan", role = AllianceRole.MEMBER)
            )
        )
        repository.setAllianceForTesting(testAlliance)
        repository.setPlayerAllianceIdForTesting("alliance_perms_test")

        // 1. Leader promotes Cadet Trojan to OFFICER
        val promoteResult = repository.promoteMember("member_1")
        assertTrue("Leader can promote member to officer", promoteResult.isSuccess)
        assertEquals(AllianceRole.OFFICER, repository.economyState.value.playerAlliance!!.members.find { it.playerId == "member_1" }!!.role)

        // 2. Leader demotes Officer Spartan to MEMBER
        val demoteResult = repository.demoteMember("officer_1")
        assertTrue("Leader can demote officer to member", demoteResult.isSuccess)
        assertEquals(AllianceRole.MEMBER, repository.economyState.value.playerAlliance!!.members.find { it.playerId == "officer_1" }!!.role)

        // 3. Leader kicks member
        val kickResult = repository.kickMember("officer_1")
        assertTrue("Leader can kick members", kickResult.isSuccess)
        assertNull(repository.economyState.value.playerAlliance!!.members.find { it.playerId == "officer_1" })
    }

    @Test
    fun test5_leadershipTransfer() {
        val testAlliance = Alliance(
            allianceId = "alliance_transfer_test",
            name = "Transfer Test Alliance",
            members = listOf(
                AllianceMember("player_local", "Old Leader", role = AllianceRole.LEADER),
                AllianceMember("officer_bob", "Bob the Bold", role = AllianceRole.OFFICER)
            )
        )
        repository.setAllianceForTesting(testAlliance)
        repository.setPlayerAllianceIdForTesting("alliance_transfer_test")

        // Transfer leadership to Bob
        val transferResult = repository.transferLeadership("officer_bob")
        assertTrue("Transferring leadership must succeed", transferResult.isSuccess)

        val updated = repository.economyState.value.playerAlliance!!
        assertEquals(AllianceRole.LEADER, updated.members.find { it.playerId == "officer_bob" }!!.role)
        assertEquals(AllianceRole.OFFICER, updated.members.find { it.playerId == "player_local" }!!.role)
        assertEquals("Officer", repository.economyState.value.playerProgress.allianceRole)
    }

    @Test
    fun test6_leavingAlliance() {
        // Player joins alliance as Member
        val alliance = repository.economyState.value.alliances.first()
        repository.joinAlliance(alliance.allianceId)
        assertNotNull(repository.economyState.value.playerAllianceId)

        // Leave alliance
        val leaveResult = repository.leaveAlliance()
        assertTrue("Leaving alliance must succeed", leaveResult.isSuccess)
        assertNull("playerAllianceId must be null after leaving", repository.economyState.value.playerAllianceId)
        assertNull(repository.economyState.value.playerProgress.allianceId)
        assertNull(repository.economyState.value.playerProgress.allianceRole)
    }

    @Test
    fun test7_allianceXpAndLevelProgression() {
        repository.createAlliance("Progression Guild", "emblem_thunderbolt", "Testing XP")
        val currentAlliance = repository.economyState.value.playerAlliance!!
        assertEquals(1, currentAlliance.level)
        assertEquals(0, currentAlliance.xp)

        // Level 1 -> 2 requires 1,000 XP
        repository.addAllianceXp(1_250)
        val updatedAlliance = repository.economyState.value.playerAlliance!!
        assertEquals("Alliance should reach Level 2", 2, updatedAlliance.level)
        assertEquals("Remainder XP should be 250", 250, updatedAlliance.xp)

        // Perk description should match Level 2
        val perk = AllianceProgressionConfig.getPerkDescription(2)
        assertTrue(perk.contains("Battle Gold Bonus", ignoreCase = true))
    }

    @Test
    fun test8_allianceContributionTracking() {
        repository.createAlliance("Contribution Alliance", "emblem_olympus_shield", "Testing scores")

        // Add contribution: 2 battles won, 20,000 damage dealt, 1 campaign clear, 3 quests completed
        repository.addAllianceContribution(
            battlesWon = 2,
            damageDealt = 20_000L,
            campaignClears = 1,
            questsCompleted = 3
        )

        val alliance = repository.economyState.value.playerAlliance!!
        val localMember = alliance.members.find { it.playerId == "player_local" }!!
        assertEquals(2, localMember.battlesWon)
        assertEquals(20_000L, localMember.damageDealt)
        assertEquals(1, localMember.campaignClears)
        assertEquals(3, localMember.questsCompleted)
        assertTrue("Contribution score must be greater than 0", localMember.contributionScore > 0)

        // Player progress should also store total alliance contribution
        assertEquals(localMember.contributionScore, repository.economyState.value.playerProgress.allianceContribution)
    }

    @Test
    fun test9_profilePersistenceWithAlliance() {
        repository.createAlliance("Immortal Legends", "emblem_spartan_helmet", "Persistent Alliance")
        repository.addAllianceContribution(battlesWon = 5, damageDealt = 50_000L)

        val originalId = repository.economyState.value.playerAllianceId
        val originalContrib = repository.economyState.value.playerProgress.allianceContribution

        // Simulate app recreation
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)

        assertEquals("Alliance ID must survive app restart", originalId, newRepo.economyState.value.playerAllianceId)
        assertEquals("Alliance Name must persist", "Immortal Legends", newRepo.economyState.value.playerProgress.allianceName)
        assertEquals("Alliance Role must persist", "Leader", newRepo.economyState.value.playerProgress.allianceRole)
        assertEquals("Contribution score must persist", originalContrib, newRepo.economyState.value.playerProgress.allianceContribution)
    }

    @Test
    fun test10_loginStreakTracking() {
        repository.checkDailyReset("2026-09-20")
        repository.setLoginStreakForTesting(4, "2026-09-20")
        assertEquals(4, repository.economyState.value.playerProgress.loginStreak)

        // Consecutive day advances streak upon claiming daily login reward
        val claimResult = repository.claimDailyLoginReward("2026-09-21")
        assertTrue("Claiming on consecutive day should succeed", claimResult.isSuccess)
        assertEquals("Consecutive day should increase login streak to 5", 5, repository.economyState.value.playerProgress.loginStreak)
        assertTrue(repository.economyState.value.playerProgress.highestLoginStreak >= 5)

        // Restart simulation
        val newRepo = PlayerEconomyRepository()
        newRepo.initPersistence(context)
        assertEquals(5, newRepo.economyState.value.playerProgress.loginStreak)
    }

    @Test
    fun test11_eventActivationDeactivation() {
        val event = EventCatalog.findEvent("event_ares_trial")
        assertNotNull(event)
        assertFalse("event_ares_trial is configured inactive initially", event!!.isActive)

        repository.setEventActiveForTesting("event_ares_trial", true)
        val activeEvent = repository.economyState.value.activeEvents.find { it.eventId == "event_ares_trial" }
        assertNotNull(activeEvent)
        assertTrue("Event should now be active", activeEvent!!.isActive)
    }

    @Test
    fun test12_eventDateBoundaries() {
        val testEvent = MythosEvent(
            eventId = "test_window",
            title = "Test Window Event",
            description = "Valid from Sep 20 to Sep 25",
            startDate = "2026-09-20",
            endDate = "2026-09-25",
            eventType = EventType.DOUBLE_GOLD,
            rewardSummary = "Double Rewards"
        )

        assertTrue("Date 2026-09-20 is within range", testEvent.isWithinDateRange("2026-09-20"))
        assertTrue("Date 2026-09-23 is within range", testEvent.isWithinDateRange("2026-09-23"))
        assertTrue("Date 2026-09-25 is within range", testEvent.isWithinDateRange("2026-09-25"))
        assertFalse("Date 2026-09-19 is before start date", testEvent.isWithinDateRange("2026-09-19"))
        assertFalse("Date 2026-09-26 is after end date", testEvent.isWithinDateRange("2026-09-26"))
    }

    @Test
    fun test13_activeDeckRemainsTwentyCards() {
        val initialDeck = repository.economyState.value.activeDeck
        assertEquals(20, initialDeck.cardIds.size)
        val initialCardList = initialDeck.cardIds.toList()

        // 1. Create Alliance
        repository.createAlliance("Deck Test Alliance", "emblem_lion_nemean", "Testing Deck")
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals(initialCardList, repository.economyState.value.activeDeck.cardIds)

        // 2. Add Contribution
        repository.addAllianceContribution(battlesWon = 1, damageDealt = 10_000L)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals(initialCardList, repository.economyState.value.activeDeck.cardIds)

        // 3. Add Alliance XP
        repository.addAllianceXp(2000)
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals(initialCardList, repository.economyState.value.activeDeck.cardIds)

        // 4. Leave Alliance
        repository.leaveAlliance()
        assertEquals(20, repository.economyState.value.activeDeck.cardIds.size)
        assertEquals(initialCardList, repository.economyState.value.activeDeck.cardIds)
    }
}
