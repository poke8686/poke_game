# Nightfall: Kingdom Frontier TD — 원작 구조 분석 & 재구현 개발 문서

> **문서 목적**: `Nightfall/` 폴더의 원작 APK(벤치마크용)를 디스어셈블·정적 분석하여
> 게임의 **구조·시스템·디자인**을 파악하고, 우리 프로젝트(`nightfall`, Kotlin + Korge)에서
> **오리지널로 재구현**하기 위한 개발 참고 문서.
>
> ⚠️ **법적 범위 (CLAUDE.md 정책)**: 원작은 Unity(IL2CPP) 상용 게임이다.
> **코드/에셋(스프라이트·사운드·셰이더·데이터값)은 저작권상 이식 불가**.
> 본 문서는 *게임 메커니즘·시스템 구조·아키텍처 패턴* 만을 클린룸 참고용으로 정리한다.
> 원작에 박힌 Firebase API 키 / AdMob App ID / 인증서 해시 등은 **절대 재사용 금지**.

분석 일자: 2026-06-15 · 분석 산출물: `Nightfall/_analysis/`

---

## 1. 앱 식별 / 빌드 메타데이터

| 항목 | 값 |
|---|---|
| 앱 이름 | **Nightfall: Kingdom Frontier TD** |
| 패키지 | `com.fansipan.nightfall.tower.simulation.strategy.td.game` |
| 버전 | versionCode `1324` / versionName `1.1.324` |
| minSdk / targetSdk / compileSdk | **24 / 35 / 35** |
| 엔진 | **Unity (IL2CPP)** — arm64-v8a 전용 |
| 개발사 | Fansipan (iOS App Store ID `6621272416` 동시 존재 → 크로스플랫폼) |
| 백엔드 | Firebase(`ezg-nightfall-938f6`) + **Supabase** + Unity Gaming Services |
| 주요 권한 | INTERNET, VIBRATE, POST_NOTIFICATIONS, WAKE_LOCK, FOREGROUND_SERVICE(DATA_SYNC), ACCESS_ADSERVICES_* |

---

## 2. 배포 패키징 — Android App Bundle(split APK) 구조

원작은 `.aab`로 배포되어 기기에 **3개 split**으로 설치됨:

| split | 크기 | 내용 |
|---|---|---|
| `base.apk` | 94 MB | dex 코드(6개 멀티덱스 ~47MB) + 리소스 + **IL2CPP 메타데이터** + 메인 에셋번들 |
| `split_config.arm64_v8a.apk` | 40 MB | arm64-v8a 네이티브 `.so` (게임 로직 본체) |
| `split_UnityDataAssetPack.apk` | 190 MB | Unity 에셋팩 (게임 데이터/리소스 번들) |

### 2.1 네이티브 라이브러리 (`split_config.arm64_v8a/lib/arm64-v8a/`)

| .so | 크기 | 역할 |
|---|---|---|
| **`libil2cpp.so`** | **109 MB** | C#(게임 전체 로직)을 C++로 AOT 변환한 본체 |
| `libunity.so` | 19 MB | Unity 엔진 런타임 |
| `lib_burst_generated.so` | 235 KB | Unity Burst(고성능 잡 시스템) 컴파일 코드 |
| `libFirebaseCpp*.so` | — | App / Auth / Firestore / Messaging / RemoteConfig / Storage / Crashlytics |
| `libcrashlytics*.so`, `libapplovin-native-crash-reporter.so` | — | 크래시 리포팅 |

> arm64 단일 ABI. 32비트 미지원. 게임 로직은 `libil2cpp.so` + `global-metadata.dat`에 분리 저장.

### 2.2 IL2CPP 자산 (`base.apk · assets/bin/Data/`)

