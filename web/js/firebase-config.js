// ==========================================
// FIREBASE CONFIGURATION & DB INITIALIZATION
// ==========================================

export const firebaseConfig = {
  apiKey: "AIzaSyDH4Pt9gejrD0hh3e-ph_LzTUyxJXGTOYE",
  authDomain: "diario-ali-kevin.firebaseapp.com",
  databaseURL: "https://diario-ali-kevin-default-rtdb.firebaseio.com",
  projectId: "diario-ali-kevin",
  storageBucket: "diario-ali-kevin.firebasestorage.app",
  messagingSenderId: "767110805470",
  appId: "1:767110805470:web:baaa7771e132758a3ced8f"
};

if (!firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}

export const db = firebase.firestore();
export const rtdb = firebase.database();

// Habilitar caché offline de Firestore para carga instantánea
db.enablePersistence({ synchronizeTabs: true }).catch(err => {
  console.log("Persistence notice:", err.code);
});
