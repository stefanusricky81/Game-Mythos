import { CATALOG } from "./catalog";
import { DocData, GameDb, Tx } from "./db";
import { fail } from "./errors";
import { LIMITS } from "./limits";
import { P } from "./paths";
import { loadPlayer, savePlayer } from "./player";
import { ID_PATTERN, Payload, asPayload, bool, oneOf, reqStr, requestId, str } from "./schema";
import { Caller, idempotent, sha } from "./security";

/**
 * Alliance authority. Membership, the 20-member cap, roles and contribution live ONLY on the server;
 * the caller's identity is the verified auth uid, so a client can never act as another member.
 * Permission rules mirror the offline Phase 8 rules in PlayerEconomyRepository:
 *   promote/demote/transfer: LEADER only · kick: LEADER or OFFICER, never the leader,
 *   an OFFICER cannot kick another OFFICER.
 * Note: the offline "5,000 gold to create" cost is NOT enforced here because gold lives in the client
 * wallet; it is checked client-side only.
 */
export type Role = "LEADER" | "OFFICER" | "MEMBER";
const ACTIONS = ["CREATE", "JOIN", "LEAVE", "PROMOTE", "DEMOTE", "KICK", "TRANSFER"] as const;
type Action = (typeof ACTIONS)[number];

interface AllianceDoc {
  allianceId: string; name: string; nameLower: string; emblem: string; description: string;
  level: number; xp: number; memberCount: number; leaderUid: string; createdAt: number; updatedAt: number;
}
interface MemberDoc {
  uid: string; displayName: string; role: Role; joinedAt: number;
  contribution: number; battlesWon: number; damageDealt: number;
}

const NAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9 '_-]*$/;
const DEFAULT_DESCRIPTION = "Warriors of Mount Olympus united in divine glory.";

const pointer = (allianceId: string | null, role: Role | null, now: number) => ({ allianceId, role, updatedAt: now });

const requireFields = (payload: Payload, action: Action, required: string[]): void => {
  const optional = action === "CREATE" ? ["emblem", "description"] : [];
  const allowed = new Set(["requestId", "action", ...required, ...optional, "uid"]);
  const extra = Object.keys(payload).filter((k) => !allowed.has(k));
  if (extra.length > 0) fail("invalid-argument", `Field(s) not valid for ${action}: ${extra.join(", ")}.`);
};

const requireRole = (member: MemberDoc | undefined, allowed: Role[], message: string): MemberDoc => {
  if (!member) return fail("failed-precondition", "Not currently in an Alliance.");
  if (!allowed.includes(member.role)) return fail("permission-denied", message);
  return member;
};

