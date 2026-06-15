using System;
using UnityEngine;

namespace FrontierDefense.Data
{
    /// <summary>스테이지 = 바이옴 + 전투력 게이트 + 웨이브 목록.</summary>
    [CreateAssetMenu(menuName = "FD/Stage Def", fileName = "Stage_")]
    public class StageDef : ScriptableObject
    {
        public int id;
        public Biome biome = Biome.Grassland;
        public int powerGate;            // 입장 권장 전투력(메타)
        public WaveDef[] waves;
    }

    [Serializable]
    public struct WaveDef
    {
        public int index;
        public bool isBoss;
        public EnemySpawn[] spawns;
    }

    [Serializable]
    public struct EnemySpawn
    {
        public int enemyId;
        public int level;
        public int count;
        public int laneIndex;
        public float delay;      // 웨이브 시작 후 첫 스폰까지
        public float interval;   // 마리 사이 간격
    }
}
