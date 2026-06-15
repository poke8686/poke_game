namespace FrontierDefense.Data
{
    /// <summary>건물 종류. (원작 검증 enum {Castle,Barracks,Income,Tower,Wall} 의 오리지널 확장)</summary>
    public enum BuildingKind { Castle, Wall, Gate, Tower, Barracks, Income, Shrine }

    /// <summary>적 아키타입. (원작 {Melee,Ranged,Fly,Slayer,Tanker} 기반 오리지널)</summary>
    public enum EnemyArchetype { Melee, Ranged, Flyer, Tank, Slayer, Sapper, Boss }

    /// <summary>적 타겟 우선순위.</summary>
    public enum TargetPriority { Nearest, Castle, Building }

    /// <summary>스테이지 바이옴.</summary>
    public enum Biome { Grassland, Snow, Lava, Abyss }

    /// <summary>투사체 궤적. (원작 BezierAttackTower 대응 = Arc)</summary>
    public enum ProjectileMotion { Straight, Homing, Arc }
}
