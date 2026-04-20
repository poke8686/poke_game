# GameVault Initial Project Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 홈 화면(카테고리 필터 + 게임 카드 그리드)과 8개 게임 placeholder 화면으로 구성된 완전히 빌드 가능한 Android 앱 초기 프로젝트를 생성한다.

**Architecture:** MVVM + Repository 패턴. Domain 레이어(인터페이스) → Data 레이어(구현체) → UI 레이어(Composable + ViewModel). Hilt로 DI, Navigation Compose로 화면 이동.

**Tech Stack:** Kotlin 2.0.0, Jetpack Compose (BOM 2024.09.02), Navigation Compose 2.7.7, Hilt 2.51.1, KSP 2.0.0-1.0.21, AGP 8.4.2, Gradle 8.7

---

## File Map

```
d:/app/android_app/game/
├── .gitignore
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── gradlew  (download)
├── gradlew.bat  (download)
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/poke86/game/
│       │   │   ├── GameApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── Routes.kt
│       │   │   ├── domain/model/Game.kt          (Game + GameTag)
│       │   │   ├── domain/model/Category.kt
│       │   │   ├── domain/repository/GameRepository.kt
│       │   │   ├── data/repository/GameRepositoryImpl.kt
│       │   │   ├── di/RepositoryModule.kt
│       │   │   ├── ui/theme/Color.kt
│       │   │   ├── ui/theme/Type.kt
│       │   │   ├── ui/theme/Theme.kt
│       │   │   ├── ui/navigation/Screen.kt
│       │   │   ├── ui/navigation/NavGraph.kt
│       │   │   ├── ui/home/HomeViewModel.kt
│       │   │   ├── ui/home/HomeScreen.kt
│       │   │   ├── ui/home/components/CategoryChips.kt
│       │   │   ├── ui/home/components/GameCard.kt
│       │   │   └── ui/games/{nunchigame,reaction,balance,wordchain,
│       │   │                   memory,colortest,spy,chosung}/*Screen.kt
│       │   └── res/
│       │       ├── values/strings.xml
│       │       ├── values/themes.xml
│       │       ├── drawable/ic_launcher_background.xml
│       │       ├── drawable/ic_launcher_foreground.xml
│       │       └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
│       └── test/java/com/poke86/game/
│           ├── data/repository/GameRepositoryImplTest.kt
│           └── ui/home/HomeViewModelTest.kt
```

---

## Chunk 1: Gradle Build System

### Task 1: 프로젝트 루트 빌드 파일 생성

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: .gitignore 생성**

```
*.iml
.gradle
/local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
/.idea/navEditor.xml
/.idea/assetWizardSettings.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
keystore/
```

- [ ] **Step 2: settings.gradle.kts 생성**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GameVault"
include(":app")
```

- [ ] **Step 3: 루트 build.gradle.kts 생성**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 4: gradle/libs.versions.toml 생성**

```toml
[versions]
agp = "8.4.2"
kotlin = "2.0.0"
ksp = "2.0.0-1.0.21"
composeBom = "2024.09.02"
activityCompose = "1.9.2"
lifecycleViewmodelCompose = "2.8.6"
navigationCompose = "2.7.7"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
datastore = "1.1.1"
junit = "4.13.2"
coroutinesTest = "1.8.1"
turbine = "1.1.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 5: gradle/wrapper/gradle-wrapper.properties 생성**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 6: Gradle Wrapper 스크립트 다운로드**

GitHub raw URL (tag ref, 권장):
```bash
mkdir -p gradle/wrapper
curl -L -o gradlew \
  "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradlew"
curl -L -o gradlew.bat \
  "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradlew.bat"
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
chmod +x gradlew
```

위 URL이 404 응답 시 아래 PowerShell 대체 명령 사용:
```powershell
# Gradle 배포판 ZIP에서 wrapper jar 추출
$ver = "8.7"
$zip = "$env:TEMP\gradle-$ver-bin.zip"
Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$ver-bin.zip" -OutFile $zip
Expand-Archive $zip -DestinationPath "$env:TEMP\gradle-$ver"
Copy-Item "$env:TEMP\gradle-$ver\gradle-$ver\lib\gradle-wrapper-*.jar" `
  "gradle/wrapper/gradle-wrapper.jar" -ErrorAction SilentlyContinue
