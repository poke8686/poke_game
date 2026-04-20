// Client → Server
const CLIENT = {
  CREATE_ROOM:   'CREATE_ROOM',
  JOIN_ROOM:     'JOIN_ROOM',
  START_GAME:    'START_GAME',
  SELECT_NUMBER: 'SELECT_NUMBER',
  NEXT_ROUND:    'NEXT_ROUND',
  LEAVE_ROOM:    'LEAVE_ROOM',
  GET_ROOMS:     'GET_ROOMS',
};

// Server → Client
const SERVER = {
  ROOM_CREATED:        'ROOM_CREATED',
  ROOM_JOINED:         'ROOM_JOINED',
  PLAYER_JOINED:       'PLAYER_JOINED',
  PLAYER_LEFT:         'PLAYER_LEFT',
  GAME_STARTED:        'GAME_STARTED',
  ROUND_START:         'ROUND_START',
  SELECTION_PROGRESS:  'SELECTION_PROGRESS',
  NUMBER_TAKEN:        'NUMBER_TAKEN',
  ROUND_RESULT:        'ROUND_RESULT',
  GAME_OVER:           'GAME_OVER',
  ERROR:               'ERROR',
  ROOM_LIST:           'ROOM_LIST',
};

const ERROR_CODES = {
  ROOM_NOT_FOUND:       'ROOM_NOT_FOUND',
  ROOM_FULL:            'ROOM_FULL',
  GAME_ALREADY_STARTED: 'GAME_ALREADY_STARTED',
  NOT_HOST:             'NOT_HOST',
  INVALID_NUMBER:       'INVALID_NUMBER',
  ALREADY_SELECTED:     'ALREADY_SELECTED',
  NOT_ENOUGH_PLAYERS:   'NOT_ENOUGH_PLAYERS',
  NOT_ACTIVE_PLAYER:    'NOT_ACTIVE_PLAYER',
};

const ERROR_MESSAGES = {
  ROOM_NOT_FOUND:       '방을 찾을 수 없습니다',
  ROOM_FULL:            '방이 꽉 찼습니다 (최대 8명)',
  GAME_ALREADY_STARTED: '이미 게임이 시작됐습니다',
  NOT_HOST:             '호스트만 가능합니다',
  INVALID_NUMBER:       '유효하지 않은 숫자입니다',
  ALREADY_SELECTED:     '이미 선택했습니다',
  NOT_ENOUGH_PLAYERS:   '최소 2명이 필요합니다',
  NOT_ACTIVE_PLAYER:    '탈락한 플레이어입니다',
};

module.exports = { CLIENT, SERVER, ERROR_CODES, ERROR_MESSAGES };
