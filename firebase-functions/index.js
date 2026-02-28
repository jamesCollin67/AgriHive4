const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

admin.initializeApp();

// Configure your email transporter
// For Gmail, use App Password: https://support.google.com/accounts/answer/185833
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'YOUR_EMAIL@gmail.com', // Replace with your email
    pass: 'YOUR_APP_PASSWORD'      // Replace with your app password
  }
});

// Generate a random 5-digit OTP
function generateOTP() {
  return Math.floor(10000 + Math.random() * 90000).toString();
}

// Store OTP in Firestore with expiration (5 minutes)
async function storeOTP(email, otp) {
  const db = admin.firestore();
  await db.collection('otps').doc(email).set({
    otp: otp,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 5 * 60 * 1000)), // 5 minutes
    verified: false
  });
}

// Send OTP via email
async function sendOTPEmail(email, otp) {
  const mailOptions = {
    from: 'AgriHive <noreply@agrihive.com>',
    to: email,
    subject: 'Your AgriHive Password Reset Code',
    html: `
      <div style="font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #FFC107;">AgriHive Password Reset</h2>
        <p>You requested a password reset for your AgriHive account.</p>
        <div style="background-color: #f5f5f5; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
          <p style="font-size: 14px; color: #666; margin: 0;">Your verification code is:</p>
          <h1 style="font-size: 36px; color: #333; margin: 10px 0; letter-spacing: 8px;">${otp}</h1>
        </div>
        <p style="color: #666; font-size: 12px;">This code will expire in 5 minutes.</p>
        <p style="color: #666; font-size: 12px;">If you didn't request this, please ignore this email.</p>
      </div>
    `
  };

  return transporter.sendMail(mailOptions);
}

// Cloud Function: Send OTP Email
exports.sendOtpEmail = functions.https.onCall(async (data, context) => {
  const email = data.email;
  
  if (!email) {
    throw new functions.https.HttpsError('invalid-argument', 'Email is required');
  }

  // Validate email format
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid email format');
  }

  try {
    // Check if user exists in Firebase Auth
    try {
      await admin.auth().getUserByEmail(email);
    } catch (error) {
      // If user doesn't exist, return success anyway (don't reveal if email exists)
      return { success: true, message: 'If the email exists, a code will be sent' };
    }

    // Generate OTP
    const otp = generateOTP();

    // Store OTP in Firestore
    await storeOTP(email, otp);

    // Send email
    await sendOTPEmail(email, otp);

    return { 
      success: true, 
      message: 'Verification code sent successfully' 
    };
  } catch (error) {
    console.error('Error sending OTP:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send verification code');
  }
});

// Cloud Function: Verify OTP
exports.verifyOtp = functions.https.onCall(async (data, context) => {
  const email = data.email;
  const otp = data.otp;

  if (!email || !otp) {
    throw new functions.https.HttpsError('invalid-argument', 'Email and OTP are required');
  }

  try {
    const db = admin.firestore();
    const otpDoc = await db.collection('otps').doc(email).get();

    if (!otpDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'No verification code found');
    }

    const otpData = otpDoc.data();

    // Check if OTP is expired
    if (otpData.expiresAt.toDate() < new Date()) {
      throw new functions.https.HttpsError('deadline-exceeded', 'Verification code has expired');
    }

    // Check if already verified
    if (otpData.verified) {
      throw new functions.https.HttpsError('failed-precondition', 'Code already verified');
    }

    // Verify OTP
    if (otpData.otp !== otp) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid verification code');
    }

    // Mark as verified
    await db.collection('otps').doc(email).update({
      verified: true
    });

    return { 
      success: true, 
      message: 'OTP verified successfully' 
    };
  } catch (error) {
    console.error('Error verifying OTP:', error);
    throw new functions.https.HttpsError('invalid-argument', error.message || 'Verification failed');
  }
});
