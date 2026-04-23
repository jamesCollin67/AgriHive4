import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";
import { getStorage } from "firebase/storage";
import { getDatabase } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyCL4qI8eRSm5sa_uJuRgQtiplzFMv8xZXg",
  authDomain: "agrihive-cd18a.firebaseapp.com",
  databaseURL: "https://agrihive-cd18a-default-rtdb.firebaseio.com",
  projectId: "agrihive-cd18a",
  storageBucket: "agrihive-cd18a.firebasestorage.app",
  messagingSenderId: "838887664490",
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const rtdb = getDatabase(app);

export default app;
