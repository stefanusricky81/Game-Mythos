/**
 * Tests verifyPlayPurchase / getPlayerEntitlements by invoking the callable's underlying
 * handler directly via CallableFunction.run() (bypassing the HTTPS/App Check transport layer,
 * which is Firebase's own infrastructure - enforceAppCheck itself is exercised structurally by
 * TypeScript compiling against the real `firebase-functions` types, not re-tested here).
 *
 * firebase-admin and googleapis are mocked. Firestore is faked with a minimal in-memory store
 * that implements the SAME optimistic-concurrency contract real Firestore transactions use
 * (retry the transaction function if a document it read changed before it committed) - this is
 * what actually lets the concurrency test below prove something real about our logic, rather
 * than just asserting a mock returned what we told it to return.
 */

type DocRef = { _path: string };
type FakeDocEntry = { data: any; version: number };

class FakeTransaction {
  private reads = new Map<string, number>();
  private writes = new Map<string, any>();
  constructor(private store: Map<string, FakeDocEntry>) {}

  async get(ref: DocRef) {
    const entry = this.store.get(ref._path);
    this.reads.set(ref._path, entry?.version ?? 0);
    return { exists: !!entry, data: () => entry?.data };
  }

  set(ref: DocRef, data: any) {
    this.writes.set(ref._path, data);
  }

  hasConflict(): boolean {
    for (const [path, readVersion] of this.reads) {
      if ((this.store.get(path)?.version ?? 0) !== readVersion) return true;
    }
    return false;
  }

  commit() {
    for (const [path, data] of this.writes) {
      const current = this.store.get(path);
      this.store.set(path, { data, version: (current?.version ?? 0) + 1 });
    }
  }
}

class FakeFirestore {
  private store = new Map<string, FakeDocEntry>();

  reset() {
    this.store.clear();
  }

  collection(name: string) {
    return {
      doc: (id: string): DocRef => ({ _path: `${name}/${id}` }),
      where: (field: string, _op: string, value: any) => ({
        get: async () => {
          const docs = [...this.store.entries()]
            .filter(([path]) => path.startsWith(`${name}/`))
            .filter(([, entry]) => entry.data[field] === value)
            .map(([path, entry]) => ({ id: path.split("/")[1], data: () => entry.data }));
          return { docs };
        },
      }),
    };
  }

  async runTransaction<T>(fn: (tx: FakeTransaction) => Promise<T>): Promise<T> {
    for (let attempt = 0; attempt < 25; attempt++) {
      const tx = new FakeTransaction(this.store);
      const result = await fn(tx);
      if (tx.hasConflict()) continue; // Firestore's real behavior: retry on write conflict
      tx.commit();
      return result;
    }
    throw new Error("FakeFirestore: transaction retry limit exceeded");
  }
}

const fakeFirestore = new FakeFirestore();

jest.mock("firebase-admin", () => ({
  firestore: Object.assign(() => fakeFirestore, {
    FieldValue: { serverTimestamp: () => "SERVER_TIMESTAMP" },
  }),
}));

jest.mock("firebase-functions/params", () => ({
  defineSecret: () => ({ value: () => JSON.stringify({ fake: "service-account" }) }),
}));

let mockPurchaseState: number | null | undefined = 0;
let mockOrderId: string | null = "GPA.1234-5678";
let mockShouldThrow = false;

jest.mock("googleapis", () => ({
  google: {
    auth: { GoogleAuth: jest.fn().mockImplementation(() => ({})) },
    androidpublisher: () => ({
      purchases: {
        products: {
          get: async () => {
            if (mockShouldThrow) throw new Error("simulated Google API failure");
            return { data: { purchaseState: mockPurchaseState, orderId: mockOrderId } };
          },
        },
      },
    }),
  },
}));

// eslint-disable-next-line @typescript-eslint/no-var-requires
const { verifyPlayPurchase, getPlayerEntitlements } = require("./playBilling");

function fakeRequest(data: any, uid: string | null) {
  return {
    data,
    auth: uid ? ({ uid } as any) : undefined,
    rawRequest: {} as any,
  };
}

beforeEach(() => {
  mockPurchaseState = 0;
  mockOrderId = "GPA.1234-5678";
  mockShouldThrow = false;
  fakeFirestore.reset();
});

