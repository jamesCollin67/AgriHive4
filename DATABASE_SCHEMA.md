# AgriHive — Firestore Database Schema

## Collections Overview

```
firestore/
├── admins/{uid}
├── users/{uid}
│   ├── activity_logs/{logId}
│   └── saved_treatments/{treatmentId}
├── apiaries/{apiaryId}
├── reports/{reportId}
├── subscriptions/{uid}
├── activity_logs/{logId}
├── bee_farms/{farmId}
└── weight_analytics/{apiaryId}
```

---

## Collection: `admins`

Grants web admin panel access. Only UIDs present here can log into the admin panel.

| Field | Type | Description |
|-------|------|-------------|
| `role` | string | Always `"admin"` |

**Document ID:** Firebase Auth UID of the admin user

---

## Collection: `users`

Beekeeper profiles. Created on mobile app registration.

| Field | Type | Description |
|-------|------|-------------|
| `uid` | string | Firebase Auth UID |
| `firstName` | string | First name |
| `lastName` | string | Last name |
| `email` | string | Email address |
| `farm` | string | Farm name (legacy) |
| `farmName` | string | Farm name (preferred) |
| `location` | string | Farm location (legacy) |
| `farmLocation` | string | Farm location (preferred) |
| `apiaries` | number | Count of apiaries |
| `active` | boolean | Account status (default: true) |
| `fcmToken` | string | Firebase Cloud Messaging token |
| `createdAt` | timestamp | Registration timestamp |
| `phone` | string | Phone number (optional) |

### Subcollection: `users/{uid}/activity_logs`

| Field | Type | Description |
|-------|------|-------------|
| `uid` | string | Owner UID |
| `type` | string | Log type enum (USER_ACCOUNT, HIVE_SENSOR, SUBSCRIPTION) |
| `title` | string | Short description |
| `description` | string | Full description |
| `timestamp` | timestamp | When the event occurred |
| `userName` | string | Display name |

### Subcollection: `users/{uid}/saved_treatments`

| Field | Type | Description |
|-------|------|-------------|
| `diseaseName` | string | Detected disease label |
| `healthScore` | number | 0–100 health score |
| `symptoms` | string | Symptom description |
| `description` | string | Treatment recommendations |
| `hiveName` | string | Name of the scanned hive |
| `apiaryId` | string | Reference to apiary |
| `imageUrl` | string | Firebase Storage URL of scan image |
| `timestamp` | number | Unix timestamp (ms) |

---

## Collection: `apiaries`

Individual hive/apiary records. Each belongs to one user.

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Document ID (same as Firestore doc ID) |
| `name` | string | Apiary name |
| `location` | string | Physical location |
| `farmName` | string | Associated farm name |
| `nodeId` | string | IoT sensor node ID |
| `ownerId` | string | Firebase Auth UID of owner |
| `temperature` | number | Current temperature (°C) |
| `humidity` | number | Current humidity (%) |
| `moisture` | number | Honey moisture content (%) |
| `weight` | number | Hive weight (kg) |
| `isConnected` | boolean | Whether IoT node is online |
| `alertsCount` | number | Number of active alerts |
| `lastUpdate` | number | Unix timestamp of last sensor update |

---

## Collection: `reports`

Issue reports submitted by beekeepers from the mobile app.

| Field | Type | Description |
|-------|------|-------------|
| `userId` | string | UID of the reporting beekeeper |
| `name` | string | Beekeeper's full name |
| `farm` | string | Farm name |
| `description` | string | Report message |
| `timestamp` | timestamp | Firestore server timestamp |
| `imageUri` | string | Optional photo attachment URI |
| `status` | string | `"pending"` or `"resolved"` |
| `unread` | boolean | Whether admin has read it |
| `reply` | string | Admin's reply text (null if not replied) |
| `repliedAt` | timestamp | When admin replied |
| `userRead` | boolean | Whether beekeeper has read the reply |
| `notified` | boolean | Whether mobile notification was sent |

---

## Collection: `subscriptions`

Payment and subscription records. One document per user (document ID = UID).

| Field | Type | Description |
|-------|------|-------------|
| `userId` | string | Firebase Auth UID |
| `name` | string | Subscriber name |
| `farm` | string | Farm name |
| `email` | string | Email address |
| `plan` | string | Plan name (e.g., "3-month") |
| `price` | number | Amount paid |
| `refNo` | string | GCash reference number |
| `pending` | boolean | Whether payment is awaiting confirmation |
| `rejected` | boolean | Whether payment was rejected |
| `purchased` | string | Purchase date (YYYY-MM-DD) |
| `due` | string | Expiry date (YYYY-MM-DD) |
| `paymentMethod` | string | Payment method used |
| `createdAt` | number | Unix timestamp |

---

## Collection: `activity_logs`

Global admin action log. Displayed in the web panel's activity drawer.

| Field | Type | Description |
|-------|------|-------------|
| `action` | string | Action label (e.g., "Account Suspended") |
| `user` | string | Who performed the action (e.g., "Admin") |
| `target` | string | Who was affected (e.g., user email) |
| `status` | string | `"Success"`, `"Warning"`, `"Error"`, `"Info"` |
| `timestamp` | timestamp | Firestore server timestamp |

---

## Collection: `bee_farms`

Reference data for farm locations shown in the Add Apiary dropdown.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Farm name |
| `address` | string | Full address |

---

## Collection: `weight_analytics`

Aggregated weight history per apiary. Written by IoT backend via Admin SDK.

| Field | Type | Description |
|-------|------|-------------|
| `currentWeight` | number | Latest weight reading (kg) |
| `totalGain` | number | Total weight gain since baseline |
| `avgDailyGain` | number | Average daily weight gain |
| `peakWeight` | number | Highest recorded weight |
| `trendStatus` | string | `"Growing"`, `"Stable"`, `"Declining"` |
| `harvestStatus` | string | Harvest readiness description |

---

## Relationships

```
users/{uid}
  └── owns → apiaries (via ownerId field)
  └── submits → reports (via userId field)
  └── has → subscriptions/{uid}
  └── has → users/{uid}/activity_logs
  └── has → users/{uid}/saved_treatments

apiaries/{id}
  └── has analytics → weight_analytics/{apiaryId}

admins/{uid}
  └── grants access to → web admin panel
```

---

## Known Field Inconsistencies (Legacy)

These dual fields exist due to incremental development. Both are supported in queries:

| Canonical Field | Legacy Alias |
|----------------|-------------|
| `farmName` | `farm` |
| `farmLocation` | `location` |
| `hiveCount` | `numHives` |
