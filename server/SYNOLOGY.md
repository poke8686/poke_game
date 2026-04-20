# GameVault 서버 - 시놀로지 NAS 배포 가이드

## 사전 준비
- DSM 7.2 이상
- Container Manager 패키지 설치 (패키지 센터에서 설치)

---

## 1단계: 파일 업로드

1. **File Station** 열기
2. `docker` 공유 폴더 진입 (없으면 제어판 → 공유 폴더 → 생성)
3. `gamevault` 폴더 생성
4. `gamevault` 안에 아래 구조로 파일 업로드:

```
/docker/gamevault/
├── docker-compose.yml
├── Dockerfile
├── package.json
└── src/
    ├── server.js
    ├── shared/
    │   └── logger.js
    └── games/
        └── nunchi/
            ├── index.js
            ├── roomManager.js
            ├── gameEngine.js
            └── messages.js
```

> **주의**: `docker-compose.yml`과 `Dockerfile`은 `gamevault` 폴더 바로 아래에, 나머지는 `src` 하위 폴더에 넣어야 합니다.

---

## 2단계: Container Manager에서 프로젝트 생성

1. **Container Manager** 열기
2. 좌측 메뉴에서 **프로젝트** 클릭
3. **생성** 버튼 클릭
4. 설정:
   - **프로젝트 이름**: `gamevault`
   - **경로**: `docker/gamevault` 선택 (docker-compose.yml이 있는 폴더)
   - **소스**: `기존 docker-compose.yml 사용` 선택
5. docker-compose.yml 내용이 표시되면 확인
6. **다음** → **완료** 클릭

> 첫 실행 시 `oven/bun:alpine` 이미지 다운로드 + bun install이 진행됩니다 (1-2분).

---

## 3단계: 역방향 프록시 설정 (WSS)

1. **제어판** → **로그인 포털** → **고급** → **역방향 프록시**
2. **생성** 클릭
3. 설정:

| 항목 | 소스 (외부) | 대상 (내부) |
|------|------------|------------|
| 설명 | GameVault | - |
| 프로토콜 | HTTPS | HTTP |
| 호스트 이름 | game.poke86.com | localhost |
| 포트 | 443 | 3000 |

4. 생성 후 → **사용자 지정 헤더** 탭 → **WebSocket** 버튼 클릭

> **WebSocket 버튼을 반드시 클릭해야 합니다.** 클릭하면 `Upgrade`, `Connection` 헤더가 자동 추가됩니다. 없으면 WSS 연결이 실패합니다.

5. **저장**

---

## 4단계: 방화벽/포트 포워딩

### 시놀로지 방화벽
1. **제어판** → **보안** → **방화벽**
2. 포트 3000 TCP 허용 규칙 추가

### 공유기 포트 포워딩
1. 공유기 관리 페이지 접속
2. 포트 포워딩 추가:
   - 외부 포트: 443
   - 내부 IP: NAS IP (예: 192.168.0.x)
   - 내부 포트: 443
   - 프로토콜: TCP

---

## 5단계: 동작 확인

브라우저에서 헬스체크:
```
https://game.poke86.com/health
```

아래처럼 응답이 오면 서버 배포 완료:
```json
{"status":"ok","games":["nunchi"]}
```

---

## 6단계: 앱 설정

1. 앱 → 서버 주소: `wss://game.poke86.com`
2. 방 만들기 / 방 입장 테스트

---

## 문제 해결

### 컨테이너가 시작되지 않음
- Container Manager → 프로젝트 → gamevault → 로그 확인
- `bun install` 실패 시: NAS 인터넷 연결 확인

### WSS 연결 실패
- 역방향 프록시 → 사용자 지정 헤더에 WebSocket 항목 있는지 확인
- 포트 포워딩 443 확인
- `https://game.poke86.com/health`는 되는데 WSS만 안되면 → WebSocket 헤더 누락

### 앱에서 연결 실패
- 앱 서버 주소가 `wss://`로 시작하는지 확인
- 같은 WiFi에서는 내부 IP도 가능: `ws://192.168.x.x:3000`

---

## 코드 업데이트 시

1. File Station에서 변경된 파일만 교체
2. Container Manager → 프로젝트 → gamevault → **중지** → **빌드** → **시작**
