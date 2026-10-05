/**
 * Firestore layout. Everything below is written ONLY by Cloud Functions (Admin SDK); firestore.rules
 * denies all client writes. Clients may read their own players/{uid}/** subtree and a few public
 * read-only collections (see firestore.rules).
 */
export const P = {
  profile: (uid: string) => `players/${uid}`,
  collection: (uid: string) => `players/${uid}/collection/state`,
  arena: (uid: string) => `players/${uid}/arena/state`,
  arenaMatches: (uid: string) => `players/${uid}/arenaMatches`,
  arenaMatch: (uid: string, matchId: string) => `players/${uid}/arenaMatches/${matchId}`,
  arenaSeason: (uid: string, seasonId: string) => `players/${uid}/arenaSeasons/${seasonId}`,
  rewards: (uid: string) => `players/${uid}/rewards`,
  reward: (uid: string, claimId: string) => `players/${uid}/rewards/${claimId}`,
  endgame: (uid: string) => `players/${uid}/endgame/state`,
  event: (uid: string, eventId: string) => `players/${uid}/events/${eventId}`,
  alliancePointer: (uid: string) => `players/${uid}/alliance/state`,

  session: (sessionId: string) => `gameSessions/${sessionId}`,

  leaderboard: (seasonId: string) => `leaderboards/${seasonId}/entries`,
  leaderboardEntry: (seasonId: string, uid: string) => `leaderboards/${seasonId}/entries/${uid}`,

  alliance: (id: string) => `alliances/${id}`,
  allianceMembers: (id: string) => `alliances/${id}/members`,
  allianceMember: (id: string, uid: string) => `alliances/${id}/members/${uid}`,
  allianceName: (nameLower: string) => `allianceNames/${nameLower}`,
  alliances: () => "alliances",
  allianceRaid: (allianceId: string, raidId: string) => `allianceRaids/${allianceId}__${raidId}`,

  boss: (bossId: string) => `worldBosses/${bossId}`,
  bossContribution: (bossId: string, uid: string) => `worldBosses/${bossId}/contributions/${uid}`,
};
