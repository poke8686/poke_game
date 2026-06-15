using System.Collections.Generic;
using UnityEngine;

namespace FrontierDefense.Core
{
    /// <summary>
    /// 프리팹 기반 컴포넌트 오브젝트 풀. 적/투사체/VFX 처럼 다량 생성·파괴되는 객체는
    /// 반드시 풀링한다(원작은 수십만 오브젝트 — GC/Instantiate 비용 회피).
    /// </summary>
    public sealed class ComponentPool<T> where T : Component
    {
        readonly T _prefab;
        readonly Transform _parent;
        readonly Stack<T> _idle = new Stack<T>();

        public ComponentPool(T prefab, Transform parent = null, int prewarm = 0)
        {
            _prefab = prefab;
            _parent = parent;
            for (int i = 0; i < prewarm; i++)
            {
                var it = Object.Instantiate(_prefab, _parent);
                it.gameObject.SetActive(false);
                _idle.Push(it);
            }
        }

        public T Get()
        {
            T it = _idle.Count > 0 ? _idle.Pop() : Object.Instantiate(_prefab, _parent);
            it.gameObject.SetActive(true);
            return it;
        }

        public void Release(T item)
        {
            if (item == null) return;
            item.gameObject.SetActive(false);
            _idle.Push(item);
        }
    }
}