describe("verifyPlayPurchase", () => {
  it("throws unauthenticated when there is no Firebase Auth context (Anonymous Auth required)", async () => {
    await expect(
      verifyPlayPurchase.run(fakeRequest({ productId: "gem_starter", purchaseToken: "tok1" }, null))
    ).rejects.toMatchObject({ code: "unauthenticated" });
  });

  it("throws invalid-argument when productId or purchaseToken is missing", async () => {
    await expect(
      verifyPlayPurchase.run(fakeRequest({ productId: "gem_starter" }, "uid_A"))
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  // Scenario 1: first purchase
  it("credits a brand new purchase token: CREDITED_NOW", async () => {
    const result = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: "tok_first" }, "uid_A")
    );
    expect(result.status).toBe("CREDITED_NOW");
    expect(result.productId).toBe("gem_starter");
  });

  // Scenario 2 & 4: duplicate purchaseToken / already credited response
  it("does not re-credit the same token for the same uid: ALREADY_CREDITED, no second grant signal", async () => {
    const req = fakeRequest({ productId: "gem_starter", purchaseToken: "tok_dup" }, "uid_A");
    const first = await verifyPlayPurchase.run(req);
    const second = await verifyPlayPurchase.run(req);
    expect(first.status).toBe("CREDITED_NOW");
    expect(second.status).toBe("ALREADY_CREDITED");
  });

  // Scenario 5: pending purchase
  it("returns PENDING without ever writing a credit record", async () => {
    mockPurchaseState = 2;
    const result = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: "tok_pending" }, "uid_A")
    );
    expect(result.status).toBe("PENDING");
    // A later PURCHASED retry for the same token must still be able to credit -
    // proves PENDING never consumed the idempotency slot.
    mockPurchaseState = 0;
    const retry = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: "tok_pending" }, "uid_A")
    );
    expect(retry.status).toBe("CREDITED_NOW");
  });

  // Scenario 6: cancelled purchase
  it("returns CANCELLED without crediting", async () => {
    mockPurchaseState = 1;
    const result = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: "tok_cancelled" }, "uid_A")
    );
    expect(result.status).toBe("CANCELLED");
  });

  // Scenario 7 & 8 & 9: invalid purchase / wrong productId / wrong packageName all surface the
  // same way - Google's own API rejects a token/productId/packageName combination that doesn't
  // correspond to a real purchase, which this function treats uniformly as ERROR (the request
  // to Google itself failed) since there's no way to distinguish those cases from the error
  // Google returns without parsing provider-specific error bodies.
  it("returns ERROR when Google Play verification itself fails (wrong productId/packageName/token)", async () => {
    mockShouldThrow = true;
    const result = await verifyPlayPurchase.run(
      fakeRequest({ productId: "not_a_real_product", purchaseToken: "tok_bad" }, "uid_A")
    );
    expect(result.status).toBe("ERROR");
  });

  it("returns INVALID when Google returns no usable purchaseState", async () => {
    mockPurchaseState = undefined;
    const result = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: "tok_malformed" }, "uid_A")
    );
    expect(result.status).toBe("INVALID");
  });

  // Scenario 10: same token + different UID
  it("refuses (INVALID) and never re-assigns ownership when a different uid presents the same token", async () => {
    const token = "tok_stolen";
    const first = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: token }, "uid_A")
    );
    const second = await verifyPlayPurchase.run(
      fakeRequest({ productId: "gem_starter", purchaseToken: token }, "uid_B")
    );
    expect(first.status).toBe("CREDITED_NOW");
    expect(second.status).toBe("INVALID");

    // The record must still show uid_A as owner - getPlayerEntitlements for uid_B must NOT
    // see this purchase.
    const entitlementsB = await getPlayerEntitlements.run(fakeRequest({}, "uid_B"));
    expect(entitlementsB.entitlements).toHaveLength(0);
    const entitlementsA = await getPlayerEntitlements.run(fakeRequest({}, "uid_A"));
    expect(entitlementsA.entitlements).toHaveLength(1);
  });

  // Scenario 3: concurrent duplicate verification - two requests for the SAME token race each
  // other. Exercises the real FakeFirestore transaction retry-on-conflict path, which mirrors
  // real Firestore's documented optimistic-concurrency guarantee for runTransaction().
  it("credits a raced pair of identical requests exactly once", async () => {
    const req = () => fakeRequest({ productId: "gem_starter", purchaseToken: "tok_race" }, "uid_A");
    const [a, b] = await Promise.all([verifyPlayPurchase.run(req()), verifyPlayPurchase.run(req())]);

    const statuses = [a.status, b.status].sort();
    expect(statuses).toEqual(["ALREADY_CREDITED", "CREDITED_NOW"]);

    // Exactly one purchase record must exist for tok_race, not two - the whole point of the
    // race being resolved to one CREDITED_NOW + one ALREADY_CREDITED above.
    const entitlements = await getPlayerEntitlements.run(fakeRequest({}, "uid_A"));
    expect(entitlements.entitlements.filter((e: any) => e.productId === "gem_starter")).toHaveLength(1);
  });
});

describe("getPlayerEntitlements", () => {
  it("throws unauthenticated without a signed-in caller", async () => {
    await expect(getPlayerEntitlements.run(fakeRequest({}, null))).rejects.toMatchObject({
      code: "unauthenticated",
    });
  });

  it("only returns entitlements belonging to the calling uid", async () => {
    await verifyPlayPurchase.run(
      fakeRequest({ productId: "bundle_starter_pack", purchaseToken: "tok_c" }, "uid_C")
    );
    const result = await getPlayerEntitlements.run(fakeRequest({}, "uid_C"));
    expect(result.entitlements.some((e: any) => e.productId === "bundle_starter_pack")).toBe(true);
  });
});
