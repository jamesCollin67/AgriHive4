# AgriHive — System Architecture

## Overview

AgriHive is a two-platform apiary management system consisting of:
- A **mobile app** (Android/Kotlin) used by beekeepers in the field
- A **web admin panel** (React/Vite) used by farm owners and administrators
- A **Firebase backend** shared by both platforms
- An **IoT sensor layer** (ESP32 nodes) that pushes live hive data

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AGRIHIVE SYSTEM                              │
│                                                                     │
│  ┌──────────────────┐          ┌──────────────────────────────┐    │
│  │   MOBILE APP     │          │      WEB ADMIN PANEL         │    │
│  │  Android/Kotlin  │          │      React 19 + Vite         │    │
│  │                  │          │      MUI + React Router      │    │
│  │  • Login/Register│          │                              │    │
│  │  • Dashboard     │          │  • Dashboard (stats)         │    │
│  │  • Hive Streams  │          │  • User Management           │    │
│  │  • AI Scanner    │          │  • Report Management         │    │
│  │  • Send Report   │          │  • Subscription Mgmt         │    │
│  │  • Subscription  │          │  • Activity Logs             │    │
│  │  • Notifications │          │  • CSV Export                │    │
│  │  • Weather Alerts│          │                              │    │
│  └────────┬─────────┘          └──────────────┬───────────────┘    │
│           │                                   │                     │
│           │  Firebase SDK                     │  Firebase SDK       │
│           │                                   │                     │
│           └──────────────┬────────────────────┘                     │
│                          │                                          │
│              ┌───────────▼──────────────┐                          │
│              │     FIREBASE BACKEND     │                          │
│              │                          │                          │
│              │  ┌─────────────────────┐ │                          │
│              │  │  Firebase Auth      │ │  • Email/Password auth   │
│              │  │                     │ │  • Email verification    │
│              │  └─────────────────────┘ │  • Admin role via        │
│              │                          │    admins/{uid}          │
│              │  ┌─────────────────────┐ │                          │
│              │  │  Cloud Firestore    │ │  Collections:            │
│              │  │  (Real-time DB)     │ │  • users                 │
│              │  │                     │ │  • apiaries              │
│              │  └─────────────────────┘ │  • reports               │
│              │                          │  • subscriptions         │
│              │  ┌─────────────────────┐ │  • activity_logs         │
│              │  │  Firebase Storage   │ │  • bee_farms             │
│              │  │                     │ │  • weight_analytics      │
│              │  └─────────────────────┘ │  • admins                │
│              │                          │                          │
│              │  ┌─────────────────────┐ │                          │
│              │  │  Firebase Cloud     │ │  • Admin reply           │
│              │  │  Messaging (FCM)    │ │    notifications         │
│              │  └─────────────────────┘ │  • Hive alerts           │
│              │                          │                          │
│              └───────────┬──────────────┘                          │
│                          │                                          │
│              ┌───────────▼──────────────┐                          │
│              │    IoT SENSOR LAYER      │                          │
│              │    ESP32 Nodes           │                          │
│              │                          │                          │
│              │  Sensors per hive:       │                          │
│              │  • Temperature (DHT22)   │                          │
│              │  • Humidity (DHT22)      │                          │
│              │  • Moisture (capacitive) │                          │
│              │  • Weight (load cell)    │                          │
│              │                          │                          │
│              │  Writes to Firestore:    │                          │
│              │  apiaries/{id} fields    │                          │
│              └──────────────────────────┘                          │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  PAYMENT LAYER                               │  │
│  │  PayMongo API  →  GCash / Maya / BDO / PayPal               │  │
│  │  Mobile app calls PayMongo directly (secret key in          │  │
│  │  BuildConfig from local.properties — not in source)         │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### 1. Beekeeper Registration & Login
```
Mobile App → Firebase Auth (createUser) → Firestore (users/{uid}) → Dashboard
```

### 2. Live Hive Monitoring
```
ESP32 Sensor → WiFi → Firestore (apiaries/{id}) → Mobile App (real-time listener)
                                                 → Web Admin (via AdminDataContext)
```

### 3. Report Submission & Admin Reply
```
Mobile App → Firestore (reports/{id}) → Web Admin sees in real-time
Web Admin replies → Firestore (reports/{id}.reply) → Mobile FCM notification
```

### 4. Subscription Payment
```
Mobile App → PayMongo API → GCash/Maya checkout
User pays → PayMongo redirects → Mobile App (deep link)
Mobile App → Firestore (subscriptions/{uid}) → Web Admin confirms
```

### 5. AI Disease Scanning
```
Camera → TFLite model (on-device) → Scan Result
User saves → Firebase Storage (image) → Firestore (saved_treatments/{id})
```

### 6. Admin Access Control
```
Web Login → Firebase Auth → Firestore (admins/{uid} exists?) → Dashboard
                                                              → Access Denied
```

---

## Security Architecture

| Layer | Mechanism |
|-------|-----------|
| Authentication | Firebase Auth (email + password + email verification) |
| Admin separation | `admins/{uid}` Firestore collection check |
| Data access | Firestore Security Rules (`isAdmin()` + `isOwner()`) |
| API keys | Firebase config in `.env` (gitignored) |
| PayMongo key | `local.properties` → `BuildConfig` (gitignored) |
| Release APK | ProGuard minification + resource shrinking |

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Mobile App | Android (Kotlin), MVVM, ViewBinding, Room DB |
| Web Admin | React 19, Vite, MUI v7, React Router v7 |
| Backend | Firebase (Auth, Firestore, Storage, FCM) |
| AI/ML | TensorFlow Lite (on-device inference) |
| IoT | ESP32 microcontroller, DHT22, load cell |
| Payments | PayMongo (GCash, Maya, BDO, PayPal) |
| Weather | OpenWeatherMap API (via RainCheckWorker) |
| Local Cache | Room Database (Android) |
