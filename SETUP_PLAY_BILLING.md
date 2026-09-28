# Google Play Billing Setup — MYTHOS

What changed in the code: the shop no longer uses `MockBillingProvider` (which always
"succeeded" after a fake 400ms delay). Release builds now go through real Google Play
Billing (`GooglePlayBillingProvider`), and every purchase is checked against the **Google
Play Developer API on a server** (a Firebase Cloud Function) before the reward is granted.
Debug builds still use the mock provider so you can test the shop UI without spending real
money (`MythosConfig.DEBUG_BUILD`).

None of this can go live until you complete the steps below — they all require your own
Google/Play Console accounts, which the app/agent has no access to.

---

## 0. Confirm the package name (do this first)

`app/build.gradle.kts` has:
```
applicationId = "com.aistudio.mythosbattle.krxwtp"
```
This looks like an auto-generated AI Studio id. **Open Play Console → your app → App content
/ App integrity, and confirm this is exactly the package name already published.** If it's
different, every purchase verification call will fail (Google 404s on package name
mismatch). If Mythos was never actually published under this id, decide the real id now —
it cannot be changed after the first release.

Once confirmed, this same string must appear in **`functions/src/playBilling.ts`**
(`PACKAGE_NAME` constant) — update it there too if it differs.

## 1. Firebase project — DONE (project `mythos-game-a8b8c`)

Created: project, Android app, `app/google-services.json` (gitignored), **Anonymous
Authentication** (leave "Auto clean-up" OFF — it would delete a player's identity, and
their purchase ownership with it), and Firestore (Standard edition, `asia-southeast1`,
production rules = deny all client access).

## 2. App Check (Play Integrity) — DONE

The Android app is registered with the Play Integrity provider using the **app signing
key** SHA-256 from Play Console (Test and release → App integrity). `MythosApplication.kt`
installs the provider at startup. For debug builds, register the debug token printed in
Logcat under App Check → Manage debug tokens.

## 3. Play Developer API access (keyless) — DONE

The Cloud Function runs AS the service account
`mythos-play-verifier@mythos-game-a8b8c.iam.gserviceaccount.com` (see
`RUNTIME_SERVICE_ACCOUNT` in `functions/src/playBilling.ts`). **No JSON key file exists**,
so there is nothing secret to store, lose or leak.

- Google Cloud: "Google Play Android Developer API" enabled; the service account has the
  single role **Cloud Datastore User** (for the Firestore ledger).
- Play Console → Users and permissions: the same account is invited for **Mythos only**
  with View app information, View app quality information and **View financial data**
  (read-only; deliberately NOT "Manage orders and subscriptions").

## 4. (removed)

The old "store a JSON key as a secret" step is obsolete — see step 3.

## 5. Deploy

Cloud Functions require the **Blaze** (pay-as-you-go) plan on the Firebase project. Then:

```bash
cd functions
npm install
npm run build
cd ..
firebase deploy --only functions,firestore:rules
```

`.firebaserc` already points at `mythos-game-a8b8c`.

## 6. Create the in-app products in Play Console

Play Console → your app → Monetize → Products → In-app products → Create product.
**Every one of these must be type "Consumable"** and the **Product ID must match exactly**
(case-sensitive) — the app already ships all this catalog data, so nothing else needs to be
kept in sync manually.

### Myth Gems (6)
| Product ID | Name | Price (set as close as possible) |
|---|---|---|
| `gem_starter` | Gem Starter | Rp9.900 |
| `gem_small` | Gem Small | Rp29.900 |
| `gem_medium` | Gem Medium | Rp59.900 |
| `gem_large` | Gem Large | Rp99.900 |
| `gem_mega` | Gem Mega | Rp199.900 |
| `gem_ultimate` | Gem Ultimate | Rp499.900 |

