const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = () => new Date(Date.now() - 5000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profile or products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .get()
  );
});

test("Owner can create and read own UserProfile with up to 3 WhatsApp Business numbers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "551234567",
      countryCode: "+966",
      phoneNumber2: "552223334",
      countryCode2: "+966",
      phoneNumber3: "559998887",
      countryCode3: "+971",
      displayName: "متجر السرعة",
      role: "customer",
      subscriptionActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update with ghost field on UserProfile fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "551234567",
      countryCode: "+966",
      displayName: "متجر السرعة",
      role: "owner",
      subscriptionActive: true,
      isAdminGhost: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cross-user access to products and messages is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .set({
        userId: ALICE_UID,
        title: "تيربو احترافي",
        description: "تيربو عالي الأداء",
        price: 1850,
        currency: "ر.س",
        category: "أداء",
        imageResKey: "turbo",
        inStock: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: BOB_UID,
        senderType: "customer",
        text: "مرحبا",
        expiresAtEpochMs: 0,
        createdAt: ts,
      })
  );
});

test("Owner can create chat message and shipment order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        senderType: "customer",
        text: "هل يتوفر تيربو؟",
        expiresAtEpochMs: 0,
        createdAt: ts,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shipments")
      .doc("ship_1")
      .set({
        userId: ALICE_UID,
        trackingCode: "TRK-9901",
        customerName: "سعد",
        carrier: "سمسا السريع",
        currentStep: 2,
        statusLabel: "خرج للتوصيل",
        etaText: "اليوم 4:00 م",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});
