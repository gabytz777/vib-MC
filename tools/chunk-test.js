const mineflayer = require('mineflayer');

const HOST = process.env.HOST || '127.0.0.1';
const PORT = parseInt(process.env.PORT || '25565', 10);

const results = [];
let newColumns = 0;

function log(name, ok, detail) {
  results.push({ name, ok, detail });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name}: ${detail}`);
}

function blockName(pos) {
  const b = bot.blockAt(pos);
  return b ? b.name : 'none';
}

function findGround(pos) {
  for (let dy = 1; dy <= 12; dy++) {
    const b = bot.blockAt(pos.offset(0, -dy, 0));
    if (b && b.name !== 'air' && b.name !== 'water') return b;
  }
  return null;
}

const bot = mineflayer.createBot({
  host: HOST,
  port: PORT,
  username: 'ChunkTester',
  version: '1.12.2',
});

bot.on('error', (err) => {
  console.log('ERROR:', err.message);
  process.exit(2);
});

bot.on('end', (reason) => {
  console.log('DISCONNECTED:', reason);
  finish();
});

bot.on('chunkColumnLoad', () => {
  newColumns++;
});

function finish() {
  const fails = results.filter((r) => !r.ok).length;
  console.log(`\n${results.length - fails}/${results.length} checks passed`);
  process.exit(fails > 0 ? 1 : 0);
}

bot.once('spawn', () => {
  console.log(`spawned at ${bot.entity.position}`);
  setTimeout(checkSpawn, 1500);
});

function groundY(pos) {
  const g = findGround(pos);
  return g ? g.position.y : null;
}

function checkSpawn() {
  const pos = bot.entity.position;
  log('spawn position sane', pos.x > 0 && pos.z > 0 && pos.y > 0, `pos=${pos}`);
  const ground = findGround(pos);
  log('solid ground below feet', ground !== null, `ground=${ground ? ground.name : 'none'} @ y=${ground ? ground.position.y : '-'}`);
  log('bedrock at y=0', blockName(pos.offset(0, -pos.y, 0)) === 'bedrock', `y0=${blockName(pos.offset(0, -pos.y, 0))}`);
  const heights = [];
  for (const dz of [0, 8, 16, 24, 32]) {
    heights.push(groundY(pos.offset(0, 0, dz)));
  }
  const known = heights.filter((h) => h !== null);
  log('terrain varies', known.length > 1 && Math.max(...known) - Math.min(...known) > 1, `heights=${heights.join(',')}`);
  log('chunk columns loaded', newColumns > 0, `loaded since join=${newColumns}`);
  setTimeout(moveTest, 500);
}

async function moveTest() {
  const startX = bot.entity.position.x;
  console.log('\n--- movement / streaming test: walking east ---');
  bot.look(-Math.PI / 2, 0, true); // face +x
  bot.setControlState('forward', true);
  await new Promise((r) => setTimeout(r, 4000));
  bot.setControlState('forward', false);
  console.log(`position after walk: ${bot.entity.position} (falling...)`);

  await new Promise((r) => setTimeout(r, 12000));

  const pos = bot.entity.position;
  log('moved east', pos.x > startX + 2, `x=${pos.x.toFixed(1)} (start ${startX.toFixed(1)})`);
  log('chunks streamed in while moving', newColumns > 0, `new columns loaded=${newColumns}`);
  const below = blockName(pos.offset(0, -1, 0));
  log('landed on something (solid or water)', below !== 'air' && below !== 'none', `below=${below}`);
  log('chunk columns total', newColumns > 3, `new columns loaded total=${newColumns}`);

  bot.quit();
}