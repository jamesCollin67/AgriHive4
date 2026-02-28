# Firebase Cloud Functions Setup for OTP Email

This directory contains the Firebase Cloud Functions needed to send OTP via email for password reset.

## Prerequisites

1. **Firebase CLI** - Install it: `npm install -g firebase-tools`
2. **Node.js** - Version 18 or higher
3. **Gmail Account** with App Password (or use another email service)

## Setup Instructions

### 1. Configure Email Credentials

Edit `index.js` and replace these values with your email credentials:

```javascript
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'your-email@gmail.com',        // Replace with your Gmail
    pass: 'your-16-character-app-password'  // Replace with your App Password
  }
});
```

### 2. Get Gmail App Password

If using Gmail:
1. Go to your Google Account → Security
2. Enable 2-Step Verification
3. Go to App Passwords (search for it)
4. Create a new app password for "Mail"
5. Use that 16-character password in the code above

### 3. Deploy Cloud Functions

Run these commands in the `firebase-functions` folder:

```bash
cd firebase-functions
npm install
firebase login
firebase init functions
# Select your Firebase project
# Use JavaScript or TypeScript
# Say NO to ESLint
# Say NO to dependencies

# Deploy the functions
firebase deploy --only functions
```

### 4. Enable Cloud Functions in Firebase Console

1. Go to Firebase Console → Your Project → Functions
2. Make sure the functions are enabled
3. Copy the region if different (default is us-central1)

### 5. Update Android Code (if needed)

If your functions are deployed to a different region, update the ViewModel:

```kotlin
// In ForgotPasswordViewModel.kt, change:
private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("your-region")
```

## How It Works

1. User enters email and taps "Send verification code"
2. App calls Cloud Function `sendOtpEmail`
3. Cloud Function:
   - Verifies the email exists in Firebase Auth
   - Generates a random 5-digit OTP
   - Stores OTP in Firestore with 5-minute expiration
   - Sends OTP via email
4. User receives the OTP code in their email
5. User enters the 5-digit OTP in the app
6. App calls Cloud Function `verifyOtp`
7. On success, user can proceed to reset password

## Testing Locally

You can test the functions locally:

```bash
firebase emulators:start --only functions
```

Then update your Android code to use localhost:
```kotlin
functions.useEmulator("10.0.2.2", 5001) // For Android emulator
```