```
Managed/Metadata/global-metadata.dat   18.7 MB  ← C# 타입/메서드/필드 심볼 테이블
data.unity3d                           35.8 MB  ← 메인 에셋 번들(UnityFS, 압축)
unity default resources                 1.1 MB
Managed/Resources/*.dll-resources.dat            ← mscorlib, Newtonsoft.Json 등 리소스
ScriptingAssemblies.json                         ← 로드되는 전체 C# 어셈블리 목록
RuntimeInitializeOnLoads.json / boot.config      ← 부트스트랩 설정
```

### 2.3 에셋팩 (`split_UnityDataAssetPack · assets/bin/Data/`)

```
datapack.unity3d        181 MB  ← 게임 본 에셋(스프라이트/스파인/사운드/씬), UnityFS 압축
resources.resource        8.6 MB
sharedassets2.resource   13 KB
google-services-desktop.json     ← 원작 Firebase 설정(키 포함 — 재사용 금지)
UnityServicesProjectConfiguration.json
```

---

## 3. 원작 기술 스택 (`ScriptingAssemblies.json` 기반) → 우리 대체재

`Assembly-CSharp.dll`(게임 로직) + `Assembly-CSharp-firstpass.dll`(플러그인)이 핵심. 미들웨어:

| 영역 | 원작 사용 | 역할 | **우리(Korge) 대체** |
|---|---|---|---|
| 경로탐색 | **A* Pathfinding Project** (`AstarPathfindingProject`, Poly2Tri, ClipperLib) | 적 내비게이션 | 레인 웨이포인트 / 간단 그리드 A* |
| 2D 애니메이션 | **Spine** (`spine-unity`) | 캐릭터/적 스켈레탈 애니 | `korge-spine` 또는 스프라이트시트 |
| 트위닝 | **DOTween / DOTweenPro** (DemiLib) | UI·연출 보간 | Korge `tween` / `animate` |
| 비동기 | **UniTask** (+Linq/DOTween/Addressables) | async/await | Kotlin coroutines |
| 직렬화/데이터 | **Odin** (`Sirenix.Serialization/OdinInspector`) + Newtonsoft.Json | 데이터 드리븐 Config | `kotlinx.serialization` + JSON 에셋 |
| UI | TextMeshPro, **Coffee.UIEffect/UIParticle**, SoftMask, **EnhancedScroller / Super.ScrollerView**, **UnityScreenNavigator**, UIExtensions, Beardy GridLayout | 텍스트/이펙트/스크롤/화면전환 | Korge `Text`+비트맵폰트, View 트리, 커스텀 스크롤 |
| 입력 | **LeanTouch** (+LeanCommon/LeanPool) | 터치/제스처/풀링 | Korge `onClick/onDown` + 오브젝트 풀 |
| 카메라/연출 | Cinemachine, Unity.Timeline | 카메라/컷신 | Korge `Camera`/스크립트 연출 |
| 수학/성능 | Unity.Mathematics, **Unity.Burst**, Unity.Collections | SIMD/잡 | 순수 Kotlin (필요시 최적화) |
| 비주얼 스크립팅 | Unity.VisualScripting.* | 일부 로직 | — (코드 직접) |
| 안티치트 | **ACTk** (Anti-Cheat Toolkit) | 메모리값 난독/탐지 | (선택) 저장값 검증 |
| **백엔드** | **Supabase** (Gotrue/Postgrest/Realtime/Storage/Functions) + Websocket | 계정/랭킹/길드 실시간 | (선택) Supabase Kotlin SDK |
| 백엔드2 | **Firebase** (Auth/Firestore/Messaging/RemoteConfig/Crashlytics/Storage) | 인증/원격설정/푸시 | Firebase Android SDK |
| 라이브옵스 | Firebase RemoteConfig | A/B·밸런스 원격 조정 | RemoteConfig 또는 자체 서버 |
| 결제 | **Unity Purchasing** (IAP) | 인앱결제 | Google Play Billing |
| 광고 | **AppLovin MAX**(`MaxSdk`) + GoogleMobileAds + Facebook Audience + AppsFlyer | 미디에이션/어트리뷰션 | (선택) Google Mobile Ads |
| 디버그 | IngameDebugConsole, **Tayx.Graphy**(FPS) | 인게임 콘솔/성능 | Korge stats 오버레이 |
| 기타 | System.Reactive(Rx), JWTDecoder | 이벤트 스트림/토큰 | Kotlin Flow |

