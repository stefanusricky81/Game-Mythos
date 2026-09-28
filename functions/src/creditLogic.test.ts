import { checkPurchaseState, decideCredit } from "./creditLogic";

describe("decideCredit", () => {
  // Scenario 1: first purchase
  it("credits when no existing record exists", () => {
    expect(decideCredit(null, "uid_A")).toEqual({ kind: "CREDIT" });
  });

  // Scenario 2 & 4: duplicate purchaseToken / already credited response
  it("returns ALREADY_CREDITED when the same uid already owns the record", () => {
    expect(decideCredit({ playerUid: "uid_A" }, "uid_A")).toEqual({ kind: "ALREADY_CREDITED" });
  });

  // Scenario 10: same token + different UID
  it("returns CONFLICT (never re-assigns ownership) when a different uid owns the record", () => {
    expect(decideCredit({ playerUid: "uid_A" }, "uid_B")).toEqual({
      kind: "CONFLICT",
      ownerUid: "uid_A",
    });
  });

  // Scenario 3: concurrent duplicate verification (logic-level proof).
  // This proves decideCredit itself is a pure function of its inputs - the actual race
  // safety comes from both calls being evaluated from a read taken INSIDE the same Firestore
  // transaction (see playBilling.ts), which Firestore retries on write conflicts so the
  // second commit to land always observes the first's write. That guarantee is Firestore's,
  // not this function's, and is exercised end-to-end in playBilling.test.ts's simulated
  // concurrent-transaction test, not provable here in isolation.
  it("is a pure function: same inputs always produce the same decision", () => {
    const a = decideCredit({ playerUid: "uid_A" }, "uid_A");
    const b = decideCredit({ playerUid: "uid_A" }, "uid_A");
    expect(a).toEqual(b);
  });
});

describe("checkPurchaseState", () => {
  it("maps 0 (purchased) to PROCEED", () => {
    expect(checkPurchaseState(0)).toBe("PROCEED");
  });

  // Scenario 6: cancelled purchase
  it("maps 1 (canceled) to CANCELLED", () => {
    expect(checkPurchaseState(1)).toBe("CANCELLED");
  });

  // Scenario 5: pending purchase
  it("maps 2 (pending) to PENDING", () => {
    expect(checkPurchaseState(2)).toBe("PENDING");
  });

  // Scenario 7: invalid purchase (malformed/missing state from Google)
  it("maps undefined/null/unknown values to INVALID", () => {
    expect(checkPurchaseState(undefined)).toBe("INVALID");
    expect(checkPurchaseState(null)).toBe("INVALID");
    expect(checkPurchaseState(99)).toBe("INVALID");
  });
});
