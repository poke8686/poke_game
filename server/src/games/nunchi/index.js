const { CLIENT, SERVER, ERROR_CODES, ERROR_MESSAGES } = require('./messages');
const { createRoom, joinRoom, removePlayer, getRoom, formatPlayers, listPublicRooms } = require('./roomManager');
const { initGame, startRound, selectNumber, nextRound } = require('./gameEngine');
const logger = require('../../shared/logger');

/** ws → { playerId, roomCode } */
const playerMap = new Map();

function send(ws, data) {
  if (ws.readyState === 1 /* OPEN */) {
    ws.send(JSON.stringify(data));
  }
}

function sendError(ws, code, message) {
  send(ws, { type: SERVER.ERROR, code, message: message ?? ERROR_MESSAGES[code] ?? code });
}

function broadcast(room, data) {
  for (const player of room.players) {
    send(player.ws, data);
  }
}

function handleNunchi(ws) {
  ws.on('message', (raw) => {
    let msg;
    try { msg = JSON.parse(raw); }
    catch { return sendError(ws, 'INVALID_JSON', '잘못된 메시지 형식입니다'); }

    const meta = playerMap.get(ws);

    switch (msg.type) {

      case CLIENT.CREATE_ROOM: {
        if (!msg.playerName?.trim()) return sendError(ws, 'INVALID_NAME', '이름을 입력해주세요');
        const isPublic = !!msg.isPublic;
        const { room, player } = createRoom(msg.playerName.trim(), ws, isPublic);
        playerMap.set(ws, { playerId: player.id, roomCode: room.code });
        send(ws, {
          type: SERVER.ROOM_CREATED,
          roomCode: room.code,
          playerId: player.id,
          players: formatPlayers(room.players),
        });
        break;
      }

      case CLIENT.JOIN_ROOM: {
        if (!msg.playerName?.trim()) return sendError(ws, 'INVALID_NAME', '이름을 입력해주세요');
        if (!msg.roomCode)           return sendError(ws, 'INVALID_ROOM_CODE', '방 코드를 입력해주세요');

        const result = joinRoom(String(msg.roomCode), msg.playerName.trim(), ws);
        if (result.error) return sendError(ws, result.error);

        const { room, player } = result;
        playerMap.set(ws, { playerId: player.id, roomCode: room.code });

        send(ws, {
          type: SERVER.ROOM_JOINED,
          roomCode: room.code,
          playerId: player.id,
          isHost: false,
          players: formatPlayers(room.players),
        });

        // 기존 플레이어에게 입장 알림
        for (const p of room.players) {
          if (p.id !== player.id) {
            send(p.ws, { type: SERVER.PLAYER_JOINED, players: formatPlayers(room.players) });
          }
        }
        break;
      }

      case CLIENT.START_GAME: {
        if (!meta) return sendError(ws, 'NOT_IN_ROOM', '방에 참여해주세요');
        const room = getRoom(meta.roomCode);
        if (!room) return sendError(ws, ERROR_CODES.ROOM_NOT_FOUND);
        if (room.hostId !== meta.playerId) return sendError(ws, ERROR_CODES.NOT_HOST);
        if (room.players.length < 2)       return sendError(ws, ERROR_CODES.NOT_ENOUGH_PLAYERS);
        if (room.gameState !== null)        return sendError(ws, ERROR_CODES.GAME_ALREADY_STARTED);

        initGame(room);
        broadcast(room, {
          type: SERVER.GAME_STARTED,
          players: formatPlayers(room.players),
          round: 1,
          timerSeconds: 10,
        });
        startRound(room, broadcast);
        break;
      }

      case CLIENT.SELECT_NUMBER: {
        if (!meta) return sendError(ws, 'NOT_IN_ROOM', '방에 참여해주세요');
        const room = getRoom(meta.roomCode);
        if (!room?.gameState) return sendError(ws, 'GAME_NOT_STARTED', '게임이 시작되지 않았습니다');

        const result = selectNumber(room, meta.playerId, msg.number, broadcast);
        if (result.error) sendError(ws, result.error);
        break;
      }

      case CLIENT.NEXT_ROUND: {
        if (!meta) return sendError(ws, 'NOT_IN_ROOM', '방에 참여해주세요');
        const room = getRoom(meta.roomCode);
        if (!room?.gameState) return;
        if (room.hostId !== meta.playerId) return sendError(ws, ERROR_CODES.NOT_HOST);

        const result = nextRound(room, broadcast);
        if (result?.error) sendError(ws, result.error);
        break;
      }

      case CLIENT.LEAVE_ROOM: {
        handleDisconnect(ws);
        break;
      }

      case CLIENT.GET_ROOMS: {
        send(ws, { type: SERVER.ROOM_LIST, rooms: listPublicRooms() });
        break;
      }

      default:
        sendError(ws, 'UNKNOWN_TYPE', `알 수 없는 메시지 타입: ${msg.type}`);
    }
  });

  ws.on('close', () => handleDisconnect(ws));
  ws.on('error', (err) => logger.error('WebSocket error:', err.message));
}

function handleDisconnect(ws) {
  const meta = playerMap.get(ws);
  if (!meta) return;
  playerMap.delete(ws);

  const { room } = removePlayer(meta.playerId);
  if (room) {
    broadcast(room, { type: SERVER.PLAYER_LEFT, players: formatPlayers(room.players) });
  }
}

module.exports = { handleNunchi };
