# Frontier Defense — Unity TD (코어 전투 스캐폴드)

원작 **Nightfall: Kingdom Frontier TD** 의 *구조·시스템 패턴*을 참고해 **오리지널 코드/콘텐츠**로 만든
풀 라이브옵스 TD의 **코어 전투(M0~M2) 스캐폴드**. 설계 근거는 레포 루트의
`docs/nightfall-original-structure.md`, `docs/nightfall-clone-build-guide.md` 참조.

> **클린룸 원칙**: 이 프로젝트의 코드/이름/수치는 전부 오리지널(또는 더미). 원작의 스프라이트·
> 사운드·셰이더·정확한 밸런스 수치·키/패키지명은 일절 사용하지 않는다.

---

## 현재 환경 주의 (중요)

이 폴더에는 **C# 스크립트 + 데이터(CSV) 만** 들어 있다. `.meta`·씬·프리팹·ProjectSettings 같은
**에디터 생성물은 포함돼 있지 않다.** 즉 Unity 에디터에서 아래 "셋업"을 거쳐야 실행된다.

요구: **Unity 6 (또는 2022 LTS+)**, 2D 또는 URP, TextMeshPro(UI 단계에서).

---

## 폴더 구조

```
Assets/_Project/
├── Scripts/
│   ├── Core/      Services, EventBus, Stat, ComponentPool   (FrontierDefense.Core)
│   ├── Data/      Enums, BuildingDef, EnemyDef, StageDef, GameDatabase (FrontierDefense.Data)
│   ├── Services/  SaveService, EconomyService               (FrontierDefense.Services)
│   ├── Gameplay/  BattleManager, Castle, Health, Targetable, Lane,
│   │              EnemyUnit, TroopUnit, Projectile, Building, BuildNode, Boot (FrontierDefense.Gameplay)
│   └── Editor/    CsvImporter (에디터 전용)                  (FrontierDefense.EditorTools)
└── Data/
    ├── CSV/       enemies.csv, buildings.csv (오리지널 더미 밸런스)
    └── SO/        (CSV 임포트로 생성됨 — 비어 있음)
```

> asmdef는 아직 없음(모두 Assembly-CSharp로 컴파일). `Editor/` 폴더 스크립트는 빌드에서 자동 제외.
> 모듈 분리는 추후 가이드 §1대로 asmdef 추가 가능.

---

## 에디터 셋업 (한 번)

1. **Unity 프로젝트 생성** (Unity Hub → New, 2D Core 또는 URP 2D). 생성된 프로젝트의 `Assets/` 에
   이 레포의 `Assets/_Project` 폴더를 복사. (루트 `.gitignore` 도 함께)
2. **데이터 베이크**: 상단 메뉴 **`FD ▸ Import CSV → SO`** 실행 → `Data/SO/` 에
   `Enemy_2001…`, `Building_1101…` SO 자동 생성.
3. **GameDatabase 생성**: Project 우클릭 → `Create ▸ FD ▸ Game Database`. 인스펙터에서
   `buildings`/`enemies` 리스트에 생성된 SO들을 드래그. (또는 폴더 멀티선택 드래그)
4. **StageDef 생성**: `Create ▸ FD ▸ Stage Def`. `waves[]` 에 웨이브 추가, 각 웨이브의 `spawns[]` 에
   `enemyId`(예: 2001), `count`, `laneIndex`, `delay`, `interval` 지정. 5번째 웨이브 등에 `isBoss`+2901.
   만든 StageDef를 GameDatabase.stages 에도 추가.
5. **Battle 씬 구성**:
   - **Castle**: 빈 GameObject + `Health` + `Castle` (중앙 배치).
   - **Lane(들)**: 빈 GameObject + `Lane`, 자식으로 웨이포인트(빈 Transform) 여러 개 만들어
     `waypoints[]` 에 성문 밖→성 입구 순서로 할당. (Gizmo 노란선으로 경로 확인)
   - **BuildNode**: 빈 GameObject + `BuildNode` 를 노드 위치마다 배치.
   - **BattleManager**: 빈 GameObject + `BattleManager`. 인스펙터에 `db`, `stage`, `castle`,
     `lanes[]`, `nodes[]`, 그리고 아래 프리팹 4종, `castleHp/startGold/...` 튜닝값 할당.
   - **Boot**(선택): 빈 GameObject + `Boot` (영구 서비스 초기화).
6. **프리팹 4종 제작** (스프라이트는 임시로 단색 Square 사용 가능):
   - **Enemy**: `Health` + `Targetable` + `EnemyUnit` + SpriteRenderer.
   - **Troop**: `Health` + `Targetable` + `TroopUnit` + SpriteRenderer.
   - **Projectile**: `Projectile` + SpriteRenderer(작은 화살/점).
   - **Building**: `Building` + SpriteRenderer. (한 프리팹으로 종류별 동작 분기)
   → BattleManager 의 enemyPrefab/troopPrefab/projectilePrefab/buildingPrefab 에 할당.
7. **플레이**: 낮 페이즈는 기본적으로 `StartNight()` 입력 대기다. 빠른 확인용으로 BattleManager 의
   `dayAutoStart` 를 3 정도로 두면 자동으로 밤이 시작된다. 건설 UI는 아직 없으니, 테스트용으로
   임시 버튼이나 키 입력으로 `BattleManager.TryBuild(node, def)` / `StartNight()` 를 호출.

---

## 구현 범위

**된 것 (M0~M2 코어)**
- 낮→밤→아침 루프(`BattleManager`, 코루틴), 성 HP 패배 조건
- 데이터 드리븐: CSV→SO 임포터, `GameDatabase` id 인덱스
- 적: 레인 추종 + 아군 교전 + 공성, 비행(레인 무시), 아키타입/보스 (CSV 8종)
- 타워 4종: 궁수(직선)/포탑(AoE)/빙결(둔화 필드값)/비전(유도+관통값), 레벨업
- 막사 병사 자동전투(전사 시 재충원), 농가 아침 수입
- 투사체 풀, 적/병사 풀, 이벤트 버스(처치→골드), 전투 골드 경제

**아직 안 된 것 (다음 마일스톤)**
- UI/HUD(골드·웨이브 표시, 노드 탭 빌드 메뉴, 밤시작 버튼) — 현재는 코드 API만
- 씬/프리팹/스프라이트(에디터에서 제작) · 효과음
- 빙결 둔화의 *적용 로직*(현재는 데이터 값만; 적 이동에 슬로우 부여는 TODO)
- 영웅/액티브 스킬/어빌리티, 보스 페이즈, 메타(어드벤처/가챠/패스), 수익화 → 가이드 M3~M7

---

## 다음 작업 (가이드 로드맵)
- M2 마무리: UI(빌드 메뉴/HUD/밤시작), 빙결 슬로우 적용, 바이옴 비주얼, 효과음.
- M3: 영웅 + 드래그 조준 액티브 스킬 + `AbilityFactory`(id→로직).
- 상세: `docs/nightfall-clone-build-guide.md` §5~§9.
