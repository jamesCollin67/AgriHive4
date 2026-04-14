# Admin Setup Instructions

## 1. Deploy Firestore Security Rules

In your project root, run:
```bash
firebase deploy --only firestore:rules
```

## 2. Register an Admin Account

The web panel now checks `admins/{uid}` in Firestore before granting access.
Regular beekeeper accounts from the mobile app **cannot** log into the admin panel.

### Steps:
1. Go to [Firebase Console](https://console.firebase.google.com) → your project
2. Open **Authentication** → find the admin user's email → copy their **UID**
3. Open **Firestore Database** → create a collection called `admins`
4. Add a document with the **UID as the document ID**
5. Add any field, e.g. `role: "admin"`

That's it. The admin can now log in at the web panel.

## 3. Add More Admins
Repeat step 2–5 for each additional admin UID.
Never share admin credentials — each admin should have their own Firebase Auth account.