# gradlew 스크립트는 ZIP 내 포함됨
Copy-Item "$env:TEMP\gradle-$ver\gradle-$ver\bin\gradlew" "gradlew"
Copy-Item "$env:TEMP\gradle-$ver\gradle-$ver\bin\gradlew.bat" "gradlew.bat"
```

또는 시스템에 Gradle이 설치된 경우:
```bash
gradle wrapper --gradle-version 8.7
chmod +x gradlew
```

### Task 2: 앱 모듈 빌드 파일 생성

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: app/build.gradle.kts 생성**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.poke86.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.poke86.game"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.material.icons.core)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 2: app/proguard-rules.pro 생성**

```
# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
```

---

## Chunk 2: 앱 스켈레톤 + 리소스

### Task 3: AndroidManifest + Application + MainActivity

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/poke86/game/GameApp.kt`
- Create: `app/src/main/java/com/poke86/game/MainActivity.kt`

- [ ] **Step 1: AndroidManifest.xml 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".GameApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.GameVault">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 2: GameApp.kt 생성**

```kotlin
package com.poke86.game

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GameApp : Application()
```

- [ ] **Step 3: MainActivity.kt 생성**

```kotlin
package com.poke86.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.poke86.game.ui.navigation.NavGraph
import com.poke86.game.ui.theme.GameVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameVaultTheme {
                NavGraph()
            }
        }
    }
}
```

### Task 4: 리소스 파일 생성

**Files:**
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

- [ ] **Step 1: strings.xml 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">GameVault</string>

    <!-- 홈 화면 -->
    <string name="home_title">게임 모음</string>

    <!-- 게임 이름 / 설명 -->
    <string name="game_nunchigame_name">눈치 게임</string>
    <string name="game_nunchigame_desc">번호 겹치면 탈락!</string>
    <string name="game_reaction_name">반응속도 대결</string>
    <string name="game_reaction_desc">화면 변화에 가장 빨리 반응하라</string>
    <string name="game_balance_name">밸런스 게임</string>
    <string name="game_balance_desc">양자택일, 다수결로 승부</string>
    <string name="game_wordchain_name">끝말잇기</string>
    <string name="game_wordchain_desc">타이머 안에 단어를 이어라</string>
    <string name="game_memory_name">숫자 기억</string>
    <string name="game_memory_desc">단기 기억력을 테스트하라</string>
    <string name="game_colortest_name">색깔 맞추기</string>
    <string name="game_colortest_desc">글자 색깔과 의미, 무엇을 따를까?</string>
    <string name="game_spy_name">스파이 게임</string>
    <string name="game_spy_desc">우리 중 스파이를 찾아라</string>
    <string name="game_chosung_name">초성 퀴즈</string>
    <string name="game_chosung_desc">초성만 보고 단어를 맞혀라</string>

    <!-- 공통 -->
    <string name="back">뒤로</string>
    <string name="coming_soon">준비 중입니다</string>
</resources>
```

- [ ] **Step 2: themes.xml 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.GameVault" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 3: 런처 아이콘 벡터 파일 생성**

`res/drawable/ic_launcher_background.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#6750A4" />
</shape>
```

`res/drawable/ic_launcher_foreground.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,26 L74,66 H34 Z" />
</vector>
```

`res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

`res/mipmap-anydpi-v26/ic_launcher_round.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

---

## Chunk 3: Domain 레이어

### Task 5: 도메인 모델 + Repository 인터페이스

**Files:**
- Create: `app/src/main/java/com/poke86/game/Routes.kt`
- Create: `app/src/main/java/com/poke86/game/domain/model/Game.kt`
- Create: `app/src/main/java/com/poke86/game/domain/model/Category.kt`
- Create: `app/src/main/java/com/poke86/game/domain/repository/GameRepository.kt`
- Test: `app/src/test/java/com/poke86/game/domain/model/GameTest.kt`

- [ ] **Step 1: Routes.kt 생성** — data/ui 양측에서 참조하는 라우트 상수

```kotlin
package com.poke86.game

