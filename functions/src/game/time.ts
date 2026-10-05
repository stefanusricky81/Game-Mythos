/**
 * All gameplay "days" and "seasons" are computed on the server in Asia/Jakarta (UTC+7, no DST) so the
 * daily reset moment is identical for every player and cannot be moved by changing the device clock.
 */
const OFFSET_MS = 7 * 60 * 60 * 1000;
const DAY_MS = 24 * 60 * 60 * 1000;

export const dateKey = (ms: number): string => new Date(ms + OFFSET_MS).toISOString().slice(0, 10);

/** Start (inclusive) of a yyyy-MM-dd day in Asia/Jakarta. */
export const startOfDateKey = (key: string): number => Date.parse(`${key}T00:00:00.000Z`) - OFFSET_MS;

/** An event/boss window [start 00:00, end 24:00) matching the client's inclusive string-date comparison. */
export const isWithinDateWindow = (now: number, startKey: string, endKey: string): boolean =>
  now >= startOfDateKey(startKey) && now < startOfDateKey(endKey) + DAY_MS;

export const windowEndMs = (endKey: string): number => startOfDateKey(endKey) + DAY_MS;

const SEASON_EPOCH_KEY = "2026-09-01";

export interface Season {
  number: number;
  id: string;
  startMs: number;
  endMs: number;
}

export const seasonOf = (now: number, seasonDays: number): Season => {
  const epoch = startOfDateKey(SEASON_EPOCH_KEY);
  const length = seasonDays * DAY_MS;
  const number = Math.max(1, Math.floor((now - epoch) / length) + 1);
  const startMs = epoch + (number - 1) * length;
  return { number, id: `season_${number}`, startMs, endMs: startMs + length };
};