> **요약**: 원작은 "데이터 드리븐(Odin Config) + UniTask 비동기 + Spine 연출 + A* 경로 + Supabase/Firebase 라이브옵스 + 광고 미디에이션"의 풀 F2P 라이브옵스 TD.

---

## 4. 씬 / 앱 흐름

에셋번들에서 확인된 프로젝트 루트: **`Assets/_Project/`**. 부팅 흐름:

```
Splash (Assets/_Project/Scenes/Splash)
  └─> Home / Lobby        ← BattleHome, 로비 메뉴(상점/영웅/어드벤처/길드/패스)
        └─> Battle        ← BattleSceneInit → BattleManager 구동(실 전투)
              └─> BattleResult  ← 승패/보상 정산
```

핵심 전투 클래스: `BattleManager` (+`BattleManagerCustom` / `BattleManagerExtension`),
`BattleComponent`, `BattleDelivery`, `BattleType`, `BattleSelected`, `BattleNodePassed`, `BattleResult`.

---

## 5. 게임 시스템 아키텍처 (IL2CPP 메타데이터에서 재구성)

> 클래스명은 `global-metadata.dat` 심볼에서 추출(`_analysis/cat_*.txt`). 데이터값은 미포함.

### 5.1 데이터 드리븐 설계 — `Config` + `Collection` 패턴 (핵심)

모든 콘텐츠가 **`XxxConfig`(단일 정의) + `XxxCollection`(목록/DB)** 으로 정의됨 (Odin 직렬화).
이 패턴을 우리는 **`@Serializable data class` + JSON 에셋**으로 재현한다.

확인된 Config/Collection 예 (`cat_config_data.txt`, 910개):
```
AdventureConfig / AdventureModeCollection / AdventureLevelStageConfig+Collection
AdventureNodeTemplateCollection      ← 노드 기반 어드벤처 맵
AdventurePowerStageConfig            ← 전투력 게이트 스테이지
AdventureHeroPackConfig / AdventurePackConfig / AdventurePassPackConfig(배틀패스)
AdventureChestRewardCollection / AdventureStageRewardCollection
AchievementCollection / AchievementQuestCollection / AllQuestConfig / AllShopConfig
ActiveStatConfig / AllyStatConfig+Collection / AppConfig
BossAbilityConfig / BossAbilityPhaseConfig / BossAbilitySubConfig
```

### 5.2 핵심 매니저 (싱글톤/서비스)

`cat`에서 확인된 게임 고유 매니저:
```
BattleManager            전투 전체 오케스트레이션
DataManager              세이브/유저데이터 허브
CoinsManager             재화
EffectManager / EffectBaseManager / EffectToolTipManager   VFX·툴팁
EnemyIndicatorManager(+ViewManager)   화면 밖 적 방향 표시
EventGamePlayManager     인게임 이벤트
AdsManager / AdsPolicyManager         광고 + 노출 정책
AuthenticationManager / FirestoreManager   계정/클라우드 세이브
BackKeyManager           안드로이드 뒤로가기 라우팅
CheatManager             개발/QA 치트
EventManager / CoroutineManager / DownloadManager(에셋팩)
```

### 5.3 전투 코어 루프 (낮→밤→아침)

우리 재구현과 동일 컨셉 (CLAUDE.md): **낮(건설/업그레이드) → 밤(웨이브 방어) → 아침(수확)**.
원작 측 단서: `BuildBuildingSet`, `BuildAreas`, `BuildAll`, `AddWave`/`AddWaveWithNoEvent`,
`AddCoinWinWave`, `AddCoinFromEnemy`, `BattleNodePassed`(노드 점령), 인컴 건물(`BuildingIncomeView`).

