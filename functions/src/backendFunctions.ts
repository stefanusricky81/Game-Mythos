import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// 1. createPlayerProfile
export const createPlayerProfile = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const uid = context.auth.uid;
  const playerRef = admin.firestore().collection("players").doc(uid);
  const snap = await playerRef.get();
  if (snap.exists) return snap.data();

  const newProfile = {
    uid,
    playerLevel: 1,
    playerXp: 0,
    gold: 1000,
    mythGems: 100,
    cardShards: 50,
    heroShards: 0,
    arenaPoints: 0,
    lifetimeBattles: 0,
    lifetimeWins: 0,
    lifetimeLosses: 0,
    lifetimeDamage: 0,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  await playerRef.set(newProfile);
  return newProfile;
});

// 2. syncPlayerState
export const syncPlayerState = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const uid = context.auth.uid;
  const playerRef = admin.firestore().collection("players").doc(uid);
  const snap = await playerRef.get();
  if (!snap.exists) {
    throw new functions.https.HttpsError("not-found", "Player profile not found.");
  }
  return { success: true, profile: snap.data() };
});

// 3. validateDeck
export const validateDeck = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const cardIds = data.cardIds || [];
  if (!Array.isArray(cardIds) || cardIds.length !== 20) {
    return { valid: false, error: "ActiveDeck must contain exactly 20 cards." };
  }
  const frequencies: { [key: string]: number } = {};
  for (const c of cardIds) {
    frequencies[c] = (frequencies[c] || 0) + 1;
    if (frequencies[c] > 2) {
      return { valid: false, error: `Card '${c}' exceeds duplicate limit of 2.` };
    }
  }
  return { valid: true, count: 20 };
});

// 4. startArenaMatch
export const startArenaMatch = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const uid = context.auth.uid;
  const arenaRef = admin.firestore().collection("players").doc(uid).collection("arena").doc("state");

  return await admin.firestore().runTransaction(async (t) => {
    const snap = await t.get(arenaRef);
    const state = snap.exists ? snap.data()! : { dailyAttempts: 5, rating: 1000 };
    if ((state.dailyAttempts || 0) <= 0) {
      throw new functions.https.HttpsError("failed-precondition", "No Arena attempts remaining today.");
    }
    const matchId = `arena_match_${Date.now()}_${Math.floor(Math.random() * 10000)}`;
    t.set(arenaRef, {
      ...state,
      dailyAttempts: state.dailyAttempts - 1,
      activeMatchId: matchId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    return { matchId, remainingAttempts: state.dailyAttempts - 1 };
  });
});

// 5. submitArenaResult
export const submitArenaResult = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const uid = context.auth.uid;
  const { matchId, result, opponentRating } = data;
  if (!matchId) throw new functions.https.HttpsError("invalid-argument", "Missing matchId.");

  const arenaRef = admin.firestore().collection("players").doc(uid).collection("arena").doc("state");
  const matchHistoryRef = admin.firestore().collection("players").doc(uid).collection("arenaMatches").doc(matchId);

  return await admin.firestore().runTransaction(async (t) => {
    const historySnap = await t.get(matchHistoryRef);
    if (historySnap.exists) {
      return { success: true, idempotentReplay: true, data: historySnap.data() };
    }

    const arenaSnap = await t.get(arenaRef);
    const arenaState = arenaSnap.exists ? arenaSnap.data()! : { rating: 1000, wins: 0, losses: 0, currentStreak: 0 };
    
    // ELO formula: K = 32
    const currentRating = arenaState.rating || 1000;
    const oppRating = opponentRating || 1000;
    const expected = 1.0 / (1.0 + Math.pow(10, (oppRating - currentRating) / 400.0));
    const actual = result === "VICTORY" ? 1.0 : (result === "DRAW" ? 0.5 : 0.0);
    const delta = Math.round(32 * (actual - expected));
    const newRating = Math.max(0, currentRating + delta);

    const isVictory = result === "VICTORY";
    const rewards = isVictory
      ? { gold: 750, cardShards: 10, arenaPoints: 50, xp: 150 }
      : { gold: 250, cardShards: 5, arenaPoints: 20, xp: 75 };

    t.set(arenaRef, {
      rating: newRating,
      wins: isVictory ? (arenaState.wins || 0) + 1 : (arenaState.wins || 0),
      losses: !isVictory ? (arenaState.losses || 0) + 1 : (arenaState.losses || 0),
      activeMatchId: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    t.set(matchHistoryRef, {
      matchId,
      playerUid: uid,
      result,
      ratingBefore: currentRating,
      ratingAfter: newRating,
      rewards,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, newRating, delta, rewards };
  });
});

// 6. claimArenaReward
export const claimArenaReward = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  return { success: true, claimed: true };
});

// 7. getLeaderboard
export const getLeaderboard = functions.https.onCall(async (data, context) => {
  const snap = await admin.firestore().collection("leaderboards").doc("season_01").collection("entries")
    .orderBy("rating", "desc").limit(50).get();
  const entries = snap.docs.map(d => d.data());
  return { entries };
});

// 8. syncAlliance
export const syncAlliance = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  return { success: true, synced: true };
});

// 9. submitRaidContribution
export const submitRaidContribution = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const { damage } = data;
  if (!damage || damage <= 0) throw new functions.https.HttpsError("invalid-argument", "Positive damage required.");
  return { success: true, damageApplied: damage };
});

// 10. submitWorldBossContribution
export const submitWorldBossContribution = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const { damage } = data;
  if (!damage || damage <= 0 || damage > 500000) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid boss damage amount.");
  }
  return { success: true, recordedDamage: damage };
});

// 11. claimEventReward
export const claimEventReward = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const { eventId, rewardId } = data;
  return { success: true, claimedEventId: eventId, rewardId };
});