### Bundles & Passes (5)
| Product ID | Name | Price |
|---|---|---|
| `bundle_starter_pack` | Mythos Starter Pack | Rp19.900 |
| `bundle_legendary_hercules` | Legendary Hercules Hero Bundle | Rp149.900 |
| `bundle_mythic_wrath` | Mythic Pantheon Bundle | Rp299.900 |
| `pass_blessing_olympus` | Blessing of Olympus | Rp49.900 |
| `pass_battle_pass_s1` | Mythos Battle Pass S1 | Rp79.900 |

### Real-money Cosmetics (2)
| Product ID | Name | Price |
|---|---|---|
| `skin_golden_hercules` | Golden Champion Hercules | Rp99.900 |
| `skin_god_of_war_ares` | God of Olympus Ares | Rp199.900 |

(The other 3 cosmetics — `frame_spartan_laurel`, `fx_celestial_lightning`,
`emote_olympian_triumph` — are bought with in-game Myth Gems, not real money, and need no
Play Console product.)

### Direct Card Purchases (12)
| Product ID | Name | Rarity | Price |
|---|---|---|---|
| `c_titans_wrath` | Titan's Wrath | Legendary | Rp99.900 |
| `c_zeus_thunderstone` | Zeus's Thunderstone | Mythic | Rp199.900 |
| `c_phoenix_rebirth` | Phoenix Ash | Mythic | Rp199.900 |
| `c_spartan_phalanx` | Spartan Phalanx | Epic | Rp49.900 |
| `c_divine_challenge` | Divine Challenge | Epic | Rp49.900 |
| `c_divine_aegis` | Divine Aegis of Olympus | Epic | Rp49.900 |
| `c_medusa_gaze` | Gaze of the Gorgon | Epic | Rp49.900 |
| `c_nemean_hide` | Nemean Lion Hide | Rare | Rp24.900 |
| `c_hydra_blade` | Hydra Venom Blade | Rare | Rp24.900 |
| `c_nectar_gods` | Nectar of the Gods | Rare | Rp24.900 |
| `c_athena_blessing` | Athena's Aegis | Rare | Rp24.900 |
| `c_cerberus_bite` | Cerberus Maw | Rare | Rp24.900 |

If you'd rather not manage 12 individual card products, consider removing the "buy this
exact card directly" feature later — that's a product decision, not something the agent
changed.

## 7. Test before releasing

1. Play Console → Setup → License testing → add your own Google account as a license
   tester (lets you "buy" things without being charged for real).
2. Upload a signed build to an **Internal testing** track (real Play Billing does not work
   from a debug/sideloaded build using a test productId the same way — internal testing is
   the closest thing to production).
3. Install from the internal testing link, open the shop, buy something as the license
   tester, confirm:
   - The purchase dialog shows the real Play price.
   - The reward appears in-game.
   - Firebase console → Functions → Logs shows `verifyPlayPurchase` succeeding.
   - Firestore → `playPurchases` collection has a new document (the idempotency guard).
4. Force-close the app mid-purchase once (kill it right after paying, before the success
   dialog) and reopen — confirm the reward still gets granted (this exercises
   `reconcileUnconsumedPurchases`, the code path that protects against exactly this).

## What this does NOT cover (still on you / a separate task)

- **Live Play prices in the shop UI.** The cards/bundles still show the hardcoded
  `Rp...` strings from `MonetizationCatalog.kt`, not the price you actually set in Play
  Console. Keep them in sync manually for now, or ask for the live-price wiring as a
  follow-up — the plumbing point is `GooglePlayBillingProvider`'s product-details query.
- **Blessing of Olympus / Battle Pass daily drip.** The pass purchases grant their upfront
  gems and set a flag, but nothing currently pays out the "50 gems/day for 30 days" part —
  that logic doesn't exist yet anywhere in the codebase.
- **Refunds / chargebacks.** If Google refunds a player, nothing currently revokes what was
  already granted (there's no server-side wallet to revoke from — everything lives in the
  player's local save). Real-time Developer Notifications (RTDN) would be the way to detect
  this later if it becomes a problem.
