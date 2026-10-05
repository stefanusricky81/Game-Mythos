import { DocData, DocEntry, GameDb, ListOptions, Tx } from "./db";

const clone = <T>(v: T): T => (v === undefined ? v : JSON.parse(JSON.stringify(v)));

const mergeDeep = (target: DocData, patch: DocData): DocData => {
  const out: DocData = { ...target };
  for (const [k, v] of Object.entries(patch)) {
    if (v && typeof v === "object" && !Array.isArray(v) && out[k] && typeof out[k] === "object" && !Array.isArray(out[k])) {
      out[k] = mergeDeep(out[k], v);
    } else {
      out[k] = clone(v);
    }
  }
  return out;
};

const assertNoUndefined = (value: unknown, path = "doc"): void => {
  if (value === undefined) throw new Error(`Firestore rejects undefined field values (${path}).`);
  if (value && typeof value === "object") {
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) assertNoUndefined(v, `${path}.${k}`);
  }
};

const parentOf = (path: string): string => path.substring(0, path.lastIndexOf("/"));
const idOf = (path: string): string => path.substring(path.lastIndexOf("/") + 1);

const matches = (data: DocData, where: ListOptions["where"]): boolean =>
  (where ?? []).every(([field, op, value]) => {
    const actual = data[field] as any;
    const expected = value as any;
    switch (op) {
      case "==": return actual === expected;
      case ">": return actual > expected;
      case "<": return actual < expected;
      case ">=": return actual >= expected;
      case "<=": return actual <= expected;
    }
  });

/**
 * Serialised in-memory GameDb for unit tests. Transactions run one at a time (like a serialisable
 * database), writes are buffered and committed only if the callback resolves, and a read after a
 * write throws exactly like a real Firestore transaction would.
 */
export class MemoryGameDb implements GameDb {
  private readonly docs = new Map<string, DocData>();
  private queue: Promise<unknown> = Promise.resolve();
  public transactionCount = 0;

  seed(path: string, data: DocData): void {
    this.docs.set(path, clone(data));
  }

  peek(path: string): DocData | undefined {
    return clone(this.docs.get(path));
  }

  paths(prefix = ""): string[] {
    return [...this.docs.keys()].filter((p) => p.startsWith(prefix)).sort();
  }

  runTransaction<T>(fn: (tx: Tx) => Promise<T>): Promise<T> {
    const run = async (): Promise<T> => {
      this.transactionCount++;
      const writes = new Map<string, { op: "set" | "merge" | "delete"; data?: DocData }>();
      let wrote = false;

      const resolve = (path: string): DocData | undefined => {
        const w = writes.get(path);
        if (!w) return clone(this.docs.get(path));
        if (w.op === "delete") return undefined;
        return clone(w.data);
      };

      const tx: Tx = {
        get: async (path) => {
          if (wrote) throw new Error("Firestore transactions require all reads to be executed before all writes.");
          return resolve(path);
        },
        list: async (collectionPath, options) => {
          if (wrote) throw new Error("Firestore transactions require all reads to be executed before all writes.");
          return this.query(collectionPath, options);
        },
        set: (path, data) => {
          assertNoUndefined(data, path);
          wrote = true;
          writes.set(path, { op: "set", data: clone(data) });
        },
        merge: (path, patch) => {
          assertNoUndefined(patch, path);
          wrote = true;
          const base = resolve(path) ?? {};
          writes.set(path, { op: "set", data: mergeDeep(base, patch) });
        },
        delete: (path) => {
          wrote = true;
          writes.set(path, { op: "delete" });
        },
      };

      const result = await fn(tx);
      for (const [path, w] of writes) {
        if (w.op === "delete") this.docs.delete(path);
        else this.docs.set(path, clone(w.data!));
      }
      return result;
    };

    const next = this.queue.then(run, run);
    this.queue = next.catch(() => undefined);
    return next;
  }

  async get(path: string): Promise<DocData | undefined> {
    return clone(this.docs.get(path));
  }

  async list(collectionPath: string, options?: ListOptions): Promise<DocEntry[]> {
    return this.query(collectionPath, options);
  }

  async count(collectionPath: string, where?: ListOptions["where"]): Promise<number> {
    return this.query(collectionPath, { where }).length;
  }

  private query(collectionPath: string, options?: ListOptions): DocEntry[] {
    let entries: DocEntry[] = [];
    for (const [path, data] of this.docs) {
      if (parentOf(path) === collectionPath && matches(data, options?.where)) {
        entries.push({ id: idOf(path), data: clone(data) });
      }
    }
    if (options?.orderBy) {
      const { field, direction } = options.orderBy;
      entries.sort((a, b) => {
        const av = a.data[field] as any;
        const bv = b.data[field] as any;
        const cmp = av < bv ? -1 : av > bv ? 1 : 0;
        return direction === "asc" ? cmp : -cmp;
      });
    }
    if (options?.limit) entries = entries.slice(0, options.limit);
    return entries;
  }
}
