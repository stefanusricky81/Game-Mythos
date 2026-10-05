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

## Deployment

Project: `mythos-game-a8b8c` (project number 45065573868, from `.firebaserc` and `app/google-services.json`).
Region: `asia-southeast1` for every function. The Firebase CLI account that can see this project is the Mythos
owner account; pass it explicitly (`--account <owner>`) instead of switching the machine-wide login.

**Status (dry run, 2026-10-05): Firestore rules compile successfully against the project, but Cloud Functions
cannot be prepared because the project has no open billing account (Blaze plan required — Cloud Build and
Artifact Registry cannot be enabled). Nothing has been deployed.**

### Prerequisites (all still open)

1. **Upgrade `mythos-game-a8b8c` to the Blaze plan** (billing account must be open).
2. **Runtime service account.** The Phase 12 functions use the default compute service account. It must be able to use
   Firestore (`roles/datastore.user`); new projects do not always grant this. Do NOT reuse the payment service
   account `mythos-play-verifier` for gameplay (it is deliberately limited to payment verification and has Play
   financial-data access). Either grant the default account `roles/datastore.user`, or create a dedicated
   `mythos-game-backend` service account and add `serviceAccount:` to `onCall(...)` in
   `functions/src/game/callables.ts`.
3. Decide `ALLOW_COLLECTION_ATTESTATION` (gap 2 above).

### Deploy ONLY the Phase 12 functions (never `--only functions`)

`functions/src/index.ts` exports the payment functions and the Phase 12 functions from the same codebase, so a plain
`--only functions` would also deploy the payment functions. Name the Phase 12 functions explicitly. No `--force`
(it deletes functions missing from the source):

```
firebase deploy --project mythos-game-a8b8c --account <owner-account> --non-interactive --only \
functions:ensurePlayerProfile,functions:validateDeck,functions:syncCollection,functions:startArenaMatch,\
functions:submitArenaResult,functions:claimArenaReward,functions:getArenaState,functions:getLeaderboard,\
functions:allianceAction,functions:syncAlliance,functions:startEndgameAttempt,functions:submitRaidContribution,\
functions:submitWorldBossContribution,functions:claimWorldBossReward,functions:claimEventReward,\
functions:getEndgameState,functions:getPendingRewards,functions:ackRewards,firestore:rules
```

Add `--dry-run` first. Indexes: none are required (every query is single-field), so there is nothing to deploy for
`firestore:indexes`.

### App Check (off by default — do not enforce yet)

Server: `ENFORCE_APP_CHECK=true` in `functions/.env.mythos-game-a8b8c`, then redeploy the Phase 12 functions. Absent
or any other value = not enforced. (The payment functions hard-code `enforceAppCheck: true` independently and are
not affected by this flag.)

Client: `FirebaseCallableTransport` installs Play Integrity (release) / Debug (debug) before the first call.

Before enforcing, all of these must be true:

- Play Integrity API enabled for the Cloud project and linked in Play Console.
- The Android app registered under Firebase → App Check with the **Play Integrity** provider using the *app signing*
  key SHA-256 (`SETUP_PLAY_BILLING.md` records this as done; it cannot be verified from the CLI).
- Debug tokens registered for every sideloaded debug build used for testing (the Debug provider is not trusted
  otherwise).
- A **release build installed from Play (internal testing)** has been exercised in ONLINE mode with App Check metrics
  showing a verified-request ratio near 100%.

Without enforcement the callables are reachable by anyone who can obtain an anonymous Firebase token; the per-uid
rate limits bound a single account but not account creation. Enforce before public release.

## Backend mode stays LOCAL until deployed and tested

`BackendConfig.MODE` stays `LOCAL_DEVELOPMENT`. To run an E2E build on a device, flip it locally (do not commit),
copy `app/google-services.json` (git-ignored) into the module, and build a debug APK.

## E2E

### Automated (emulators, no production touched)

```
cd e2e && npm install && npm test
```
Real Firebase client SDK against the Auth + Functions + Firestore emulators (`firebase.emulators.json` is a
dedicated config; `firebase.json` is untouched). Covers: unauthenticated rejection, anonymous-auth uid derived from
the auth context, payload-uid spoof rejection, unknown/forged field rejection (reward, rating), fabricated and
foreign match ids, request-id idempotency, the minimum-battle-time gate, server rating/reward, result replay (a
replay cannot flip an outcome), leaderboard, reward outbox pending/ack, raid unlock + alliance contribution, World
Boss damage cap and claim gating, event shop (wallet, unknown item, duplicate request), and alliance
create/join/promote/demote/transfer/leave.

### Manual, after deployment (device build, ONLINE mode, license-tester account)

Identity
- [ ] First launch signs in anonymously; the Firestore `players/{uid}` document key equals the Firebase Auth uid.
- [ ] Kill and relaunch the app: same uid, same profile.
- [ ] A call with a hand-edited payload `uid` is refused (`permission-denied`).

Arena
- [ ] Find Match: attempts drop 5 → 4 on the server, a `gameSessions/arena_*` document exists, opponent comes from the server.
- [ ] Cancel and Find Match again: the same match resumes, attempts stay 4.
- [ ] Win: rating/rewards on the result screen equal what the server returned; gold/XP arrive once (outbox PENDING → APPLIED).
- [ ] Airplane mode at game over: the "offline" message appears, nothing is granted; reconnect, relaunch: result delivered once, reward applied once.
- [ ] Leaderboard shows the player at the server-computed rank.

Alliance
- [ ] Create, join (second device), promote, demote, kick, transfer leadership, leave; the 21st join is refused.

Endgame
- [ ] World Boss: attempt count, shared HP drops, claim refused until the boss is defeated/ended, claim once.
- [ ] Raid: needs an alliance, stage 2 locked until stage 1 clears, 3 attempts/day.
- [ ] Event shop: server token wallet, purchase limit, duplicate tap does not double-spend.

Security (script against the deployed project with a real anonymous token)
- [ ] No token → `unauthenticated`; extra field (`gold`, `opponentRating`, `reward`) → `invalid-argument`.
- [ ] Re-sending a `requestId` returns the original result, not a second grant.
- [ ] Client SDK write to `players/{uid}`, `purchases`, `gameSessions`, `idempotency`, `rateLimits` is refused.

Payment (read-only check)
- [ ] `sha256sum -c` of the payment file list is unchanged and `PaymentSecurityTest` / `MonetizationUnitTest` pass.

## Other production items

- Enable Firestore **TTL** on `idempotency.expiresAt` and `rateLimits.expiresAt`; consider a retention policy for `gameSessions`.
- Alerting on function error rate and `resource-exhausted` spikes.
- Keep `functions/src/game/gameCatalog.json` in sync: `UPDATE_GAME_CATALOG=1 ./gradlew test --tests "*GameCatalogParityTest*"`
  regenerates it from the Kotlin catalogs, and the same test fails the build if it drifts.

## Tests

```
./gradlew test                       # Android (needs JDK 21+ for Robolectric on SDK 36)
cd functions && npm test             # server logic + API contract (in-memory Firestore double)
cd functions && npm run build
cd rules-tests && npm install && npm test   # Firestore rules against the emulator (needs JDK 21+)
cd e2e && npm install && npm test           # client SDK -> Auth/Functions/Firestore emulators (needs JDK 21+)
# production Firestore adapter against the emulator:
firebase emulators:exec --only firestore --project demo-mythos "cd functions && npx jest src/game/firestoreAdapter"
```
