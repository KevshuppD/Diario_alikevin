const firebase = require('firebase/compat/app');
require('firebase/compat/firestore');

const sourceConfig = {
  apiKey: "AIzaSyAY4lXaHIXNlftWZ3XcLgm4xuQE0atLnoM",
  authDomain: "diario-pareja-a2d35.firebaseapp.com",
  projectId: "diario-pareja-a2d35",
  storageBucket: "diario-pareja-a2d35.appspot.com",
  messagingSenderId: "542387114631",
  appId: "1:542387114631:web:ca3bb4b8cf6c491295f5be"
};

const targetConfig = {
  apiKey: "AIzaSyDH4Pt9gejrD0hh3e-ph_LzTUyxJXGTOYE",
  authDomain: "diario-ali-kevin.firebaseapp.com",
  projectId: "diario-ali-kevin",
  storageBucket: "diario-ali-kevin.firebasestorage.app",
  messagingSenderId: "767110805470",
  appId: "1:767110805470:android:e9f57cbde491f048064f4f"
};

async function runMigration() {
  console.log('🔄 Inicializando instancias de Firestore...');
  const appSource = firebase.initializeApp(sourceConfig, 'sourceApp');
  const dbSource = appSource.firestore();

  const appTarget = firebase.initializeApp(targetConfig, 'targetApp');
  const dbTarget = appTarget.firestore();

  const collections = [
    'pets',
    'messages',
    'calendar',
    'medications',
    'recipes',
    'spirit_seasons',
    'spirit_checklist',
    'locations',
    'users'
  ];

  let totalDocs = 0;

  for (const colName of collections) {
    console.log(`\n📦 Leyendo colección '${colName}' desde BD Origen (diario-pareja-a2d35)...`);
    try {
      const snap = await dbSource.collection(colName).get();
      console.log(`   Encontrados ${snap.size} documentos. Migrando a BD Destino (diario-ali-kevin)...`);

      let count = 0;
      for (const doc of snap.docs) {
        const data = doc.data();
        await dbTarget.collection(colName).document(doc.id).set(data, { merge: true });
        count++;
        totalDocs++;

        if (colName === 'locations') {
          const subcols = ['zones', 'users', 'history_kevin', 'history_ali', 'pings'];
          for (const sub of subcols) {
            try {
              const subSnap = await dbSource.collection('locations').document(doc.id).collection(sub).get();
              for (const subDoc of subSnap.docs) {
                await dbTarget.collection('locations').document(doc.id).collection(sub).document(subDoc.id).set(subDoc.data(), { merge: true });
                totalDocs++;
              }
            } catch (e) {}
          }
        }
      }
      console.log(`✅ Colección '${colName}' migrada con éxito (${count} documentos).`);
    } catch (err) {
      console.error(`❌ Error migrando colección '${colName}':`, err.message);
    }
  }

  console.log(`\n🎉 ¡MIGRACIÓN COMPLETADA! Total documentos transferidos: ${totalDocs}`);
  process.exit(0);
}

runMigration().catch(err => {
  console.error('Error fatal en migración:', err);
  process.exit(1);
});
