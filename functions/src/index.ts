import * as admin from "firebase-admin";

export { verifyPlayPurchase, getPlayerEntitlements } from "./playBilling";

admin.initializeApp();
