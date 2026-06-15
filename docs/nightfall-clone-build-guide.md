# Frontier Defense (가칭) — Unity 풀 라이브옵스 TD 재구현 빌드 가이드

> **목적**: `Nightfall: Kingdom Frontier TD`(원작)의 **구조·시스템·게임 디자인 패턴**을 참고하여,
> **당신의 Unity(C#) 앱**에 **오리지널 에셋/코드/수치**로 동급 TD 라이브옵스 게임을 만드는 실전 가이드.
>
> ⚠️ **클린룸 원칙**: 게임 메커니즘·장르 구조·데이터 아키텍처는 자유롭게 참고 가능(저작권 비대상).
> **금지**: 원작의 스프라이트/사운드/셰이더, 원작 클래스/필드 *값*, 정확한 밸런스 수치, 아트 디자인 복제.
> 본 문서의 캐릭터/스탯/이름은 전부 **새로 만든 더미**다. 원작 분석은 `nightfall-original-structure.md` 참조.

대상: Unity 6 / 2022 LTS+ · 언어 C# · 범위 **풀 라이브옵스** · 더미 아트(이모지+스프라이트 스펙 둘 다)

---

## 0. 한눈에 보는 게임

탑다운 거점 디펜스. **낮(건설/업그레이드) → 밤(웨이브 방어) → 아침(수확)** 루프.
중앙 성을 지키며, 노드에 타워/막사/농가를 짓고, 영웅의 액티브 스킬로 위기를 넘긴다.
메타: 어드벤처 스테이지 진행 · 영웅 가챠/성장 · 장비/카드 · 배틀패스 · 시즌 이벤트 · 보스 길드 레이드.

핵심 재미 = **빌드 결정(무엇을 어디에) + 실시간 스킬 개입 + 장기 성장**.

---

## 1. Unity 프로젝트 구조

원작은 `Assets/_Project/` 루트 + `Assembly-CSharp` 단일 + 자체 `_4.CORE` 프레임워크 + CSV→ScriptableObject.
이를 **asmdef로 모듈 분리**한 깔끔한 버전으로 재현한다.

```
Assets/_Project/
├── Art/            (Sprites, Spine/SkeletalData, VFX, Fonts, Materials)
├── Audio/          (BGM, SFX)
├── Data/           (ScriptableObject 에셋 + 원본 CSV)
│   ├── CSV/        (buildings.csv, enemies.csv, waves.csv, heroes.csv, ...)
│   └── SO/         (생성된 .asset)
├── Prefabs/        (Towers, Enemies, Heroes, Projectiles, VFX, UI)
├── Scenes/         (Boot, Home, Battle)
├── Scripts/
│   ├── Core/           → asmdef: Frontier.Core      (프레임워크: DI, 이벤트, 풀, 스탯)
│   ├── Data/           → asmdef: Frontier.Data      (SO 정의 + CSV 임포터)
│   ├── Gameplay/       → asmdef: Frontier.Gameplay  (전투: 타워/적/영웅/웨이브)
│   ├── Meta/           → asmdef: Frontier.Meta      (가챠/패스/퀘스트/상점/이벤트)
│   ├── Services/       → asmdef: Frontier.Services  (Save/Economy/Ads/IAP/Backend)
│   └── UI/             → asmdef: Frontier.UI         (화면/팝업/HUD)
└── Settings/       (URP, Input, AddressableAssetsData)
```

**권장 패키지/에셋** (원작 미들웨어의 오리지널 대체):

| 용도 | 원작 | 권장 |
|---|---|---|
| 비동기 | UniTask | **UniTask** (MIT, 그대로 OK) |
| 트위닝 | DOTween | **DOTween** (무료) 또는 PrimeTween |
| 경로탐색 | A* Pathfinding Project | **Unity NavMesh(2D)** 또는 간단 웨이포인트/그리드 A* 자작 |
| 풀링 | LeanPool | **Unity `ObjectPool<T>`** (내장) |
| UI 화면전환 | UnityScreenNavigator | **UnityScreenNavigator**(MIT) 또는 자작 ScreenStack |
| 데이터 | Odin + CSV | **ScriptableObject + CSV 임포터**(자작, §4) |
| 입력 | LeanTouch | **Unity Input System** |
| 스켈레탈 | Spine | **Spine**(유료) 또는 스프라이트시트 애니 |
| DI | (자체) | **VContainer** 또는 간단 ServiceLocator(§3) |
| 광고/IAP/백엔드 | Max/Firebase/Supabase | (수익화 단계) GoogleMobileAds, Unity IAP, Firebase |

---

## 2. 아키텍처 — 데이터 드리븐 + 레이어드

원작 핵심 = **데이터(CSV/SO) ↔ 서비스(매니저) ↔ 뷰(MVP)** 3계층 + **이벤트 버스**.

```
            ┌──────────────── ScriptableObject / CSV (정적 데이터) ────────────────┐
            │  BuildingDef · EnemyDef · WaveDef · HeroDef · AbilityDef · RewardDef  │
            └──────────────────────────────┬───────────────────────────────────────┘
                                           │ 주입
   ┌──────────── Services (싱글톤/DI) ──────┴───────────────────────────┐
   │ Save · Economy · Build · Combat · Wave · Hero · Gacha · Quest ...  │
   └───────────────┬────────────────────────────────────┬──────────────┘
                   │ 이벤트(EventBus) / 상태(ReactiveProperty)
        ┌──────────┴──────────┐                 ┌────────┴─────────┐
        │  Gameplay (전투 씬)  │                 │   UI (MVP 뷰)    │
        │  BattleManager 오케  │                 │  Presenter→View  │
        └─────────────────────┘                 └──────────────────┘
```

**원칙**
- **단일 진실원천**: 모든 콘텐츠는 SO. 코드에 수치 하드코딩 금지(§4 CSV 파이프라인).
- **서비스는 로직, 뷰는 표시**: MonoBehaviour 뷰는 상태를 *구독*만, 변경은 서비스가.
- **이벤트 버스로 결합도↓**: `EventBus.Publish(new EnemyKilled(...))` → Economy/Quest/VFX가 각자 구독.
- **세이브는 직렬화 DTO**: 런타임 객체와 분리된 `SaveData` POCO를 JSON 저장(§7).

### 2.1 핵심 프레임워크 코드 (Frontier.Core)

```csharp
// 초경량 서비스 로케이터 (VContainer 쓰면 대체)
public static class Services {
    static readonly Dictionary<Type, object> _map = new();
    public static void Register<T>(T svc) => _map[typeof(T)] = svc;
    public static T Get<T>() => (T)_map[typeof(T)];
}

// 이벤트 버스
public static class EventBus {
    static readonly Dictionary<Type, Delegate> _h = new();
    public static void Subscribe<T>(Action<T> cb) =>
        _h[typeof(T)] = (_h.TryGetValue(typeof(T), out var d) ? (Action<T>)d : null) + cb;
    public static void Unsubscribe<T>(Action<T> cb) {
        if (_h.TryGetValue(typeof(T), out var d)) _h[typeof(T)] = (Action<T>)d - cb;
    }
    public static void Publish<T>(T e) {
        if (_h.TryGetValue(typeof(T), out var d)) ((Action<T>)d)?.Invoke(e);
    }
}

// RPG 스탯 시스템 (원작 _4.CORE.Stats 대응) — 가산/곱연산 모디파이어 누적
public enum StatType { Hp, Atk, AtkSpeed, Range, MoveSpeed, Armor, CritChance, GoldGain }
public sealed class Stat {
    public float Base; readonly List<float> _add = new(); readonly List<float> _mul = new();
    public Stat(float b){ Base=b; }
    public void AddFlat(float v)=>_add.Add(v);
    public void AddPercent(float p)=>_mul.Add(p);     // 0.15f = +15%
    public float Value { get { float v=Base; foreach(var a in _add)v+=a;
        float m=1; foreach(var p in _mul)m+=p; return v*m; } }
}
public sealed class StatSheet {
    readonly Dictionary<StatType,Stat> _s = new();
    public Stat this[StatType t] => _s.TryGetValue(t, out var s) ? s : (_s[t]=new Stat(0));
    public StatSheet Set(StatType t, float b){ _s[t]=new Stat(b); return this; }
}
```

---

## 3. 합성 루트 & 씬 흐름

```
Boot 씬   → 서비스 초기화(Save 로드, 데이터 로드, 백엔드 핸드셰이크) → Home 으로
Home 씬   → 로비: 어드벤처/영웅/상점/가챠/패스/이벤트/길드 메뉴. 출격 선택 → Battle
Battle 씬 → BattleManager 가 StageDef 로 전투 구동 → 결과 → 보상 정산 → Home 복귀
```

```csharp
// Boot.cs — 합성 루트(Composition Root)
public class Boot : MonoBehaviour {
    [SerializeField] GameDatabase db;     // 모든 SO를 묶은 루트 SO
    async void Start() {
        var save = new SaveService();           save.Load();
        Services.Register(save);
        Services.Register(new EconomyService(save));
        Services.Register(new HeroService(db, save));
        Services.Register(new GachaService(db, save));
        Services.Register(new QuestService(db, save));
        Services.Register(new ProgressService(db, save)); // 어드벤처 진행
        // Services.Register(new AdsService()); ... 수익화 단계
        await SceneManager.LoadSceneAsync("Home");
    }
}
```

---

## 4. 데이터 파이프라인 — CSV → ScriptableObject

원작과 동일 철학: **밸런스는 CSV로 관리, 빌드시 SO로 베이크**. 기획자가 엑셀로 수정 → 임포터 실행.

### 4.1 SO 정의 (Frontier.Data)

```csharp
[CreateAssetMenu(menuName="FD/Building")]
public class BuildingDef : ScriptableObject {
    public int id;                       // 1101 등
    public string nameKey;               // 로컬라이즈 키
    public BuildingKind kind;            // Tower/Barracks/Income/Castle/Wall/Gate
    public string placeholderEmoji;      // "🏹" (더미 아트)
    public Sprite icon;                  // 실제 아트 교체 슬롯
    public BuildingLevel[] levels;       // 레벨별 스탯(1~8)
}
[Serializable] public struct BuildingLevel {
    public int level; public int upgradeCost;
    public float hp, atk, atkSpeed, range; public int troopCount; // 막사용
    public bool unlockTrait;            // 레벨업시 특성 선택 분기
}
public enum BuildingKind { Castle, Wall, Gate, Tower, Barracks, Income, Shrine }

[CreateAssetMenu(menuName="FD/Enemy")]
public class EnemyDef : ScriptableObject {
    public int id; public string nameKey; public EnemyArchetype archetype;
    public string placeholderEmoji; public Color placeholderColor; public Sprite sprite;
    public float baseHp, atk, atkSpeed, moveSpeed, range; public int goldReward;
    public TargetPriority targeting;    // Nearest / Castle / Hero / Building
    public int[] abilityIds;            // 보스/특수 적 어빌리티
}
public enum EnemyArchetype { Melee, Ranged, Flyer, Tank, Slayer, Sapper, Boss }
public enum TargetPriority { Nearest, Castle, Hero, Building }

[CreateAssetMenu(menuName="FD/Stage")]
public class StageDef : ScriptableObject {
    public int id; public Biome biome; public int powerGate; // 전투력 게이트
    public WaveDef[] waves; public RewardDef firstClearReward;
}
[Serializable] public struct WaveDef {
    public int index; public bool isBoss; public EnemySpawn[] spawns;
}
[Serializable] public struct EnemySpawn {
    public int enemyId, level, count; public int laneIndex; public float delay, interval;
}
public enum Biome { Grassland, Snow, Lava, Abyss }
```

### 4.2 CSV 임포터 (Editor)

`enemies.csv`:
```
id,nameKey,archetype,emoji,color,hp,atk,atkSpeed,moveSpeed,range,gold,targeting
2001,enemy_grunt,Melee,🟢,#5BBF5B,80,8,1.0,55,12,6,Castle
2002,enemy_slinger,Ranged,🟡,#C9C25B,55,10,0.8,45,140,8,Nearest
```

```csharp
#if UNITY_EDITOR
public static class CsvImporter {
    [MenuItem("FD/Import CSV → SO")]
    public static void Import() {
        foreach (var row in ReadRows("Assets/_Project/Data/CSV/enemies.csv")) {
            var so = LoadOrCreate<EnemyDef>($"Assets/_Project/Data/SO/Enemy_{row["id"]}.asset");
            so.id = int.Parse(row["id"]);
            so.nameKey = row["nameKey"];
            so.archetype = Enum.Parse<EnemyArchetype>(row["archetype"]);
            so.placeholderEmoji = row["emoji"];
            ColorUtility.TryParseHtmlString(row["color"], out so.placeholderColor);
            so.baseHp = float.Parse(row["hp"]); /* ... 나머지 매핑 ... */
            EditorUtility.SetDirty(so);
        }
        AssetDatabase.SaveAssets();
    }
}
#endif
```

> 이렇게 하면 **밸런스 반복**이 엑셀 수정 → 메뉴 클릭 한 번. 라이브에서는 동일 CSV를
> 서버(Firebase Remote Config/Supabase)로 내려 **앱 업데이트 없이 밸런싱**도 가능.

---

## 5. 코어 전투 시스템 (Frontier.Gameplay)

### 5.1 전투 오케스트레이터

```csharp
public enum Phase { Day, Night, Morning, Result }   // 낮 건설 / 밤 방어 / 아침 수확
public class BattleManager : MonoBehaviour {
    public Phase Phase { get; private set; }
    [SerializeField] StageDef stage;
    int waveIndex;
    async UniTaskVoid Run() {
        foreach (var wave in stage.waves) {
            await DayPhase();                 // 건설/업그레이드, UI에서 "밤 시작" 누르면 진행
            await NightPhase(wave);           // 웨이브 스폰 + 방어, 전멸 또는 시간초과까지
            await MorningPhase();             // 골드 수확, 보상
            waveIndex++;
        }
        EndBattle(victory:true);
    }
}
```

### 5.2 유닛 공통 — 컴포넌트 합성

원작은 수십만 GameObject. **반드시 오브젝트 풀** + 데이터 지향으로. 권장 컴포넌트 분리:

- `Health` (hp, 데미지/사망 이벤트) · `Mover` (웨이포인트/NavMesh 추종) ·
  `Attacker` (range/rate/타겟선정 + 발사) · `UnitView` (스프라이트/스파인/이모지 표시) ·
  `Targetable` (팩션/레이어). 적·아군·영웅이 동일 컴포넌트 재사용.

```csharp
public class Attacker : MonoBehaviour {
    public StatSheet stats; public LayerMask targetMask;
    float _cd;
    void Update() {
        _cd -= Time.deltaTime;
        if (_cd > 0) return;
        var target = TargetFinder.Find(transform.position, stats[StatType.Range].Value, targetMask);
        if (target == null) return;
        Fire(target);                          // 투사체 풀에서 꺼내 발사
        _cd = 1f / stats[StatType.AtkSpeed].Value;
    }
}
```

### 5.3 타워 / 막사 / 건물

- **노드 배치**: 맵에 미리 정의된 `BuildNode`(빈 슬롯). 낮에 노드 클릭 → 빌드 메뉴 → 건물 선택.
- **타워**: `Attacker`로 투사체 발사(직선/포물선/유도). 레벨업으로 stat 상승 + 특정 레벨에 **특성 선택**(예: 관통 vs 스플래시).
- **막사**: 주기적으로 `Troop` 스폰(풀링). 병사는 게이트 앞 집결 → 적과 근접 교전. 근접/원거리 분리.
- **농가(Income)**: 아침마다 골드. **수확형 경제**(밤 생존 보상 + 인컴).
- **성/성벽/성문**: 성 HP 0 = 패배. 성벽/성문은 적 진로 차단(부수면 통과).

### 5.4 적 & 웨이브

- `WaveService`가 `WaveDef` 읽어 `EnemySpawn`을 레인별/지연별 스폰.
- 타겟팅: `TargetPriority`로 성/영웅/최근접 선택. Flyer는 성벽 무시.
- 5웨이브마다(또는 stage 지정) **보스**: 다중 페이즈(HP% 분기) + 어빌리티 세트(§6.3).

### 5.5 투사체 & VFX

- 투사체: 풀 필수. 직선/유도/포물선(베지어) 3종. 적중시 `Health.TakeDamage` + 히트 VFX.
- VFX: `ParticleSystem` 프리팹 풀. 코인 흡입 연출, 사망 파티클, 스킬 이펙트.

---

## 6. 영웅 · 어빌리티 · 보스 (라이브옵스 코어)

### 6.1 영웅 + 액티브 스킬 (드래그 조준)

```csharp
[CreateAssetMenu(menuName="FD/Hero")]
public class HeroDef : ScriptableObject {
    public int id; public string nameKey; public Rarity rarity; public Element element;
    public string placeholderEmoji; public Sprite portrait;
    public StatBlock baseStats; public int activeSkillId; public int[] passiveAbilityIds;
    public GrowthCurve growth;     // 레벨/별/장비로 성장
}
public enum Rarity { Common, Rare, Epic, Legendary, Mythic }
public enum Element { Fire, Water, Earth, Light, Dark }
```

- **액티브 스킬**: HUD 버튼 홀드 → 드래그로 위치/방향 조준 → 발동(쿨다운). 예: 메테오(지점 AoE), 화살비(라인), 방패강타(주변 스턴).
- **영웅 1기 출전**(또는 소수). 성장: 레벨업 + 별(중복 카드 → 승급) + 장비/카드.

### 6.2 어빌리티 = 데이터 + 팩토리 (원작 `AbilityMechanicFactory` 패턴)

숫자 ID로 어빌리티를 정의하고, **메커닉 팩토리**가 ID→실행 로직 매핑. 신규 스킬 = 데이터 추가 + 메커닉 1개.

```csharp
public enum MechanicType { Projectile, Aoe, Buff, Summon, Dash, Chain }
[CreateAssetMenu(menuName="FD/Ability")]
public class AbilityDef : ScriptableObject {
    public int id; public MechanicType mechanic; public float cooldown, value, radius, duration;
    public StatType buffStat; public GameObject vfxPrefab; public int targetingMode; // 0=point 1=dir
}
public static class AbilityFactory {
    public static IAbility Create(AbilityDef d) => d.mechanic switch {
        MechanicType.Aoe        => new AoeMechanic(d),
        MechanicType.Projectile => new ProjectileMechanic(d),
        MechanicType.Buff       => new BuffMechanic(d),
        MechanicType.Summon     => new SummonMechanic(d),
        _ => new NoopMechanic(d),
    };
}
```

### 6.3 보스 — 다중 페이즈

원작 오디오 분석상 **4~5 페이즈 + 스킬 7종**. HP 임계값마다 페이즈 전환(연출 + 어빌리티 세트 교체).

```csharp
[Serializable] public struct BossPhase { public float hpThresholdPercent; public int[] abilityIds; public float atkMultiplier; }
[CreateAssetMenu(menuName="FD/Boss")]
public class BossDef : ScriptableObject {
    public EnemyDef baseEnemy; public BossPhase[] phases; // 내림차순 임계값
    public bool isGuildRaid; public int ticketCost;       // 길드 레이드용
}
```

---

## 7. 메타 · 수익화 시스템 (풀 라이브옵스)

| 시스템 | 핵심 설계 | 데이터 |
|---|---|---|
| **어드벤처 맵** | 노드형 스테이지 그래프(직선/분기). `powerGate`로 전투력 게이트. 클리어 → 다음 해제 + first-clear 보상 | `StageDef`, `ProgressSave` |
| **영웅 가챠** | 등급별 확률 풀(천장 포함). 광고 무료뽑기/프리미엄. 중복 → 조각 | `GachaPoolDef`(아래) |
| **장비/카드** | 영웅 장착 슬롯, 희귀도 등급. 세트 효과 | `EquipDef`, `CardDef` |
| **배틀패스** | 시즌 트랙(무료/유료 레인), 노드별 보상, 퀘스트로 XP | `BattlePassDef` |
| **퀘스트/업적** | 일일/주간/누적. `EventBus` 구독으로 진행 자동 집계 | `QuestDef` |
| **상점** | 일일 무료/광고/유료 패키지, 골드 상점 | `ShopOfferDef` |
| **시즌 이벤트** | 기간제 미니게임/리스킨(할로윈/추석/부활절 등) | `EventDef` (start/end) |
| **보스 길드 레이드** | 티켓 입장, 기여도 랭킹, 일일 최고딜 | `BossDef.isGuildRaid` |

```csharp
[CreateAssetMenu(menuName="FD/GachaPool")]
public class GachaPoolDef : ScriptableObject {
    [Serializable] public struct Entry { public int heroId; public Rarity rarity; public float weight; }
    public Entry[] entries;
    public int pityCount;           // 천장: N회 내 Legendary+ 보장
    public RewardDef freeDailyPull;
}
// 가챠 뽑기 (가중치 + 천장)
public int Roll(GachaPoolDef pool, ref int sinceLego) {
    sinceLego++;
    bool forceHigh = sinceLego >= pool.pityCount;
    var pick = WeightedPick(pool.entries, forceHigh);
    if (pick.rarity >= Rarity.Legendary) sinceLego = 0;
    return pick.heroId;
}
```

### 7.1 세이브 (DTO + JSON)

```csharp
[Serializable] public class SaveData {
    public long gold, gems; public int highestStage; public int pityCounter;
    public List<HeroSave> heroes = new();
    public List<int> unlockedBuildings = new();
    public List<QuestEntry> questProgress = new();
    public string schemaVersion = "1.0";        // 마이그레이션용
}
public class SaveService {
    SaveData _d; string Path => Application.persistentDataPath + "/save.json";
    public void Load() => _d = File.Exists(Path)
        ? JsonUtility.FromJson<SaveData>(File.ReadAllText(Path)) : new SaveData();
    public void Save() => File.WriteAllText(Path, JsonUtility.ToJson(_d));
    // 클라우드: Firebase Firestore에 동일 JSON 백업(로그인시 머지)
}
```

### 7.2 수익화 (마지막 단계)

- **광고**(GoogleMobileAds/AppLovin MAX): 리워드(2배 보상/무료뽑기/부활), 전면(스테이지 사이), 배너.
- **IAP**(Unity IAP): 보석 팩, 배틀패스, 스타터 패키지, 광고 제거. SKU는 **스토어/서버에서 구성**(원작도 카탈로그 비움).
- **분석/리텐션**: Firebase Analytics + 푸시(FCM) + RemoteConfig A/B.

---

## 8. 더미 캐릭터 디자인 (오리지널)

> 즉시 구현용 **이모지/도형 플레이스홀더** + 추후 교체용 **스프라이트 스펙** 둘 다 제공.
> 이름/수치 전부 오리지널. ID 대역: 1000 건물 · 2000 적 · 3000 영웅 · 4000 어빌리티.
> 스탯은 *상대적 밸런스 예시*(1레벨 기준) — 실제는 §4 CSV로 튜닝.

### 8.1 플레이어 건물/타워 (1000번대)

| ID | 이름(가칭) | 종류 | 이모지/도형 | 스프라이트 스펙(추후 교체) | 핵심 스탯(Lv1) | 역할 |
|---|---|---|---|---|---|---|
| 1001 | 왕성 Keep | Castle | 🏰 / 회색 8각 | 3층 석조 성채, 깃발, 64×80 | HP 1000 | 파괴=패배 |
| 1002 | 성벽 Wall | Wall | 🟫 / 갈색 바 | 석조 블록 타일 32×16 | HP 200 | 진로 차단 |
| 1003 | 성문 Gate | Gate | 🚪 / 목재 | 철창 양문, 개폐 애니 48×40 | HP 350 | 병사 출입구 |
| 1101 | 궁수탑 Archer | Tower | 🏹 / 청록 원 | 2층 목조 터릿+궁수 실루엣 48×72 | DMG 14 · Rate 1.2 · Rng 150 | 단일 원거리 |
| 1102 | 포탑 Bombard | Tower | 💣 / 주황 원 | 석조 대포, 포구 연기 56×64 | DMG 30 · Rate 0.5 · Rng 120 · AoE 40 | 광역 |
| 1103 | 빙결탑 Frost | Tower | ❄️ / 하늘 원 | 수정 첨탑, 냉기 오라 48×72 | DMG 8 · Slow 40% · Rng 130 | 둔화 |
| 1104 | 비전탑 Arcane | Tower | 🔮 / 보라 원 | 룬 오벨리스크, 관통 빔 48×80 | DMG 18 · Pierce 3 · Rng 140 | 관통 |
| 1201 | 막사 Barracks | Barracks | ⚔️ / 적색 사각 | 천막+훈련목, 깃발 56×56 | 병사 3기 · HP 120 | 근접 부대 |
| 1202 | 사격장 Range | Barracks | 🎯 / 황색 사각 | 과녁+사대 56×56 | 병사 2기 · DMG 9 · Rng 110 | 원거리 부대 |
| 1301 | 농가 Farm | Income | 🌾 / 연두 사각 | 풍차+밭 56×56 | 아침마다 +25골드 | 경제 |
| 1302 | 사당 Shrine | Shrine | ✨ / 금색 사각 | 제단+촛불, 빛기둥 48×64 | 주변 아군 +10% ATK | 버프 |

**병사(막사 스폰 유닛, 1400번대)**
| ID | 이름 | 도형 | 스펙 | 스탯 |
|---|---|---|---|---|
| 1401 | 보병 Footman | 🔵 작은 원 | 방패+검 32×32 | HP 60 · DMG 8 · 근접 |
| 1402 | 석궁병 Marksman | 🟣 작은 원 | 석궁+망토 32×32 | HP 40 · DMG 9 · Rng 110 |

### 8.2 적 (2000번대)

| ID | 이름(가칭) | 아키타입 | 이모지/색 | 스프라이트 스펙 | 스탯(Lv1) | 행동 |
|---|---|---|---|---|---|---|
| 2001 | 고블린 졸개 Grunt | Melee | 🟢 녹색 원 | 몽둥이 든 소형 고블린 40×40 | HP 80·DMG 8·Spd 55 | 성으로 직진 |
| 2002 | 고블린 투척꾼 Slinger | Ranged | 🟡 황녹 원 | 슬링 든 고블린 40×40 | HP 55·DMG 10·Rng 140 | 사거리 유지 |
| 2003 | 박쥐떼 Bat | Flyer | 🦇 보라 마름모 | 날개 펄럭 군집 36×36 | HP 35·DMG 6·Spd 90 | 성벽 무시 비행 |
| 2004 | 오크 강철투사 Brute | Tank | 🟩 진녹 큰 원 | 대형 갑옷 오크, 도끼 56×56 | HP 320·DMG 18·Spd 40·Armor | 느림/고HP |
| 2005 | 망령 Wraith | Slayer | 👻 청백 유령 | 반투명 낫 든 망령 44×48 | HP 90·DMG 22·Spd 60 | **영웅 우선 타겟** |
| 2006 | 공병 Sapper | Sapper | 💥 적주황 | 폭탄 멘 고블린 40×40 | HP 70·DMG 40·Spd 50 | **건물 우선 자폭** |
| 2007 | 비행 정찰병 Harpy | Flyer | 🟪 자주 마름모 | 하피, 깃털 투척 44×44 | HP 60·DMG 9·Rng 90·Spd 80 | 비행+원거리 |

**보스 (2900번대) — 다중 페이즈**

| ID | 이름(가칭) | 이모지/도형 | 스펙 | 페이즈 설계 |
|---|---|---|---|---|
| 2901 | 트롤 군주 Warlord | 👹 적색 거대원 | 4m 트롤, 통나무 곤봉 96×96 | P1(100%) 일반 강타 → P2(70%) 광폭화+속도↑ → P3(40%) 바위 투척(AoE) → P4(15%) 분노 연타 |
| 2902 | 리치 Lich | 💀 청자 거대원 | 부유 해골마법사, 망토 96×96 | P1 해골소환 → P2(60%) 냉기 장판 → P3(30%) 광역 넉백+부활 |

스킬 SFX 슬롯: `boss_skill_1~7` 대응(원작 분석 기반) — 더미 7종 스킬 키만 정의해두고 점진 구현.

### 8.3 영웅 (3000번대, 가챠)

| ID | 이름(가칭) | 등급 | 속성 | 이모지/도형 | 스프라이트 스펙 | 액티브 스킬(4000) | 스탯(Lv1) |
|---|---|---|---|---|---|---|---|
| 3001 | 기사 롤란드 Roland | Common | Earth | 🛡️ / 은색 | 판금기사+대검 64×96 | 4001 방패강타(주변 스턴 1.5s) | HP 220·DMG 16 |
| 3002 | 대마법사 엘라라 Elara | Epic | Fire | 🔥 / 주황 | 로브 마녀+지팡이 64×96 | 4002 메테오(지점 AoE 80) | HP 150·DMG 26 |
| 3003 | 순찰자 리라 Lyra | Rare | Water | 🏹 / 청록 | 후드 궁수+장궁 64×96 | 4003 화살비(라인 관통) | HP 160·DMG 20 |
| 3004 | 광전사 브롬 Brom | Rare | Dark | 🪓 / 적갈 | 쌍도끼 광전사 64×96 | 4004 회전베기(근접 AoE) | HP 240·DMG 22 |
| 3005 | 발키리 세라핀 Seraphine | Legendary | Light | ⚡ / 백금 | 날개 창기사 64×96 | 4005 천상의 창(직선 대미지+감속) | HP 200·DMG 30 |

> 더미 단계: 영웅/적/타워 모두 **이모지 + 단색 도형**으로 즉시 플레이 가능. 동작·밸런스 검증 후
> 스프라이트 스펙대로 아트 교체(AI 생성/외주). 스펙은 64×96(영웅/타워), 40×40(적), 32×32(병사) 기준 통일.

### 8.4 어빌리티 더미 정의 (4000번대)

| ID | 이름 | MechanicType | value | radius | cooldown | 비고 |
|---|---|---|---|---|---|---|
| 4001 | 방패강타 | Aoe(+stun) | 0(스턴) | 60 | 12s | 주변 스턴 1.5s |
| 4002 | 메테오 | Aoe | 80 | 80 | 14s | 지점 조준 |
| 4003 | 화살비 | Projectile(line) | 20×5 | 라인폭30 | 10s | 방향 조준 |
| 4004 | 회전베기 | Aoe | 35 | 70 | 9s | 자기중심 |
| 4005 | 천상의 창 | Projectile+Buff | 60 | 라인 | 16s | 직선+감속 |

---

## 9. 구현 로드맵 (마일스톤)

풀 라이브옵스라도 **코어 재미부터 검증** 후 메타를 쌓는다. 각 M = 플레이 가능한 빌드.

- **M0 — 셋업(1주)**: 프로젝트/asmdef/씬 3개/`Services`·`EventBus`·`Stat` 코어. 더미 이모지 렌더.
- **M1 — 코어 전투(2~3주)**: 낮/밤/아침 루프, 노드 빌드, 궁수탑+막사, 적 2종, 웨이브, 성 HP, 패배/승리. **풀링 필수**.
- **M2 — 전투 확장(2주)**: 타워 4종+특성, 적 7종, 보스 1종(페이즈), 투사체 3종, VFX/SFX, 바이옴 3종.
- **M3 — 영웅/스킬(2주)**: 영웅 출전, 드래그 액티브 스킬, AbilityFactory, 영웅 성장(레벨/별).
- **M4 — 메타 진행(2주)**: 어드벤처 노드맵, 스테이지 게이트, 보상, CSV 파이프라인 정착, 세이브/클라우드.
- **M5 — 수집/경제(2주)**: 가챠(천장), 장비/카드, 상점, 일일보상, 골드/보석 이코노미.
- **M6 — 라이브옵스(2주)**: 퀘스트/업적, 배틀패스, 시즌 이벤트 1종, 보스 길드 레이드.
- **M7 — 수익화/출시(2주)**: 광고(리워드/전면), IAP, 분석/푸시/RemoteConfig, 튜토리얼, 스토어 빌드.

> 권장: **M1~M3 완성 = 게임성 검증 게이트**. 여기서 재미 없으면 메타 붙여도 안 됨. 더미 아트로 충분히 검증 가능.

---

## 10. 클린룸 체크리스트 (법적 안전)

- [ ] 원작 스프라이트/사운드/폰트/셰이더/Spine 데이터 **미사용** (전부 자체 제작/AI생성/라이선스 에셋).
- [ ] 원작 클래스/필드 *값*, 정확 밸런스 수치 **미복제** (본 가이드 더미값 또는 자체 튜닝).
- [ ] 원작 아트 스타일/캐릭터 디자인 **미모방** (장르 관습 수준은 OK, 특정 캐릭터 베끼기 금지).
- [ ] 원작 `google-services.json`/API 키/AdMob ID/패키지명 **미사용** (자체 신규 발급).
- [ ] 게임 *메커니즘/구조/장르 패턴*만 참고 (저작권 비대상, 본 가이드 범위).

---

### 참고
- 원작 정밀 분석: `docs/nightfall-original-structure.md`
- 클래스 구조 원본: `Nightfall/_analysis/dump_game_classes.txt`
- 본 가이드의 모든 캐릭터/수치는 **오리지널 더미**이며, 빌드하며 자유롭게 교체·튜닝한다.