export const allianceAction = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["requestId", "action", "name", "emblem", "description", "allianceId", "targetUid"]);
  const reqId = requestId(payload);
  const action = oneOf(payload, "action", ACTIONS);

  let name = ""; let emblem = ""; let description = ""; let allianceId = ""; let targetUid = "";
  switch (action) {
    case "CREATE": {
      requireFields(payload, action, ["name"]);
      name = (reqStr(payload, "name", { min: LIMITS.ALLIANCE_NAME_MIN, max: LIMITS.ALLIANCE_NAME_MAX })).trim();
      if (name.length < LIMITS.ALLIANCE_NAME_MIN || !NAME_PATTERN.test(name)) fail("invalid-argument", "Alliance name has invalid characters or is too short.");
      emblem = str(payload, "emblem", { max: 64, optional: true }) ?? CATALOG.alliance.emblems[0];
      if (!CATALOG.alliance.emblems.includes(emblem)) fail("invalid-argument", "Unknown emblem.");
      description = (str(payload, "description", { min: 0, max: LIMITS.ALLIANCE_DESCRIPTION_MAX, optional: true }) ?? "").trim() || DEFAULT_DESCRIPTION;
      break;
    }
    case "JOIN":
      requireFields(payload, action, ["allianceId"]);
      allianceId = reqStr(payload, "allianceId", { max: 64, pattern: ID_PATTERN });
      break;
    case "LEAVE":
      requireFields(payload, action, []);
      break;
    default:
      requireFields(payload, action, ["targetUid"]);
      targetUid = reqStr(payload, "targetUid", { max: 128, pattern: ID_PATTERN });
  }

  return db.runTransaction((tx) =>
    idempotent(tx, caller, `alliance:${action}`, reqId, async () => {
      const mePointer = await tx.get(P.alliancePointer(caller.uid));
      const myAllianceId = (mePointer?.allianceId as string | null | undefined) ?? null;

      if (action === "CREATE") {
        if (myAllianceId) fail("failed-precondition", "Already in an Alliance. Leave your current Alliance first.");
        const nameLower = name.toLowerCase();
        if (await tx.get(P.allianceName(nameLower))) fail("already-exists", "That Alliance name is already taken.");
        const player = await loadPlayer(tx, caller.uid, caller.now);

        const id = `al_${sha(`${caller.uid}|${reqId}`, 16)}`;
        const alliance: AllianceDoc = {
          allianceId: id, name, nameLower, emblem, description, level: 1, xp: 0, memberCount: 1,
          leaderUid: caller.uid, createdAt: caller.now, updatedAt: caller.now,
        };
        const member: MemberDoc = {
          uid: caller.uid, displayName: player.profile.displayName, role: "LEADER", joinedAt: caller.now,
          contribution: 0, battlesWon: 0, damageDealt: 0,
        };
        savePlayer(tx, player, caller.now);
        tx.set(P.alliance(id), alliance as unknown as DocData);
        tx.set(P.allianceName(nameLower), { allianceId: id });
        tx.set(P.allianceMember(id, caller.uid), member as unknown as DocData);
        tx.set(P.alliancePointer(caller.uid), pointer(id, "LEADER", caller.now));
        return { success: true, action, allianceId: id };
      }

      if (action === "JOIN") {
        if (myAllianceId) fail("failed-precondition", "Already in an Alliance. Leave your current Alliance first.");
        const alliance = (await tx.get(P.alliance(allianceId))) as AllianceDoc | undefined;
        if (!alliance) return fail("not-found", "Alliance not found.");
        if (alliance.memberCount >= CATALOG.alliance.maxMembers) {
          return fail("failed-precondition", `Alliance is full (maximum ${CATALOG.alliance.maxMembers} members).`);
        }
        const player = await loadPlayer(tx, caller.uid, caller.now);
        savePlayer(tx, player, caller.now);
        tx.merge(P.alliance(allianceId), { memberCount: alliance.memberCount + 1, updatedAt: caller.now });
        tx.set(P.allianceMember(allianceId, caller.uid), {
          uid: caller.uid, displayName: player.profile.displayName, role: "MEMBER", joinedAt: caller.now,
          contribution: 0, battlesWon: 0, damageDealt: 0,
        } as DocData);
        tx.set(P.alliancePointer(caller.uid), pointer(allianceId, "MEMBER", caller.now));
        return { success: true, action, allianceId };
      }

      // Everything below acts on the caller's CURRENT alliance, taken from the server, never the payload.
      if (!myAllianceId) return fail("failed-precondition", "Not currently in an Alliance.");
      const alliance = (await tx.get(P.alliance(myAllianceId))) as AllianceDoc | undefined;
      const me = (await tx.get(P.allianceMember(myAllianceId, caller.uid))) as MemberDoc | undefined;
      if (!alliance || !me) return fail("failed-precondition", "Alliance membership is out of date.");

      if (action === "LEAVE") {
        if (alliance.memberCount <= 1) {
          tx.delete(P.alliance(myAllianceId));
          tx.delete(P.allianceName(alliance.nameLower));
          tx.delete(P.allianceMember(myAllianceId, caller.uid));
          tx.set(P.alliancePointer(caller.uid), pointer(null, null, caller.now));
          return { success: true, action, allianceId: myAllianceId, disbanded: true };
        }
        let successor: MemberDoc | undefined;
        if (me.role === "LEADER") {
          const members = (await tx.list(P.allianceMembers(myAllianceId))).map((m) => m.data as MemberDoc).filter((m) => m.uid !== caller.uid);
          successor = members.sort((a, b) => b.contribution - a.contribution || a.joinedAt - b.joinedAt)[0];
        }
        tx.delete(P.allianceMember(myAllianceId, caller.uid));
        tx.set(P.alliancePointer(caller.uid), pointer(null, null, caller.now));
        tx.merge(P.alliance(myAllianceId), {
          memberCount: alliance.memberCount - 1,
          updatedAt: caller.now,
          ...(successor ? { leaderUid: successor.uid } : {}),
        });
        if (successor) {
          tx.merge(P.allianceMember(myAllianceId, successor.uid), { role: "LEADER" });
          tx.merge(P.alliancePointer(successor.uid), { role: "LEADER", updatedAt: caller.now });
        }
        return { success: true, action, allianceId: myAllianceId, disbanded: false };
      }

      if (targetUid === caller.uid) fail("invalid-argument", "You cannot target yourself.");
      const target = (await tx.get(P.allianceMember(myAllianceId, targetUid))) as MemberDoc | undefined;
      if (!target) return fail("not-found", "Member not found in your Alliance.");

      switch (action) {
        case "PROMOTE":
          requireRole(me, ["LEADER"], "Only the Alliance Leader can promote members.");
          if (target.role === "LEADER") fail("failed-precondition", "Leader cannot be promoted.");
          if (target.role === "OFFICER") fail("failed-precondition", "Member is already an Officer.");
          tx.merge(P.allianceMember(myAllianceId, targetUid), { role: "OFFICER" });
          tx.merge(P.alliancePointer(targetUid), { role: "OFFICER", updatedAt: caller.now });
          break;
        case "DEMOTE":
          requireRole(me, ["LEADER"], "Only the Alliance Leader can demote officers.");
          if (target.role !== "OFFICER") fail("failed-precondition", "Member is not an Officer.");
          tx.merge(P.allianceMember(myAllianceId, targetUid), { role: "MEMBER" });
          tx.merge(P.alliancePointer(targetUid), { role: "MEMBER", updatedAt: caller.now });
          break;
        case "KICK":
          requireRole(me, ["LEADER", "OFFICER"], "You do not have permission to kick members.");
          if (target.role === "LEADER") fail("failed-precondition", "Cannot kick the Alliance Leader.");
          if (me.role === "OFFICER" && target.role === "OFFICER") fail("permission-denied", "Officers cannot kick other officers.");
          tx.delete(P.allianceMember(myAllianceId, targetUid));
          tx.set(P.alliancePointer(targetUid), pointer(null, null, caller.now));
          tx.merge(P.alliance(myAllianceId), { memberCount: alliance.memberCount - 1, updatedAt: caller.now });
          break;
        case "TRANSFER":
          requireRole(me, ["LEADER"], "Only the Alliance Leader can transfer leadership.");
          tx.merge(P.allianceMember(myAllianceId, targetUid), { role: "LEADER" });
          tx.merge(P.allianceMember(myAllianceId, caller.uid), { role: "OFFICER" });
          tx.merge(P.alliancePointer(targetUid), { role: "LEADER", updatedAt: caller.now });
          tx.merge(P.alliancePointer(caller.uid), { role: "OFFICER", updatedAt: caller.now });
          tx.merge(P.alliance(myAllianceId), { leaderUid: targetUid, updatedAt: caller.now });
          break;
      }
      return { success: true, action, allianceId: myAllianceId };
    })
  );
};