### 5.4 타워 / 부대 / 건물

```
타워:   ArcheryTower, ArcaneTowers, Ballista, BezierAttackTower, BezierChurchTower
        (BezierAttackTower → 투사체가 베지어 곡선 궤적)
부대:   Barrack(s) → Troop 스폰. 근접/원거리 분리(BuffTroopMelee/Range),
        연구로 강화(BuffTroopMeleeResearch), 스폰 버프(BuffTroopSpawn)
건물:   Tower/Prayer/Income 빌딩, 잠금·해제 티어(AddLockBuilding / AddUnlockBuildingLevel),
        AbyssGate(특수 게이트), 성벽(AddWallInBattle)
버프:   BlessCurseBuffForTroopAndHeroAndBuilding (축복/저주 글로벌 버프)
```

### 5.5 영웅 / 액티브 스킬 / 어빌리티 (데이터 ID)

```
어빌리티: Ability10001 ~ Ability10022, Ability17003 … (숫자 ID로 데이터화)
          AbilityMechanicFactory / AbilityMechanicType / AbilityMechanicDelivery
          → "메커닉 팩토리"로 ID→로직 매핑 (data-driven ability system)
액티브 스킬: ActiveSkill, ActiveSkillDrag / ActiveSkillHoldDrag(드래그 조준),
            ActiveSkillDuration / Finish / Fail (지속/종료/실패)
영웅:     AdventureHeroPack(가챠/획득), 영웅 스탯(AllyStatConfig)
```

### 5.6 보스 / 길드 레이드 (라이브옵스 핵심)

```
BossAbilityBase / BossAbilityLogic / BossAbilityFactory / BossAbilityType
BossAbilityPhaseConfig + BossChangePhase   ← 페이즈 전환형 보스
BossGuild (길드 보스 레이드) + BossGuildBattleManager
BossGuild100005Ability/Animator/Helper     ← 데이터 ID(100005)별 보스 정의
BonusBuyBossTicket / ActivityPointPerBossFight   ← 입장 티켓·기여도
BiggestDailyBossDamage                     ← 일일 최고딜 랭킹
```

### 5.7 메타 진행 (어드벤처 맵 / 퀘스트 / 패스 / 가챠 / 시즌)

```
어드벤처:  노드형 맵(AdventureNodeTemplate) + 스테이지(레벨/전투력 게이트) + 챕터 보상
퀘스트/업적: AchievementQuest, AddQuestProgress, AddQuestEnemyDieCache,
            장비/카드 희귀도 달성 퀘스트(AllEquipmentReachRarityQuestProgress)
배틀패스:  BattlePass / BattlePassPack / BattlePassQuestPack / AddPassNode
가챠:      AddGachaCount/Exp, AddGachaWatchAdsNormal/PremiumCount (광고 무료뽑기)
장비/카드: 카드·장비 + 희귀도(rarity) 등급 시스템
시즌이벤트: BreakEggEventPetView(펫 알깨기), BreakPotMidAutumnView(추석 항아리 이벤트)
경제연출:  AddCoinWithMove(코인 흡입 애니), GoldRush/X2Reward(리워드광고 부스트), 일일보상
```

---

## 6. 데이터 모델 (엔티티 요약)

> **검증된 실제 필드/enum은 §10.3 참조** (IL2CPP 메타데이터에서 정밀 추출).
원작은 위 Config 들이 런타임 엔티티를 생성. 재구현 시 우리가 정의할 핵심 엔티티:

| 엔티티 | 핵심 필드(설계) |
|---|---|
| `Enemy` | id, hp, speed, atk, atkInterval, reward, lane, spineSkin, abilities[] |
| `Tower` | id, type(Archery/Arcane/Ballista/Bezier), range, dmg, fireRate, projType, level, tier |
| `Troop`/`Soldier` | id, hp, atk, speed, melee/range, spawnCount, researchBuffs |
| `Building` | id, type(Tower/Income/Prayer/Wall/Gate), nodeId, level(1~8), unlockTier, cost |
| `Wave` | index, spawns[(enemyId,count,delay,lane)], isBoss, reward |
| `Hero` | id, stats, activeSkillId, abilities[], rarity |
| `Ability` | id(숫자), mechanicType, params, targeting, vfxId |
| `Boss` | id, phases[(hp%, abilities[])], guildRaid?, ticketCost |
| `StageConfig` | id, biome, waves[], rewardId, powerGate |
| `Reward` | type(coin/gem/hero/card/chest), amount, chance |

---

## 7. 현재 레포 Korge 재구현 범위 — 갭 분석

현재 `app/.../game/nightfall/NightfallWorld.kt` (714줄) = **코어 전투 루프만** 단순 구현.
좌표 720×1280 고정, 중앙 성(R=86) + 메인게이트 + 좌/우 사이드게이트 + 3레인.

| 시스템 | 원작 | 현재 레포 | 갭 / 다음 작업 |
|---|---|---|---|
| 낮/밤/아침 루프 | ✅ | ✅ (`Phase` enum) | — |
| 중앙 성 + 게이트 + 성벽 | ✅ | ✅ (성/게이트/방벽) | 다방향 게이트 일부 |
| 노드 기반 건물 | ✅ 잠금 티어 | ✅ 화살탑/막사/집 (Lv1~8) | 인컴/기도 건물, 해제 티어 |
| 적 웨이브 | ✅ 데이터 | ✅ 5웨이브마다 보스 | 적 종류·어빌리티 다양화 |
| 막사 병사 자동전투 | ✅ Troop+연구 | ✅ 기본 | 근접/원거리 분리, 연구 버프 |
| 투사체 | ✅ 베지어 | ✅ 직선 | 베지어 궤적, 타워 타입 |
| 파티클 VFX | ✅ UIParticle | ✅ 기본 | 풍부한 연출 |
| 바이옴 | ✅ | ✅ 초원→설원→용암 | — |
| 저장/복원 | ✅ Firestore | ✅ SharedPreferences | 클라우드(선택) |
| **영웅/액티브스킬** | ✅ 가챠+드래그조준 | ❌ | **신규** |
| **어빌리티 시스템** | ✅ ID+팩토리 | ❌ | **신규(데이터 드리븐)** |
| **보스 페이즈/길드레이드** | ✅ | ❌(단순 보스만) | 페이즈 보스 |
| **어드벤처 맵/스테이지** | ✅ 노드맵 | ❌ | 메타 진행 |
| **퀘스트/업적/배틀패스** | ✅ | ❌ | 라이브옵스 |
| **가챠/상점/장비/카드** | ✅ | ❌ | 수익화·수집 |
| **시즌 이벤트** | ✅ | ❌ | 운영 |
| 효과음(오디오) | ✅ | ❌(미구현) | AudioTrack |
| 광고/IAP/백엔드 | ✅ 풀스택 | ❌ | (수익화 단계에서) |

---

## 8. 재구현 로드맵 (권장 순서)

원작은 수십 명 규모 라이브옵스 게임 → 전부가 아닌 **코어 재미 → 메타 진행** 순으로.

