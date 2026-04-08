import { initializeApp } from "firebase/app";
import { getFirestore, doc, setDoc, getDoc } from "firebase/firestore";
import { getAuth } from "firebase/auth";
import { getStorage } from "firebase/storage";
import { getDatabase } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyCL4qI8eRSm5sa_uJuRgQtiplzFMv8xZXg",
  authDomain: "agrihive-cd18a.firebaseapp.com",
  projectId: "agrihive-cd18a",
  storageBucket: "agrihive-cd18a.firebasestorage.app",
  messagingSenderId: "838887664490",
  appId: "1:838887664490:web:1234567890abcdef"
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const rtdb = getDatabase(app);

// Seed bee_farms collection once (safe to run multiple times — uses setDoc with merge)
async function seedBeeFarms() {
  const farms = [
    { id: "apis_prince", name: "Apis Prince Honeybee Farm", address: "Apis Prince Honeybee Farm, Greener's Farm, Taptap, Cebu City, Cebu" },
    { id: "gkg_farm",    name: "GKG FARM CEBU PH",          address: "Cansiguiring, Carmen, Cebu" },
  ];
  for (const farm of farms) {
    const ref = doc(db, "bee_farms", farm.id);
    const snap = await getDoc(ref);
    if (!snap.exists()) {
      await setDoc(ref, { name: farm.name, address: farm.address });
    }
  }
}
seedBeeFarms().catch(console.error);

export default app;