export const syncAlliance = async (db: GameDb, caller: Caller, data: unknown) => {
  const payload = asPayload(data, ["includeOpen"]);
  const includeOpen = bool(payload, "includeOpen", true);

  const ptr = await db.get(P.alliancePointer(caller.uid));
  const allianceId = (ptr?.allianceId as string | null | undefined) ?? null;

  let mine: DocData | null = null;
  if (allianceId) {
    const [alliance, members] = await Promise.all([
      db.get(P.alliance(allianceId)),
      db.list(P.allianceMembers(allianceId), { orderBy: { field: "contribution", direction: "desc" }, limit: CATALOG.alliance.maxMembers }),
    ]);
    if (alliance) mine = { ...alliance, members: members.map((m) => m.data) };
  }

  const open = includeOpen
    ? (await db.list(P.alliances(), {
      where: [["memberCount", "<", CATALOG.alliance.maxMembers]],
      orderBy: { field: "memberCount", direction: "desc" },
      limit: LIMITS.OPEN_ALLIANCES_LIMIT,
    })).map((a) => a.data)
    : [];

  return { success: true, alliance: mine, myRole: (ptr?.role as Role | null | undefined) ?? null, openAlliances: open };
};

// ---------------------------------------------------------------------------------------------
// Contribution hook used by Raid / World Boss submissions. Two phases because a Firestore
// transaction must finish all reads before it writes.
// ---------------------------------------------------------------------------------------------
export interface AllianceCtx {
  allianceId: string | null;
  alliance?: AllianceDoc;
  member?: MemberDoc;
}

export const loadAllianceCtx = async (tx: Tx, uid: string): Promise<AllianceCtx> => {
  const ptr = await tx.get(P.alliancePointer(uid));
  const allianceId = (ptr?.allianceId as string | null | undefined) ?? null;
  if (!allianceId) return { allianceId: null };
  const alliance = (await tx.get(P.alliance(allianceId))) as AllianceDoc | undefined;
  const member = (await tx.get(P.allianceMember(allianceId, uid))) as MemberDoc | undefined;
  if (!alliance || !member) return { allianceId: null };
  return { allianceId, alliance, member };
};

export const applyContribution = (
  tx: Tx, ctx: AllianceCtx, uid: string, battlesWon: number, damage: number, now: number
): { score: number } => {
  if (!ctx.allianceId || !ctx.alliance || !ctx.member) return { score: 0 };
  const score = battlesWon * 50 + Math.floor(damage / 200);

  let level = ctx.alliance.level;
  let xp = ctx.alliance.xp + battlesWon * 50;
  while (level < CATALOG.alliance.maxLevel) {
    const required = CATALOG.alliance.xpTable[level - 1] ?? level * 5000;
    if (required > 0 && xp >= required) { xp -= required; level += 1; } else break;
  }
  tx.merge(P.allianceMember(ctx.allianceId, uid), {
    contribution: ctx.member.contribution + score,
    battlesWon: ctx.member.battlesWon + battlesWon,
    damageDealt: ctx.member.damageDealt + damage,
  });
  tx.merge(P.alliance(ctx.allianceId), { level, xp, updatedAt: now });
  return { score };
};