1. **코어 전투 폴리시** (현재 기반 위) — 타워 타입(베지어/아케인/발리스타), 적 종류·어빌리티, 효과음.
2. **데이터 드리븐 전환** — `@Serializable` Config + `assets/.../*.json` 로 적/타워/웨이브/스테이지 분리. (원작 Config+Collection 패턴 미러링)
3. **영웅 + 액티브 스킬** — 드래그 조준 액티브, 어빌리티 `MechanicFactory`(id→람다) 패턴.
4. **메타 진행** — 어드벤처 노드맵 + 스테이지 게이트 + 스테이지 보상.
5. **보스 페이즈** — hp% 기반 페이즈 전환 + 어빌리티 세트.
6. **수집/성장** — 영웅 가챠, 장비/카드 희귀도, 연구 버프.
7. **라이브옵스(선택)** — 퀘스트/업적/배틀패스/일일보상.
8. **수익화(선택)** — 광고(리워드 X2/무료뽑기), IAP, 클라우드 세이브.

각 단계는 `domain/model` 추가 → `GameRepositoryImpl` 등록 → `game/nightfall/` 로직 → `NightfallScreen` 연결 (CLAUDE.md 신규게임 규칙 준용).

---

## 9. 부록

### 9.1 분석 산출물 (`Nightfall/_analysis/`)

| 파일 | 내용 |
|---|---|
| **`parse_metadata.py`** | IL2CPP v31 메타데이터 파서 (의존성 0) |
| **`dump_game_classes.txt`** | **Assembly-CSharp 8,433개 클래스 + 필드 + 메서드** 정밀 덤프 ⭐ |
| `game_namespaces.txt` | 게임 네임스페이스별 클래스 수 |
| **`extract_assets.py` / `extract_names.py`** | UnityPy 에셋 인벤토리 추출기 |
| `asset_type_histogram.txt` | 에셋 오브젝트 타입별 개수 |
| `inventory_Sprite/AudioClip/AnimationClip/AnimatorController/Material/MonoScript.txt` | 에셋 이름 인벤토리 |
| `textassets/` | 추출된 TextAsset (TMP설정·IAP카탈로그 등 6개) |
| `global-metadata.dat` | 추출된 IL2CPP 심볼 (18.7MB) |
| `metadata_identifiers.txt` / `game_domain_candidates.txt` | 1차 문자열 추출(118,468 / 17,764개) |
| `cat_*.txt` | 카테고리별 클래스 후보 (enemy/tower_unit/structure/wave_stage/config_data/ui_screen/hero_skill/economy) |
| `ScriptingAssemblies.json` / `boot.config` / `builddatas.json` / `RuntimeInitializeOnLoads.json` | 원작 어셈블리 목록 · 부팅 설정 |

### 9.2 더 깊은 추출이 필요할 때 (도구)

본 분석은 **문자열 정적 추출** 기반. 정밀 구조가 더 필요하면:

| 목적 | 도구 | 방법 |
|---|---|---|
| 정확한 C# 클래스/필드/메서드 시그니처 | **Il2CppDumper** | `libil2cpp.so` + `global-metadata.dat` → `dump.cs` 생성 |
| 스프라이트/스파인/프리팹/씬/오디오 추출 | **AssetStudio / AssetRipper / UnityPy** | `data.unity3d`·`datapack.unity3d`(UnityFS) 디코드 |
| 매니페스트/리소스 디코드 | **apktool** | `apktool d base.apk` |

> ⚠️ 위 도구로 추출한 **에셋/코드 자체는 사용 금지**(저작권). 수치 밸런스·연출 타이밍의 *감각*만 참고하고 우리 오리지널 에셋/코드로 재현한다.

### 9.3 주의 — 원작에 박힌 민감정보 (재사용 금지)

`google-services-desktop.json` 에 원작사 Firebase 프로젝트(`ezg-nightfall-938f6`), API 키,
AdMob App ID, OAuth client id, 인증서 해시가 포함됨. **우리 빌드에 절대 복사 금지** —
우리 프로젝트는 자체 Firebase/광고 계정으로 신규 발급.

---

## 10. 정밀 추출 결과 (IL2CPP 메타데이터 v31 + UnityPy)

