const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });
const cloudinary = require('cloudinary').v2;
const fs = require('fs');
const https = require('https');

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || 'dhaqjw7se',
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

const fnggSprites = JSON.parse(fs.readFileSync(path.join(__dirname, '../scratch_fortnite_sprites.json'), 'utf-8'));
const fnggMap = {};
fnggSprites.forEach(s => { fnggMap[s.title] = s.img_url; });

const s2Mapping = {
  '01': 'Klombo Sprite',
  '02': 'Gold Klombo Sprite',
  '03': 'Cheat Master Klombo Sprite',
  '52': 'Loot Hacker Klombo Sprite',

  '04': 'Killswitch Sprite',
  '05': 'Gold Killswitch Sprite',
  '06': 'Cheat Master Killswitch Sprite',
  '51': 'Loot Hacker Killswitch Sprite',

  '07': 'Jonesy Sprite',
  '08': 'Gold Jonesy Sprite',
  '09': 'Cheat Master Jonesy Sprite',
  '53': 'Loot Hacker Jonesy Sprite',

  '10': '8-Bit Sprite',
  '11': 'Gold 8-Bit Sprite',
  '12': 'Cheat Master 8-Bit Sprite',
  '54': 'Loot Hacker 8-Bit Sprite',

  '13': 'Jackrabbit Sprite',
  '14': 'Gold Jackrabbit Sprite',
  '15': 'Cheat Master Jackrabbit Sprite',
  '55': 'Loot Hacker Jackrabbit Sprite',

  '16': 'Crown Sprite',
  '17': 'Gold Crown Sprite',
  '18': 'Cheat Master Crown Sprite',
  '56': 'Loot Hacker Crown Sprite',

  '19': 'Adventure Sprite',
  '20': 'Gold Adventure Sprite',
  '21': 'Cheat Master Adventure Sprite',
  '50': 'Loot Hacker Adventure Sprite',

  '22': 'Bush Sprite',
  '23': 'Gold Bush Sprite',
  '24': 'Cheat Master Bush Sprite',
  '61': 'Loot Hacker Bush Sprite',

  '25': 'Storm Scout Sprite',
  '26': 'Gold Storm Scout Sprite',
  '27': 'Cheat Master Storm Scout Sprite',
  '60': 'Loot Hacker Storm Scout Sprite',

  '28': 'Shadow Sprite',
  '29': 'Gold Shadow Sprite',
  '30': 'Cheat Master Shadow Sprite',
  '58': 'Loot Hacker Shadow Sprite',

  '31': 'Tails Sprite',
  '32': 'Gold Tails Sprite',
  '33': 'Cheat Master Tails Sprite',
  '59': 'Loot Hacker Tails Sprite',

  '34': 'Sonic Sprite',
  '35': 'Gold Sonic Sprite',
  '36': 'Cheat Master Sonic Sprite',
  '57': 'Loot Hacker Sonic Sprite',

  '37': 'Overshield Sprite',
  '39': 'Gold Overshield Sprite',
  '38': 'Cheatmaster Overshield Sprite',
  '40': 'Loot Hacker Overshield Sprite',

  '41': 'Onigiri Sprite (Created by Enorull)',
  '43': 'Gold Onigiri Sprite (Created by Enorull)',
  '42': 'Cheatmaster Onigiri Sprite (Created by Enorull)',
  '44': 'Loot Hacker Onigiri Sprite (Created by Enorull)',

  '45': 'X-Ray Sprite (Created by Avila215)',
  '47': 'Gold X-Ray Sprite (Created by Avila215)',
  '46': 'Cheatmaster X-Ray Sprite (Created by Avila215)',
  '48': 'Loot Hacker X-Ray Sprite (Created by Avila215)',

  '49': 'Mega Man Sprite'
};

async function syncSeason2() {
  console.log('Iniciando subida a Cloudinary de Temporada 2 (61 espíritus)...');
  const uploadedUrls = {};
  
  const entries = Object.entries(s2Mapping);
  for (let i = 0; i < entries.length; i++) {
    const [sid, fnggTitle] = entries[i];
    const sourceUrl = fnggMap[fnggTitle];
    if (!sourceUrl) {
      console.warn('⚠️ No se encontró URL para:', sid, fnggTitle);
      continue;
    }
    const publicId = 'spirits_s2/ic_spirit_s2_' + sid;
    try {
      const res = await cloudinary.uploader.upload(sourceUrl, {
        public_id: publicId,
        overwrite: true,
        resource_type: 'image'
      });
      uploadedUrls[sid] = res.secure_url;
      console.log(`[${i+1}/${entries.length}] #${sid} (${fnggTitle}) -> ${res.secure_url}`);
    } catch (err) {
      console.error(`Error subiendo #${sid}:`, err.message);
    }
  }

  fs.writeFileSync(path.join(__dirname, '../scratch_s2_cloudinary_urls.json'), JSON.stringify(uploadedUrls, null, 2));
  console.log('✅ Finalizada subida a Cloudinary de Temporada 2!');
}

syncSeason2().catch(console.error);
