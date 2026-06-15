using System.Collections.Generic;

namespace FrontierDefense.Core
{
    /// <summary>전투/유닛 스탯 종류. (원작 _4.CORE.Stats 패턴의 오리지널 재구현)</summary>
    public enum StatType
    {
        Hp, Atk, AtkSpeed, Range, MoveSpeed, Armor,
        CritChance, GoldGain, AoeRadius, Pierce, SlowPercent
    }

    /// <summary>
    /// 단일 스탯. Base 위에 가산(flat) / 곱연산(percent) 모디파이어를 누적한다.
    /// 연구·버프·장비가 모디파이어를 더하는 RPG 스탯 시스템의 최소 단위.
    /// </summary>
    public sealed class Stat
    {
        public float Base;
        readonly List<float> _flat = new List<float>();
        readonly List<float> _percent = new List<float>();

        public Stat(float baseValue) { Base = baseValue; }

        public void AddFlat(float v) => _flat.Add(v);
        public void RemoveFlat(float v) => _flat.Remove(v);
        public void AddPercent(float p) => _percent.Add(p);   // 0.15f == +15%
        public void RemovePercent(float p) => _percent.Remove(p);
        public void ClearModifiers() { _flat.Clear(); _percent.Clear(); }

        public float Value
        {
            get
            {
                float v = Base;
                for (int i = 0; i < _flat.Count; i++) v += _flat[i];
                float mul = 1f;
                for (int i = 0; i < _percent.Count; i++) mul += _percent[i];
                return v * mul;
            }
        }
    }

    /// <summary>스탯 묶음. 없으면 0짜리 스탯을 lazily 생성.</summary>
    public sealed class StatSheet
    {
        readonly Dictionary<StatType, Stat> _stats = new Dictionary<StatType, Stat>();

        public Stat this[StatType type]
        {
            get
            {
                if (!_stats.TryGetValue(type, out var s)) { s = new Stat(0f); _stats[type] = s; }
                return s;
            }
        }

        public StatSheet Set(StatType type, float baseValue) { _stats[type] = new Stat(baseValue); return this; }
        public float Value(StatType type) => this[type].Value;
    }
}
