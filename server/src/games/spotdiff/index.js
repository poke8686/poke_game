const fs = require('fs');
const path = require('path');
const logger = require('../../shared/logger');

/**
 * Stages data file: server/data/spotdiff/stages.json
 *   [{ id: string, title: string, difficulty: int,
 *      diffs: [{ x: 0..1, y: 0..1, r: 0..1 }] }]
 *
 * Images: server/public/spotdiff/<id>/{a,b}.{jpg|png|webp}
 *
 * Missing data file → empty list; client falls back to procedural stages.
 */
const DATA_PATH = path.join(__dirname, '..', '..', '..', 'data', 'spotdiff', 'stages.json');
const PUBLIC_DIR = path.join(__dirname, '..', '..', '..', 'public', 'spotdiff');

function loadStages() {
  try {
    if (!fs.existsSync(DATA_PATH)) return [];
    const list = JSON.parse(fs.readFileSync(DATA_PATH, 'utf-8'));
    if (!Array.isArray(list)) return [];
    return list.filter(s => s && s.id && Array.isArray(s.diffs));
  } catch (e) {
    logger.error('spotdiff: failed to load stages:', e);
    return [];
  }
}

function stageImageUrl(req, id, side) {
  const dir = path.join(PUBLIC_DIR, id);
  for (const ext of ['jpg', 'png', 'webp']) {
    const filename = `${side}.${ext}`;
    if (fs.existsSync(path.join(dir, filename))) {
      const host = req.headers.host || 'localhost:3000';
      const proto = req.headers['x-forwarded-proto'] || 'https';
      return `${proto}://${host}/spotdiff/img/${id}/${filename}`;
    }
  }
  return null;
}

function jsonResponse(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(body));
}

function handleListStages(req, res) {
  const stages = loadStages().map(s => ({
    id: s.id,
    title: s.title || s.id,
    difficulty: s.difficulty || 1,
    diffCount: s.diffs.length
  }));
  jsonResponse(res, 200, { stages });
}

function handleStageDetail(req, res, id) {
  const stage = loadStages().find(s => s.id === id);
  if (!stage) return jsonResponse(res, 404, { error: 'stage_not_found' });

  const imageAUrl = stageImageUrl(req, id, 'a');
  const imageBUrl = stageImageUrl(req, id, 'b');
  if (!imageAUrl || !imageBUrl) {
    return jsonResponse(res, 404, { error: 'stage_images_missing' });
  }

  jsonResponse(res, 200, {
    id: stage.id,
    title: stage.title || stage.id,
    difficulty: stage.difficulty || 1,
    imageAUrl,
    imageBUrl,
    diffs: stage.diffs
  });
}

const MIME = {
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.png': 'image/png',
  '.webp': 'image/webp'
};

function handleStaticImage(req, res, urlPath) {
  const rel = urlPath.replace(/^\/spotdiff\/img\//, '');
  if (rel.includes('..') || rel.includes('\\')) {
    return jsonResponse(res, 400, { error: 'bad_path' });
  }
  const full = path.join(PUBLIC_DIR, rel);
  if (!full.startsWith(PUBLIC_DIR)) {
    return jsonResponse(res, 400, { error: 'bad_path' });
  }
  if (!fs.existsSync(full)) {
    return jsonResponse(res, 404, { error: 'not_found' });
  }
  const ext = path.extname(full).toLowerCase();
  res.writeHead(200, {
    'Content-Type': MIME[ext] || 'application/octet-stream',
    'Cache-Control': 'public, max-age=86400'
  });
  fs.createReadStream(full).pipe(res);
}

/** Returns true if the HTTP request was handled by this module. */
function handleHttp(req, res) {
  const url = req.url || '/';

  if (url === '/spotdiff/stages') {
    handleListStages(req, res);
    return true;
  }

  const detailMatch = url.match(/^\/spotdiff\/stages\/([\w-]+)$/);
  if (detailMatch) {
    handleStageDetail(req, res, detailMatch[1]);
    return true;
  }

  if (url.startsWith('/spotdiff/img/')) {
    handleStaticImage(req, res, url);
    return true;
  }

  return false;
}

module.exports = { handleHttp };
