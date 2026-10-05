import contract from "./apiContract.json";
import { HANDLERS, handleCall } from "./handlers";
import { MemoryGameDb } from "./memoryDb";

/**
 * The server rejects unknown fields strictly, so a client that sends a field the schema does not list
 * would break silently in production. This test feeds every payload shape the Android client sends
 * (see ApiContractTest.kt for the client side of the same contract) to the real handlers and asserts
 * none is refused for having an unexpected field.
 */
const dummyFor = (key: string): unknown => {
  switch (key) {
    case "cardIds": case "claimIds": return [];
    case "ownedCardCounts": return {};
    case "limit": case "stage": case "turns": case "damageDealt": case "damageTaken": case "damage": return 1;
    case "includeOpen": case "victory": case "cleared": return true;
    case "requestId": return "contract-request-1";
    default: return "x";
  }
};

describe("client/server API contract", () => {
  const entries = Object.entries(contract).filter(([name]) => !name.startsWith("_"));

  test.each(entries)("%s: every field the client sends is accepted by the server schema", async (name, keys) => {
    const [fn, discriminator] = name.split(":");
    expect(HANDLERS[fn]).toBeDefined();

    const payload: Record<string, unknown> = {};
    for (const key of keys as string[]) payload[key] = dummyFor(key);
    if (fn === "allianceAction") payload.action = discriminator;
    if (fn === "startEndgameAttempt") payload.kind = discriminator;

    const env = { db: new MemoryGameDb(), collectionAttestationAllowed: true };
    let message = "";
    try {
      await handleCall(env, fn, { uid: "contract-user" }, payload, Date.UTC(2026, 9, 2, 5));
    } catch (e) {
      message = (e as Error).message;
    }
    expect(message).not.toMatch(/Unexpected field/);
    expect(message).not.toMatch(/^Field\(s\) not valid for/);
  });

  test("every server function is covered by the contract", () => {
    const covered = new Set(entries.map(([name]) => name.split(":")[0]));
    expect([...Object.keys(HANDLERS)].filter((f) => !covered.has(f))).toEqual([]);
  });
});
