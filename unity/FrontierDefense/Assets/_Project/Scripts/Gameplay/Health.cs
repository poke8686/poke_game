using System;
using UnityEngine;

namespace FrontierDefense.Gameplay
{
    /// <summary>체력 컴포넌트. 적/아군/건물/성이 공유. 데미지/사망 이벤트를 노출.</summary>
    public class Health : MonoBehaviour
    {
        public float Max { get; private set; }
        public float Current { get; private set; }
        public bool IsDead { get; private set; }

        /// <summary>사망 시 1회. 인자 = 자기 자신.</summary>
        public event Action<Health> OnDied;
        /// <summary>변경 시(current, max). HP바 갱신용.</summary>
        public event Action<float, float> OnChanged;

        public float Ratio => Max <= 0f ? 0f : Mathf.Clamp01(Current / Max);

        public void Init(float max)
        {
            Max = max;
            Current = max;
            IsDead = false;
            OnChanged?.Invoke(Current, Max);
        }

        public void TakeDamage(float dmg)
        {
            if (IsDead || dmg <= 0f) return;
            Current = Mathf.Max(0f, Current - dmg);
            OnChanged?.Invoke(Current, Max);
            if (Current <= 0f)
            {
                IsDead = true;
                OnDied?.Invoke(this);
            }
        }

        public void Heal(float v)
        {
            if (IsDead || v <= 0f) return;
            Current = Mathf.Min(Max, Current + v);
            OnChanged?.Invoke(Current, Max);
        }
    }
}
