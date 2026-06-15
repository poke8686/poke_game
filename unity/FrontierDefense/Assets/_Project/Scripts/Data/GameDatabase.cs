using System.Collections.Generic;
using UnityEngine;

namespace FrontierDefense.Data
{
    /// <summary>모든 정의 SO를 묶는 루트. id→def 인덱스를 제공.</summary>
    [CreateAssetMenu(menuName = "FD/Game Database", fileName = "GameDatabase")]
    public class GameDatabase : ScriptableObject
    {
        public List<BuildingDef> buildings = new List<BuildingDef>();
        public List<EnemyDef> enemies = new List<EnemyDef>();
        public List<StageDef> stages = new List<StageDef>();

        Dictionary<int, BuildingDef> _b;
        Dictionary<int, EnemyDef> _e;
        Dictionary<int, StageDef> _s;

        public void BuildIndex()
        {
            _b = new Dictionary<int, BuildingDef>();
            foreach (var x in buildings) if (x != null) _b[x.id] = x;
            _e = new Dictionary<int, EnemyDef>();
            foreach (var x in enemies) if (x != null) _e[x.id] = x;
            _s = new Dictionary<int, StageDef>();
            foreach (var x in stages) if (x != null) _s[x.id] = x;
        }

        public BuildingDef Building(int id) { if (_b == null) BuildIndex(); return _b.TryGetValue(id, out var v) ? v : null; }
        public EnemyDef Enemy(int id) { if (_e == null) BuildIndex(); return _e.TryGetValue(id, out var v) ? v : null; }
        public StageDef Stage(int id) { if (_s == null) BuildIndex(); return _s.TryGetValue(id, out var v) ? v : null; }

        void OnEnable() => BuildIndex();
    }
}
