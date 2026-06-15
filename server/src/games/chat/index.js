const logger = require('../../shared/logger');

// 초기 채팅방 세팅
const rooms = new Map(); // roomId -> { id, name, users, messages, lastMessage, time }

function initializeRooms() {
  const defaultRooms = [
    { id: 'general', name: '자유 채팅방' },
    { id: 'nunchi', name: '눈치게임 대기실' },
    { id: 'dev', name: '개발자에게 문의하기' }
  ];
  
  defaultRooms.forEach(room => {
    rooms.set(room.id, {
      id: room.id,
      name: room.name,
      users: new Map(),
      messages: [],
      lastMessage: '새로운 채팅방이 생성되었습니다.',
      time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
    });
  });
}

initializeRooms();

function handleChat(ws, req) {
  let currentUser = {
    id: Math.random().toString(36).substr(2, 9),
    name: `User_${Math.floor(Math.random() * 1000)}`,
    ws: ws
  };

  ws.on('message', (data) => {
    try {
      const message = JSON.parse(data.toString());
      const { type, roomId, content, userName, emoticonUrl } = message;

      if (userName) currentUser.name = userName;

      switch (type) {
        case 'GET_ROOMS':
          sendRoomList(ws);
          break;
        case 'JOIN':
          joinRoom(roomId, currentUser);
          break;
        case 'MESSAGE':
          handleIncomingMessage(roomId, currentUser, content, 'TEXT');
          break;
        case 'EMOTICON':
          handleIncomingMessage(roomId, currentUser, emoticonUrl, 'EMOTICON');
          break;
        case 'LEAVE':
          leaveRoom(roomId, currentUser.id);
          break;
      }
    } catch (e) {
      logger.error('Chat message error:', e);
    }
  });

  ws.on('close', () => {
    rooms.forEach((room, roomId) => {
      if (room.users.has(currentUser.id)) {
        leaveRoom(roomId, currentUser.id);
      }
    });
    logger.info(`Chat user disconnected: ${currentUser.id}`);
  });
}

function handleIncomingMessage(roomId, user, content, msgType) {
  const room = rooms.get(roomId);
  if (!room) return;

  const now = new Date();
  const timeString = now.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  
  const msg = {
    id: Date.now().toString(),
    senderId: user.id,
    senderName: user.name,
    content: content,
    msgType: msgType, // 'TEXT' or 'EMOTICON'
    timestamp: timeString
  };

  room.lastMessage = msgType === 'EMOTICON' ? '(이모티콘)' : content;
  room.time = timeString;

  broadcastMessage(roomId, msg);
  broadcastRoomList(); // 다른 사용자의 목록 업데이트
}

function sendRoomList(ws) {
  const roomList = Array.from(rooms.values()).map(r => ({
    id: r.id,
    name: r.name,
    lastMessage: r.lastMessage,
    time: r.time,
    unreadCount: 0 // 더미로 처리
  }));
  ws.send(JSON.stringify({ type: 'ROOM_LIST', rooms: roomList }));
}

function broadcastRoomList() {
  const roomList = Array.from(rooms.values()).map(r => ({
    id: r.id,
    name: r.name,
    lastMessage: r.lastMessage,
    time: r.time,
    unreadCount: 0
  }));
  const data = JSON.stringify({ type: 'ROOM_LIST', rooms: roomList });
  
  rooms.forEach(room => {
    room.users.forEach(user => {
      if (user.ws.readyState === 1) user.ws.send(data);
    });
  });
}

function joinRoom(roomId, user) {
  const room = rooms.get(roomId);
  if (!room) return;
  
  room.users.set(user.id, user);
  
  const now = new Date();
  const timeString = now.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

  broadcastMessage(roomId, {
    id: `system_${Date.now()}`,
    senderId: 'system',
    senderName: 'System',
    content: `${user.name}님이 입장하셨습니다.`,
    msgType: 'TEXT',
    timestamp: timeString,
    isSystem: true
  });

  user.ws.send(JSON.stringify({
    type: 'HISTORY',
    messages: room.messages.slice(-50)
  }));
}

function leaveRoom(roomId, userId) {
  const room = rooms.get(roomId);
  if (room && room.users.has(userId)) {
    const user = room.users.get(userId);
    room.users.delete(userId);
    
    const now = new Date();
    const timeString = now.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

    broadcastMessage(roomId, {
      id: `system_${Date.now()}`,
      senderId: 'system',
      senderName: 'System',
      content: `${user.name}님이 퇴장하셨습니다.`,
      msgType: 'TEXT',
      timestamp: timeString,
      isSystem: true
    });
  }
}

function broadcastMessage(roomId, msg) {
  const room = rooms.get(roomId);
  if (room) {
    room.messages.push(msg);
    const data = JSON.stringify({ type: 'MESSAGE', ...msg });
    room.users.forEach(user => {
      if (user.ws.readyState === 1) {
        user.ws.send(data);
      }
    });
  }
}

module.exports = { handleChat };
