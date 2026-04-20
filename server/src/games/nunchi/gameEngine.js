const logger = require('../../shared/logger');

const INITIAL_TIMER_SECONDS = 10;
const REACTION_TIMER_MS = 1000;

function initGame(room) {
  room.gameState = {
    phase: 'PLAYING',
    round: 1,
    activePlayers: [...room.players],
    selections: new Map(),   // playerId → number
    takenNumbers: new Set(), // locked numbers (no duplicates possible)
    eliminated: [],
    remaining: [],
    timer: null,       // 10s initial timer (reset on first press)
    pressTimer: null,  // 1s reaction timer (reset on each press)
  };
}

function startRound(room, broadcast) {
  const gs = room.gameState;
  gs.selections = new Map();
  gs.takenNumbers = new Set();
  if (gs.timer)      { clearTimeout(gs.timer);      gs.timer = null; }
  if (gs.pressTimer) { clearTimeout(gs.pressTimer); gs.pressTimer = null; }

  const activePlayers = gs.activePlayers.map(p => ({ id: p.id, name: p.name }));

  broadcast(room, {
    type: 'ROUND_START',
    round: gs.round,
    activePlayers,
    timerSeconds: INITIAL_TIMER_SECONDS,
  });

  // 10s timeout — if nobody presses at all
  gs.timer = setTimeout(() => {
    if (room.gameState?.phase === 'PLAYING') {
      logger.info(`Room ${room.code}: initial timer expired (round ${gs.round})`);
      finishRound(room, broadcast);
    }
  }, INITIAL_TIMER_SECONDS * 1000);
}

function selectNumber(room, playerId, number, broadcast) {
  const gs = room.gameState;
  if (!gs || gs.phase !== 'PLAYING')              return { error: 'GAME_NOT_ACTIVE' };
  if (!gs.activePlayers.some(p => p.id === playerId)) return { error: 'NOT_ACTIVE_PLAYER' };
  if (gs.selections.has(playerId))                return { error: 'ALREADY_SELECTED' };
  if (number < 1 || number > gs.activePlayers.length) return { error: 'INVALID_NUMBER' };
  if (gs.takenNumbers.has(number))                return { error: 'NUMBER_ALREADY_TAKEN' };

  // Lock immediately — no duplicates possible
  gs.selections.set(playerId, number);
  gs.takenNumbers.add(number);

  const player = gs.activePlayers.find(p => p.id === playerId);
  const selectionCount = gs.selections.size;
  const total = gs.activePlayers.length;

  // Broadcast immediately so everyone disables that button
  broadcast(room, {
    type: 'NUMBER_TAKEN',
    number,
    playerId,
    playerName: player?.name ?? '',
    selectionCount,
    total,
  });

  logger.info(`Room ${room.code} round ${gs.round}: ${player?.name} → #${number} (${selectionCount}/${total})`);

  // Cancel initial 10s timer on first press
  if (gs.timer) { clearTimeout(gs.timer); gs.timer = null; }

  // All players selected → end round immediately (no one eliminated)
  if (selectionCount === total) {
    if (gs.pressTimer) { clearTimeout(gs.pressTimer); gs.pressTimer = null; }
    finishRound(room, broadcast);
    return {};
  }

  // Reset 1-second reaction timer
  if (gs.pressTimer) clearTimeout(gs.pressTimer);
  gs.pressTimer = setTimeout(() => {
    if (room.gameState?.phase === 'PLAYING') {
      logger.info(`Room ${room.code}: reaction timer expired (round ${gs.round})`);
      finishRound(room, broadcast);
    }
  }, REACTION_TIMER_MS);

  return {};
}

function finishRound(room, broadcast) {
  const gs = room.gameState;
  gs.phase = 'RESULT';
  if (gs.timer)      { clearTimeout(gs.timer);      gs.timer = null; }
  if (gs.pressTimer) { clearTimeout(gs.pressTimer); gs.pressTimer = null; }

  const selectionList = [];
  const eliminated = [];

  for (const player of gs.activePlayers) {
    const number = gs.selections.get(player.id) ?? null;
    selectionList.push({ playerId: player.id, playerName: player.name, number });

    // Eliminated: didn't press within time
    if (number === null) {
      eliminated.push({ id: player.id, name: player.name });
    }
  }

  const remaining = gs.activePlayers.filter(p => !eliminated.some(e => e.id === p.id));
  gs.eliminated = eliminated;
  gs.remaining = remaining;

  broadcast(room, {
    type: 'ROUND_RESULT',
    round: gs.round,
    selections: selectionList,
    eliminated: eliminated.map(p => ({ id: p.id, name: p.name })),
    remaining:  remaining.map(p => ({ id: p.id, name: p.name })),
  });

  logger.info(`Room ${room.code} round ${gs.round}: elim=${eliminated.length} remain=${remaining.length}`);

  if (remaining.length <= 1) {
    gs.phase = 'GAME_OVER';
    broadcast(room, {
      type: 'GAME_OVER',
      winner: remaining[0] ? { playerId: remaining[0].id, playerName: remaining[0].name } : null,
      totalRounds: gs.round,
    });
  }
}

function nextRound(room, broadcast) {
  const gs = room.gameState;
  if (!gs || gs.phase === 'GAME_OVER') return { error: 'GAME_ALREADY_OVER' };

  gs.activePlayers = gs.remaining;
  gs.round += 1;
  gs.phase = 'PLAYING';

  startRound(room, broadcast);
  return {};
}

module.exports = { initGame, startRound, selectNumber, nextRound };
