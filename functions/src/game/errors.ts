export type GameErrorCode =
  | "unauthenticated"
  | "permission-denied"
  | "invalid-argument"
  | "not-found"
  | "failed-precondition"
  | "already-exists"
  | "resource-exhausted"
  | "internal";

/**
 * Domain error. Handlers throw this; the callable wrapper maps it to an HttpsError so clients get
 * an explicit, stable error code instead of an opaque internal error.
 */
export class GameError extends Error {
  constructor(
    public readonly code: GameErrorCode,
    message: string,
    public readonly details?: Record<string, unknown>
  ) {
    super(message);
    this.name = "GameError";
  }
}

export const fail = (code: GameErrorCode, message: string, details?: Record<string, unknown>): never => {
  throw new GameError(code, message, details);
};
