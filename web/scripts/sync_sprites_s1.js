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

const s1Mapping = {
  '01': 'Water Sprites',
  '02': 'Golden Water Sprite',
  '03': 'Gummy Water Sprite',
  '04': 'Galaxy Water Sprite',
  
  '06': 'Golden Dream Sprite',
  '07': 'Gummy Dream Sprite',
  '08': 'Galaxy Dream Sprite',
  
  '10': 'Golden Earth Sprite',
  '11': 'Gummy Earth Sprite',
  '12': 'Galaxy Earth Sprite',
  
  '15': 'Golden Demon Sprite',
  '16': 'Gummy Demon Sprite',
  '17': 'Galaxy Demon Sprite',
  
  '19': 'Golden Fire Sprite',
  '20': 'Gummy Fire Sprite',
  '21': 'Galaxy Fire Sprite',
  
  '23': 'Golden Punk Sprite',
  '24': 'Gummy Punk Sprite',
  '25': 'Galaxy Punk Sprite',
  
  '26': 'Ugly Sprite',
  '27': 'Golden Duck Sprite',
  '28': 'Gummy Duck Sprite',
  '29': 'Galaxy Duck Sprite',
  
  '31': 'Golden King Sprite',
  '32': 'Gummy King Sprite',
  '33': 'Galaxy King Sprite',
  
  '35': 'Golden Ghost Sprite',
  '36': 'Gummy Ghost Sprite',
  '37': 'Galaxy Ghost Sprite',
  
  '38': 'Zero Point Sprite',
  '39': 'Golden Zero Point Sprite',
  '40': 'Gummy Zero Point Sprite',
  '41': 'Galaxy Zero Point Sprite',
  
  '42': 'Aura Sprite',
  '43': 'Golden Aura Sprite',
  '44': 'Galaxy Aura Sprite',
  '45': 'Gummy Aura Sprite',
  
  '46': 'Seven Sprite',
  '47': 'Golden Seven Sprite',
  '48': 'Galaxy Seven Sprite',
  '49': 'Gummy Seven Sprite',
  
  '50': 'Grim Sprite',
  '51': 'Golden Grim Sprite',
  '52': 'Galaxy Grim Sprite',
  '53': 'Gummy Grim Sprite',
  
  '54': 'Striker Sprite',
  '55': 'Golden Striker Sprite',
  '56': 'Galaxy Striker Sprite',
  '57': 'Gummy Striker Sprite',
  
  '58': 'Boss Sprite',
  '59': 'Golden Boss Sprite',
  '60': 'Galaxy Boss Sprite',
  '61': 'Gummy Boss Sprite',
  
  '62': 'Fishy Sprite',
  '63': 'Golden Fishy Sprite',
  '64': 'Galaxy Fishy Sprite',
  '65': 'Gummy Fishy Sprite',
  
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
  
  '98': 'Batman Sprite',
  '99': 'Golden Batman Sprite',
  '100': 'Gummy Batman Sprite',
  '101': 'Galaxy Batman Sprite',
  '103': 'Holofoil Batman Sprite',
  '104': 'Cube Batman Sprite',
  
  '105': 'Air Sprite',
  '106': 'Golden Air Sprite',
  '107': 'Gummy Air Sprite',
  '108': 'Galaxy Air Sprite',
  '110': 'Holofoil Air Sprite',
  
  '113': 'Cube Dream Sprite',
  '114': 'Cube Earth Sprite',
  '116': 'Cube Fire Sprite',
  '117': 'Cube Punk Sprite',
  
  '119': 'Pollo Sprite',
  '120': 'Vini Jr. Sprite',
  '121': 'Seven Sprite',
  
  '122': 'Llama Sprite',
  '123': 'Golden Llama Sprite',
  '124': 'Gummy Llama Sprite',
  '125': 'Galaxy Llama Sprite',
  '126': 'Gem Llama Sprite',
  
  '129': 'Peely Sprite',
  '130': 'Golden Peely Sprite',
  '131': 'Gummy Peely Sprite',
  '132': 'Holofoil Peely Sprite',
  '133': 'Galaxy Peely Sprite',
  
  '134': 'Holofoil Grim Sprite',
  '135': 'Gem Grim Sprite',
  
  '136': 'Quack Earth Sprite',
  '137': 'Quack Fire Sprite',
  '138': 'Quack Water Sprite',
  '139': 'Quack Zero Point Sprite',
  
  '140': 'John Wick Sprite',
  '141': 'Ironmouse Sprite'
};

async function syncSeason1() {
  console.log(`Iniciando subida a Cloudinary de Temporada 1 (${Object.keys(s1Mapping).length} espíritus)...`);
  const uploadedUrls = {};
  
  const entries = Object.entries(s1Mapping);
  for (let i = 0; i < entries.length; i++) {
    const [id, fnggTitle] = entries[i];
    const sourceUrl = fnggMap[fnggTitle];
    if (!sourceUrl) {
      console.warn(`[!] No se encontró URL para #${id} (${fnggTitle})`);
      continue;
    }
    
    const publicId = `spirits/ic_spirit_${id}`;
    try {
      const res = await cloudinary.uploader.upload(sourceUrl, {
        public_id: publicId,
        overwrite: true,
        invalidate: true,
        resource_type: 'image',
        format: 'png'
      });
      uploadedUrls[id] = res.secure_url;
      console.log(`[${i+1}/${entries.length}] #${id} (${fnggTitle}) -> ${res.secure_url}`);
    } catch (err) {
      console.error(`Error subiendo #${id} (${fnggTitle}):`, err.message);
    }
  }

  fs.writeFileSync(path.join(__dirname, '../scratch_s1_cloudinary_urls.json'), JSON.stringify(uploadedUrls, null, 2));
  console.log('Finalizada la subida a Cloudinary para T1.');
}

if (require.main === module) {
  syncSeason1();
}

module.exports = { s1Mapping, syncSeason1 };
