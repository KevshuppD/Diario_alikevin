// ==========================================
// CONSTANTS & PALETTES - DIARIO ALI Y KEVIN
// ==========================================

export const defaultSpiritsListT1 = Array.from({ length: 141 }, (_, i) => String(i + 1).padStart(2, '0'));
export const defaultSpiritsListT2 = Array.from({ length: 105 }, (_, i) => String(i + 1).padStart(2, '0'));
export const defaultSpiritsList = defaultSpiritsListT1;

export const defaultCategoriesT1 = [
  { name: "Espíritu de Batman", spiritIds: ["98", "99", "100", "101", "102", "103", "104"] },
  { name: "Espíritu de Agua", spiritIds: ["01", "02", "03", "04", "66", "67", "112"] },
  { name: "Espíritu de Tierra", spiritIds: ["09", "10", "11", "12", "70", "71", "114"] },
  { name: "Espíritu de Fuego", spiritIds: ["18", "19", "20", "21", "74", "75", "116"] },
  { name: "Espíritu Pato", spiritIds: ["26", "27", "28", "29", "78", "79", "118", "136", "137", "138", "139"] },
  { name: "Espíritu Fantasma", spiritIds: ["34", "35", "36", "37", "82", "83"] },
  { name: "Espíritu Dormilón", spiritIds: ["05", "06", "07", "08", "68", "69", "113"] },
  { name: "Espíritu Demoníaco", spiritIds: ["14", "15", "16", "17", "72", "73", "115"] },
  { name: "Espíritu Punk", spiritIds: ["22", "23", "24", "25", "76", "77", "117"] },
  { name: "Espíritu Monarca", spiritIds: ["30", "31", "32", "33", "80", "81"] },
  { name: "Espíritu del Punto Cero", spiritIds: ["38", "39", "40", "41", "84", "85"] },
  { name: "Espíritu Pescado", spiritIds: ["62", "63", "64", "65", "96", "97"] },
  { name: "Espíritu Futbolero", spiritIds: ["54", "55", "56", "57", "92", "93"] },
  { name: "Espíritu de Aura", spiritIds: ["42", "43", "44", "45", "86", "87"] },
  { name: "Espíritu Jefe", spiritIds: ["58", "59", "60", "61", "94", "95"] },
  { name: "Espíritu de la Parca", spiritIds: ["50", "51", "52", "53", "90", "91", "134", "135"] },
  { name: "Espíritu de Viento", spiritIds: ["105", "106", "107", "108", "109", "110", "111"] },
  { name: "Espíritu de la Fundación", spiritIds: ["46", "47", "48", "49", "88", "89"] },
  { name: "Espíritu de Llama", spiritIds: ["122", "123", "124", "125", "126"] },
  { name: "Espíritu de Bananín", spiritIds: ["129", "130", "131", "132", "133"] },
  { name: "Espíritu de Cubo", spiritIds: ["127", "128"] },
  { name: "Espíritu Especial/Invitado", spiritIds: ["13", "119", "120", "121", "140", "141"] }
];
export const defaultCategories = defaultCategoriesT1;

export const defaultCategoriesT2 = [
  { name: "Espíritu de Rex", spiritIds: ["01", "02", "03", "52", "71"] },
  { name: "Espíritu Táctico", spiritIds: ["04", "05", "06", "51", "72"] },
  { name: "Espíritu Agente", spiritIds: ["07", "08", "09", "53", "73"] },
  { name: "Espíritu Game Boy", spiritIds: ["10", "11", "12", "54", "74"] },
  { name: "Espíritu Conejo", spiritIds: ["13", "14", "15", "55", "75"] },
  { name: "Espíritu Rey", spiritIds: ["16", "17", "18", "56", "76"] },
  { name: "Espíritu Pícaro", spiritIds: ["19", "20", "21", "50", "77"] },
  { name: "Espíritu Erizo", spiritIds: ["22", "23", "24", "61", "78"] },
  { name: "Espíritu Oni", spiritIds: ["25", "26", "27", "60", "79"] },
  { name: "Espíritu de Shadow", spiritIds: ["28", "29", "30", "58", "80"] },
  { name: "Espíritu de Tails", spiritIds: ["31", "32", "33", "59", "81"] },
  { name: "Espíritu de Sonic", spiritIds: ["34", "35", "36", "57", "82"] },
  { name: "Espíritu Caballero", spiritIds: ["37", "38", "39", "40", "95"] },
  { name: "Espíritu Onigiri", spiritIds: ["41", "42", "43", "44", "83"] },
  { name: "Espíritu Científico", spiritIds: ["45", "46", "47", "48", "84"] },
  { name: "Espíritu Especial/Invitado", spiritIds: ["49"] },
  { name: "Espíritu de Blinky", spiritIds: ["62", "63", "64", "65", "66"] },
  { name: "Espíritu de Cash Bandicoot", spiritIds: ["67", "68", "69", "70", "85"] },
  { name: "Espíritu del Estanque", spiritIds: ["96", "97", "98", "99", "100"] },
  { name: "Espíritu de Morgana", spiritIds: ["86", "87", "88", "89", "90"] },
  { name: "Espíritu de Cumpleaños", spiritIds: ["101", "102", "103", "104", "105"] }
];

