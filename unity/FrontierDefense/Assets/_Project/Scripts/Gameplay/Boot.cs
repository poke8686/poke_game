using UnityEngine;
using FrontierDefense.Core;
using FrontierDefense.Services;

namespace FrontierDefense.Gameplay
{
    /// <summary>
    /// 합성 루트. 영구 서비스(Save/Economy)를 1회 등록하고 씬 전환에도 살아남는다.
    /// 풀 게임에서는 여기서 Home 씬을 로드. 코어 전투 단독 씬에서는 단순 초기화만 한다.
    /// </summary>
    public class Boot : MonoBehaviour
    {
        void Awake()
        {
            if (!Services.TryGet<SaveService>(out _))
            {
                var save = new SaveService();
                save.Load();
                Services.Register(save);
                Services.Register(new EconomyService(save));
            }
            DontDestroyOnLoad(gameObject);
        }
    }
}
