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

## 주요 업데이트 내역 (2026-06-05)

- **신규 게임 「왕국 국경 디펜스」(nightfall)**: Thronefall/Nightfall 스타일의 탑다운 거점 디펜스.
  낮(건설/업그레이드) → 밤(웨이브 방어) → 아침(수확) 루프. 중앙 성 + 성벽/성문/방벽,
  노드 기반 건물(화살탑/막사/집), 막사 병사 자동전투, 티어 업그레이드(Lv1~8),
  성 레벨업, 바이옴(초원→설원→용암), 다방향 성문, 5웨이브마다 보스, 파티클 VFX, 저장/복원.
- **Korge 게임엔진 도입**: 대규모 유닛/파티클을 위해 `nightfall` 게임만 Korge 6.0.0 사용.
- (이전) 타워 디펜스 등급판/성장, 앱 아이콘, JDK 경로 설정.

### Nightfall (Korge) 기술 메모

- **엔진**: Korge `com.soywiz.korge:korge:6.0.0` — **라이브러리로만** 추가(Korge Gradle 플러그인 미적용, Kotlin 플러그인 충돌 방지).
- **호환성**: Korge 6.0.0이 Kotlin 2.0.20로 빌드됨 → 프로젝트 **Kotlin 2.0.0→2.0.20, KSP→2.0.20-1.0.25, JVM target 17→21** 상향(JBR 21).
- **임베드**: `ui/games/nightfall/NightfallScreen.kt` 가 Compose `AndroidView` 로 `KorgeAndroidView` 를 감싸고 `loadModule(NightfallKorge.config())` 호출. `onRelease`에서 `unloadModule`.
- **엔진 코드**: `game/nightfall/` (순수 Kotlin, Compose/Hilt 비의존). 게임 로직은 `NightfallWorld.kt`.
- **폰트**: 한글은 Korge 기본 폰트(ASCII 전용)로 안 나옴 → `assets/fonts/NanumGothicBold.ttf`(OFL, TrueType) 번들 후 `resourcesVfs[...].readTtfFont()` 로 로드.
- **저장**: `NightfallSave`(KorgeKt) 브리지에 `NightfallScreen`이 SharedPreferences 람다 주입 → `NightfallWorld`가 직렬화 문자열 저장/복원.
- **벤치마크 원작**은 Unity(IL2CPP) 게임 — 코드/에셋 이식 불가(저작권), 디자인/구조만 오리지널로 재현.
- **남은 폴리시**: 효과음(AudioTrack) 미구현, 추가 적/유닛 종류, 밸런스 튜닝.

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