object Routes {
    const val HOME = "home"
    const val NUNCHIGAME = "game/nunchigame"
    const val REACTION = "game/reaction"
    const val BALANCE = "game/balance"
    const val WORDCHAIN = "game/wordchain"
    const val MEMORY = "game/memory"
    const val COLORTEST = "game/colortest"
    const val SPY = "game/spy"
    const val CHOSUNG = "game/chosung"
}
```

- [ ] **Step 2: domain/model/Game.kt 생성**

```kotlin
package com.poke86.game.domain.model

data class Game(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val categories: List<String>,
    val tags: List<GameTag>,
    val route: String
)

enum class GameTag(val label: String) {
    MULTI("멀티"),
    SOLO("혼자"),
    QUICK("빠른판"),
    BRAIN("두뇌")
}
```

- [ ] **Step 3: domain/model/Category.kt 생성**

```kotlin
package com.poke86.game.domain.model

data class Category(
    val id: String,
    val label: String
)
```

- [ ] **Step 4: domain/repository/GameRepository.kt 생성**

```kotlin
package com.poke86.game.domain.repository

import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game

interface GameRepository {
    fun getGames(): List<Game>
    fun getCategories(): List<Category>
}
```

- [ ] **Step 5: 도메인 모델 단위 테스트 작성**

`app/src/test/java/com/poke86/game/domain/model/GameTest.kt`:
```kotlin
package com.poke86.game.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GameTest {

    @Test
    fun `Game data class equality works correctly`() {
        val game1 = Game("id", "name", "desc", "🎮", listOf("party"), listOf(GameTag.MULTI), "route")
        val game2 = Game("id", "name", "desc", "🎮", listOf("party"), listOf(GameTag.MULTI), "route")
        assertEquals(game1, game2)
    }

    @Test
    fun `GameTag has correct labels`() {
        assertEquals("멀티", GameTag.MULTI.label)
        assertEquals("혼자", GameTag.SOLO.label)
        assertEquals("빠른판", GameTag.QUICK.label)
        assertEquals("두뇌", GameTag.BRAIN.label)
    }
}
```

- [ ] **Step 6: 테스트 실행 확인**

```bash
./gradlew :app:test --tests "com.poke86.game.domain.model.GameTest"
```
Expected: BUILD SUCCESSFUL

---

## Chunk 4: Data 레이어

### Task 6: GameRepositoryImpl + Hilt DI 모듈

**Files:**
- Create: `app/src/main/java/com/poke86/game/data/repository/GameRepositoryImpl.kt`
- Create: `app/src/main/java/com/poke86/game/di/RepositoryModule.kt`
- Test: `app/src/test/java/com/poke86/game/data/repository/GameRepositoryImplTest.kt`

- [ ] **Step 1: 테스트 먼저 작성 (TDD)**

`app/src/test/java/com/poke86/game/data/repository/GameRepositoryImplTest.kt`:
```kotlin
package com.poke86.game.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameRepositoryImplTest {

    private lateinit var repository: GameRepositoryImpl

    @Before
    fun setUp() {
        repository = GameRepositoryImpl()
    }

    @Test
    fun `getGames returns exactly 8 games`() {
        assertEquals(8, repository.getGames().size)
    }

    @Test
    fun `getCategories returns exactly 5 categories`() {
        assertEquals(5, repository.getCategories().size)
    }

    @Test
    fun `first category id is all`() {
        assertEquals("all", repository.getCategories().first().id)
    }

    @Test
    fun `all games have non-empty route`() {
        assertTrue(repository.getGames().all { it.route.isNotEmpty() })
    }

    @Test
    fun `all games have at least one tag`() {
        assertTrue(repository.getGames().all { it.tags.isNotEmpty() })
    }

    @Test
    fun `all games have at least one category`() {
        assertTrue(repository.getGames().all { it.categories.isNotEmpty() })
    }

    @Test
    fun `party games exist`() {
        val partyGames = repository.getGames().filter { "party" in it.categories }
        assertFalse(partyGames.isEmpty())
    }

    @Test
    fun `solo games exist`() {
        val soloGames = repository.getGames().filter { "solo" in it.categories }
        assertFalse(soloGames.isEmpty())
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew :app:test --tests "com.poke86.game.data.repository.GameRepositoryImplTest"
```
Expected: FAILED (GameRepositoryImpl 없음)

- [ ] **Step 3: GameRepositoryImpl 구현**

`app/src/main/java/com/poke86/game/data/repository/GameRepositoryImpl.kt`:
```kotlin
package com.poke86.game.data.repository

import com.poke86.game.Routes
import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.model.GameTag
import com.poke86.game.domain.repository.GameRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor() : GameRepository {

    override fun getGames(): List<Game> = listOf(
        Game(
            id = "nunchigame",
            name = "눈치 게임",
            description = "번호 겹치면 탈락!",
            icon = "👁️",
            categories = listOf("party", "reflex"),
            tags = listOf(GameTag.MULTI),
            route = Routes.NUNCHIGAME
        ),
        Game(
            id = "reaction",
            name = "반응속도 대결",
            description = "화면 변화에 가장 빨리 반응하라",
            icon = "⚡",
            categories = listOf("party", "reflex"),
            tags = listOf(GameTag.MULTI, GameTag.QUICK),
            route = Routes.REACTION
        ),
        Game(
            id = "balance",
            name = "밸런스 게임",
            description = "양자택일, 다수결로 승부",
            icon = "⚖️",
            categories = listOf("party", "brain"),
            tags = listOf(GameTag.MULTI, GameTag.BRAIN),
            route = Routes.BALANCE
        ),
        Game(
            id = "wordchain",
            name = "끝말잇기",
            description = "타이머 안에 단어를 이어라",
            icon = "🔤",
            categories = listOf("party", "brain"),
            tags = listOf(GameTag.MULTI, GameTag.BRAIN),
            route = Routes.WORDCHAIN
        ),
        Game(
            id = "memory",
            name = "숫자 기억",
            description = "단기 기억력을 테스트하라",
            icon = "🔢",
            categories = listOf("solo", "brain"),
            tags = listOf(GameTag.SOLO, GameTag.BRAIN),
            route = Routes.MEMORY
        ),
        Game(
            id = "colortest",
            name = "색깔 맞추기",
            description = "글자 색깔과 의미, 무엇을 따를까?",
            icon = "🎨",
            categories = listOf("solo", "reflex"),
            tags = listOf(GameTag.SOLO, GameTag.QUICK),
            route = Routes.COLORTEST
        ),
        Game(
            id = "spy",
            name = "스파이 게임",
            description = "우리 중 스파이를 찾아라",
            icon = "🕵️",
            categories = listOf("party", "brain"),
            tags = listOf(GameTag.MULTI, GameTag.BRAIN),
            route = Routes.SPY
        ),
        Game(
            id = "chosung",
            name = "초성 퀴즈",
            description = "초성만 보고 단어를 맞혀라",
            icon = "🔡",
            categories = listOf("solo", "brain"),
            tags = listOf(GameTag.SOLO, GameTag.BRAIN),
            route = Routes.CHOSUNG
        )
    )

    override fun getCategories(): List<Category> = listOf(
        Category(id = "all", label = "전체"),
        Category(id = "party", label = "파티"),
        Category(id = "solo", label = "혼자"),
        Category(id = "reflex", label = "반응속도"),
        Category(id = "brain", label = "두뇌")
    )
}
```

- [ ] **Step 4: di/RepositoryModule.kt 생성**

```kotlin
package com.poke86.game.di

import com.poke86.game.data.repository.GameRepositoryImpl
import com.poke86.game.domain.repository.GameRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
}
```

- [ ] **Step 5: 테스트 재실행 — 통과 확인**

```bash
./gradlew :app:test --tests "com.poke86.game.data.repository.GameRepositoryImplTest"
```
Expected: BUILD SUCCESSFUL, 8 tests passed

- [ ] **Step 6: 커밋**

```bash
git init
git add .
git commit -m "feat: add build system, domain layer, data layer"
```

---

## Chunk 5: UI 테마

### Task 7: Material3 테마 설정

**Files:**
- Create: `app/src/main/java/com/poke86/game/ui/theme/Color.kt`
- Create: `app/src/main/java/com/poke86/game/ui/theme/Type.kt`
- Create: `app/src/main/java/com/poke86/game/ui/theme/Theme.kt`

- [ ] **Step 1: Color.kt 생성**

```kotlin
package com.poke86.game.ui.theme

import androidx.compose.ui.graphics.Color

val Purple10 = Color(0xFF21005D)
val Purple40 = Color(0xFF6750A4)
val Purple80 = Color(0xFFD0BCFF)
val Purple90 = Color(0xFFEADDFF)

val PurpleGrey40 = Color(0xFF625B71)
val PurpleGrey80 = Color(0xFFCCC2DC)

val Pink40 = Color(0xFF7D5260)
val Pink80 = Color(0xFFEFB8C8)
```

- [ ] **Step 2: Type.kt 생성**

```kotlin
package com.poke86.game.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 3: Theme.kt 생성**

```kotlin
package com.poke86.game.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun GameVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Chunk 6: Navigation

### Task 8: Screen sealed class + NavGraph

**Files:**
- Create: `app/src/main/java/com/poke86/game/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Screen.kt 생성**

```kotlin
package com.poke86.game.ui.navigation

import com.poke86.game.Routes

sealed class Screen(val route: String) {
    object Home : Screen(Routes.HOME)
    object NunchiGame : Screen(Routes.NUNCHIGAME)
    object Reaction : Screen(Routes.REACTION)
    object Balance : Screen(Routes.BALANCE)
    object WordChain : Screen(Routes.WORDCHAIN)
    object Memory : Screen(Routes.MEMORY)
    object ColorTest : Screen(Routes.COLORTEST)
    object Spy : Screen(Routes.SPY)
    object Chosung : Screen(Routes.CHOSUNG)
}
```

- [ ] **Step 2: NavGraph.kt 생성** — game 화면 composable들은 Chunk 8 이후 추가

```kotlin
package com.poke86.game.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poke86.game.ui.games.balance.BalanceScreen
import com.poke86.game.ui.games.chosung.ChosungScreen
import com.poke86.game.ui.games.colortest.ColorTestScreen
import com.poke86.game.ui.games.memory.MemoryScreen
import com.poke86.game.ui.games.nunchigame.NunchiGameScreen
import com.poke86.game.ui.games.reaction.ReactionScreen
import com.poke86.game.ui.games.spy.SpyScreen
import com.poke86.game.ui.games.wordchain.WordChainScreen
import com.poke86.game.ui.home.HomeScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.NunchiGame.route) { NunchiGameScreen(navController) }
        composable(Screen.Reaction.route) { ReactionScreen(navController) }
        composable(Screen.Balance.route) { BalanceScreen(navController) }
        composable(Screen.WordChain.route) { WordChainScreen(navController) }
        composable(Screen.Memory.route) { MemoryScreen(navController) }
        composable(Screen.ColorTest.route) { ColorTestScreen(navController) }
        composable(Screen.Spy.route) { SpyScreen(navController) }
        composable(Screen.Chosung.route) { ChosungScreen(navController) }
    }
}
```

---

## Chunk 7: 홈 화면

### Task 9: HomeViewModel

**Files:**
- Create: `app/src/main/java/com/poke86/game/ui/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/poke86/game/ui/home/HomeViewModelTest.kt`

- [ ] **Step 1: 테스트 먼저 작성 (TDD)**

```kotlin
package com.poke86.game.ui.home

import app.cash.turbine.test
import com.poke86.game.data.repository.GameRepositoryImpl
import com.poke86.game.domain.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel
    private val repository: GameRepository = GameRepositoryImpl()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial selectedCategory is all`() = runTest {
        assertEquals("all", viewModel.selectedCategory.value)
    }

    @Test
    fun `categories returns 5 items`() = runTest {
        assertEquals(5, viewModel.categories.value.size)
    }

    @Test
    fun `filteredGames shows all 8 games when category is all`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.filteredGames.value.size)
    }

    @Test
    fun `selecting party filters to party games only`() = runTest {
        viewModel.onCategorySelected("party")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.filteredGames.test {
            val games = awaitItem()
            assertTrue(games.isNotEmpty())
            assertTrue(games.all { "party" in it.categories })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting solo filters to solo games only`() = runTest {
        viewModel.onCategorySelected("solo")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.filteredGames.test {
            val games = awaitItem()
            assertTrue(games.isNotEmpty())
            assertTrue(games.all { "solo" in it.categories })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching back to all shows all games`() = runTest {
        viewModel.onCategorySelected("party")
        viewModel.onCategorySelected("all")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.filteredGames.value.size)
    }
}
```

- [ ] **Step 2: HomeViewModel 구현**

```kotlin
package com.poke86.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<Category>> =
        MutableStateFlow(repository.getCategories()).asStateFlow()

    val filteredGames: StateFlow<List<Game>> = combine(
        MutableStateFlow(repository.getGames()),
        _selectedCategory
    ) { games, category ->
        if (category == "all") games
        else games.filter { category in it.categories }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.getGames()
    )

    fun onCategorySelected(categoryId: String) {
        _selectedCategory.value = categoryId
    }
}
```

- [ ] **Step 3: 테스트 실행 확인**

```bash
./gradlew :app:test --tests "com.poke86.game.ui.home.HomeViewModelTest"
```
Expected: BUILD SUCCESSFUL, 6 tests passed

### Task 10: HomeScreen + 컴포넌트

**Files:**
- Create: `app/src/main/java/com/poke86/game/ui/home/components/CategoryChips.kt`
- Create: `app/src/main/java/com/poke86/game/ui/home/components/GameCard.kt`
- Create: `app/src/main/java/com/poke86/game/ui/home/HomeScreen.kt`

- [ ] **Step 1: CategoryChips.kt 생성**

```kotlin
package com.poke86.game.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.poke86.game.domain.model.Category
import com.poke86.game.ui.theme.GameVaultTheme

