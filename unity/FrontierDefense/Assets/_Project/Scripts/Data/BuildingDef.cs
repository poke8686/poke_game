using System;
using UnityEngine;

namespace FrontierDefense.Data
{
    /// <summary>건물 정의(타워/막사/농가/성벽 등). 레벨별 스탯 배열을 갖는 데이터 드리븐 SO.</summary>
    [CreateAssetMenu(menuName = "FD/Building Def", fileName = "Building_")]
    public class BuildingDef : ScriptableObject
    {
        public int id;
        public string nameKey;
        public BuildingKind kind;

        [Header("Placeholder Art")]
        public string placeholderEmoji;
        public Color placeholderColor = Color.white;
        public Sprite icon;

        [Tooltip("1레벨부터 순서대로(인덱스0 = Lv1)")]
        public BuildingLevel[] levels;

        public int MaxLevel => levels == null ? 0 : levels.Length;

        public BuildingLevel GetLevel(int level)
        {
            if (levels == null || levels.Length == 0) return default;
            int idx = Mathf.Clamp(level - 1, 0, levels.Length - 1);
            return levels[idx];
        }
    }

    [Serializable]
    public struct BuildingLevel
    {
        public int level;
        public int upgradeCost;

        [Header("Tower")]
        public float atk;
        public float atkSpeed;        // 초당 발사 수
        public float range;
        public ProjectileMotion projectile;
        public float aoeRadius;       // 0 = 단일
        public int pierce;            // 0 = 관통 없음
        public float slowPercent;     // 0~1 (빙결탑)

        [Header("Barracks")]
        public int troopCount;
        public float troopHp;
        public float troopAtk;
        public float troopRange;      // 0 = 근접

        [Header("Income")]
        public int goldPerMorning;

        [Header("Common")]
        public float hp;              // 성벽/성문/성 내구도
        public bool unlockTrait;      // 이 레벨에서 특성 분기 선택
    }
}
