#if UNITY_EDITOR
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using UnityEditor;
using UnityEngine;
using FrontierDefense.Data;

namespace FrontierDefense.EditorTools
{
    /// <summary>
    /// CSV → ScriptableObject 베이크. (원작의 CSV→SO 데이터 파이프라인을 오리지널 재현)
    /// 메뉴: FD/Import CSV → SO. 기획자가 CSV(엑셀) 수정 → 메뉴 1클릭으로 SO 갱신.
    /// </summary>
    public static class CsvImporter
    {
        const string CsvDir = "Assets/_Project/Data/CSV/";
        const string SoDir = "Assets/_Project/Data/SO/";

        [MenuItem("FD/Import CSV → SO")]
        public static void ImportAll()
        {
            EnsureDir(SoDir);
            int e = ImportEnemies();
            int b = ImportBuildings();
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();
            Debug.Log($"[FD] CSV import done: {e} enemies, {b} buildings");
        }

        static int ImportEnemies()
        {
            int n = 0;
            foreach (var r in ReadRows(CsvDir + "enemies.csv"))
            {
                var so = LoadOrCreate<EnemyDef>(SoDir + $"Enemy_{I(r, "id")}.asset");
                so.id = I(r, "id");
                so.nameKey = S(r, "nameKey");
                so.archetype = E<EnemyArchetype>(r, "archetype");
                so.placeholderEmoji = S(r, "emoji");
                ColorUtility.TryParseHtmlString(S(r, "color"), out so.placeholderColor);
                so.baseHp = F(r, "hp");
                so.atk = F(r, "atk");
                so.atkSpeed = F(r, "atkSpeed");
                so.moveSpeed = F(r, "moveSpeed");
                so.range = F(r, "range");
                so.goldReward = I(r, "gold");
                so.targeting = E<TargetPriority>(r, "targeting");
                so.isFlyer = B(r, "flyer");
                so.ignoresWalls = B(r, "ignoresWalls");
                EditorUtility.SetDirty(so);
                n++;
            }
            return n;
        }

        static int ImportBuildings()
        {
            // 한 건물이 여러 레벨 행을 가짐 → id 로 그룹핑 후 levels[] 구성
            var byId = new Dictionary<int, List<Dictionary<string, string>>>();
            foreach (var r in ReadRows(CsvDir + "buildings.csv"))
            {
                int id = I(r, "id");
                if (!byId.TryGetValue(id, out var list)) { list = new List<Dictionary<string, string>>(); byId[id] = list; }
                list.Add(r);
            }

            int n = 0;
            foreach (var kv in byId)
            {
                var first = kv.Value[0];
                var so = LoadOrCreate<BuildingDef>(SoDir + $"Building_{kv.Key}.asset");
                so.id = kv.Key;
                so.nameKey = S(first, "nameKey");
                so.kind = E<BuildingKind>(first, "kind");
                so.placeholderEmoji = S(first, "emoji");
                ColorUtility.TryParseHtmlString(S(first, "color"), out so.placeholderColor);

                kv.Value.Sort((a, b) => I(a, "level").CompareTo(I(b, "level")));
                var levels = new List<BuildingLevel>();
                foreach (var r in kv.Value)
                {
                    levels.Add(new BuildingLevel
                    {
                        level = I(r, "level"),
                        upgradeCost = I(r, "cost"),
                        atk = F(r, "atk"),
                        atkSpeed = F(r, "atkSpeed"),
                        range = F(r, "range"),
                        projectile = E<ProjectileMotion>(r, "projectile"),
                        aoeRadius = F(r, "aoe"),
                        pierce = I(r, "pierce"),
                        slowPercent = F(r, "slow"),
                        troopCount = I(r, "troopCount"),
                        troopHp = F(r, "troopHp"),
                        troopAtk = F(r, "troopAtk"),
                        troopRange = F(r, "troopRange"),
                        goldPerMorning = I(r, "income"),
                        hp = F(r, "hp"),
                    });
                }
                so.levels = levels.ToArray();
                EditorUtility.SetDirty(so);
                n++;
            }
            return n;
        }

        // ---- helpers ----
        static void EnsureDir(string p) { if (!Directory.Exists(p)) Directory.CreateDirectory(p); }

        static T LoadOrCreate<T>(string path) where T : ScriptableObject
        {
            var a = AssetDatabase.LoadAssetAtPath<T>(path);
            if (a == null) { a = ScriptableObject.CreateInstance<T>(); AssetDatabase.CreateAsset(a, path); }
            return a;
        }

        static List<Dictionary<string, string>> ReadRows(string path)
        {
            var res = new List<Dictionary<string, string>>();
            if (!File.Exists(path)) { Debug.LogWarning($"[FD] missing CSV: {path}"); return res; }
            var lines = File.ReadAllLines(path);
            if (lines.Length < 2) return res;
            var head = lines[0].Split(',');
            for (int i = 1; i < lines.Length; i++)
            {
                if (string.IsNullOrWhiteSpace(lines[i])) continue;
                var cells = lines[i].Split(',');
                var row = new Dictionary<string, string>();
                for (int c = 0; c < head.Length && c < cells.Length; c++) row[head[c].Trim()] = cells[c].Trim();
                res.Add(row);
            }
            return res;
        }

        static string S(Dictionary<string, string> r, string k) => r.TryGetValue(k, out var v) ? v : "";
        static int I(Dictionary<string, string> r, string k) => int.TryParse(S(r, k), NumberStyles.Any, CultureInfo.InvariantCulture, out var v) ? v : 0;
        static float F(Dictionary<string, string> r, string k) => float.TryParse(S(r, k), NumberStyles.Any, CultureInfo.InvariantCulture, out var v) ? v : 0f;
        static bool B(Dictionary<string, string> r, string k) { var s = S(r, k).ToLowerInvariant(); return s == "1" || s == "true"; }
        static T E<T>(Dictionary<string, string> r, string k) where T : struct => Enum.TryParse<T>(S(r, k), true, out var v) ? v : default;
    }
}
#endif