@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.label) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryChipsPreview() {
    GameVaultTheme {
        CategoryChips(
            categories = listOf(
                Category("all", "전체"),
                Category("party", "파티"),
                Category("solo", "혼자"),
                Category("reflex", "반응속도"),
                Category("brain", "두뇌")
            ),
            selectedCategoryId = "all",
            onCategorySelected = {}
        )
    }
}
```

- [ ] **Step 2: GameCard.kt 생성**

```kotlin
package com.poke86.game.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.model.GameTag
import com.poke86.game.ui.theme.GameVaultTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = game.icon, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = game.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                game.tags.forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag.label, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameCardPreview() {
    GameVaultTheme {
        GameCard(
            game = Game(
                id = "nunchigame",
                name = "눈치 게임",
                description = "번호 겹치면 탈락!",
                icon = "👁️",
                categories = listOf("party", "reflex"),
                tags = listOf(GameTag.MULTI),
                route = "game/nunchigame"
            ),
            onClick = {}
        )
    }
}
```

- [ ] **Step 3: HomeScreen.kt 생성**

```kotlin
package com.poke86.game.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.poke86.game.R
import com.poke86.game.domain.model.Category
import com.poke86.game.domain.model.Game
import com.poke86.game.domain.model.GameTag
import com.poke86.game.ui.home.components.CategoryChips
import com.poke86.game.ui.home.components.GameCard
import com.poke86.game.ui.theme.GameVaultTheme

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val filteredGames by viewModel.filteredGames.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    HomeContent(
        categories = categories,
        filteredGames = filteredGames,
        selectedCategory = selectedCategory,
        onCategorySelected = viewModel::onCategorySelected,
        onGameClick = { navController.navigate(it.route) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    categories: List<Category>,
    filteredGames: List<Game>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onGameClick: (Game) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CategoryChips(
                categories = categories,
                selectedCategoryId = selectedCategory,
                onCategorySelected = onCategorySelected
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    GameCard(game = game, onClick = { onGameClick(game) })
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentPreview() {
    GameVaultTheme {
        HomeContent(
            categories = listOf(
                Category("all", "전체"), Category("party", "파티"),
                Category("solo", "혼자"), Category("reflex", "반응속도"),
                Category("brain", "두뇌")
            ),
            filteredGames = listOf(
                Game("nunchigame", "눈치 게임", "번호 겹치면 탈락!", "👁️",
                    listOf("party"), listOf(GameTag.MULTI), "game/nunchigame"),
                Game("reaction", "반응속도 대결", "화면 변화에 반응하라", "⚡",
                    listOf("party"), listOf(GameTag.MULTI, GameTag.QUICK), "game/reaction"),
                Game("memory", "숫자 기억", "단기 기억력 테스트", "🔢",
                    listOf("solo"), listOf(GameTag.SOLO, GameTag.BRAIN), "game/memory")
            ),
            selectedCategory = "all",
            onCategorySelected = {},
            onGameClick = {}
        )
    }
}
```

- [ ] **Step 4: 커밋**

```bash
git add .
git commit -m "feat: add theme, navigation, home screen with category filter"
```

---

## Chunk 8: 게임 Placeholder 화면 (8종)

각 화면은 동일한 구조. 아래 템플릿을 8개 게임에 적용한다.

### Task 11: 8개 게임 화면 생성

**Files:**
- Create: `app/src/main/java/com/poke86/game/ui/games/nunchigame/NunchiGameScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/reaction/ReactionScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/balance/BalanceScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/wordchain/WordChainScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/memory/MemoryScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/colortest/ColorTestScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/spy/SpyScreen.kt`
- Create: `app/src/main/java/com/poke86/game/ui/games/chosung/ChosungScreen.kt`

- [ ] **Step 1: NunchiGameScreen.kt**

```kotlin
package com.poke86.game.ui.games.nunchigame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.poke86.game.R
import com.poke86.game.ui.theme.GameVaultTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NunchiGameScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_nunchigame_name)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.coming_soon)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun NunchiGameScreenPreview() {
    GameVaultTheme { NunchiGameScreen(rememberNavController()) }
}
```

- [ ] **Step 2: ReactionScreen.kt** — 동일 구조, `game_reaction_name` 사용

- [ ] **Step 3: BalanceScreen.kt** — 동일 구조, `game_balance_name` 사용

- [ ] **Step 4: WordChainScreen.kt** — 동일 구조, `game_wordchain_name` 사용

- [ ] **Step 5: MemoryScreen.kt** — 동일 구조, `game_memory_name` 사용

- [ ] **Step 6: ColorTestScreen.kt** — 동일 구조, `game_colortest_name` 사용

- [ ] **Step 7: SpyScreen.kt** — 동일 구조, `game_spy_name` 사용

- [ ] **Step 8: ChosungScreen.kt** — 동일 구조, `game_chosung_name` 사용

> 각 화면의 패키지명, 클래스명, 문자열 리소스 키만 변경하고 나머지 구조는 동일하게 유지.

- [ ] **Step 9: 커밋**

```bash
git add .
git commit -m "feat: add placeholder screens for all 8 games"
```

---

## Chunk 9: 빌드 검증

### Task 12: Debug 빌드 및 전체 테스트

- [ ] **Step 1: 전체 단위 테스트 실행**

```bash
./gradlew :app:test
```
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Debug APK 빌드**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL
결과물: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: 빌드 성공 확인 후 최종 커밋**

```bash
git add .
git commit -m "chore: verify initial build passes"
```
