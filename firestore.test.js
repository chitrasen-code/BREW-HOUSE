const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_customer_2";
const STAFF_UID = "barista_staff_1";
const OWNER_UID = "owner_admin_1";
const OWNER_EMAIL = "chitrasengautam16@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(ms = 2000) {
  return new Date(Date.now() - ms);
}

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

test("Unauthenticated user cannot read or write any Brew House collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("stores").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      loyaltyPoints: 0,
      lifetimePoints: 0,
      notificationsEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Email spoofing test: unverified owner email cannot perform admin writes; verified owner email succeeds", async () => {
  const spoofedDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedDb.collection("stores").doc("store_main").set({
      name: "Brew House Flagship",
      address: "Editable Cafe Address",
      phone: "+91 00000 00000",
      openingHours: "08:00 AM - 10:00 PM",
      currencyCode: "INR",
      taxRateBps: 500,
      open: true,
      pickupEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const verifiedOwnerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    verifiedOwnerDb.collection("stores").doc("store_main").set({
      name: "Brew House Flagship",
      address: "Editable Cafe Address",
      phone: "+91 00000 00000",
      openingHours: "08:00 AM - 10:00 PM",
      pickupInstructions: "Collect at Express Bar Counter",
      currencyCode: "INR",
      taxRateBps: 500,
      open: true,
      pickupEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Customer profile isolation, zero-initial-points enforcement, and PII protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  // Reject self-granting loyalty points on initial creation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      loyaltyPoints: 5000,
      lifetimePoints: 5000,
      notificationsEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Valid initial profile creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      phone: "+91 90000 00001",
      preferredStoreId: "store_main",
      preferredMilk: "Oat Milk",
      loyaltyPoints: 0,
      lifetimePoints: 0,
      notificationsEnabled: true,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Partial update by owner succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredMilk: "Almond Milk",
      updatedAt: pastDate(),
    })
  );

  // Shadow update with extra ghost key fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(),
    })
  );

  // Cross-user PII read and list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Customer persistent cart subcollection CRUD and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("cart_1").set({
      productId: "prod_caramel_latte",
      productName: "Caramel Latte",
      imageKey: "hot_latte",
      quantity: 1,
      unitPricePaise: 22000,
      temperature: "HOT",
      size: "MEDIUM",
      milkOption: "Oat Milk",
      sugarLevel: "Regular sugar",
      addons: ["Caramel syrup"],
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("cart_1").update({
      quantity: 2,
      updatedAt: pastDate(),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );
});

test("Orders: customer scoped query, staff workflow transitions, and terminal state lock", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("stores").doc("store_main").set({
      name: "Brew House Flagship",
      address: "Editable Cafe Address",
      phone: "+91 00000 00000",
      openingHours: "08:00 AM - 10:00 PM",
      currencyCode: "INR",
      taxRateBps: 500,
      open: true,
      pickupEnabled: true,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("staff").doc(STAFF_UID).set({
      uid: STAFF_UID,
      name: "Aarav Barista",
      email: "aarav@brewhouse.local",
      role: "BARISTA",
      storeId: "store_main",
      active: true,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  const orderPayload = {
    orderNumber: "BH-1001",
    userId: ALICE_UID,
    customerName: "Alice",
    customerPhone: "+91 90000 00001",
    storeId: "store_main",
    storeName: "Brew House Flagship",
    pickupOption: "PICKUP_NOW",
    pickupInstructions: "Extra hot please",
    status: "PENDING",
    paymentMethod: "PAY_AT_COUNTER",
    paymentStatus: "UNPAID",
    subtotalPaise: 22000,
    discountPaise: 0,
    taxPaise: 1100,
    totalPaise: 23100,
    loyaltyPointsEarned: 23,
    itemsSummary: ["1x Caramel Latte • Medium • Hot • Oat Milk (₹220)"],
    itemCount: 1,
    createdAt: pastDate(5000),
    updatedAt: pastDate(5000),
  };

  // Customer cannot spoof initial status as COMPLETED or future timestamp
  await assertFails(
    aliceDb.collection("orders").doc("order_bad").set({
      ...orderPayload,
      status: "COMPLETED",
    })
  );
  await assertFails(
    aliceDb.collection("orders").doc("order_future").set({
      ...orderPayload,
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Oversized ID rejected
  await assertFails(
    aliceDb.collection("orders").doc("a".repeat(200)).set(orderPayload)
  );

  // Valid customer order create succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1001").set(orderPayload)
  );

  // Scoped query succeeds; unfiltered query fails for customer
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("order_1001").get());

  // Customer cannot self-approve order to READY_FOR_PICKUP
  await assertFails(
    aliceDb.collection("orders").doc("order_1001").update({
      status: "READY_FOR_PICKUP",
      updatedAt: pastDate(),
    })
  );

  // Staff can list all orders and progress status to COMPLETED
  await assertSucceeds(staffDb.collection("orders").get());
  await assertSucceeds(
    staffDb.collection("orders").doc("order_1001").update({
      status: "PREPARING",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    staffDb.collection("orders").doc("order_1001").update({
      status: "COMPLETED",
      paymentStatus: "PAID",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: staff cannot modify COMPLETED order
  await assertFails(
    staffDb.collection("orders").doc("order_1001").update({
      status: "CANCELLED",
      updatedAt: pastDate(),
    })
  );
});

test("Inventory & admin catalog permissions", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("staff").doc(STAFF_UID).set({
      uid: STAFF_UID,
      name: "Aarav Barista",
      email: "aarav@brewhouse.local",
      role: "BARISTA",
      storeId: "store_main",
      active: true,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("inventory_items").doc("inv_beans").set({
      name: "House Espresso Beans",
      category: "Coffee Beans",
      unit: "kg",
      currentStock: 18,
      lowStockThreshold: 5,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  // Customer denied inventory and product writes
  await assertFails(aliceDb.collection("inventory_items").get());
  await assertFails(
    aliceDb.collection("products").doc("prod_x").set({
      name: "Hack",
      categoryId: "cat_1",
      categoryName: "Espresso",
      description: "Test",
      basePricePaise: 100,
      imageKey: "hot_latte",
      available: true,
      bestSeller: false,
      seasonal: false,
      allowHot: true,
      allowIced: false,
      allowMilkOptions: false,
      allowSugarOptions: true,
      allowAddons: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Staff can update stock count & record stock movement
  await assertSucceeds(
    staffDb.collection("inventory_items").doc("inv_beans").update({
      currentStock: 15,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    staffDb.collection("stock_movements").doc("mov_1").set({
      inventoryItemId: "inv_beans",
      itemName: "House Espresso Beans",
      delta: -3,
      reason: "Morning espresso calibration",
      actorUid: STAFF_UID,
      createdAt: pastDate(),
    })
  );

  // Verified owner can create products
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_espresso").set({
      name: "Espresso",
      categoryId: "cat_espresso",
      categoryName: "Espresso",
      description: "Rich double-ristretto shot with velvety crema.",
      ingredients: "100% Arabica Estate Beans, Filtered Water",
      basePricePaise: 12000,
      imageKey: "hot_latte",
      available: true,
      bestSeller: true,
      seasonal: false,
      allowHot: true,
      allowIced: false,
      allowMilkOptions: false,
      allowSugarOptions: true,
      allowAddons: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});
