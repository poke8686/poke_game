using System;
using UnityEngine;
using FrontierDefense.Data;

namespace FrontierDefense.Gameplay
{
    /// <summary>타워 투사체. 직선/유도/포물(시각) 궤적. 적중 시 단일 또는 AoE 데미지 후 풀로 반환.</summary>
    public class Projectile : MonoBehaviour
    {
        Targetable _target;
        Vector3 _aim;
        float _dmg, _aoe, _speed;
        ProjectileMotion _motion;
        Faction _hitFaction;
        Action<Projectile> _onDone;
        bool _active;

        public void Launch(Vector3 start, Targetable target, float dmg, ProjectileMotion motion,
                           float aoe, float speed, Faction hitFaction, Action<Projectile> onDone)
        {
            transform.position = start;
            _target = target;
            _aim = target != null ? target.Position : start;
            _dmg = dmg;
            _motion = motion;
            _aoe = aoe;
            _speed = speed <= 0f ? 12f : speed;
            _hitFaction = hitFaction;
            _onDone = onDone;
            _active = true;
        }

        void Update()
        {
            if (!_active) return;

            // 유도/포물은 타겟 추적, 직선은 발사 시점 좌표로 비행
            Vector3 dest = _motion == ProjectileMotion.Straight
                ? _aim
                : (_target != null && _target.Alive ? _target.Position : _aim);
            _aim = dest;

            Vector3 cur = transform.position;
            Vector3 to = dest - cur;
            float dist = to.magnitude;
            float step = _speed * Time.deltaTime;
            if (dist > 0.0001f) transform.right = to / dist;

            if (dist <= step + 0.05f) { transform.position = dest; Impact(); return; }
            transform.position = cur + to / dist * step;
        }

        void Impact()
        {
            if (_target != null && _target.Alive) _target.health.TakeDamage(_dmg);
            if (_aoe > 0f)
            {
                var all = Targetable.All;
                float r2 = _aoe * _aoe;
                for (int i = 0; i < all.Count; i++)
                {
                    var t = all[i];
                    if (t == null || t.faction != _hitFaction || !t.Alive) continue;
                    if (t != _target && (t.Position - transform.position).sqrMagnitude <= r2)
                        t.health.TakeDamage(_dmg);
                }
            }
            _active = false;
            _onDone?.Invoke(this);
        }
    }
}
