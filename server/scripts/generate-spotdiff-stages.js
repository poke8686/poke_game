#!/usr/bin/env node
/**
 * Generates spotdiff stages from free photos (picsum.photos).
 *
 * Each stage b.jpg is created by applying HUE ROTATION to N small circular
 * regions of the original photo. This looks like intentional color edits
 * (e.g. "the umbrella turned blue") rather than obvious copy-paste patches.
 *
 * Run: npm run generate:spotdiff
 */
const fs = require('fs');
const path = require('path');
const https = require('https');
const sharp = require('sharp');

const ROOT = path.join(__dirname, '..');
const OUT_DIR = path.join(ROOT, 'public', 'spotdiff');
const META_PATH = path.join(ROOT, 'data', 'spotdiff', 'stages.json');

const STAGES = [
  { id: 'beach',    title: '해변',     difficulty: 1,  seed: 'gv-beach-7' },
  { id: 'forest',   title: '숲',       difficulty: 2,  seed: 'gv-forest-3' },
  { id: 'cafe',     title: '카페',     difficulty: 3,  seed: 'gv-cafe-1' },
  { id: 'mountain', title: '산',       difficulty: 4,  seed: 'gv-mountain-9' },
  { id: 'flowers',  title: '꽃밭',     difficulty: 5,  seed: 'gv-flower-2' },
  { id: 'city',     title: '도시',     difficulty: 6,  seed: 'gv-city-5' },
  { id: 'desert',   title: '사막',     difficulty: 7,  seed: 'gv-desert-4' },
  { id: 'ocean',    title: '바다',     difficulty: 8,  seed: 'gv-ocean-8' },
  { id: 'forest_b', title: '깊은 숲',  difficulty: 9,  seed: 'gv-deepforest-6' },
  { id: 'aurora',   title: '오로라',   difficulty: 10, seed: 'gv-aurora-10' },
];

const IMG_W = 1200;
const IMG_H = 800;

function diffCountFor(d) {
  if (d <= 2) return 3;
  if (d <= 4) return 4;
  if (d <= 6) return 5;
  if (d <= 8) return 6;
  if (d === 9) return 7;
  return 8;
}

// Tap radius (normalized). Higher difficulty = smaller target = harder to tap.
function tapRadiusFor(d) {
  return 0.085 - (d - 1) * 0.004; // 0.085 → 0.049
}

// Patch radius in pixels. Higher difficulty = smaller patch = harder to spot.
function patchRadiusPxFor(d, imgW) {
  // Easy (★1): ~8% of width — big obvious change. Hard (★10): ~4.5% — subtle.
  const ratio = 0.08 - (d - 1) * 0.0035;
  return Math.max(22, Math.round(ratio * imgW));
}

// Hue rotation angle. Vary per diff so they look different from each other.
function hueAngleFor(diffIndex, rng) {
  // Pick a significant angle (avoid near-0 which looks unchanged)
  const angles = [90, 120, 150, 180, 210, 240, 270];
  return angles[Math.floor(rng() * angles.length)];
}

function mulberry32(a) {
  return function () {
    a = (a + 0x6D2B79F5) | 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
function hashCode(s) {
  return s.split('').reduce((a, c) => ((a * 31 + c.charCodeAt(0)) | 0), 0);
}

function downloadBuffer(url, redirectsLeft = 5) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location) {
        if (redirectsLeft <= 0) return reject(new Error('Too many redirects'));
        res.resume();
        const next = res.headers.location.startsWith('http')
          ? res.headers.location
          : new URL(res.headers.location, url).toString();
        return resolve(downloadBuffer(next, redirectsLeft - 1));
      }
      if (res.statusCode !== 200) {
        res.resume();
        return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
      }
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    }).on('error', reject);
  });
}

function placeDiffs(diffCount, tapRadius, rng) {
  const positions = [];
  const minDist = tapRadius * 3.0; // keep diffs well separated
  let tries = 0;
  while (positions.length < diffCount && tries < 600) {
    const x = 0.10 + rng() * 0.80;
    const y = 0.10 + rng() * 0.80;
    if (!positions.some((p) => Math.hypot(p.x - x, p.y - y) < minDist)) {
      positions.push({ x, y });
    }
    tries++;
  }
  return positions;
}

/**
 * For each diff position, extract a circular region, apply hue rotation,
 * blend back with a feathered mask. The result looks like an intentional
 * color-grade applied to that area — NOT a copy-paste block.
 */
