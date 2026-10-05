import * as admin from "firebase-admin";

export type DocData = Record<string, any>;

export interface ListOptions {
  where?: Array<[string, "==" | ">" | "<" | ">=" | "<=", unknown]>;
  orderBy?: { field: string; direction: "asc" | "desc" };
  limit?: number;
}

export interface DocEntry {
  id: string;
  data: DocData;
}

/**
 * Transaction handle. Mirrors Firestore's rule that every read must happen before the first write;
 * the in-memory test double enforces the same rule so ordering bugs fail in unit tests.
 */
export interface Tx {
  get(path: string): Promise<DocData | undefined>;
  list(collectionPath: string, options?: ListOptions): Promise<DocEntry[]>;
  /** Replaces the document. */
  set(path: string, data: DocData): void;
  /** Merges fields into the document (creating it if missing). */
  merge(path: string, patch: DocData): void;
  delete(path: string): void;
}

export interface GameDb {
  runTransaction<T>(fn: (tx: Tx) => Promise<T>): Promise<T>;
  get(path: string): Promise<DocData | undefined>;
  list(collectionPath: string, options?: ListOptions): Promise<DocEntry[]>;
  count(collectionPath: string, where?: ListOptions["where"]): Promise<number>;
}

const applyQuery = (query: admin.firestore.Query, options?: ListOptions): admin.firestore.Query => {
  let q = query;
  for (const [field, op, value] of options?.where ?? []) q = q.where(field, op, value);
  if (options?.orderBy) q = q.orderBy(options.orderBy.field, options.orderBy.direction);
  if (options?.limit) q = q.limit(options.limit);
  return q;
};

class FirestoreTx implements Tx {
  constructor(private readonly fs: admin.firestore.Firestore, private readonly t: admin.firestore.Transaction) {}

  async get(path: string): Promise<DocData | undefined> {
    return (await this.t.get(this.fs.doc(path))).data();
  }

  async list(collectionPath: string, options?: ListOptions): Promise<DocEntry[]> {
    const snap = await this.t.get(applyQuery(this.fs.collection(collectionPath), options));
    return snap.docs.map((d) => ({ id: d.id, data: d.data() }));
  }

  set(path: string, data: DocData): void {
    this.t.set(this.fs.doc(path), data);
  }

  merge(path: string, patch: DocData): void {
    this.t.set(this.fs.doc(path), patch, { merge: true });
  }

  delete(path: string): void {
    this.t.delete(this.fs.doc(path));
  }
}

/** Production Firestore implementation (Admin SDK; bypasses security rules by design). */
export class FirestoreGameDb implements GameDb {
  constructor(private readonly fs: admin.firestore.Firestore = admin.firestore()) {}

  runTransaction<T>(fn: (tx: Tx) => Promise<T>): Promise<T> {
    return this.fs.runTransaction((t) => fn(new FirestoreTx(this.fs, t)));
  }

  async get(path: string): Promise<DocData | undefined> {
    return (await this.fs.doc(path).get()).data();
  }

  async list(collectionPath: string, options?: ListOptions): Promise<DocEntry[]> {
    const snap = await applyQuery(this.fs.collection(collectionPath), options).get();
    return snap.docs.map((d) => ({ id: d.id, data: d.data() }));
  }

  async count(collectionPath: string, where?: ListOptions["where"]): Promise<number> {
    const snap = await applyQuery(this.fs.collection(collectionPath), { where }).count().get();
    return snap.data().count;
  }
}
