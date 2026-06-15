using UnityEngine;

namespace FrontierDefense.Gameplay
{
    /// <summary>건물을 지을 수 있는 빈 슬롯. 낮에 탭하면 빌드 메뉴(UI)가 BattleManager.TryBuild 를 호출.</summary>
    public class BuildNode : MonoBehaviour
    {
        public Building Current { get; set; }
        public bool IsEmpty => Current == null;
        public Vector3 Position => transform.position;

#if UNITY_EDITOR
        void OnDrawGizmos()
        {
            Gizmos.color = new Color(0.8f, 0.7f, 0.3f, 0.5f);
            Gizmos.DrawWireCube(transform.position, Vector3.one * 0.8f);
        }
#endif
    }
}
