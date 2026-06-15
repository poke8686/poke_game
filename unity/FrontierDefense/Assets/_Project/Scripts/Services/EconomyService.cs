using FrontierDefense.Core;

namespace FrontierDefense.Services
{
    /// <summary>
    /// 메타(영구) 재화 — 골드/보석. 전투 내 "건설 골드"는 휘발성이라 BattleManager 가 별도 관리.
    /// 변경 시 GoldChanged 이벤트 발행(HUD 등 구독).
    /// </summary>
    public class EconomyService
    {
        readonly SaveService _save;
        public EconomyService(SaveService save) { _save = save; }

        public long Gold => _save.Data.gold;
        public long Gems => _save.Data.gems;

        public void AddGold(long v)
        {
            _save.Data.gold += v; _save.Save();
            EventBus.Publish(new GoldChanged { gold = (int)_save.Data.gold });
        }

        public bool SpendGold(long v)
        {
            if (_save.Data.gold < v) return false;
            _save.Data.gold -= v; _save.Save();
            EventBus.Publish(new GoldChanged { gold = (int)_save.Data.gold });
            return true;
        }

        public void AddGems(long v) { _save.Data.gems += v; _save.Save(); }
        public bool SpendGems(long v) { if (_save.Data.gems < v) return false; _save.Data.gems -= v; _save.Save(); return true; }
    }
}
