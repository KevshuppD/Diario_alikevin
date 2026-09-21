const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });
const cloudinary = require('cloudinary').v2;
const fs = require('fs');

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || 'dhaqjw7se',
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

const fnggSprites = JSON.parse(fs.readFileSync(path.join(__dirname, '../scratch_fortnite_sprites.json'), 'utf-8'));
const fnggMap = {};
fnggSprites.forEach(s => { fnggMap[s.title] = s.img_url; });

const s1FnggMapping = {
  // Agua (1..4)
  '01': 'Water Sprites',
  '02': 'Golden Water Sprite',
  '03': 'Gummy Water Sprite',
  '04': 'Galaxy Water Sprite',
  
  // Dormilón (6..8)
  '06': 'Golden Dream Sprite',
  '07': 'Gummy Dream Sprite',
  '08': 'Galaxy Dream Sprite',
  
  // Tierra (10..12)
  '10': 'Golden Earth Sprite',
  '11': 'Gummy Earth Sprite',
  '12': 'Galaxy Earth Sprite',
  
  // Demoníaco (15..17)
  '15': 'Golden Demon Sprite',
  '16': 'Gummy Demon Sprite',
  '17': 'Galaxy Demon Sprite',
  
  // Fuego (19..21)
  '19': 'Golden Fire Sprite',
  '20': 'Gummy Fire Sprite',
  '21': 'Galaxy Fire Sprite',
  
  // Punk (23..25)
  '23': 'Golden Punk Sprite',
  '24': 'Gummy Punk Sprite',
  '25': 'Galaxy Punk Sprite',
  
  // Pato (26..29)
  '26': 'Ugly Sprite',
  '27': 'Golden Duck Sprite',
  '28': 'Gummy Duck Sprite',
  '29': 'Galaxy Duck Sprite',
  
  // Monarca (31..33)
  '31': 'Golden King Sprite',
  '32': 'Gummy King Sprite',
  '33': 'Galaxy King Sprite',
  
  // Fantasma (35..37)
  '35': 'Golden Ghost Sprite',
  '36': 'Gummy Ghost Sprite',
  '37': 'Galaxy Ghost Sprite',
  
  // Punto Cero (38..41)
  '38': 'Zero Point Sprite',
  '39': 'Golden Zero Point Sprite',
  '40': 'Gummy Zero Point Sprite',
  '41': 'Galaxy Zero Point Sprite',
  
  // Aura (42..45) Note: 44 is Galaxia, 45 is Gomita
  '42': 'Aura Sprite',
  '43': 'Golden Aura Sprite',
  '44': 'Galaxy Aura Sprite',
  '45': 'Gummy Aura Sprite',
  
  // Fundación (46..49) Note: 48 is Galaxia, 49 is Gomita
  '46': 'Seven Sprite',
  '47': 'Golden Seven Sprite',
  '48': 'Galaxy Seven Sprite',
  '49': 'Gummy Seven Sprite',
  
  // Parca (50..53)
  '50': 'Grim Sprite',
  '51': 'Golden Grim Sprite',
  '52': 'Galaxy Grim Sprite',
  '53': 'Gummy Grim Sprite',
  
  // Futbolero (54..57)
  '54': 'Striker Sprite',
  '55': 'Golden Striker Sprite',
  '56': 'Galaxy Striker Sprite',
  '57': 'Gummy Striker Sprite',
  
  // Jefe (58..61)
  '58': 'Boss Sprite',
  '59': 'Golden Boss Sprite',
  '60': 'Galaxy Boss Sprite',
  '61': 'Gummy Boss Sprite',
  
  // Pescado (62..65)
  '62': 'Fishy Sprite',
  '63': 'Golden Fishy Sprite',
  '64': 'Galaxy Fishy Sprite',
  '65': 'Gummy Fishy Sprite',
  
  // Variantes Gema y Holofoil (66..97)
  '66': 'Gem Water Sprite',
  '67': 'Holofoil Water Sprite',
  '70': 'Gem Earth Sprite',
  '72': 'Gem Demon Sprite',
  '75': 'Holofoil Fire Sprite',
  '78': 'Gem Duck Sprite',
  '81': 'Holofoil King Sprite',
  '83': 'Holofoil Ghost Sprite',
  '84': 'Gem Zero Point Sprite',
  '85': 'Holofoil Zero Point Sprite',
  '86': 'Gem Aura Sprite',
  '89': 'Holofoil Seven Sprite',
  '90': 'Gem Grim Sprite',
  '91': 'Holofoil Grim Sprite',
  '93': 'Holofoil Striker Sprite',
  
  // Batman (98..104)
  '98': 'Batman Sprite',
  '99': 'Golden Batman Sprite',
  '100': 'Gummy Batman Sprite',
  '101': 'Galaxy Batman Sprite',
  '103': 'Holofoil Batman Sprite',
  '104': 'Cube Batman Sprite',
  
  // Viento (105..110)
  '105': 'Air Sprite',
  '106': 'Golden Air Sprite',
  '107': 'Gummy Air Sprite',
  '108': 'Galaxy Air Sprite',
  '110': 'Holofoil Air Sprite',
  
  // Cubo (113..117)
  '113': 'Cube Dream Sprite',
  '114': 'Cube Earth Sprite',
  '116': 'Cube Fire Sprite',
  '117': 'Cube Punk Sprite',
  
  // Especiales (119..121)
  '119': 'Pollo Sprite',
  '120': 'Vini Jr. Sprite',
  '121': 'Seven Sprite',
  
  // Llama (122..126)
  '122': 'Llama Sprite',
  '123': 'Golden Llama Sprite',
  '124': 'Gummy Llama Sprite',
  '125': 'Galaxy Llama Sprite',
  '126': 'Gem Llama Sprite',
  
  // Bananín (129..133)
  '129': 'Peely Sprite',
  '130': 'Golden Peely Sprite',
  '131': 'Gummy Peely Sprite',
  '132': 'Holofoil Peely Sprite',
  '133': 'Galaxy Peely Sprite',
  
  // Parca Especial (134..135)
  '134': 'Holofoil Grim Sprite',
  '135': 'Gem Grim Sprite',
  
  // Quack (136..139)
  '136': 'Quack Earth Sprite',
  '137': 'Quack Fire Sprite',
  '138': 'Quack Water Sprite',
  '139': 'Quack Zero Point Sprite',
  
  // Collabs (140..141)
  '140': 'John Wick Sprite',
  '141': 'Ironmouse Sprite'
};

