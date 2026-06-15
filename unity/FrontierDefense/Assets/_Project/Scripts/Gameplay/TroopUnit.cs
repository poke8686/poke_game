using System;
using UnityEngine;

namespace FrontierDefense.Gameplay
{
    /// <summary>막사가 스폰하는 아군 병사. 가까운 적을 찾아 이동·교전. 사망 시 막사가 재충원.</summary>
    [RequireComponent(typeof(Health))]
    [RequireComponent(typeof(Targetable))]
    public class TroopUnit : MonoBehaviour
    {
        const float SearchRange = 6f;

        Health _health;
        Targetable _targetable;
        float _atk, _range, _speed, _atkSpeed, _atkCd;
        Vector3 _rally;
        Action<TroopUnit> _onDeath;

        void Awake()
        {
            _health = GetComponent<Health>();
            _targetable = GetComponent<Targetable>();
        }

        public void Spawn(Vector3 pos, float hp, float atk, float range, float atkSpeed, float speed, Action<TroopUnit> onDeath)
        {
            transform.position = pos; _rally = pos;
            _atk = atk;
            _range = Mathf.Max(0.4f, range);
            _atkSpeed = Mathf.Max(0.2f, atkSpeed);
            _speed = speed;
            _onDeath = onDeath;
            _atkCd = 0f;
            _targetable.faction = Faction.Player;
            _targetable.isFlyer = false;
            _health.Init(hp);
            _health.OnDied -= OnDied;
            _health.OnDied += OnDied;
        }

        void Update()
        {
            if (_health.IsDead) return;
            var enemy = TargetFinder.FindNearest(transform.position, SearchRange, Faction.Enemy);
            if (enemy == null) { MoveToward(_rally); return; }

            float d = (enemy.Position - transform.position).magnitude;
            if (d <= _range)
            {
                _atkCd -= Time.deltaTime;
                if (_atkCd <= 0f) { enemy.health.TakeDamage(_atk); _atkCd = 1f / _atkSpeed; }
            }
            else MoveToward(enemy.Position);
        }

        void MoveToward(Vector3 t)
        {
            Vector3 c = transform.position;
            Vector3 to = t - c;
            float d = to.magnitude;
            if (d < 0.01f) return;
            float step = _speed * Time.deltaTime;
            transform.position = d <= step ? t : c + to / d * step;
        }

        void OnDied(Health h)
        {
            _health.OnDied -= OnDied;
            _onDeath?.Invoke(this);
        }
    }
}
