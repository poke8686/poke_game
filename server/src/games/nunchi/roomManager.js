const { v4: uuidv4 } = require('uuid');
const logger = require('../../shared/logger');

/** @type {Map<string, object>} roomCode → Room */
const rooms = new Map();

function generateRoomCode() {
  let code;
  do {
    code = String(Math.floor(1000 + Math.random() * 9000));
  } while (rooms.has(code));
  return code;
}

function createRoom(playerName, ws, isPublic = false) {
  const roomCode = generateRoomCode();
  const playerId = uuidv4();

  const player = { id: playerId, name: playerName, ws, isHost: true };
  const room = {
    code: roomCode,
    hostId: playerId,
    hostName: playerName,
    players: [player],
    gameState: null,
    isPublic,
    createdAt: Date.now(),
  };

  rooms.set(roomCode, room);
  logger.info(`Room created: ${roomCode} host=${playerName} public=${isPublic}`);
  return { room, player };
}

function listPublicRooms() {
  const list = [];
  for (const room of rooms.values()) {
    if (room.isPublic && room.gameState === null) {
      list.push({
        roomCode: room.code,
        hostName: room.hostName,
        playerCount: room.players.length,
        maxPlayers: 8,
      });
    }
  }
  return list;
}

function joinRoom(roomCode, playerName, ws) {
  const room = rooms.get(roomCode);
  if (!room)                    return { error: 'ROOM_NOT_FOUND' };
  if (room.players.length >= 8) return { error: 'ROOM_FULL' };
  if (room.gameState !== null)   return { error: 'GAME_ALREADY_STARTED' };

  const playerId = uuidv4();
  const player = { id: playerId, name: playerName, ws, isHost: false };
  room.players.push(player);

  logger.info(`Room ${roomCode}: ${playerName} joined (${room.players.length} players)`);
  return { room, player };
}

function removePlayer(playerId) {
  for (const [code, room] of rooms.entries()) {
    const idx = room.players.findIndex(p => p.id === playerId);
    if (idx === -1) continue;

    const name = room.players[idx].name;
    room.players.splice(idx, 1);
    logger.info(`Room ${code}: ${name} left (${room.players.length} remaining)`);

    if (room.players.length === 0) {
      // 타이머 정리 후 방 삭제
      if (room.gameState?.timer) clearTimeout(room.gameState.timer);
      rooms.delete(code);
      logger.info(`Room ${code} deleted (empty)`);
      return { room: null, code };
    }

    // 호스트가 나갔으면 첫 번째 플레이어를 호스트로
    if (room.hostId === playerId) {
      room.players[0].isHost = true;
      room.hostId = room.players[0].id;
      logger.info(`Room ${code}: new host = ${room.players[0].name}`);
    }

    return { room, code };
  }
  return { room: null, code: null };
}

function getRoom(roomCode) {
  return rooms.get(roomCode);
}

function formatPlayers(players) {
  return players.map(p => ({ id: p.id, name: p.name, isHost: p.isHost }));
}

module.exports = { createRoom, joinRoom, removePlayer, getRoom, formatPlayers, listPublicRooms };