> 1차(§5)는 문자열 추출 기반. 본 절은 **`global-metadata.dat`(v31) 구조체 파싱**(`parse_metadata.py`,
> 모든 구조체 검증 통과 `name_ok=200/200`, `size%==0`)과 **UnityPy 에셋 디코드**로 얻은 검증된 정밀 데이터.

### 10.1 어셈블리 / 네임스페이스 맵 (검증)

- **총 161개 어셈블리**, 게임 코드 = `Assembly-CSharp.dll` **8,327 타입** + `Assembly-CSharp-firstpass.dll` 106 타입.
- 메인 네임스페이스 **`Game.Runtime`**(683). 대부분은 `<global>`(7,530, 네임스페이스 없음 — 인디 Unity 전형).
- 식별된 내부 프레임워크/스튜디오 코드:
  - **`Assets.Scripts._4.CORE.*`** — 자체 코어 프레임워크 (`Frameworks`, **`Stats.StatCollections`**, `Extensions`)
  - **`Zitga.CsvTools`** — **CSV 기반 데이터 파이프라인** (Zitga = 모바일 게임 퍼블리셔)
  - **`EZG` / `Ezg.Localization(.Tutorials)`** — 스튜디오 코드 (Firebase `ezg-nightfall`와 일치)
  - **`BlackFace.Libraries.Modules.*`** — `Pooling`, `InstanceFactory`, `Network`
  - **`_Project.Features.*`** — `MiniMap`, `Events.Halloween` / `Christmas` / `EasterEgg` / `MidAutumn` (시즌 이벤트)

### 10.2 데이터 아키텍처 (확정)

| 계층 | 방식 |
|---|---|
| 정적 밸런스 | **CSV**(`Zitga.CsvTools`) → 빌드 시 **ScriptableObject(MonoBehaviour)** 로 베이크. `BuildingStatCsv{statType,type,isLocalize,statValue}` 가 CSV 행 스키마 |
| 런타임 SO | 에셋번들에 **MonoBehaviour 295,816개** (ScriptableObject 설정 + UI 컴포넌트) |
| 스탯 시스템 | **RPG 스탯 모디파이어** — `_4.CORE.Stats.StatCollections`, `BuildingResearchRpgStatModifierQueue` (연구로 스탯 가산/곱연산) |
| 라이브 밸런스/IAP | **백엔드 주도** — `IAPProductCatalog` 가 비어있음(런타임/대시보드 구성), Firebase RemoteConfig + Supabase |

### 10.3 검증된 핵심 enum / 데이터 스키마 (메타데이터 필드)

```csharp
enum Building { Castle, Barracks, Income, Tower, Wall }          // 건물 5종
enum Enemy    { Melee, Ranged, Fly, Slayer, Tanker,             // 적 아키타입
                BaseDestroyed, BuildingDestroyed }              // + 상태 플래그
enum Ally     { Melee, Ranged, Hero }
enum Layer    { Hero, Building, Enemy, Ally, Fly,               // 물리 레이어
                TriggerAllUnit, HeroDetect, BuildingIgnoreHero }

class EnemyData {                  // 웨이브 스폰 단위 (실제 필드)
    enemyId; enemyLevel; spawnNumber; spawnLocation;
    spawnDelay; indicatorTime; isIgnoreDamage;
}
class BuildingStatCsv     { statType; type; isLocalize; statValue; }       // CSV 행
class BuildingStatConfig  { level; isLockLevel; haveChooseType;            // 레벨별 스탯
                            isNewTrait; straitInfo; }                       // 레벨업시 특성(trait) 선택
class BuildingStatContainer    { setId; subContainers; }
class BuildingStatSubContainer { idBuilding; nameBuilding; configs; }
class GameConstant {               // 전역 상수
    LEVEL_UPGRADE; TIME_ENEMY_ROTATE; PREPARE_TIME_INIT;
    TIME_INTERVAL_SAVE_TIME; UNITASK_TIME;
    DAILY_QUEST_MAX; WEEKLY_QUEST_MAX; RealX2; RealX3; ...
}
```
> 전체 8,433 클래스의 필드/메서드는 `_analysis/dump_game_classes.txt` 참조.
> (덤프의 `: Parent` 상속 표기는 일부 부정확 — parentIndex가 바이너리 타입테이블 인덱스라 미해석. **클래스/필드/메서드 이름은 검증됨**.)