async function buildBImage(srcBuf, diffs, difficulty, rng) {
  const meta = await sharp(srcBuf).metadata();
  const w = meta.width;
  const h = meta.height;
  const rPx = patchRadiusPxFor(difficulty, w);
  const patchSize = rPx * 2;

  const overlays = [];

  for (let i = 0; i < diffs.length; i++) {
    const d = diffs[i];
    const cx = Math.round(d.x * w);
    const cy = Math.round(d.y * h);
    const left = Math.max(0, cx - rPx);
    const top  = Math.max(0, cy - rPx);
    const right  = Math.min(w, cx + rPx);
    const bottom = Math.min(h, cy + rPx);
    const extractW = right - left;
    const extractH = bottom - top;

    if (extractW < 4 || extractH < 4) continue;

    // Extract the region, apply hue rotation, keep same size
    const hue = hueAngleFor(i, rng);
    const shifted = await sharp(srcBuf)
      .extract({ left, top, width: extractW, height: extractH })
      .modulate({ hue })
      .png()
      .toBuffer();

    // Feathered elliptical mask — smooth edges so there's no visible hard border
    const innerR = Math.min(extractW, extractH) / 2;
    const mask = Buffer.from(
      `<svg width="${extractW}" height="${extractH}" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <radialGradient id="g" cx="50%" cy="50%" r="50%">
            <stop offset="0%"   stop-color="white" stop-opacity="1"/>
            <stop offset="55%"  stop-color="white" stop-opacity="1"/>
            <stop offset="100%" stop-color="white" stop-opacity="0"/>
          </radialGradient>
        </defs>
        <ellipse cx="${extractW / 2}" cy="${extractH / 2}"
                 rx="${innerR * 0.95}" ry="${innerR * 0.95}"
                 fill="url(#g)"/>
      </svg>`
    );

    const masked = await sharp(shifted)
      .composite([{ input: mask, blend: 'dest-in' }])
      .png()
      .toBuffer();

    overlays.push({ input: masked, top, left });
  }

  return sharp(srcBuf).composite(overlays).jpeg({ quality: 90 }).toBuffer();
}

async function main() {
  fs.mkdirSync(path.dirname(META_PATH), { recursive: true });

  const result = [];
  for (let i = 0; i < STAGES.length; i++) {
    const stage = STAGES[i];
    const order = i + 1;
    const diffCount = diffCountFor(stage.difficulty);
    const tapR = tapRadiusFor(stage.difficulty);

    console.log(`★${stage.difficulty} ${stage.id} — ${diffCount} diffs, tapR=${tapR.toFixed(3)}, patchPx=${patchRadiusPxFor(stage.difficulty, IMG_W) * 2}px`);

    const url = `https://picsum.photos/seed/${stage.seed}/${IMG_W}/${IMG_H}`;
    const srcBuf = await downloadBuffer(url);

    const stageDir = path.join(OUT_DIR, stage.id);
    fs.mkdirSync(stageDir, { recursive: true });
    fs.writeFileSync(path.join(stageDir, 'a.jpg'), srcBuf);

    const rng = mulberry32(hashCode(stage.seed));
    const diffs = placeDiffs(diffCount, tapR, rng);

    const bBuf = await buildBImage(srcBuf, diffs, stage.difficulty, rng);
    fs.writeFileSync(path.join(stageDir, 'b.jpg'), bBuf);

    result.push({
      id: stage.id,
      title: stage.title,
      difficulty: stage.difficulty,
      order,
      diffs: diffs.map((p) => ({
        x: Number(p.x.toFixed(4)),
        y: Number(p.y.toFixed(4)),
        r: Number(tapR.toFixed(4)),
      })),
    });
  }

  fs.writeFileSync(META_PATH, JSON.stringify(result, null, 2) + '\n');
  console.log(`\n✅ ${result.length} stages → ${META_PATH}`);
  console.log(`   Images → ${OUT_DIR}/<id>/{a,b}.jpg`);
  console.log('\nDifferences applied: hue rotation per circular region (feathered edges)');
  console.log('  Easy (★1–2): ~160px patch, obvious color shift');
  console.log('  Hard (★9–10): ~90px patch, subtle hue change');
}

main().catch((e) => {
  console.error('Generation failed:', e);
  process.exit(1);
});
