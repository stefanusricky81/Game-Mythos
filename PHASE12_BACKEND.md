# Phase 12 — server-authoritative gameplay backend

Payment/Billing is **not** part of this document and is untouched by Phase 12 (`SETUP_PLAY_BILLING.md`
still governs it). The only things shared with payment are the Firebase project, the region
(`asia-southeast1`) and the anonymous Firebase Auth session (one player = one Firebase UID).

## Modes

`BackendConfig.MODE` (`app/.../backend/BackendConfig.kt`):

| Mode | Behaviour |
|---|---|
| `LOCAL_DEVELOPMENT` (default) | Offline prototype, exactly as before. No Phase 12 network traffic. |
| `ONLINE_AUTHORITATIVE` | Arena, World Boss, Raid, event rewards and Alliance go through the Cloud Functions. Nothing authoritative is granted locally. |

**Do not switch to `ONLINE_AUTHORITATIVE` until the checklist below is done.** The default is `LOCAL_DEVELOPMENT`
because the functions cannot be deployed yet (Firebase Blaze plan).

## What the server decides

| Area | Server-authoritative | Still client-side (explicit limitation) |
|---|---|---|
| Identity | uid comes only from the verified auth context; a payload `uid` that differs is rejected | — |
| Deck | exactly 20 cards, max 2 copies, valid card/hero IDs, owned quantity | see *Card ownership* below |
| Arena | match creation + ID, opponent, attempts (5/day), rating before/after (ELO K=32), win-streak and first-win bonuses, rewards, history, seasons (28 days, soft reset), season rewards, leaderboard, replay protection | **the battle simulation** — the server receives the claimed outcome |
| Alliance | membership, 20-member cap, roles, promote/demote/kick/transfer rules, contribution, alliance XP/level, unique names | the 5,000-gold creation cost (gold lives in the client wallet) |
| World Boss | shared boss HP, per-player contribution, attempts (5/day), session binding, damage caps, tiered rewards, claim state | the damage a battle produced |
| Raid | alliance-membership check, sequential stage unlock, attempts (3/day), server-scaled enemy HP and rewards | whether the stage was really cleared |
| Events | token wallet, event window, per-item purchase limits, duplicate-claim protection | — |
| Rewards | every grant is computed by the server and delivered through an outbox (PENDING → APPLIED) | the local wallet (gold/XP/hero shards) is still applied by the client |

### Residual trust gaps (be honest about these before production)

1. **Battle outcomes are claims, not proof.** Arena/Boss/Raid battles run on the device. The server bounds them
   (session issued to that uid, single use, minimum battle time, damage caps tied to the enemy HP it issued, daily
   attempt caps) but a modified client can still claim `VICTORY` for a session it legitimately started. Closing
   this needs a deterministic server-side battle simulation or replay verification.
2. **Card ownership is partly client-attested.** The server knows the starter set and everything it granted. Cards
   from the offline summon/shop live in the client economy, so `syncCollection` lets the client *attest* them
   (catalog IDs only, never lowers a count). Set `ALLOW_COLLECTION_ATTESTATION=false` for strict mode once
   summons/purchases are granted server-side. Until then a modified client can attest cards it does not own.
3. **Gold / gems / XP wallets are client-side.** Gems are the payment currency and the payment code is frozen.
   The server records and grants *server-origin* currency (event tokens, arena points, card shards, frames,
   cosmetics, titles, cards it grants) and sends gold/XP/hero shards to the client through the reward outbox.
   Card shards, arena points, frames, cosmetics and server-granted cards are held on the server but are **not yet
   mirrored into the local economy** — that needs one new method in `PlayerEconomyRepository` (a payment-hash-frozen
   file), so it was not done.
4. **World Boss is one hot document** (`worldBosses/{id}`). Firestore sustains ~1 write/second per document, so a
   very large concurrent player base needs sharded counters before launch.

## Cloud Functions (region `asia-southeast1`)

`ensurePlayerProfile`, `validateDeck`, `syncCollection`, `startArenaMatch`, `submitArenaResult`,
`claimArenaReward`, `getArenaState`, `getLeaderboard`, `allianceAction`, `syncAlliance`,
`startEndgameAttempt`, `submitRaidContribution`, `submitWorldBossContribution`, `claimWorldBossReward`,
`claimEventReward`, `getEndgameState`, `getPendingRewards`, `ackRewards`.

Every call runs the same pipeline (`functions/src/game/handlers.ts`): authenticated uid → payload-uid check →
server-side rate limit (Firestore, per uid and bucket, in its own transaction so failed requests still count) →
strict schema validation (unknown fields are rejected, never ignored) → state validation inside a Firestore
transaction → idempotent execution (`requestId`, or the match/session id) → explicit success/error status.

Gameplay "days" use Asia/Jakarta (UTC+7) so the reset moment is the same for every player.

## Firestore

All authoritative data is written only by Cloud Functions (Admin SDK). `firestore.rules` contains **no client
write rule at all**.

| Path | Client read | Client write |
|---|---|---|
| `players/{uid}` and every subcollection (profile, collection, arena, arenaMatches, arenaSeasons, rewards, endgame, events, alliance pointer) | owner only | never |
| `leaderboards/{season}/entries/{uid}` | signed-in | never |
| `worldBosses/{id}` | signed-in | never |
| `worldBosses/{id}/contributions/{uid}` | owner only | never |
| `alliances/{id}` | signed-in | never |
| `alliances/{id}/members/{uid}` | members only | never |
| `purchases`, `gameSessions`, `idempotency`, `rateLimits`, `allianceNames`, `allianceRaids`, everything else | never | never |

`purchases` is the payment ledger and stays deny-all.

## Production checklist

1. Upgrade the Firebase project to **Blaze** and deploy: `firebase deploy --only functions,firestore:rules`.
   (Deploying replaces the old `firestore.rules`; the new rules are stricter for every path except the public reads above.)
2. **App Check.** The app registers Play Integrity (release) / Debug (debug) in `FirebaseCallableTransport`. Register the
   app in Firebase → App Check, add debug tokens for development devices, then set
   `ENFORCE_APP_CHECK=true` in `functions/.env.<project-id>` and redeploy. It is off by default so an unregistered
   app is not locked out.
3. Decide `ALLOW_COLLECTION_ATTESTATION` (see gap 2).
4. Enable Firestore **TTL** on `idempotency.expiresAt` and `rateLimits.expiresAt`. Consider a retention policy for `gameSessions`.
5. No composite indexes are required (every query is single-field).
6. Add alerting on function error rate and `resource-exhausted` spikes.
7. Flip `BackendConfig.MODE` to `ONLINE_AUTHORITATIVE`, build, and test against the real project with a license-tester
   account before any public release.
8. Keep `functions/src/game/gameCatalog.json` in sync: `UPDATE_GAME_CATALOG=1 ./gradlew test --tests "*GameCatalogParityTest*"`
   regenerates it from the Kotlin catalogs, and the same test fails the build if it drifts.

## Tests

```
./gradlew test                       # Android (needs JDK 21+ for Robolectric on SDK 36)
cd functions && npm test             # server logic + API contract (in-memory Firestore double)
cd functions && npm run build
cd rules-tests && npm install && npm test   # Firestore rules against the emulator (needs JDK 21+)
```
