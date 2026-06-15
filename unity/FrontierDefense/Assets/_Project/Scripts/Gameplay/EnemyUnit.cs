using System;
using UnityEngine;
using FrontierDefense.Core;
using FrontierDefense.Data;

namespace FrontierDefense.Gameplay
{
    /// <summary>
    /// 적 유닛: 레인을 따라 전진 → 근처 아군(병사) 교전 → 성 도달 시 공성.
    /// 비행(isFlyer)은 레인 무시하고 성으로 직행. 사망 시 EnemyKilled 발행 후 풀 반환.
    /// </summary>
    [RequireComponent(typeof(Health))]
    [RequireComponent(typeof(Targetable))]
    public class EnemyUnit : MonoBehaviour
    {
        const float EngageRange = 0.6f;

        Health _health;
        Targetable _targetable;
        EnemyDef _def;
        int _level;
        Lane _lane;
        Castle _castle;
        Action<EnemyUnit> _onDespawn;
        int _seg;
        float _atkCd, _speed, _atk, _range;
        int _reward;

        void Awake()
        {
            _health = GetComponent<Health>();
            _targetable = GetComponent<Targetable>();
        }

        public void Spawn(EnemyDef def, int level, Lane lane, Castle castle, Action<EnemyUnit> onDespawn)
        {
            _def = def; _level = level; _lane = lane; _castle = castle; _onDespawn = onDespawn;
            _speed = def.moveSpeed;
            _atk = def.AtkAt(level);
            _range = Mathf.Max(def.range, EngageRange);
            _reward = def.goldReward;
            _seg = 1; _atkCd = 0f;
            _targetable.faction = Faction.Enemy;
            _targetable.isFlyer = def.isFlyer;
            transform.position = lane != null ? lane.Start : transform.position;
            _health.Init(def.HpAt(level));
            _health.OnDied -= OnDied;
            _health.OnDied += OnDied;
        }

        void Update()
        {
            if (_health.IsDead || _castle == null) return;

            // 1) 근처 아군 교전 (병사가 진로를 막음)
            var foe = TargetFinder.FindNearest(transform.position, _range, Faction.Player);
            if (foe != null) { Attack(foe.health); return; }

            // 2) 성 도달 → 공성
            if ((transform.position - _castle.Position).magnitude <= _range + 0.3f) { AttackCastle(); return; }

            // 3) 전진 (비행은 직행, 지상은 레인)
            if (_def.isFlyer || _lane == null) MoveToward(_castle.Position);
            else if (MoveToward(_lane.Point(_seg)) && _seg < _lane.Count - 1) _seg++;
        }

        bool MoveToward(Vector3 tgt)
        {
            Vector3 cur = transform.position;
            Vector3 to = tgt - cur;
            float d = to.magnitude;
            float step = _speed * Time.deltaTime;
            if (d <= step) { transform.position = tgt; return true; }
            transform.position = cur + to / d * step;
            return false;
        }

        void Attack(Health h)
        {
            _atkCd -= Time.deltaTime;
            if (_atkCd <= 0f) { h.TakeDamage(_atk); _atkCd = 1f / Mathf.Max(0.1f, _def.atkSpeed); }
        }

        void AttackCastle()
        {
            _atkCd -= Time.deltaTime;
            if (_atkCd <= 0f) { _castle.TakeDamage(_atk); _atkCd = 1f / Mathf.Max(0.1f, _def.atkSpeed); }
        }

        void OnDied(Health h)
        {
            EventBus.Publish(new EnemyKilled { enemyId = _def.id, goldReward = _reward, position = transform.position });
            Despawn();
        }

        public void Despawn()
        {
            _health.OnDied -= OnDied;
            _onDespawn?.Invoke(this);
        }
    }
}
