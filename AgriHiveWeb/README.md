# AgriHive Admin Panel

Web-based administration dashboard for the **AgriHive** apiary management platform. Built for farm owners and administrators to manage beekeepers, monitor hive reports, and handle subscriptions in real time.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19 + Vite |
| UI Components | Material UI (MUI) v7 |
| Routing | React Router v7 |
| Backend | Firebase (Auth + Firestore) |
| State | React Context API |

---

## Getting Started

### Prerequisites
- Node.js 18+
- A Firebase project with **Firestore** and **Authentication** enabled

### Installation

```bash
cd AgriHiveWeb
npm install
```

### Environment Setup

```bash
cp .env.example .env
```

Fill in your Firebase credentials in `.env`:

```
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
```

### Run Development Server

```bash
npm run dev
```

Opens at `http://localhost:5173`

---

## Admin Account Setup

1. Go to [Firebase Console](https://console.firebase.google.com) → **Authentication** → **Users**
2. Click **Add user** → enter admin email and password
3. Copy the user's **UID**
4. Go to **Firestore Database** → create collection `admins`
5. Add a document with **Document ID = the UID**
6. Add field: `role` = `"admin"` (string)
7. Log in at `http://localhost:5173`

> Regular beekeeper accounts from the mobile app **cannot** access this panel — only UIDs registered in the `admins` collection are granted access.

---

## Features

| Feature | Description |
|---------|-------------|
| Dashboard | Real-time overview of farms, hives, and pending reports |
| Users | View, search, deactivate, reactivate, and delete beekeeper accounts |
| Reports | Read, reply to, and manage hive issue reports from the mobile app |
| Subscriptions | Confirm or reject payment requests with GCash reference verification |
| Activity Log | Real-time log of all admin actions |
| CSV Export | Download user list as a CSV file |

---

## Build for Production

```bash
npm run build
```

Output is in the `dist/` folder. Deploy to:
- **Firebase Hosting**: `firebase deploy --only hosting`
- **Vercel**: drag and drop `dist/` or connect the repo
- **Netlify**: connect repo, set build command to `npm run build`, publish dir to `dist`

---

## Project Structure

```
AgriHiveWeb/
├── src/
│   ├── context/
│   │   ├── AuthContext.jsx       # Auth state + admin role check
│   │   └── AdminDataContext.jsx  # Shared Firestore listeners
│   ├── layouts/
│   │   └── DashboardLayout.jsx   # Sidebar + AppBar wrapper
│   ├── pages/
│   │   ├── Dashboard.jsx
│   │   ├── Users.jsx
│   │   ├── Reports.jsx
│   │   └── Subscriptions.jsx
│   ├── App.jsx                   # Routes + AdminRoute guard
│   ├── ErrorBoundary.jsx         # Global error handler
│   ├── firebase.js               # Firebase initialization
│   ├── theme.js                  # MUI theme config
│   └── main.jsx                  # Entry point
├── .env                          # Firebase credentials (not committed)
├── .env.example                  # Template for credentials
└── vite.config.js
```

---

## Security

- All routes are protected by `AdminRoute` — unauthenticated users are redirected to `/login`
- Admin role is verified against the `admins/{uid}` Firestore collection on every login
- Firebase API credentials are stored in `.env` and excluded from version control
- Firestore Security Rules enforce per-role data access (see `firestore.rules`)
