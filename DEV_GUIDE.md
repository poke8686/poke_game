# DEV_GUIDE.md — GameVault 개발자 가이드

---

## 목차

1. [개발 환경 설정](#1-개발-환경-설정)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [아키텍처 설명](#3-아키텍처-설명)
4. [새 게임 추가하기](#4-새-게임-추가하기)
5. [새 카테고리 추가하기](#5-새-카테고리-추가하기)
6. [빌드 및 배포](#6-빌드-및-배포)
7. [코딩 컨벤션](#7-코딩-컨벤션)
8. [의존성 관리](#8-의존성-관리)
9. [문제 해결 (Troubleshooting)](#9-문제-해결-troubleshooting)

---

## 1. 개발 환경 설정

### 필수 도구

| 도구 | 버전 | 비고 |
|---|---|---|
| Android Studio | Hedgehog 이상 | Jellyfish 권장 |
| JDK | 17 이상 | Android Studio 번들 JDK 사용 가능 |
| Android SDK | API 34 (compileSdk) | SDK Manager에서 설치 |
| Kotlin | 1.9.x 이상 | Gradle 플러그인과 함께 자동 설정 |

### 초기 셋업

```bash
# 1. 레포지토리 클론
git clone https://github.com/<owner>/gamevault.git
cd gamevault

# 2. Android Studio에서 프로젝트 열기
# File > Open > 클론한 디렉터리 선택

# 3. Gradle 동기화
# Android Studio 상단 "Sync Now" 클릭 또는:
./gradlew build
```

### SDK 및 에뮬레이터 설정

1. Android Studio → SDK Manager → **Android 8.0 (API 26)** 이상 설치
2. AVD Manager → 에뮬레이터 생성 (Pixel 6, API 34 권장)
3. 또는 물리 기기를 USB 디버깅 모드로 연결

---

## 2. 프로젝트 구조

```
gamevault/
├── app/
│   ├── build.gradle.kts              # 앱 모듈 빌드 설정
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/poke86/game/
│       │   ├── MainActivity.kt        # 앱 진입점, NavHost 설정
│       │   ├── GameApp.kt        # Hilt Application 클래스
│       │   ├── ui/
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt          # 홈 화면 Composable
│       │   │   │   ├── HomeViewModel.kt       # 홈 화면 ViewModel
│       │   │   │   └── components/
│       │   │   │       ├── CategoryChips.kt   # 카테고리 필터 칩
│       │   │   │       └── GameCard.kt        # 게임 카드 컴포넌트
│       │   │   ├── games/
│       │   │   │   ├── nunchigame/
│       │   │   │   │   └── NunchiGameScreen.kt
│       │   │   │   ├── reaction/
│       │   │   │   │   └── ReactionScreen.kt
│       │   │   │   ├── balance/
│       │   │   │   │   └── BalanceScreen.kt
│       │   │   │   ├── wordchain/
│       │   │   │   │   └── WordChainScreen.kt
│       │   │   │   ├── memory/
│       │   │   │   │   └── MemoryScreen.kt
│       │   │   │   ├── colortest/
│       │   │   │   │   └── ColorTestScreen.kt
│       │   │   │   ├── spy/
│       │   │   │   │   └── SpyScreen.kt
│       │   │   │   └── chosung/
│       │   │   │       └── ChosungScreen.kt
│       │   │   └── theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── domain/
│       │   │   ├── model/
│       │   │   │   ├── Game.kt            # Game 데이터 클래스
│       │   │   │   ├── GameTag.kt         # GameTag enum
│       │   │   │   └── Category.kt        # Category 데이터 클래스
│       │   │   └── repository/
│       │   │       └── GameRepository.kt  # Repository 인터페이스
│       │   └── data/
│       │       └── repository/
│       │           └── GameRepositoryImpl.kt  # 하드코딩 초기 데이터
│       └── res/
│           ├── values/
│           │   └── strings.xml            # 한국어 문자열 리소스
│           └── ...
├── build.gradle.kts                   # 루트 빌드 설정
├── settings.gradle.kts
├── gradle/libs.versions.toml          # Version Catalog
├── CLAUDE.md                          # Claude Code 컨텍스트 문서
├── DEV_GUIDE.md                       # 이 파일
└── README.md                          # 사용자용 앱 소개
```

---

## 3. 아키텍처 설명

### MVVM + Repository 패턴

```
UI Layer (Composable)
    ↕ StateFlow / collectAsState
ViewModel Layer
    ↕ suspend fun / Flow
Repository Interface (domain)
    ↕ implements
Repository Impl (data)
    ↕
Data Source (하드코딩 / DataStore / Room)
```

**흐름 예시 — 홈 화면 카테고리 필터:**

1. `HomeScreen`에서 칩 클릭 이벤트 발생
2. `HomeViewModel.onCategorySelected(categoryId)` 호출
3. ViewModel 내부 `_selectedCategory: MutableStateFlow<String>` 업데이트
4. `filteredGames: StateFlow<List<Game>>` 자동 재계산 (combine)
5. `HomeScreen`이 새 리스트로 recompose

### Hilt DI 구조

```kotlin
// GameRepository 바인딩 예시
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
}
```

### Navigation 구조

```kotlin
// 라우트 상수 (Screen.kt)
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NunchiGame : Screen("game/nunchigame")
    object Reaction : Screen("game/reaction")
    // ...
}

// NavHost (MainActivity.kt)
NavHost(navController, startDestination = Screen.Home.route) {
    composable(Screen.Home.route) { HomeScreen(navController) }
    composable(Screen.NunchiGame.route) { NunchiGameScreen(navController) }
    // ...
}
```

---

## 4. 새 게임 추가하기

게임을 추가할 때는 아래 순서를 따른다. 순서를 지키면 빌드 에러 없이 작업할 수 있다.

### Step 1 — 도메인 데이터 등록

`data/repository/GameRepositoryImpl.kt`의 게임 목록에 항목 추가:

```kotlin
Game(
    id = "quiz",
    name = "OX 퀴즈",
    description = "상식 OX 문제를 맞혀라",
    icon = "❓",
    categories = listOf("party", "brain"),
    tags = listOf(GameTag.MULTI, GameTag.BRAIN),
    route = Screen.Quiz.route
)
```

### Step 2 — Navigation 라우트 추가

`ui/navigation/Screen.kt`에 라우트 추가:

```kotlin
object Quiz : Screen("game/quiz")
```

### Step 3 — 게임 화면 파일 생성

`ui/games/quiz/QuizScreen.kt` 파일 생성:

```kotlin
@Composable
fun QuizScreen(navController: NavController) {
    // 구현
}

@Preview
@Composable
private fun QuizScreenPreview() {
    GameVaultTheme {
        QuizScreen(navController = rememberNavController())
    }
}
```

### Step 4 — NavHost 연결

`MainActivity.kt`의 NavHost에 composable 블록 추가:

```kotlin
composable(Screen.Quiz.route) {
    QuizScreen(navController)
}
```

### Step 5 — strings.xml 추가

`res/values/strings.xml`에 게임 이름/설명 문자열 추가:

```xml
<string name="game_quiz_name">OX 퀴즈</string>
<string name="game_quiz_desc">상식 OX 문제를 맞혀라</string>
```

---

## 5. 새 카테고리 추가하기

`GameRepositoryImpl.kt`의 카테고리 목록에 항목 추가:

```kotlin
Category(id = "speed", label = "스피드")
```

카테고리는 코드 어디에도 하드코딩하지 않는다. `HomeViewModel`이 Repository에서 카테고리 목록을 읽어 UI에 전달하므로, Repository에만 추가하면 자동으로 반영된다.

---

## 6. 빌드 및 배포

### 디버그 빌드 (개발 중)

```bash
./gradlew assembleDebug
# 결과물: app/build/outputs/apk/debug/app-debug.apk
```

### 릴리즈 APK 빌드

```bash
./gradlew assembleRelease
# 결과물: app/build/outputs/apk/release/app-release.apk
```

> 릴리즈 빌드는 키스토어 서명이 필요하다. `app/build.gradle.kts`의 `signingConfigs` 블록에 키스토어 정보를 설정해야 한다.

### Play Store용 AAB 빌드

```bash
./gradlew bundleRelease
# 결과물: app/build/outputs/bundle/release/app-release.aab
```

### 핸드폰에 apk 설치 (빌드 설치)
```bash
adb -s R3CT30Z61TA install -r app/build/outputs/apk/debug/app-debug.apk
```



### 키스토어 설정 (릴리즈 빌드 전 필수)

`app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

비밀번호는 환경변수 또는 `local.properties`에서 관리하고, 절대 git에 커밋하지 않는다.

### GitHub 푸시

```bash
git add .
git commit -m "feat: 작업 내용 요약"
git push origin main
```

---

## 7. 코딩 컨벤션

### Composable 함수

- 파일당 하나의 공개 Composable + 하나의 `@Preview`
- 공개 Composable: PascalCase (`HomeScreen`, `GameCard`)
- 내부 헬퍼 Composable: private 함수로 같은 파일에 위치
- 상태는 항상 ViewModel에서 관리, Composable에서 직접 `remember`로 비즈니스 상태를 갖지 않는다

### ViewModel

- UI 상태는 `StateFlow` 또는 `MutableStateFlow`로 노출
- 단방향 데이터 흐름: ViewModel → UI (StateFlow), UI → ViewModel (이벤트 함수)
- `viewModelScope.launch`로 비동기 작업 수행

### 네이밍

| 종류 | 컨벤션 | 예시 |
|---|---|---|
| Composable | PascalCase | `GameCard`, `HomeScreen` |
| ViewModel | `<화면명>ViewModel` | `HomeViewModel` |
| StateFlow | `_<이름>` (private) + `<이름>` (public) | `_selectedCategory`, `selectedCategory` |
| 라우트 상수 | `Screen.<게임명>` | `Screen.NunchiGame` |
| string 리소스 | `snake_case` | `game_memory_name` |

### 문자열

모든 사용자에게 보이는 문자열은 `strings.xml`에 정의한다. Composable에서 직접 한국어 리터럴을 사용하지 않는다.

```kotlin
// Bad
Text("눈치 게임")

// Good
Text(stringResource(R.string.game_nunchigame_name))
```

---

## 8. 의존성 관리

`gradle/libs.versions.toml`을 Version Catalog로 사용한다.

```toml
[versions]
compose-bom = "2024.04.00"
hilt = "2.51"
navigation-compose = "2.7.7"
datastore = "1.1.1"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

의존성 버전을 올릴 때는 `libs.versions.toml`의 `[versions]` 섹션만 수정한다.

---

## 9. 문제 해결 (Troubleshooting)

### Hilt 관련 빌드 에러

```
error: [Hilt] Cannot process an @HiltAndroidApp application
```

`AndroidManifest.xml`의 `android:name`이 `GameApp`으로 설정되어 있는지 확인:

```xml
<application
    android:name=".GameApp"
    ...>
```

### Compose Preview 렌더링 실패

- Preview가 ViewModel 의존성을 직접 주입받으면 실패한다.
- Preview용 파라미터를 인터페이스나 람다로 추상화하거나, `@PreviewParameter`를 활용한다.

### Navigation 화면 전환이 안 될 때

- `NavController`가 컴포저블 트리 내에서 동일 인스턴스인지 확인
- `rememberNavController()`는 `NavHost`와 같은 레벨의 Composable에서 한 번만 호출

### Gradle 동기화 실패

```bash
./gradlew clean
./gradlew --stop   # Gradle 데몬 종료
./gradlew build    # 재빌드
```

캐시 문제라면 `~/.gradle/caches` 폴더 삭제 후 재시도.

---

## 관련 문서

- [CLAUDE.md](./CLAUDE.md) — Claude Code 컨텍스트 및 작업 규칙
- [README.md](./README.md) — 사용자용 앱 소개 및 설치 방법
- [Android Jetpack Compose 공식 문서](https://developer.android.com/jetpack/compose)
- [Hilt 공식 문서](https://dagger.dev/hilt/)
- [Navigation Compose 공식 문서](https://developer.android.com/jetpack/compose/navigation)


