using System;
using System.Collections.Generic;
using UnityEngine;
using FrontierDefense.Data;

namespace FrontierDefense.Gameplay
{
    /// <summary>
    /// 노드에 배치되는 건물. 종류별 동작:
    ///  - Tower:    밤에 사거리 내 적에게 투사체 발사
    ///  - Barracks: 밤에 병사를 troopCount 만큼 유지(전사 시 재충원)
    ///  - Income:   아침에 골드 수확(BattleManager.MorningPhase 가 HarvestGold 호출)
    /// </summary>
    public class Building : MonoBehaviour
    {
        public BuildingDef Def { get; private set; }
        public int Level { get; private set; }

        BuildingLevel _stats;
        BattleManager _battle;
        float _fireCd, _spawnCd;
        readonly List<TroopUnit> _troops = new List<TroopUnit>();

        public void Init(BuildingDef def, int level, BattleManager battle)
        {
            Def = def; _battle = battle; SetLevel(level);
        }

        public void SetLevel(int level)
        {
            Level = Mathf.Clamp(level, 1, Mathf.Max(1, Def.MaxLevel));
            _stats = Def.GetLevel(Level);
        }

        void Update()
        {
            if (_battle == null || !_battle.IsNight) return;
            switch (Def.kind)
            {
                case BuildingKind.Tower: TickTower(); break;
                case BuildingKind.Barracks: TickBarracks(); break;
            }
        }

        void TickTower()
        {
            _fireCd -= Time.deltaTime;
            if (_fireCd > 0f) return;
            var t = TargetFinder.FindNearest(transform.position, _stats.range, Faction.Enemy);
            if (t == null) return;
            _fireCd = 1f / Mathf.Max(0.1f, _stats.atkSpeed);
            _battle.FireProjectile(transform.position, t, _stats);
        }

        void TickBarracks()
        {
            _troops.RemoveAll(x => x == null || !x.gameObject.activeSelf);
            if (_troops.Count >= _stats.troopCount) return;
            _spawnCd -= Time.deltaTime;
            if (_spawnCd > 0f) return;
            _spawnCd = 2f;
            var tr = _battle.SpawnTroop(transform.position, _stats, t => _troops.Remove(t));
            if (tr != null) _troops.Add(tr);
        }

        public int HarvestGold() => Def.kind == BuildingKind.Income ? _stats.goldPerMorning : 0;
        public int UpgradeCost() => Level < Def.MaxLevel ? Def.GetLevel(Level + 1).upgradeCost : 0;
    }
}