async function uploadAllSeason1() {
  console.log('🚀 Iniciando restauración y sincronización completa de los 141 Espíritus de Temporada 1...');
  const results = {};

  for (let i = 1; i <= 141; i++) {
    const id = String(i).padStart(2, '0');
    const publicId = `spirits/ic_spirit_${id}`;
    const fnggTitle = s1FnggMapping[id];
    let uploadSource = null;
    let sourceType = '';

    if (fnggTitle && fnggMap[fnggTitle]) {
      uploadSource = fnggMap[fnggTitle];
      sourceType = `FNGG (${fnggTitle})`;
    } else {
      const origPath = path.join(__dirname, '../../scripts/spirits_t1_original/drawable', `ic_spirit_${id}.png`);
      if (fs.existsSync(origPath)) {
        uploadSource = origPath;
        sourceType = `ORIGINAL (${origPath})`;
      } else {
        console.error(`❌ No hay fuente para #${id}`);
        continue;
      }
    }

    try {
      const res = await cloudinary.uploader.upload(uploadSource, {
        public_id: publicId,
        overwrite: true,
        invalidate: true,
        resource_type: 'image',
        format: 'png'
      });
      results[id] = res.secure_url;
      console.log(`[${i}/141] #${id} [${sourceType}] -> ${res.secure_url}`);
    } catch (err) {
      console.error(`❌ Error en #${id}:`, err.message);
    }
  }

  fs.writeFileSync(path.join(__dirname, '../scratch_s1_cloudinary_urls.json'), JSON.stringify(results, null, 2));
  console.log('✅ Restauración y subida completa de los 141 Espíritus de Temporada 1 finalizada!');
}

if (require.main === module) {
  uploadAllSeason1();
}

module.exports = { uploadAllSeason1 };