export const defaultNames = [
  "Espíritu de Agua", "Espíritu de Agua Dorado", "Espíritu de Agua Gomita", "Espíritu de Agua Galaxia",
  "Espíritu Dormilón", "Espíritu Dormilón Dorado", "Espíritu Dormilón Gomita", "Espíritu Dormilón Galaxia",
  "Espíritu de Tierra", "Espíritu de Tierra Dorado", "Espíritu de Tierra Gomita", "Espíritu de Tierra Galaxia",
  "TheBurntPeanut (Espíritu del Cacahuete)",
  "Espíritu Demoníaco", "Espíritu Demoníaco Dorado", "Espíritu Demoníaco Gomita", "Espíritu Demoníaco Galaxia",
  "Espíritu de Fuego", "Espíritu de Fuego Dorado", "Espíritu de Fuego Gomita", "Espíritu de Fuego Galaxia",
  "Espíritu Punk", "Espíritu Punk Dorado", "Espíritu Punk Gomita", "Espíritu Punk Galaxia",
  "Espíritu Pato", "Espíritu Pato Dorado", "Espíritu Pato Gomita", "Espíritu Pato Galaxia",
  "Espíritu Monarca", "Espíritu Monarca Dorado", "Espíritu Monarca Gomita", "Espíritu Monarca Galaxia",
  "Espíritu Fantasma", "Espíritu Fantasma Dorado", "Espíritu Fantasma Gomita", "Espíritu Fantasma Galaxia",
  "Espíritu del Punto Cero", "Espíritu del Punto Cero Dorado", "Espíritu del Punto Cero Gomita", "Espíritu del Punto Cero Galaxia",
  "Espíritu de Aura", "Espíritu de Aura Dorado", "Espíritu de Aura Galaxia", "Espíritu de Aura Gomita",
  "Espíritu de la Fundación", "Espíritu de la Fundación Dorado", "Espíritu de la Fundación Galaxia", "Espíritu de la Fundación Gomita",
  "Espíritu de la Parca", "Espíritu de la Parca Dorado", "Espíritu de la Parca Galaxia", "Espíritu de la Parca Gomita",
  "Espíritu Futbolero", "Espíritu Futbolero Dorado", "Espíritu Futbolero Galaxia", "Espíritu Futbolero Gomita",
  "Espíritu Jefe", "Espíritu Jefe Dorado", "Espíritu Jefe Galaxia", "Espíritu Jefe Gomita",
  "Espíritu Pescado", "Espíritu Pescado Dorado", "Espíritu Pescado Galaxia", "Espíritu Pescado Gomita",
  "Espíritu de Agua Gema", "Espíritu de Agua Holofoil",
  "Espíritu Dormilón Gema", "Espíritu Dormilón Holofoil",
  "Espíritu de Tierra Gema", "Espíritu de Tierra Holofoil",
  "Espíritu Demoníaco Gema", "Espíritu Demoníaco Holofoil",
  "Espíritu de Fuego Gema", "Espíritu de Fuego Holofoil",
  "Espíritu Punk Gema", "Espíritu Punk Holofoil",
  "Espíritu Pato Gema", "Espíritu Pato Holofoil",
  "Espíritu Monarca Gema", "Espíritu Monarca Holofoil",
  "Espíritu Fantasma Gema", "Espíritu Fantasma Holofoil",
  "Espíritu del Punto Cero Gema", "Espíritu del Punto Cero Holofoil",
  "Espíritu de Aura Gema", "Espíritu de Aura Holofoil",
  "Espíritu de la Fundación Gema", "Espíritu de la Fundación Holofoil",
  "Espíritu de la Parca Gema", "Espíritu de la Parca Holofoil",
  "Espíritu Futbolero Gema", "Espíritu Futbolero Holofoil",
  "Espíritu Jefe Gema", "Espíritu Jefe Holofoil",
  "Espíritu Pescado Gema", "Espíritu Pescado Holofoil",
  "Espíritu de Batman", "Espíritu de Batman Dorado", "Espíritu de Batman Gomita", "Espíritu de Batman Galaxia", "Espíritu de Batman Gema", "Espíritu de Batman Holofoil", "Espíritu de Batman Cubo",
  "Espíritu de Viento", "Espíritu de Viento Dorado", "Espíritu de Viento Gomita", "Espíritu de Viento Galaxia", "Espíritu de Viento Gema", "Espíritu de Viento Holofoil", "Espíritu de Viento Cubo",
  "Espíritu de Agua Cubo", "Espíritu Dormilón Cubo", "Espíritu de Tierra Cubo", "Espíritu Demoníaco Cubo", "Espíritu de Fuego Cubo", "Espíritu Punk Cubo", "Espíritu Pato Cubo",
  "Espíritu Pollo", "Espíritu de Vini Jr.", "Espíritu de la Fundación Especial",
  "Espíritu de Llama", "Espíritu de Llama Dorado", "Espíritu de Llama Gomita", "Espíritu de Llama Galaxia", "Espíritu de Llama Gema",
  "Espíritu de Cubo Arcoíris", "Espíritu de Cubo Galaxia Oscura",
  "Espíritu de Bananín", "Espíritu de Bananín Dorado", "Espíritu de Bananín Gomita", "Espíritu de Bananín Arcoíris", "Espíritu de Bananín Galaxia",
  "Espíritu de la Parca Arcoíris", "Espíritu de la Parca Gema",
  "Espíritu de Tierra Quack", "Espíritu de Fuego Quack", "Espíritu de Agua Quack", "Espíritu de Punto Cero Quack",
  "Espíritu de John Wick", "Espíritu de Ironmouse"
];

