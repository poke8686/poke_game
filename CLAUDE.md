# CLAUDE.md — Android 게임 모음 앱 (GameVault)

이 파일은 Claude Code가 이 프로젝트에서 작업할 때 참고하는 프로젝트 컨텍스트 문서입니다.

---

## 프로젝트 개요

안드로이드 게임 모음 앱. 홈 화면에서 카테고리 필터로 게임 목록을 탐색하고, 각 게임을 선택해서 플레이하는 구조.

- **앱 이름**: GameVault
- **패키지명**: `com.poke86.game`
- **최소 SDK**: API 26 (Android 8.0)
- **타겟 SDK**: API 34

---

## 기술 스택

| 항목 | 선택 |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose |
| 내비게이션 | Navigation Compose |
| 아키텍처 | MVVM + Repository 패턴 |
| DI | Hilt |
| 저장소 | DataStore (설정/카테고리) |
| 빌드 도구 | Gradle (Kotlin DSL) |

---

## 앱 구조

```
app/src/main/java/com/gamevault/app/
├── ui/
│   ├── home/              # 홈 화면 (게임 목록 + 카테고리 필터)
│   ├── games/             # 각 게임 화면
│   │   ├── nunchigame/    # 눈치 게임
│   │   ├── reaction/      # 반응속도 대결
│   │   ├── balance/       # 밸런스 게임
│   │   ├── wordchain/     # 끝말잇기
│   │   ├── memory/        # 숫자 기억
│   │   ├── colortest/     # 색깔 맞추기
│   │   ├── spy/           # 스파이 게임
│   │   └── chosung/       # 초성 퀴즈
│   └── theme/             # 공통 테마/색상
├── domain/
│   ├── model/             # Game, Category 데이터 클래스
│   └── repository/        # GameRepository 인터페이스
└── data/
    └── repository/        # GameRepositoryImpl
```

---

## 데이터 모델

```kotlin
data class Game(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,           // 이모지 문자열
    val categories: List<String>,
    val tags: List<GameTag>,
    val route: String
)

enum class GameTag { MULTI, SOLO, QUICK, BRAIN }

data class Category(
    val id: String,
    val label: String
)
```

---

## 게임 목록 (초기 8종)

| ID | 게임 이름 | 카테고리 | 태그 | 특징 |
|---|---|---|---|---|
| `nunchigame` | 눈치 게임 | party, reflex | MULTI | 번호 겹치면 탈락 |
| `reaction` | 반응속도 대결 | party, reflex | MULTI, QUICK | 화면 변화 반응 |
| `balance` | 밸런스 게임 | party, brain | MULTI, BRAIN | 양자택일 다수결 |
| `wordchain` | 끝말잇기 | party, brain | MULTI, BRAIN | 타이머 포함 |
| `memory` | 숫자 기억 | solo, brain | SOLO, BRAIN | 단기 기억력 |
| `colortest` | 색깔 맞추기 | solo, reflex | SOLO, QUICK | 스트룹 효과 |
| `spy` | 스파이 게임 | party, brain | MULTI, BRAIN | 주제 추리 |
| `chosung` | 초성 퀴즈 | solo, brain | SOLO, BRAIN | 초성 → 단어 |

---

## 카테고리 목록

| ID | 표시 이름 |
|---|---|
| `all` | 전체 |
| `party` | 파티 |
| `solo` | 혼자 |
| `reflex` | 반응속도 |
| `brain` | 두뇌 |

---

## 홈 화면 스펙

- **상단**: 가로 스크롤 카테고리 칩 필터 (선택된 칩은 강조 색상)
- **본문**: 2열 `LazyVerticalGrid` 게임 카드
- **카드 내용**: 아이콘(이모지), 게임 이름, 한 줄 설명, 태그 뱃지

---

## 주요 업데이트 내역 (2026-04-21)

- **타워 디펜스 시각 효과**: 캐릭터 등급(F~S+)별 바닥 장판(Grade Plate) 및 화려한 애니메이션 추가
- **앱 아이콘 리뉴얼**: 4분할 컬러 그리드와 중앙 컨트롤러 디자인의 'GameVault' 전용 아이콘 적용
- **빌드 환경 최적화**: `gradle.properties`에 JDK 경로 명시하여 빌드 호환성 해결

---

## 작업 규칙

- 각 게임은 독립적인 파일/패키지로 분리 (추후 추가가 쉽도록)
- 카테고리 목록은 하드코딩하지 않고 데이터로 관리
- Compose `@Preview` 어노테이션 포함
- 한국어 string resource 사용 (`res/values/strings.xml`)
- 빌드 에러 없이 실행 가능한 상태로 마무리
- 신규 게임 추가 시: `domain/model` 데이터 추가 → `GameRepositoryImpl` 등록 → `ui/games/<id>/` 화면 작성 → Navigation 라우트 연결
- **빌드 주의사항**: JDK 버전 호환성을 위해 `gradle.properties`의 `org.gradle.java.home` 설정을 확인한다.

---

## "마무리해줘" 명령어

사용자가 **"마무리해줘"** 라고 말하면 아래 단계를 순서대로 수행한다:

1. **CLAUDE.md 업데이트** — 현재 세션에서 작업한 내용을 CLAUDE.md에 반영
2. **README.md 업데이트** — GitHub용 README.md에 사용자용 매뉴얼 반영
3. **DEV_GUIDE.md 업데이트** — DEV_GUIDE.md에 개발자용 매뉴얼 반영
4. **릴리즈 APK 빌드** — `./gradlew assembleRelease` 실행하여 최종 빌드 검증
5. **릴리즈 AAB 빌드** — `./gradlew bundleRelease` 실행하여 Play Store 배포용 AAB 생성
6. **GitHub 푸시** — 변경 사항을 커밋하고 `git push origin main`

---

## 다음 세션 예정 작업

- 각 게임 화면 구현 (눈치 게임부터 시작)
- 멀티플레이어 게임의 방 생성/참여 흐름
- 점수/기록 저장 (DataStore 또는 Room)
- 게임 결과 화면