### 10.4 에셋 구성 (UnityPy)

- `data.unity3d` **10,853 오브젝트** + `datapack.unity3d` **920,550 오브젝트**.

| 타입 | 개수 | 타입 | 개수 |
|---|---|---|---|
| MonoBehaviour | 295,816 | Sprite | 15,609 |
| GameObject | 207,784 | Texture2D | 3,383 |
| RectTransform | 159,606 | ParticleSystem | 9,361 |
| CanvasRenderer | 136,628 | AnimationClip | 1,003 |
| Transform | 48,178 | AudioClip | 90 |
| MonoScript | 6,541 | Shader | 106 |

- **고유 이름 인벤토리**: Sprite 13,204 · MonoScript 6,467 · AnimationClip 408 · AnimatorController 268 · Material 241 · AudioClip 90.
- **자산 명명 = 숫자 ID 규칙**: `17003_skill_0001~0019`(어빌리티 프레임), `mount_43000~43002`(탈것), `bite_16001`/`hit_17001`(적/스킬 ID). → 콘텐츠가 ID 네임스페이스로 체계화됨(10000=어빌리티, 16000=적, 17000=스킬, 43000=탈것 대역으로 추정).

### 10.5 오디오 인벤토리 → 시스템 역설계 (90개)

- **전투 루프 BGM**: `battle_day`, `battle_night` (낮/밤 음악 전환)
- **보스 = 4~5 페이즈**: `boss_bgm_prefight`, `boss_init_phase_1`, `boss_phase_change_2/3/4_5`, `boss_skill_1`~`boss_skill_7` (페이즈별 BGM + 7종 스킬 SFX)
- **가챠 연출 시퀀스**: `fx_before_gacha_rare_0/1` → `fx_after_gacha_rare_*` → `fx_reveal_rare_*` → `gacha_unique`/`gacha_grade_s`
- **전투 SFX**: `arrow_shoot`, `crossbow_shoot`, `flail_swing`, `BuildingImpact_Final`, `building_destroyed`
- **경제**: `Coin_Generate`, `Coin_Upgrade_Final`, `firstPurchase_coin_drop/toss`
- **시즌 이벤트 BGM**: `event_halloween_bgm`, `event_mid_autumn_bgm`, `etfx_easter_egg_bgm`, `bgm_birthday`

### 10.6 한계 & 추가 정밀화 도구

| 미해결 | 이유 | 해결 도구 |
|---|---|---|
| MonoBehaviour(ScriptableObject) **필드 값** | IL2CPP 릴리즈 빌드는 typetree 미포함 → UnityPy 역직렬화 불가 | **Il2CppInspector**(typetree 생성) 또는 **Il2CppDumper** — 둘 다 .NET 런타임 필요(현 환경 미설치) |
| 필드/리턴 **타입** | `Il2CppType` 테이블이 `libil2cpp.so`에 있음 | Il2CppDumper (so + metadata 동시 파싱) |
| 컨테이너 경로(asset path) | 번들에 container 매니페스트 부재 | (자산 경로는 SO 참조로만 추적 가능) |

> 정확한 수치 밸런스가 꼭 필요하면 `.NET 6 런타임` 설치 후 **Il2CppDumper**(→`dump.cs`, 완전한 타입 포함) +
> **Il2CppInspector**(→ typetree → UnityPy로 SO 값 추출) 순으로 진행. 단, **추출된 값/에셋 자체는 사용 금지**(저작권),
> 스키마와 *밸런스 감각*만 오리지널 재현에 참고.
