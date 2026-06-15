using UnityEngine;

namespace FrontierDefense.Gameplay
{
    /// <summary>중앙 성. HP 0 = 패배. 적은 이 성을 직접 공격 목표로 삼는다(타워 표적 리스트와는 분리).</summary>
    [RequireComponent(typeof(Health))]
    public class Castle : MonoBehaviour
    {
        public Health Health { get; private set; }

        void Awake() { Health = GetComponent<Health>(); }

        public void Init(float hp) => Health.Init(hp);
        public void TakeDamage(float dmg) => Health.TakeDamage(dmg);
        public Vector3 Position => transform.position;
    }
}