export const defaultSpiritTypesT1 = [
  { name: "Normal", suffix: "" },
  { name: "Dorado", suffix: " Dorado" },
  { name: "Gomita", suffix: " Gomita" },
  { name: "Galaxia", suffix: " Galaxia" },
  { name: "Gema", suffix: " Gema" },
  { name: "Holofoil", suffix: " Holofoil" },
  { name: "Cubo", suffix: " Cubo" },
  { name: "Arcoíris", suffix: " Arcoíris" },
  { name: "Quack", suffix: " Quack" },
  { name: "Galaxia Oscura", suffix: " Galaxia Oscura" },
  { name: "Especial", suffix: " Especial" }
];

export const defaultSpiritTypesT2 = [
  { name: "Normal", suffix: "" },
  { name: "Dorado", suffix: " Dorado" },
  { name: "Hacker", suffix: " Hacker" },
  { name: "Hacker de botin", suffix: " Hacker de botin" },
  { name: "Cazarrecompensas", suffix: " Cazarrecompensas" },
  { name: "Matrix", suffix: " Matrix" },
  { name: "Galaxia", suffix: " Galaxia" },
  { name: "Cazarrecompensas Especial", suffix: " Cazarrecompensas Especial" },
  { name: "Especial", suffix: " Especial" }
];

export const LIGHT_COLOR_FAMILIES = [
  { title: "Pasteles Clásicos", colors: ["#D1C4E9", "#F8BBD0", "#BBDEFB", "#C8E6C9", "#FFF9C4", "#FFE0B2", "#E1BEE7"] },
  { title: "Tonos Vintage & Cálidos", colors: ["#D7CCC8", "#CFD8DC", "#FFCCBC", "#DCEDC8", "#B2DFDB", "#B3E5FC", "#D1D5DB"] },
  { title: "Brillantes Suaves", colors: ["#80DEEA", "#A5D6A7", "#FFE082", "#FFAB91", "#CE93D8", "#90CAF9", "#F48FB1"] }
];

export const DARK_COLOR_FAMILIES = [
  { title: "Neón & Arcade", colors: ["#9C27B0", "#E040FB", "#00E5FF", "#00E676", "#FFD600", "#FF1744", "#651FFF"] },
  { title: "Gemas & Profundos", colors: ["#4A148C", "#880E4F", "#0D47A1", "#1B5E20", "#F57F17", "#BF360C", "#311B92"] },
  { title: "Ciberpunk & Futuristas", colors: ["#EC4899", "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6", "#06B6D4", "#EF4444"] }
];

export const MONO_COLOR_FAMILIES = [
  { title: "Monocromático Puro", colors: ["#FFFFFF", "#E0E0E0", "#9E9E9E", "#616161", "#212121", "#BDBDBD", "#757575"] }
];
