const fs = require('fs');
const path = require('path');
const logger = require('../../shared/logger');

const DATA_DIR = path.join(__dirname, '..', '..', '..', 'data', 'td');

function ensureDataDir() {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
}

function userFilePath(userId) {
  return path.join(DATA_DIR, `${userId}.json`);
}

function readUser(userId) {
  const file = userFilePath(userId);
  if (!fs.existsSync(file)) return null;
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    return null;
  }
}

function writeUser(userId, data) {
  ensureDataDir();
  fs.writeFileSync(userFilePath(userId), JSON.stringify(data), 'utf8');
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try { resolve(body ? JSON.parse(body) : {}); }
      catch { reject(new Error('Invalid JSON')); }
    });
    req.on('error', reject);
  });
}

function send(res, status, data) {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(data));
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

async function handleHttp(req, res) {
  const { method, url } = req;

  // POST /td/users — UUID 등록
  if (method === 'POST' && url === '/td/users') {
    let body;
    try { body = await parseBody(req); } catch { return send(res, 400, { error: 'Invalid JSON' }); }
    const { userId } = body;
    if (!userId || !UUID_RE.test(userId)) return send(res, 400, { error: 'Invalid userId' });
    if (!readUser(userId)) {
      writeUser(userId, {
        userId,
        progress: { round: 1, unlockedRounds: 1, score: 0 },
        resources: { gold: 150, lives: 20 },
        characters: [],
        savedAt: Date.now(),
      });
      logger.info(`TD: new user registered userId=${userId}`);
    }
    return send(res, 200, { ok: true });
  }

  // GET /td/users/:userId/save
  const getMatch = url.match(/^\/td\/users\/([^/]+)\/save$/);
  if (method === 'GET' && getMatch) {
    const userId = getMatch[1];
    const data = readUser(userId);
    if (!data) return send(res, 404, { error: 'User not found' });
    return send(res, 200, data);
  }

  // PUT /td/users/:userId/save — 전체 저장
  const putMatch = url.match(/^\/td\/users\/([^/]+)\/save$/);
  if (method === 'PUT' && putMatch) {
    const userId = putMatch[1];
    let body;
    try { body = await parseBody(req); } catch { return send(res, 400, { error: 'Invalid JSON' }); }
    writeUser(userId, { ...body, userId, savedAt: Date.now() });
    logger.info(`TD: saveAll userId=${userId}`);
    return send(res, 200, { ok: true });
  }

  // PATCH /td/users/:userId/save/progress
  const progressMatch = url.match(/^\/td\/users\/([^/]+)\/save\/progress$/);
  if (method === 'PATCH' && progressMatch) {
    const userId = progressMatch[1];
    const existing = readUser(userId);
    if (!existing) return send(res, 404, { error: 'User not found' });
    let body;
    try { body = await parseBody(req); } catch { return send(res, 400, { error: 'Invalid JSON' }); }
    writeUser(userId, { ...existing, progress: body, savedAt: Date.now() });
    return send(res, 200, { ok: true });
  }

  // PATCH /td/users/:userId/save/resources
  const resourcesMatch = url.match(/^\/td\/users\/([^/]+)\/save\/resources$/);
  if (method === 'PATCH' && resourcesMatch) {
    const userId = resourcesMatch[1];
    const existing = readUser(userId);
    if (!existing) return send(res, 404, { error: 'User not found' });
    let body;
    try { body = await parseBody(req); } catch { return send(res, 400, { error: 'Invalid JSON' }); }
    writeUser(userId, { ...existing, resources: body, savedAt: Date.now() });
    return send(res, 200, { ok: true });
  }

  // PATCH /td/users/:userId/save/characters
  const charsMatch = url.match(/^\/td\/users\/([^/]+)\/save\/characters$/);
  if (method === 'PATCH' && charsMatch) {
    const userId = charsMatch[1];
    const existing = readUser(userId);
    if (!existing) return send(res, 404, { error: 'User not found' });
    let body;
    try { body = await parseBody(req); } catch { return send(res, 400, { error: 'Invalid JSON' }); }
    writeUser(userId, { ...existing, characters: body, savedAt: Date.now() });
    return send(res, 200, { ok: true });
  }

  return false;
}

module.exports = { handleHttp };
