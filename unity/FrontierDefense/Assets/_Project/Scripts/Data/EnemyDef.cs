using UnityEngine;

namespace FrontierDefense.Data
{
    /// <summary>적 정의. 레벨 스케일링은 간단한 곡선으로 산출(밸런스는 CSV로 튜닝).</summary>
    [CreateAssetMenu(menuName = "FD/Enemy Def", fileName = "Enemy_")]
    public class EnemyDef : ScriptableObject
    {
        public int id;
        public string nameKey;
        public EnemyArchetype archetype;

        [Header("Placeholder Art")]
        public string placeholderEmoji;
        public Color placeholderColor = Color.white;
        public Sprite sprite;

        [Header("Stats (Lv1 base)")]
        public float baseHp = 80f;
        public float atk = 8f;
        public float atkSpeed = 1f;     // 초당 공격
        public float moveSpeed = 2f;    // 월드 유닛/초
        public float range = 0.4f;      // 근접 교전 거리(원거리는 크게)
        public int goldReward = 6;
        public TargetPriority targeting = TargetPriority.Castle;
        public bool isFlyer;            // 성벽 무시 비행
        public bool ignoresWalls;       // 공병 등

        public float HpAt(int level) => baseHp * Mathf.Pow(1.12f, Mathf.Max(0, level - 1));
        public float AtkAt(int level) => atk * (1f + 0.08f * Mathf.Max(0, level - 1));
    }
}
