using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using FrontierDefense.Core;
using FrontierDefense.Data;

namespace FrontierDefense.Gameplay
{
    /// <summary>
    /// 전투 오케스트레이터. 낮(건설) → 밤(웨이브 방어) → 아침(수확) 루프를 코루틴으로 구동.
    /// 적/투사체/병사 풀을 소유하고, 건물이 호출하는 발사/스폰 서비스를 제공한다.
    /// </summary>
    public class BattleManager : MonoBehaviour
    {
        [Header("Data")]
        public GameDatabase db;
        public StageDef stage;

        [Header("Scene Refs")]
        public Castle castle;
        public Lane[] lanes;
        public BuildNode[] nodes;

        [Header("Prefabs")]
        public EnemyUnit enemyPrefab;
        public TroopUnit troopPrefab;
        public Projectile projectilePrefab;
        public Building buildingPrefab;

        [Header("Tuning")]
        public float castleHp = 1000f;
        public int startGold = 130;
        public float dayAutoStart = 0f;     // 0 = StartNight() 입력 대기, >0 = 자동 시작 딜레이
        public float troopMoveSpeed = 2.2f;
        public float projectileSpeed = 14f;

        public bool IsNight { get; private set; }
        public int BattleGold { get; private set; }
        public int WaveIndex { get; private set; }

        ComponentPool<EnemyUnit> _enemyPool;
        ComponentPool<Projectile> _projPool;
        ComponentPool<TroopUnit> _troopPool;
        readonly List<EnemyUnit> _aliveEnemies = new List<EnemyUnit>();
        bool _startNightFlag, _gameOver;

        void Awake()
        {
            _enemyPool = new ComponentPool<EnemyUnit>(enemyPrefab, transform);
            _projPool = new ComponentPool<Projectile>(projectilePrefab, transform);
            _troopPool = new ComponentPool<TroopUnit>(troopPrefab, transform);
        }

        void OnEnable() => EventBus.Subscribe<EnemyKilled>(OnEnemyKilled);
        void OnDisable() => EventBus.Unsubscribe<EnemyKilled>(OnEnemyKilled);

        void Start()
        {
            if (db != null) db.BuildIndex();
            BattleGold = startGold;
            EventBus.Publish(new GoldChanged { gold = BattleGold });
            if (castle != null) { castle.Init(castleHp); castle.Health.OnDied += _ => Lose(); }
            StartCoroutine(RunBattle());
        }

        IEnumerator RunBattle()
        {
            if (stage == null || stage.waves == null) { Debug.LogWarning("[Battle] stage/waves not assigned"); yield break; }
            for (int w = 0; w < stage.waves.Length && !_gameOver; w++)
            {
                WaveIndex = w;
                yield return DayPhase();
                if (_gameOver) yield break;
                yield return NightPhase(stage.waves[w]);
                if (_gameOver) yield break;
                MorningPhase();
            }
            if (!_gameOver) Win();
        }

        IEnumerator DayPhase()
        {
            IsNight = false; _startNightFlag = false;
            if (dayAutoStart > 0f)
            {
                float t = 0f;
                while (t < dayAutoStart) { t += Time.deltaTime; yield return null; }
            }
            else while (!_startNightFlag) yield return null;
        }

        /// <summary>UI "밤 시작" 버튼이 호출.</summary>
        public void StartNight() => _startNightFlag = true;

        IEnumerator NightPhase(WaveDef wave)
        {
            IsNight = true;
            EventBus.Publish(new WaveStarted { index = wave.index, isBoss = wave.isBoss });

            int spawning = 0;
            if (wave.spawns != null)
                foreach (var sp in wave.spawns) { spawning++; StartCoroutine(SpawnGroup(sp, () => spawning--)); }

            while (spawning > 0 || _aliveEnemies.Count > 0)
            {
                if (_gameOver) yield break;
                yield return null;
            }
            EventBus.Publish(new WaveCleared { index = wave.index });
        }

