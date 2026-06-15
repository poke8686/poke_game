using System;
using System.Collections.Generic;
using UnityEngine;

namespace FrontierDefense.Core
{
    /// <summary>초경량 서비스 로케이터. (DI 컨테이너 도입 시 대체 가능)</summary>
    public static class Services
    {
        static readonly Dictionary<Type, object> _map = new Dictionary<Type, object>();

        public static void Register<T>(T service) => _map[typeof(T)] = service;
        public static T Get<T>() => (T)_map[typeof(T)];
        public static bool TryGet<T>(out T service)
        {
            if (_map.TryGetValue(typeof(T), out var o)) { service = (T)o; return true; }
            service = default; return false;
        }
        public static void Clear() => _map.Clear();
    }

    /// <summary>타입별 이벤트 버스. 결합도를 낮추는 발행/구독 허브.</summary>
    public static class EventBus
    {
        static readonly Dictionary<Type, Delegate> _handlers = new Dictionary<Type, Delegate>();

        public static void Subscribe<T>(Action<T> cb)
        {
            _handlers.TryGetValue(typeof(T), out var d);
            _handlers[typeof(T)] = (d as Action<T>) + cb;
        }
        public static void Unsubscribe<T>(Action<T> cb)
        {
            if (_handlers.TryGetValue(typeof(T), out var d))
                _handlers[typeof(T)] = (d as Action<T>) - cb;
        }
        public static void Publish<T>(T evt)
        {
            if (_handlers.TryGetValue(typeof(T), out var d)) (d as Action<T>)?.Invoke(evt);
        }
        public static void Clear() => _handlers.Clear();
    }

    // ---- 게임 이벤트 (전투/경제). 각 시스템이 자유롭게 구독 ----
    public struct EnemyKilled   { public int enemyId; public int goldReward; public Vector3 position; }
    public struct CastleDamaged { public float current; public float max; }
    public struct GoldChanged   { public int gold; }
    public struct WaveStarted   { public int index; public bool isBoss; }
    public struct WaveCleared   { public int index; }
    public struct BattleEnded   { public bool victory; public int wavesSurvived; }
}
