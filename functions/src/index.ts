import * as admin from "firebase-admin";

admin.initializeApp();

export { verifyPlayPurchase, getPlayerEntitlements } from "./playBilling";
export * from "./backendFunctions";