        IEnumerator SpawnGroup(EnemySpawn sp, Action onDone)
        {
            EnemyDef def = db != null ? db.Enemy(sp.enemyId) : null;
            if (def == null) { Debug.LogWarning($"[Battle] enemy id {sp.enemyId} not found"); onDone?.Invoke(); yield break; }
            Lane lane = (lanes != null && lanes.Length > 0) ? lanes[Mathf.Clamp(sp.laneIndex, 0, lanes.Length - 1)] : null;

            if (sp.delay > 0f) yield return new WaitForSeconds(sp.delay);
            int level = sp.level <= 0 ? 1 : sp.level;
            for (int i = 0; i < sp.count && !_gameOver; i++)
            {
                SpawnEnemy(def, level, lane);
                if (sp.interval > 0f) yield return new WaitForSeconds(sp.interval); else yield return null;
            }
            onDone?.Invoke();
        }

        void SpawnEnemy(EnemyDef def, int level, Lane lane)
        {
            var e = _enemyPool.Get();
            _aliveEnemies.Add(e);
            e.Spawn(def, level, lane, castle, released =>
            {
                _aliveEnemies.Remove(released);
                _enemyPool.Release(released);
            });
        }

        void MorningPhase()
        {
            IsNight = false;
            int income = 0;
            if (nodes != null)
                foreach (var n in nodes)
                    if (n != null && n.Current != null) income += n.Current.HarvestGold();
            if (income > 0) AddBattleGold(income);
        }

        // ---- 빌드(UI에서 호출) ----
        public bool TryBuild(BuildNode node, BuildingDef def)
        {
            if (node == null || def == null || !node.IsEmpty) return false;
            int cost = def.GetLevel(1).upgradeCost;   // Lv1 의 upgradeCost = 최초 건설 비용
            if (!SpendBattleGold(cost)) return false;
            var b = Instantiate(buildingPrefab, node.Position, Quaternion.identity, transform);
            b.Init(def, 1, this);
            node.Current = b;
            return true;
        }

        public bool TryUpgrade(BuildNode node)
        {
            if (node == null || node.IsEmpty) return false;
            var b = node.Current;
            if (b.Level >= b.Def.MaxLevel) return false;
            if (!SpendBattleGold(b.UpgradeCost())) return false;
            b.SetLevel(b.Level + 1);
            return true;
        }

        // ---- 풀 서비스(건물에서 호출) ----
        public void FireProjectile(Vector3 from, Targetable target, BuildingLevel stats)
        {
            var p = _projPool.Get();
            p.Launch(from, target, stats.atk, stats.projectile, stats.aoeRadius, projectileSpeed, Faction.Enemy,
                     pr => _projPool.Release(pr));
        }

        public TroopUnit SpawnTroop(Vector3 from, BuildingLevel stats, Action<TroopUnit> onReleased)
        {
            var t = _troopPool.Get();
            Vector3 pos = from + (Vector3)(UnityEngine.Random.insideUnitCircle * 0.4f);
            float range = stats.troopRange <= 0f ? 0.5f : stats.troopRange;
            t.Spawn(pos, stats.troopHp, stats.troopAtk, range, 1f, troopMoveSpeed, rel =>
            {
                _troopPool.Release(rel);
                onReleased?.Invoke(rel);
            });
            return t;
        }

        // ---- 전투 골드(휘발성) ----
        public void AddBattleGold(int v) { BattleGold += v; EventBus.Publish(new GoldChanged { gold = BattleGold }); }
        public bool SpendBattleGold(int v)
        {
            if (BattleGold < v) return false;
            BattleGold -= v;
            EventBus.Publish(new GoldChanged { gold = BattleGold });
            return true;
        }

        void OnEnemyKilled(EnemyKilled e) => AddBattleGold(e.goldReward);

        void Win()
        {
            _gameOver = true;
            EventBus.Publish(new BattleEnded { victory = true, wavesSurvived = stage.waves.Length });
            Debug.Log("[Battle] VICTORY");
        }

        void Lose()
        {
            if (_gameOver) return;
            _gameOver = true;
            EventBus.Publish(new BattleEnded { victory = false, wavesSurvived = WaveIndex });
            Debug.Log("[Battle] DEFEAT");
        }
    }
}
