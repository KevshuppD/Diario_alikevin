// ==========================================
// FIREBASE CONFIGURATION & DB INITIALIZATION
// ==========================================

import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  initializeFirestore,
  getFirestore,
  persistentLocalCache,
  persistentMultipleTabManager
} from 'firebase/firestore';
import { getDatabase } from 'firebase/database';

export const firebaseConfig = {
  apiKey: "AIzaSyDH4Pt9gejrD0hh3e-ph_LzTUyxJXGTOYE",
  authDomain: "diario-ali-kevin.firebaseapp.com",
  databaseURL: "https://diario-ali-kevin-default-rtdb.firebaseio.com",
  projectId: "diario-ali-kevin",
  storageBucket: "diario-ali-kevin.firebasestorage.app",
  messagingSenderId: "767110805470",
  appId: "1:767110805470:web:baaa7771e132758a3ced8f"
};

const app = getApps().length ? getApp() : initializeApp(firebaseConfig);

// Persistencia multi-pestaña en IndexedDB para carga instantánea y sincronización en tiempo real
let firestoreInstance;
try {
  firestoreInstance = initializeFirestore(app, {
    localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() })
  });
} catch (err) {
  console.warn("Firestore persistence notice:", err.code || err.message);
  firestoreInstance = getFirestore(app);
}

export const db = firestoreInstance;
export const rtdb = getDatabase(app);

export { doc, collection, setDoc, onSnapshot, serverTimestamp } from 'firebase/firestore';
export { ref as rtdbRef, onValue as rtdbOnValue, set as rtdbSet, update as rtdbUpdate } from 'firebase/database';
