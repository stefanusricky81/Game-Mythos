import { fail } from "./errors";

/**
 * Hand-rolled strict input validation (no new dependency). Every callable declares the exact set of
 * fields it accepts; anything else - including fields a cheating client might add such as `reward`,
 * `gold`, `opponentRating` or a forged result - is rejected, never silently ignored.
 */
export type Payload = Record<string, unknown>;

export const asPayload = (data: unknown, allowed: readonly string[]): Payload => {
  const input = data === undefined || data === null ? {} : data;
  if (typeof input !== "object" || Array.isArray(input)) fail("invalid-argument", "Request body must be an object.");
  const payload = input as Payload;
  const permitted = new Set<string>([...allowed, "uid"]);
  const unknown = Object.keys(payload).filter((k) => !permitted.has(k));
  if (unknown.length > 0) fail("invalid-argument", `Unexpected field(s): ${unknown.join(", ")}.`, { fields: unknown });
  return payload;
};

export const str = (
  payload: Payload,
  name: string,
  opts: { min?: number; max?: number; pattern?: RegExp; optional?: boolean } = {}
): string | undefined => {
  const value = payload[name];
  if (value === undefined || value === null) {
    if (opts.optional) return undefined;
    return fail("invalid-argument", `${name} is required.`);
  }
  if (typeof value !== "string") return fail("invalid-argument", `${name} must be a string.`);
  if (value.length < (opts.min ?? 1)) return fail("invalid-argument", `${name} is too short.`);
  if (value.length > (opts.max ?? 128)) return fail("invalid-argument", `${name} is too long.`);
  if (opts.pattern && !opts.pattern.test(value)) return fail("invalid-argument", `${name} has an invalid format.`);
  return value;
};

export const reqStr = (payload: Payload, name: string, opts: { min?: number; max?: number; pattern?: RegExp } = {}): string =>
  str(payload, name, opts) as string;

export const int = (payload: Payload, name: string, opts: { min?: number; max?: number } = {}): number => {
  const value = payload[name];
  if (typeof value !== "number" || !Number.isInteger(value)) return fail("invalid-argument", `${name} must be an integer.`);
  if (opts.min !== undefined && value < opts.min) return fail("invalid-argument", `${name} must be >= ${opts.min}.`);
  if (opts.max !== undefined && value > opts.max) return fail("invalid-argument", `${name} must be <= ${opts.max}.`);
  return value;
};

export const bool = (payload: Payload, name: string, optional = false): boolean => {
  const value = payload[name];
  if (value === undefined && optional) return false;
  if (typeof value !== "boolean") return fail("invalid-argument", `${name} must be a boolean.`);
  return value;
};

export const oneOf = <T extends string>(payload: Payload, name: string, values: readonly T[]): T => {
  const value = payload[name];
  if (typeof value !== "string" || !(values as readonly string[]).includes(value)) {
    return fail("invalid-argument", `${name} must be one of: ${values.join(", ")}.`);
  }
  return value as T;
};

export const strArray = (payload: Payload, name: string, opts: { min?: number; max?: number; item?: RegExp } = {}): string[] => {
  const value = payload[name];
  if (!Array.isArray(value)) return fail("invalid-argument", `${name} must be an array.`);
  if (value.length < (opts.min ?? 0)) return fail("invalid-argument", `${name} has too few entries.`);
  if (value.length > (opts.max ?? 100)) return fail("invalid-argument", `${name} has too many entries.`);
  for (const item of value) {
    if (typeof item !== "string" || item.length === 0 || item.length > 128 || (opts.item && !opts.item.test(item))) {
      return fail("invalid-argument", `${name} contains an invalid entry.`);
    }
  }
  return value as string[];
};

export const ID_PATTERN = /^[A-Za-z0-9_-]{1,128}$/;
export const REQUEST_ID_PATTERN = /^[A-Za-z0-9_-]{8,64}$/;

export const requestId = (payload: Payload): string => reqStr(payload, "requestId", { min: 8, max: 64, pattern: REQUEST_ID_PATTERN });
