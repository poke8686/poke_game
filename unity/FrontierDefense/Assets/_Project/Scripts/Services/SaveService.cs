using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace FrontierDefense.Services
{
    /// <summary>영구 저장 DTO. 런타임 객체와 분리(POCO) → JSON 직렬화. schemaVersion 으로 마이그레이션.</summary>
    [Serializable]
    public class SaveData
    {
        public long gold;
        public long gems;
        public int highestStage;
        public int pityCounter;                       // 가챠 천장 카운터(메타)
        public List<int> unlockedBuildings = new List<int>();
        public string schemaVersion = "1.0";
    }

    /// <summary>save.json 로드/저장. (클라우드 백업은 추후 Firebase Firestore로 동일 JSON 미러)</summary>
    public class SaveService
    {
        public SaveData Data { get; private set; } = new SaveData();
        string FilePath => Path.Combine(Application.persistentDataPath, "save.json");

        public void Load()
        {
            try
            {
                if (File.Exists(FilePath))
                    Data = JsonUtility.FromJson<SaveData>(File.ReadAllText(FilePath)) ?? new SaveData();
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[Save] load failed, starting fresh: {e.Message}");
                Data = new SaveData();
            }
        }

        public void Save()
        {
            try { File.WriteAllText(FilePath, JsonUtility.ToJson(Data)); }
            catch (Exception e) { Debug.LogWarning($"[Save] write failed: {e.Message}"); }
        }
    }
}
