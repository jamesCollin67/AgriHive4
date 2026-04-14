# AgriHive — Test Case Document

## Mobile App (Android)

### TC-M01: User Registration
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Open app, tap "Sign Up" | Registration screen opens |
| 2 | Fill all fields with valid data | No inline errors shown |
| 3 | Tap "Register" | Loading spinner appears |
| 4 | Registration completes | "Verify your email" message shown, redirected to Login |
| 5 | Check email inbox | Verification email received |

**Edge Cases:**
- Empty fields → error message shown per field
- Invalid email format → "Please enter a valid email"
- Password without uppercase/number/special char → password strength error
- Duplicate email → "This email is already registered"

---

### TC-M02: User Login
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Enter valid email + password | Login button enabled |
| 2 | Tap "Login" | Loading spinner appears |
| 3 | Login succeeds | Dashboard screen opens |
| 4 | Try login with unverified email | "Please verify your email" error, user signed out |
| 5 | Try wrong password | "Incorrect password" error |
| 6 | Try non-existent email | "No account found" error |

---

### TC-M03: Add Apiary
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Tap "+" on Dashboard | Add Apiary screen opens |
| 2 | Select farm from dropdown | Location auto-fills |
| 3 | Enter apiary name and node ID | Fields populated |
| 4 | Tap "Add Apiary" with no internet | "No internet connection" toast |
| 5 | Tap "Add Apiary" with internet | Loading state, then success toast |
| 6 | Return to Dashboard | New apiary appears in list |

---

### TC-M04: Hive Streams — Live Sensor Data
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Tap an apiary card | HiveStreams screen opens |
| 2 | IoT node is connected | Green status dot, real sensor values shown |
| 3 | IoT node is disconnected | Red status dot, values show "--" |
| 4 | Temperature > 36°C | Status label shows "Too Hot" in red |
| 5 | Moisture > 18% | Status label shows "Too Wet" in orange |
| 6 | Moisture ≤ 18% | Status label shows "Harvest Ready" in green |

---

### TC-M05: AI Scanner
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Tap camera icon on HiveStreams | AI Scanner screen opens |
| 2 | Tap "Take Photo" | Camera permission requested (first time) |
| 3 | Take a photo of bees | Crop screen appears |
| 4 | Confirm crop | Loading animation with "Running neural network inference..." |
| 5 | Scan completes | Scan Result screen with disease name, health score, confidence list |
| 6 | Confidence list shows real percentages | All values sum to ~100%, no fabricated data |
| 7 | Tap "Save Result" | Saved to Firestore, navigates to Saved Treatments |

---

### TC-M06: Send Report
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Navigate to Settings → Report Issue | Send Report screen opens |
| 2 | Leave description empty, tap Submit | "Please describe the issue" error |
| 3 | Enter description, tap Submit | Report saved to Firestore, success screen shown |
| 4 | Admin replies from web panel | Mobile notification received within 60 seconds |

---

### TC-M07: Subscription Expiry
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | User with expired subscription logs in | Dashboard loads |
| 2 | Expiry check runs | Toast: "Your subscription has expired" |
| 3 | User is redirected | Subscription screen opens |

---

### TC-M08: Logout
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Go to Settings → Logout | Confirmation dialog appears |
| 2 | Tap "Log out" | Session cleared, Login screen shown |
| 3 | Press back button | Cannot return to Dashboard |

---

## Web Admin Panel

### TC-W01: Admin Login
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Open `http://localhost:5173` | Login page shown |
| 2 | Enter non-admin Firebase user credentials | "Access Denied" screen shown |
| 3 | Enter admin credentials | Dashboard loads |
| 4 | Navigate to `/dashboard` without logging in | Redirected to `/login` |
| 5 | Refresh page while logged in | Stays on current page (auth persists) |

---

### TC-W02: Dashboard
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | View Dashboard | 4 stat cards show correct counts |
| 2 | Pending reports exist | "Pending Reports" card shows red gradient |
| 3 | Click a farm card | Navigates to Reports filtered by that farm |
| 4 | Click "Pending Reports" card | Navigates to Reports page |

---

### TC-W03: User Management
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Search by first name | Matching users shown |
| 2 | Search by farm name | Matching users shown |
| 3 | Click a user card | User detail view opens |
| 4 | Click "Deactivate Account" | Confirmation dialog appears |
| 5 | Confirm deactivation | User status changes to "Inactive" |
| 6 | Click "Reactivate Account" on inactive user | User status changes to "Active" |
| 7 | Click "Delete Account" | Confirmation dialog with warning |
| 8 | Confirm delete | User removed from list |
| 9 | Click "Export CSV" | CSV file downloaded with all users |

---

### TC-W04: Reports Management
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | View Reports page | All reports listed, pending ones have green left border |
| 2 | Filter by "Pending" | Only unread reports shown |
| 3 | Filter by farm name chip | Only that farm's reports shown |
| 4 | Search "disease" | Reports containing "disease" in message shown |
| 5 | Click a report | Detail view opens with timestamp |
| 6 | Click "Reply" | Reply text box appears |
| 7 | Type reply and send | Success dialog shown, reply saved to Firestore |
| 8 | Click "Send Follow-up" on replied report | Follow-up reply box appears |
| 9 | Click "Delete" | Confirmation dialog appears |
| 10 | Confirm delete | Report removed from list |

---

### TC-W05: Subscriptions
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | View Subscriptions page | Confirmed and pending counts shown |
| 2 | Click "Confirm" on pending subscription | Confirmation dialog with payment details |
| 3 | Confirm subscription | Success animation, status changes to "Confirmed" |
| 4 | Click "Reject" on pending subscription | Rejection confirmation dialog |
| 5 | Confirm rejection | Subscription marked as rejected |
| 6 | Confirmed subscription near expiry (≤7 days) | Red "⚠ Xd left" shown on card |

---

### TC-W06: Logout
| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Click Logout button | Confirmation dialog appears |
| 2 | Confirm logout | Firebase session ended, redirected to Login |
| 3 | Navigate to `/dashboard` after logout | Redirected to `/login` |

---

## Error Scenario Tests

| ID | Scenario | Expected Behavior |
|----|----------|------------------|
| E01 | Mobile app loses internet mid-operation | "No Internet Connection. Showing cached data." message |
| E02 | Firestore write fails | Error toast shown, operation can be retried |
| E03 | Web panel JS error in a component | Error Boundary catches it, shows "Something went wrong" with Reload button |
| E04 | Admin tries to access `/dashboard` without being in `admins` collection | "Access Denied" screen shown |
| E05 | Beekeeper tries to log in with unverified email | Signed out, error message shown |
| E06 | AI Scanner model fails to load | "AI Engine failed to start" error message |
| E07 | Report submission with empty description | "Please describe the issue" validation error |
| E08 | Add Apiary with missing fields | "Please fill in all fields" error |
