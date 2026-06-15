using UnityEngine;

namespace FrontierDefense.Gameplay
{
    /// <summary>적이 따라가는 웨이포인트 경로. 마지막 포인트 = 성 입구.</summary>
    public class Lane : MonoBehaviour
    {
        [Tooltip("성문 밖(시작) → 성 입구(끝) 순서")]
        public Transform[] waypoints;

        public int Count => waypoints == null ? 0 : waypoints.Length;
        public Vector3 Point(int i) => waypoints[Mathf.Clamp(i, 0, Count - 1)].position;
        public Vector3 Start => Point(0);
        public Vector3 End => Point(Count - 1);

#if UNITY_EDITOR
        void OnDrawGizmos()
        {
            if (waypoints == null || waypoints.Length < 2) return;
            Gizmos.color = Color.yellow;
            for (int i = 0; i < waypoints.Length - 1; i++)
                if (waypoints[i] && waypoints[i + 1])
                    Gizmos.DrawLine(waypoints[i].position, waypoints[i + 1].position);
        }
#endif
    }
}
