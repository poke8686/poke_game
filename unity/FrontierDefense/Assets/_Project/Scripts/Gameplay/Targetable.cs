using System.Collections.Generic;
using UnityEngine;

namespace FrontierDefense.Gameplay
{
    public enum Faction { Player, Enemy }

    /// <summary>타겟 후보. 전역 레지스트리에 등록되어 TargetFinder 가 검색한다(물리 레이어 대신 가벼운 리스트).</summary>
    [RequireComponent(typeof(Health))]
    public class Targetable : MonoBehaviour
    {
        public Faction faction = Faction.Enemy;
        public bool isFlyer;

        [HideInInspector] public Health health;

        public static readonly List<Targetable> All = new List<Targetable>();

        public Vector3 Position => transform.position;
        public bool Alive => health != null && !health.IsDead;

        void Awake() { if (health == null) health = GetComponent<Health>(); }
        void OnEnable() { All.Add(this); }
        void OnDisable() { All.Remove(this); }
    }

    /// <summary>타겟 검색 유틸. (적/아군 진영 + 사거리 + 비행 포함 여부)</summary>
    public static class TargetFinder
    {
        public static Targetable FindNearest(Vector3 pos, float range, Faction wantFaction, bool includeFlyers = true)
        {
            Targetable best = null;
            float bestSqr = range * range;
            var all = Targetable.All;
            for (int i = 0; i < all.Count; i++)
            {
                var t = all[i];
                if (t == null || t.faction != wantFaction || !t.Alive) continue;
                if (!includeFlyers && t.isFlyer) continue;
                float sqr = (t.Position - pos).sqrMagnitude;
                if (sqr <= bestSqr) { bestSqr = sqr; best = t; }
            }
            return best;
        }
    }
}
