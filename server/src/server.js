const { createServer } = require('http');
const { WebSocketServer } = require('ws');
const { handleNunchi } = require('./games/nunchi');
const logger = require('./shared/logger');

const PORT = process.env.PORT || 3000;

// ── HTTP 서버 (헬스체크) ─────────────────────────────────────────
const httpServer = createServer((req, res) => {
  const url = req.url ?? '/';

  if (url === '/' || url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'ok', games: ['nunchi'] }));
  }

  const gameHealth = url.match(/^\/(\w+)\/health$/);
  if (gameHealth) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'ok', game: gameHealth[1] }));
  }

  res.writeHead(404);
  res.end('Not Found');
});

// ── WebSocket 서버 ────────────────────────────────────────────────
const wss = new WebSocketServer({ server: httpServer });

const GAME_HANDLERS = {
  nunchi: handleNunchi,
};

wss.on('connection', (ws, req) => {
  const url = req.url ?? '';
  const match = url.match(/^\/(\w+)\/ws$/);

  if (!match) {
    logger.warn(`Invalid WS path: ${url}`);
    ws.close(1008, 'Invalid path. Use /{game}/ws');
    return;
  }

  const game = match[1];
  const handler = GAME_HANDLERS[game];

  if (!handler) {
    logger.warn(`Unknown game: ${game}`);
    ws.close(1008, `Unknown game: ${game}`);
    return;
  }

  logger.info(`WS connected: game=${game} ip=${req.socket.remoteAddress}`);
  handler(ws, req);
});

// ── 시작 ─────────────────────────────────────────────────────────
httpServer.listen(PORT, () => {
  logger.info(`GameVault server running on port ${PORT}`);
  logger.info(`Health : GET  http://localhost:${PORT}/health`);
  logger.info(`Nunchi : WSS  ws://localhost:${PORT}/nunchi/ws`);
});
