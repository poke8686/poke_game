package com.poke86.game.game.nightfall

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.input.onClick
import korlibs.korge.view.Circle
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.addUpdater
import korlibs.korge.view.circle
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Angle
import korlibs.time.seconds
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Nightfall(왕국 국경 TD) — 탑다운 왕국 디펜스 (Thronefall 스타일).
 *
 * 낮(건설/업그레이드) → 밤(웨이브 방어) → 아침(수확) 루프. 중앙 성 + 성벽/성문/방벽,
 * 노드 기반 건물(화살탑/막사/집), 막사 병사 자동전투, 바이옴(초원→설원→용암),
 * 다방향 성문(좌/우 추가), 5웨이브마다 보스.
 *
 * 도형 기반(미니멀). 스프라이트 아트는 추후 교체.
 */
class NightfallWorld(private val stage: Container) {

    private enum class Phase { DAY, NIGHT, GAMEOVER }

    private enum class Build(val label: String, val icon: String, val cost: Int, val color: RGBA) {
        TOWER("화살탑", "↑", 55, Colors["#8a8f98"]),
        BARRACKS("막사", "검", 75, Colors["#b5793a"]),
        HOUSE("집", "집", 45, Colors["#caa46a"]),
    }

    private enum class Biome(val ground: RGBA, val patch: RGBA, val plaza: RGBA, val wall: RGBA, val wallDark: RGBA, val road: RGBA, val label: String) {
        GRASS(Colors["#4f9d4f"], Colors["#46934a"], Colors["#5aa85a"], Colors["#7d756a"], Colors["#5f594f"], Colors["#7a6b4a"], "초원"),
        SNOW(Colors["#c9dbe6"], Colors["#b6ccd9"], Colors["#d8e7ef"], Colors["#8c97a3"], Colors["#6f7984"], Colors["#aab8c4"], "설원"),
        LAVA(Colors["#5a3326"], Colors["#6b3a26"], Colors["#6e4234"], Colors["#4a3228"], Colors["#2f2018"], Colors["#8a4524"], "용암"),
    }

    private val maxLevel = 8

    // ---- 레이아웃 ----
    private val W = 720.0
    private val H = 1280.0
    private val cx = 360.0
    private val cy = 720.0
    private val castleR = 86.0
    private val gateX = 360.0
    private val gateY = 332.0
    private val kLeft = 80.0; private val kTop = 300.0; private val kRight = 640.0; private val kBottom = 1150.0
    private val sideGateY = 700.0; private val sideGateH = 70.0

    private class Pt(val x: Double, val y: Double)

    private val laneTop = listOf(Pt(360.0, -60.0), Pt(360.0, 180.0), Pt(gateX, gateY), Pt(360.0, 600.0))
    private val laneLeft = listOf(Pt(-60.0, sideGateY + 35.0), Pt(90.0, sideGateY + 35.0), Pt(240.0, sideGateY + 35.0), Pt(cx - 60.0, cy))
    private val laneRight = listOf(Pt(780.0, sideGateY + 35.0), Pt(630.0, sideGateY + 35.0), Pt(480.0, sideGateY + 35.0), Pt(cx + 60.0, cy))

    private class Lane(val path: List<Pt>, val top: Boolean)

    private fun biomeFor(w: Int) = when { w < 6 -> Biome.GRASS; w < 13 -> Biome.SNOW; else -> Biome.LAVA }
    private fun leftActive() = biome == Biome.LAVA
    private fun rightActive() = biome != Biome.GRASS
    private fun activeLanes(): List<Lane> {
        val l = mutableListOf(Lane(laneTop, true))
        if (rightActive()) l.add(Lane(laneRight, false))
        if (leftActive()) l.add(Lane(laneLeft, false))
        return l
    }

    // ---- 밸런스 ----
    private val projSpeed = 660.0
    private val soldierSpeed = 80.0
    private val soldierR = 12.0
    private val meleeRange = 30.0
    private val enemyAtk = 12.0
    private val enemyAtkInt = 0.7
    private val enemyR = 15.0
    private val engageRange = 40.0
    private val killReward = 6

    private fun towerDmg(l: Int) = 32.0 * (1.0 + 0.30 * (l - 1))
    private fun towerRange(l: Int) = 235.0 + 12.0 * (l - 1)
    private fun towerFire(l: Int) = 0.70 * 0.95.pow(l - 1)
    private fun barracksMax(l: Int) = 3 + l + (castleLevel - 1)
    private fun barracksProduce(l: Int) = (2.0 - 0.12 * (l - 1)).coerceAtLeast(0.9)
    private fun soldierHp(l: Int) = 80.0 + 22.0 * (l - 1)
    private fun soldierAtk(l: Int) = 16.0 + 5.0 * (l - 1)
    private fun houseOutput(l: Int) = 14 * l
    private fun barricadeMaxHp(l: Int) = 180.0 * l
    private fun upgradeCost(type: Build, level: Int) = (type.cost * 0.8 * level).toInt()
    private val barricadeBaseCost = 40
    private fun barricadeUpCost(level: Int) = (barricadeBaseCost * 0.8 * level).toInt()

    // ---- 상태 ----
    private var phase = Phase.DAY
    private var gold = 130
    private var lives = 12
    private var wave = 0
    private var castleLevel = 1
    private var biome = Biome.GRASS
    private var nightAlpha = 0.0

    private var spawnRemaining = 0
    private var spawnTimer = 0.0
    private var spawnInterval = 0.8
    private var bossPending = false

    // ---- 엔티티 ----
    private class Enemy(val view: Container, val body: Container, val hpBar: SolidRect, var x: Double, var y: Double, var hp: Double, val maxHp: Double, val speed: Double, val r: Double, val path: List<Pt>, val top: Boolean, val isBoss: Boolean, var seg: Int, val offset: Double, var bob: Double, var atkCd: Double)
    private class Soldier(val view: Container, val body: Container, val hpBar: SolidRect, var x: Double, var y: Double, var hp: Double, val maxHp: Double, val atk: Double, var atkCd: Double, val owner: Building)
    private class Building(val type: Build, val view: Container, val badge: Text, val x: Double, val y: Double, var cd: Double, var level: Int, var invested: Int)
    private class Node(val x: Double, val y: Double, val pad: Container, val plus: Text, var building: Building?)
    private class Barricade(var view: Container, var hpBar: SolidRect, var hp: Double, var maxHp: Double, var level: Int)
    private class Proj(val view: Container, var x: Double, var y: Double, val target: Enemy, val dmg: Double)
    private class Floater(val view: Text, var y: Double, var life: Double)
    private class Particle(val view: Circle, var x: Double, var y: Double, val vx: Double, val vy: Double, var life: Double, val maxLife: Double, val r: Double)

    private val enemies = mutableListOf<Enemy>()
    private val particles = mutableListOf<Particle>()
    private val soldiers = mutableListOf<Soldier>()
    private val projectiles = mutableListOf<Proj>()
    private val floaters = mutableListOf<Floater>()
    private val nodes = mutableListOf<Node>()
    private var barricade: Barricade? = null

    // ---- 레이어 / HUD ----
    private lateinit var groundLayer: Container
    private lateinit var kingdomLayer: Container
    private lateinit var worldLayer: Container
    private lateinit var nightTint: SolidRect
    private lateinit var hudGold: Text
    private lateinit var hudLives: Text
    private lateinit var hudWave: Text
    private lateinit var hudPhase: Text
    private lateinit var actionButton: Container
    private lateinit var gatePad: Container
    private lateinit var gatePrompt: Container
    private lateinit var uiFont: Font
    private var overlay: Container? = null
    private var menuLayer: Container? = null
    private var bannerView: Container? = null
    private var nightDecor: Container? = null
    private val torches = mutableListOf<Circle>()
    private var animTime = 0.0
    private var shake = 0.0

    private val nodePts = listOf(
        Pt(190.0, 470.0), Pt(530.0, 470.0),
        Pt(140.0, 560.0), Pt(580.0, 560.0),
        Pt(190.0, 950.0), Pt(530.0, 950.0),
        Pt(290.0, 1055.0), Pt(430.0, 1055.0),
    )

    suspend fun setup() {
        uiFont = resourcesVfs["fonts/NanumGothicBold.ttf"].readTtfFont()
        groundLayer = stage.container()
        worldLayer = stage.container()
        kingdomLayer = worldLayer.container()
        applyBiome(Biome.GRASS)
        bannerView = worldLayer.castleArt(cx, cy)
        drawNodes()
        drawGatePad()
        restore()
        nightTint = stage.solidRect(W, H, Colors["#0a1430"]).apply { alpha = 0.0; hitTestEnabled = false }
        buildNightDecor()
        buildHud()
        updateHud()
        stage.addUpdater { dt -> update(dt.seconds) }
    }

    // ---- 지형/바이옴 ----
    private fun applyBiome(b: Biome) {
        biome = b
        groundLayer.removeChildren()
        kingdomLayer.removeChildren()
        drawGround()
        drawKingdom()
    }

    private fun drawGround() {
        groundLayer.solidRect(W, H, biome.ground)
        val patches = listOf(Pt(120.0, 250.0), Pt(620.0, 200.0), Pt(80.0, 1150.0), Pt(650.0, 1180.0), Pt(360.0, 120.0))
        for (p in patches) groundLayer.circle(70.0, biome.patch) { position(p.x - 70.0, p.y - 70.0); alpha = 0.6 }
        groundLayer.solidRect(70.0, 400.0, biome.road) { position(gateX - 35.0, -40.0); alpha = 0.6 }
        if (rightActive()) groundLayer.solidRect(170.0, 70.0, biome.road) { position(kRight - 20.0, sideGateY); alpha = 0.6 }
        if (leftActive()) groundLayer.solidRect(160.0, 70.0, biome.road) { position(-60.0, sideGateY); alpha = 0.6 }
        drawScenery()
    }

    private fun drawScenery() {
        val snowy = biome == Biome.SNOW
        val lava = biome == Biome.LAVA
        val trees = listOf(Pt(60.0, 200.0), Pt(660.0, 165.0), Pt(48.0, 1205.0), Pt(682.0, 1225.0), Pt(150.0, 95.0), Pt(560.0, 105.0))
        for (p in trees) groundLayer.container { position(p.x, p.y) }.treeArt(snowy)
        val rocks = listOf(Pt(702.0, 880.0), Pt(26.0, 470.0), Pt(694.0, 1090.0), Pt(36.0, 760.0))
        for (p in rocks) groundLayer.container { position(p.x, p.y) }.rockArt(lava)
        val bushes = listOf(Pt(110.0, 1245.0), Pt(620.0, 1255.0), Pt(44.0, 1000.0), Pt(676.0, 560.0))
        for (p in bushes) groundLayer.container { position(p.x, p.y) }.bushArt()
        groundLayer.solidRect(W, 70.0, Colors.BLACK) { position(0.0, 0.0); alpha = 0.10; hitTestEnabled = false }
        groundLayer.solidRect(W, 90.0, Colors.BLACK) { position(0.0, H - 90.0); alpha = 0.12; hitTestEnabled = false }
    }

    private fun drawKingdom() {
        kingdomLayer.solidRect(kRight - kLeft, kBottom - kTop, biome.plaza) { position(kLeft, kTop) }
        val wallC = biome.wall; val th = 20.0
        kingdomLayer.solidRect(kRight - kLeft, th, wallC) { position(kLeft, kBottom - th) }
        kingdomLayer.solidRect(gateX - 60.0 - kLeft, th, wallC) { position(kLeft, kTop) }
        kingdomLayer.solidRect(kRight - (gateX + 60.0), th, wallC) { position(gateX + 60.0, kTop) }
        kingdomLayer.solidRect(16.0, 40.0, biome.wallDark) { position(gateX - 60.0, kTop - 6.0) }
        kingdomLayer.solidRect(16.0, 40.0, biome.wallDark) { position(gateX + 44.0, kTop - 6.0) }
        // 좌우 벽 (활성 성문은 틈)
        drawSideWall(kLeft, leftActive(), wallC, biome.wallDark, th)
        drawSideWall(kRight - th, rightActive(), wallC, biome.wallDark, th)
    }

    private fun drawSideWall(x: Double, gate: Boolean, wallC: RGBA, darkC: RGBA, th: Double) {
        if (!gate) {
            kingdomLayer.solidRect(th, kBottom - kTop, wallC) { position(x, kTop) }
        } else {
            kingdomLayer.solidRect(th, sideGateY - kTop, wallC) { position(x, kTop) }
            kingdomLayer.solidRect(th, kBottom - (sideGateY + sideGateH), wallC) { position(x, sideGateY + sideGateH) }
            kingdomLayer.solidRect(th, 14.0, darkC) { position(x, sideGateY - 14.0) }
            kingdomLayer.solidRect(th, 14.0, darkC) { position(x, sideGateY + sideGateH) }
        }
    }

    private fun drawNodes() {
        for (p in nodePts) {
            val pad = worldLayer.container().apply { position(p.x, p.y) }
            pad.solidRect(60.0, 60.0, Colors["#000000"]) { position(-28.0, -24.0); alpha = 0.12 }
            pad.solidRect(60.0, 60.0, Colors["#c9b78c"]) { position(-30.0, -30.0); alpha = 0.85 }
            val plus = pad.text("+", textSize = 40.0, color = Colors["#6b5a32"], font = uiFont) { position(-12.0, -28.0) }
            val node = Node(p.x, p.y, pad, plus, null)
            pad.onClick { onNodeTap(node) }
            nodes.add(node)
        }
    }

    private fun drawGatePad() {
        gatePad = worldLayer.container().apply { position(gateX, gateY + 8.0) }
        gatePrompt = gatePad.container()
        gatePrompt.solidRect(116.0, 30.0, Colors["#c9b78c"]) { position(-58.0, -15.0); alpha = 0.5 }
        gatePrompt.text("방벽 +", textSize = 22.0, color = Colors["#5a4a28"], font = uiFont) { position(-40.0, -14.0) }
        gatePad.onClick { onGateTap() }
    }

    // ---- HUD ----
    private fun buildHud() {
        val bar = stage.container()
        bar.solidRect(W, 96.0, NF_OUTLINE)
        bar.solidRect(W, 90.0, Colors["#16243f"]) { alpha = 0.96 }
        bar.container { position(40.0, 30.0) }.coinIcon(13.0)
        hudGold = bar.text("130", textSize = 32.0, color = Colors["#ffd34d"], font = uiFont) { position(62.0, 14.0) }
        bar.container { position(40.0, 66.0) }.heartIcon(13.0)
        hudLives = bar.text("12", textSize = 32.0, color = Colors["#ff6b6b"], font = uiFont) { position(62.0, 50.0) }
        hudWave = bar.text("WAVE 0", textSize = 32.0, color = Colors.WHITE, font = uiFont) { position(W - 250.0, 12.0) }
        hudPhase = bar.text("낮 · 건설", textSize = 24.0, color = Colors["#cfe3ff"], font = uiFont) { position(W - 250.0, 54.0) }

        actionButton = stage.container().apply { position(W / 2 - 130.0, H - 104.0) }
        actionButton.solidRect(260.0, 80.0, NF_OUTLINE) { position(0.0, 4.0); alpha = 0.5 }
        actionButton.solidRect(260.0, 74.0, NF_OUTLINE)
        actionButton.solidRect(252.0, 66.0, Colors["#2f6fb0"]) { position(4.0, 4.0) }
        actionButton.solidRect(252.0, 20.0, Colors["#3f86d0"]) { position(4.0, 4.0); alpha = 0.6 }
        actionButton.text("밤 시작", textSize = 34.0, color = Colors.WHITE, font = uiFont) { position(82.0, 20.0) }
        actionButton.onClick { if (phase == Phase.DAY) startNight() }
    }

    private fun buildNightDecor() {
        val nd = stage.container().apply { alpha = 0.0; hitTestEnabled = false }
        val stars = listOf(Pt(80.0, 110.0), Pt(180.0, 70.0), Pt(300.0, 130.0), Pt(470.0, 90.0), Pt(600.0, 140.0), Pt(660.0, 80.0), Pt(120.0, 200.0), Pt(540.0, 210.0))
        for (s in stars) nd.circle(2.4, Colors.WHITE) { position(s.x, s.y); alpha = 0.9 }
        val torchPts = listOf(Pt(cx - 86.0, cy - 150.0), Pt(cx + 86.0, cy - 150.0), Pt(gateX - 66.0, gateY), Pt(gateX + 66.0, gateY))
        for (tp in torchPts) {
            val glow = nd.circle(20.0, Colors["#ffb24a"]) { position(tp.x - 20.0, tp.y - 20.0); alpha = 0.0 }
            torches.add(glow)
        }
        nightDecor = nd
    }

    private fun updateAmbient(dt: Double) {
        bannerView?.rotation = Angle.fromDegrees(sin(animTime * 3.0) * 7.0)
        nightDecor?.alpha = nightAlpha
        for ((i, t) in torches.withIndex()) t.alpha = 0.45 + 0.45 * sin(animTime * 5.0 + i * 1.3)
        if (shake > 0.0) {
            shake -= dt
            val m = (shake / 0.35).coerceIn(0.0, 1.0) * 8.0
            worldLayer.position((Random.nextDouble() - 0.5) * m, (Random.nextDouble() - 0.5) * m)
        } else {
            worldLayer.position(0.0, 0.0)
        }
    }

    private fun updateHud() {
        hudGold.text = "$gold"
        hudLives.text = "$lives"
        hudWave.text = "WAVE $wave"
        hudPhase.text = when (phase) {
            Phase.DAY -> "낮·건설 ${biome.label} 성Lv$castleLevel"
            Phase.NIGHT -> if (bossPending || enemies.any { it.isBoss }) "밤·보스!" else "밤 · 방어"
            Phase.GAMEOVER -> "게임 오버"
        }
        actionButton.visible = phase == Phase.DAY
        gatePrompt.visible = phase == Phase.DAY && barricade == null
    }

    // ---- 건설/업그레이드 메뉴 ----
    private fun onNodeTap(node: Node) {
        if (phase != Phase.DAY) return
        if (node.building == null) openBuildMenu(node) else openUpgradeMenu(node)
    }

    private fun onGateTap() { if (phase == Phase.DAY) openBarricadeMenu() }

    private fun panel(title: String, h: Double): Container {
        closeMenu()
        val menu = stage.container()
        menu.solidRect(W, H, Colors["#000000"]) { alpha = 0.45; onClick { closeMenu() } }
        val pw = 600.0; val px = (W - pw) / 2; val py = H - h - 130.0
        menu.solidRect(pw, h, Colors["#1d2a44"]) { position(px, py) }
        menu.text(title, textSize = 28.0, color = Colors.WHITE, font = uiFont) { position(px + 22.0, py + 14.0) }
        menuLayer = menu
        return menu
    }

    private fun openBuildMenu(node: Node) {
        val menu = panel("건물 선택", 230.0)
        val py = H - 230.0 - 130.0; val px = (W - 600.0) / 2
        val cardW = 170.0; val gap = 30.0; var bx = px + 30.0
        for (b in Build.entries) {
            val affordable = gold >= b.cost
            val card = menu.container().apply { position(bx, py + 64.0) }
            card.solidRect(cardW, 140.0, if (affordable) Colors["#33507f"] else Colors["#2a3550"])
            card.solidRect(64.0, 64.0, b.color) { position(cardW / 2 - 32.0, 14.0) }
            card.text(b.icon, textSize = 30.0, color = Colors.WHITE, font = uiFont) { position(cardW / 2 - 16.0, 28.0) }
            card.text(b.label, textSize = 26.0, color = Colors.WHITE, font = uiFont) { position(cardW / 2 - 34.0, 84.0) }
            card.text("${b.cost}", textSize = 24.0, color = Colors["#ffd34d"], font = uiFont) { position(cardW / 2 - 18.0, 112.0) }
            if (affordable) card.onClick { build(node, b) }
            bx += cardW + gap
        }
    }

    private fun openUpgradeMenu(node: Node) {
        val b = node.building ?: return
        val menu = panel("${b.type.label}  Lv${b.level}", 220.0)
        val px = (W - 600.0) / 2; val py = H - 220.0 - 130.0
        menu.text(statLine(b), textSize = 24.0, color = Colors["#cfe3ff"], font = uiFont) { position(px + 22.0, py + 58.0) }
        if (b.level < maxLevel) {
            val cost = upgradeCost(b.type, b.level); val ok = gold >= cost
            val up = menu.container().apply { position(px + 30.0, py + 104.0) }
            up.solidRect(330.0, 76.0, if (ok) Colors["#2f8f4f"] else Colors["#2a3550"])
            up.text("업그레이드  $cost", textSize = 28.0, color = Colors.WHITE, font = uiFont) { position(34.0, 22.0) }
            if (ok) up.onClick { upgrade(b) }
        } else {
            menu.text("최대 레벨", textSize = 26.0, color = Colors["#9fe0a8"], font = uiFont) { position(px + 40.0, py + 118.0) }
        }
        val sell = menu.container().apply { position(px + 30.0 + 350.0, py + 104.0) }
        sell.solidRect(190.0, 76.0, Colors["#8a3b3b"])
        sell.text("철거 +${b.invested / 2}", textSize = 24.0, color = Colors.WHITE, font = uiFont) { position(22.0, 24.0) }
        sell.onClick { sell(node, b) }
    }

    private fun openBarricadeMenu() {
        val cur = barricade
        val menu = panel(if (cur == null) "성문 방벽 건설" else "성문 방벽  Lv${cur.level}", 200.0)
        val px = (W - 600.0) / 2; val py = H - 200.0 - 130.0
        val cost = if (cur == null) barricadeBaseCost else barricadeUpCost(cur.level)
        val label = if (cur == null) "건설  $cost" else "보강  $cost"
        if (cur != null) menu.text("내구도 ${cur.maxHp.toInt()}", textSize = 24.0, color = Colors["#cfe3ff"], font = uiFont) { position(px + 22.0, py + 56.0) }
        if (cur == null || cur.level < 6) {
            val ok = gold >= cost
            val btn = menu.container().apply { position(px + 30.0, py + 100.0) }
            btn.solidRect(540.0, 72.0, if (ok) Colors["#2f8f4f"] else Colors["#2a3550"])
            btn.text(label, textSize = 28.0, color = Colors.WHITE, font = uiFont) { position(210.0, 20.0) }
            if (ok) btn.onClick { buildOrUpgradeBarricade() }
        } else {
            menu.text("최대 레벨", textSize = 26.0, color = Colors["#9fe0a8"], font = uiFont) { position(px + 230.0, py + 120.0) }
        }
    }

    private fun statLine(b: Building): String = when (b.type) {
        Build.TOWER -> "공격력 ${towerDmg(b.level).toInt()}  사거리 ${towerRange(b.level).toInt()}"
        Build.BARRACKS -> "병사 최대 ${barracksMax(b.level)}  체력 ${soldierHp(b.level).toInt()}"
        Build.HOUSE -> "아침 수입 +${houseOutput(b.level)}"
    }

    private fun closeMenu() { menuLayer?.removeFromParent(); menuLayer = null }

    private fun build(node: Node, type: Build) {
        if (gold < type.cost || node.building != null) return
        gold -= type.cost
        placeBuilding(node, type, 1, type.cost)
        closeMenu(); persist(); updateHud()
    }

    private fun placeBuilding(node: Node, type: Build, level: Int, invested: Int) {
        node.plus.visible = false
        val v = node.pad.container()
        drawBuilding(v, type)
        val badge = v.text(if (level > 1) "Lv$level" else "", textSize = 20.0, color = Colors.WHITE, font = uiFont) { position(14.0, -44.0) }
        val s = 1.0 + 0.04 * (level - 1); v.scaleX = s; v.scaleY = s
        node.building = Building(type, v, badge, node.x, node.y, 0.0, level, invested)
    }

    private fun drawBuilding(v: Container, type: Build) {
        when (type) {
            Build.TOWER -> v.towerArt()
            Build.BARRACKS -> v.barracksArt()
            Build.HOUSE -> v.houseArt()
        }
    }

    private fun upgrade(b: Building) {
        val cost = upgradeCost(b.type, b.level)
        if (gold < cost || b.level >= maxLevel) return
        gold -= cost; b.invested += cost; b.level++
        b.badge.text = "Lv${b.level}"
        val s = 1.0 + 0.04 * (b.level - 1); b.view.scaleX = s; b.view.scaleY = s
        closeMenu(); persist(); updateHud()
    }

    private fun sell(node: Node, b: Building) {
        gold += b.invested / 2
        b.view.removeFromParent()
        soldiers.filter { it.owner === b }.forEach { removeSoldier(it) }
        node.building = null; node.plus.visible = true
        closeMenu(); persist(); updateHud()
    }

    private fun buildOrUpgradeBarricade() {
        val cur = barricade
        if (cur == null) {
            if (gold < barricadeBaseCost) return
            gold -= barricadeBaseCost; barricade = makeBarricade(1)
        } else {
            val cost = barricadeUpCost(cur.level)
            if (gold < cost || cur.level >= 6) return
            gold -= cost; cur.view.removeFromParent(); barricade = makeBarricade(cur.level + 1)
        }
        closeMenu(); persist(); updateHud()
    }

    private fun makeBarricade(level: Int): Barricade {
        val hp = barricadeMaxHp(level)
        val v = gatePad.container().apply { position(0.0, -2.0) }
        val bar = v.barricadeArt()
        return Barricade(v, bar, hp, hp, level)
    }

    // ---- 밤/낮 ----
    private fun startNight() {
        closeMenu()
        phase = Phase.NIGHT
        spawnRemaining = 6 + wave * 2
        spawnTimer = 0.0
        spawnInterval = (0.8 - wave * 0.025).coerceAtLeast(0.32)
        bossPending = (wave + 1) % 5 == 0
        updateHud()
    }

    private fun endNight() {
        wave += 1
        val houseGold = nodes.mapNotNull { it.building }.filter { it.type == Build.HOUSE }.sumOf { houseOutput(it.level) }
        gold += 12 + houseGold
        clearSoldiers()
        barricade?.let { it.hp = it.maxHp; it.hpBar.scaleX = 1.0 }
        if (wave % 5 == 0) { castleLevel++; lives += 3 }
        val nb = biomeFor(wave); if (nb != biome) applyBiome(nb)
        phase = Phase.DAY
        persist()
        updateHud()
    }

    private fun gameOver() {
        phase = Phase.GAMEOVER
        clearSave()
        updateHud()
        val o = stage.container()
        o.solidRect(W, H, Colors["#000000"]) { alpha = 0.65 }
        o.text("게임 오버", textSize = 66.0, color = Colors.WHITE, font = uiFont) { position(W / 2 - 132.0, H / 2 - 120.0) }
        o.text("WAVE $wave 까지 방어했습니다", textSize = 30.0, color = Colors["#cfe3ff"], font = uiFont) { position(W / 2 - 200.0, H / 2 - 26.0) }
        val btn = o.container().apply { position(W / 2 - 110.0, H / 2 + 50.0) }
        btn.solidRect(220.0, 72.0, Colors["#2f6fb0"])
        btn.text("다시 시작", textSize = 32.0, color = Colors.WHITE, font = uiFont) { position(48.0, 18.0) }
        btn.onClick { restart() }
        overlay = o
    }

    private fun restart() {
        for (e in enemies) e.view.removeFromParent()
        for (p in projectiles) p.view.removeFromParent()
        enemies.clear(); projectiles.clear()
        for (p in particles) p.view.removeFromParent(); particles.clear()
        for (f in floaters) f.view.removeFromParent(); floaters.clear()
        shake = 0.0; worldLayer.position(0.0, 0.0)
        clearSoldiers()
        for (n in nodes) { n.building?.view?.removeFromParent(); n.building = null; n.plus.visible = true }
        barricade?.view?.removeFromParent(); barricade = null
        overlay?.removeFromParent(); overlay = null
        gold = 130; lives = 12; wave = 0; castleLevel = 1; bossPending = false
        if (biome != Biome.GRASS) applyBiome(Biome.GRASS)
        phase = Phase.DAY
        clearSave()
        updateHud()
    }

    private fun clearSoldiers() { for (s in soldiers) s.view.removeFromParent(); soldiers.clear() }

    // ---- 루프 ----
    private fun update(dt: Double) {
        if (dt <= 0.0) return
        animTime += dt
        val target = if (phase == Phase.NIGHT) 0.5 else 0.0
        nightAlpha += (target - nightAlpha) * (dt * 2.5).coerceAtMost(1.0)
        nightTint.alpha = nightAlpha
        updateAmbient(dt)
        updateParticles(dt)
        updateFloaters(dt)
        if (phase != Phase.NIGHT) return

        if (spawnRemaining > 0 || bossPending) {
            spawnTimer -= dt
            if (spawnTimer <= 0.0) {
                if (bossPending && spawnRemaining <= (6 + wave * 2) / 2) { spawnBoss(); bossPending = false; updateHud() }
                else if (spawnRemaining > 0) { spawnEnemy(); spawnRemaining-- }
                spawnTimer = spawnInterval
            }
        }
        updateSoldiers(dt)
        updateEnemies(dt)
        fireBuildings(dt)
        moveProjectiles(dt)
        if (spawnRemaining == 0 && !bossPending && enemies.isEmpty()) endNight()
    }

    private fun spawnEnemy() {
        val lane = activeLanes().random()
        val hp = 100.0 * 1.12.pow(wave)
        val speed = 56.0 + wave * 3.0
        val offset = (-50..50).random().toDouble()
        val sp = lane.path[0]
        val art = worldLayer.enemyArt(enemyR)
        art.view.position(sp.x + offset, sp.y)
        enemies.add(Enemy(art.view, art.body, art.hpBar, sp.x + offset, sp.y, hp, hp, speed, enemyR, lane.path, lane.top, false, 1, offset, (0..628).random() / 100.0, 0.0))
    }

    private fun spawnBoss() {
        val r = 34.0
        val hp = 100.0 * 1.12.pow(wave) * 18.0
        val speed = 40.0 + wave * 1.5
        val sp = laneTop[0]
        val art = worldLayer.bossArt(r)
        art.view.position(sp.x, sp.y)
        enemies.add(Enemy(art.view, art.body, art.hpBar, sp.x, sp.y, hp, hp, speed, r, laneTop, true, true, 1, 0.0, 0.0, 0.0))
    }

    private fun updateEnemies(dt: Double) {
        for (e in enemies.toList()) {
            val foe = soldiers.filter { hypot(it.x - e.x, it.y - e.y) <= engageRange + e.r }.minByOrNull { hypot(it.x - e.x, it.y - e.y) }
            if (foe != null) {
                e.atkCd -= dt
                if (e.atkCd <= 0.0) {
                    foe.hp -= enemyAtk * if (e.isBoss) 2.0 else 1.0; e.atkCd = enemyAtkInt
                    foe.hpBar.scaleX = (foe.hp / foe.maxHp).coerceIn(0.0, 1.0)
                    if (foe.hp <= 0.0) removeSoldier(foe)
                }
            } else {
                val bar = barricade
                if (bar != null && e.top && e.seg >= 3) {
                    e.atkCd -= dt
                    if (e.atkCd <= 0.0) {
                        bar.hp -= enemyAtk * if (e.isBoss) 3.0 else 1.0; e.atkCd = enemyAtkInt
                        bar.hpBar.scaleX = (bar.hp / bar.maxHp).coerceIn(0.0, 1.0)
                        if (bar.hp <= 0.0) { bar.view.removeFromParent(); barricade = null }
                    }
                } else {
                    val tgt = e.path.getOrNull(e.seg)
                    if (tgt != null) {
                        val tx = tgt.x + if (e.seg < e.path.size - 1) e.offset else 0.0
                        val dx = tx - e.x; val dy = tgt.y - e.y; val d = hypot(dx, dy); val step = e.speed * dt
                        if (d <= step) { e.x = tx; e.y = tgt.y; e.seg++ } else { e.x += dx / d * step; e.y += dy / d * step }
                    } else {
                        lives -= if (e.isBoss) 5 else 1; removeEnemy(e); updateHud()
                        if (lives <= 0) { gameOver(); return }
                        continue
                    }
                }
            }
            e.bob += dt * 9.0
            e.view.position(e.x, e.y)
            e.body.position(0.0, sin(e.bob) * 3.0)
        }
    }

    private fun updateSoldiers(dt: Double) {
        for (s in soldiers.toList()) {
            val target = enemies.minByOrNull { hypot(it.x - s.x, it.y - s.y) } ?: continue
            val d = hypot(target.x - s.x, target.y - s.y)
            if (d <= meleeRange + target.r) {
                s.atkCd -= dt
                if (s.atkCd <= 0.0) {
                    target.hp -= s.atk; s.atkCd = 0.55
                    target.hpBar.scaleX = (target.hp / target.maxHp).coerceIn(0.0, 1.0)
                    if (target.hp <= 0.0) { onEnemyKilled(target) }
                }
            } else {
                val step = soldierSpeed * dt
                s.x += (target.x - s.x) / d * step; s.y += (target.y - s.y) / d * step
                s.view.position(s.x, s.y)
                s.body.position(0.0, sin(animTime * 10.0 + s.x) * 2.0)
            }
        }
    }

    private fun fireBuildings(dt: Double) {
        for (n in nodes) {
            val b = n.building ?: continue
            when (b.type) {
                Build.TOWER -> {
                    b.cd -= dt
                    if (b.cd > 0.0) continue
                    val range = towerRange(b.level)
                    val t = enemies.filter { hypot(it.x - b.x, it.y - b.y) <= range }.minByOrNull { hypot(it.x - b.x, it.y - b.y) } ?: continue
                    b.cd = towerFire(b.level)
                    val pv = worldLayer.arrowArt()
                    pv.position(b.x, b.y - 30.0)
                    projectiles.add(Proj(pv, b.x, b.y - 30.0, t, towerDmg(b.level)))
                }
                Build.BARRACKS -> {
                    b.cd -= dt
                    val owned = soldiers.count { it.owner === b }
                    if (b.cd <= 0.0 && owned < barracksMax(b.level)) { b.cd = barracksProduce(b.level); spawnSoldier(b) }
                }
                Build.HOUSE -> {}
            }
        }
    }

    private fun spawnSoldier(owner: Building) {
        val sx = owner.x; val sy = owner.y - 36.0
        val hp = soldierHp(owner.level)
        val art = worldLayer.soldierArt(soldierR)
        art.view.position(sx, sy)
        soldiers.add(Soldier(art.view, art.body, art.hpBar, sx, sy, hp, hp, soldierAtk(owner.level), 0.0, owner))
    }

    private fun moveProjectiles(dt: Double) {
        for (p in projectiles.toList()) {
            if (p.target !in enemies) { removeProjectile(p); continue }
            val dx = p.target.x - p.x; val dy = p.target.y - p.y; val d = hypot(dx, dy); val step = projSpeed * dt
            p.view.rotation = Angle.fromRadians(atan2(dy, dx))
            if (d <= step + 8.0) {
                p.target.hp -= p.dmg
                p.target.hpBar.scaleX = (p.target.hp / p.target.maxHp).coerceIn(0.0, 1.0)
                spawnBurst(p.x, p.y, Colors["#ffe066"], 4, 110.0, 3.5)
                removeProjectile(p)
                if (p.target.hp <= 0.0) onEnemyKilled(p.target)
                continue
            }
            p.x += dx / d * step; p.y += dy / d * step
            p.view.position(p.x, p.y)
        }
    }

    private fun onEnemyKilled(e: Enemy) {
        val reward = if (e.isBoss) 60 + wave * 6 else killReward
        gold += reward
        spawnFloater(e.x, e.y - e.r, "+$reward")
        if (e.isBoss) { spawnBurst(e.x, e.y, Colors["#b06bff"], 22, 240.0, 9.0); shake = 0.35 }
        else spawnBurst(e.x, e.y, Colors["#ff7a3c"], 9, 170.0, 6.0)
        removeEnemy(e); updateHud()
    }

    // ---- 파티클 VFX ----
    private fun spawnBurst(x: Double, y: Double, color: RGBA, count: Int, spd: Double, rad: Double) {
        repeat(count) {
            val ang = Random.nextDouble(0.0, 6.2832)
            val s = spd * Random.nextDouble(0.4, 1.0)
            val pv = worldLayer.circle(rad, color) { position(x - rad, y - rad) }
            particles.add(Particle(pv, x, y, cos(ang) * s, sin(ang) * s, 0.45, 0.45, rad))
        }
    }

    private fun updateParticles(dt: Double) {
        for (p in particles.toList()) {
            p.life -= dt
            if (p.life <= 0.0) { p.view.removeFromParent(); particles.remove(p); continue }
            p.x += p.vx * dt; p.y += p.vy * dt
            p.view.alpha = p.life / p.maxLife
            p.view.position(p.x - p.r, p.y - p.r)
        }
    }

    private fun spawnFloater(x: Double, y: Double, label: String) {
        val t = worldLayer.text(label, textSize = 26.0, color = Colors["#ffe066"], font = uiFont) { position(x - 16.0, y) }
        floaters.add(Floater(t, y, 0.9))
    }

    private fun updateFloaters(dt: Double) {
        for (f in floaters.toList()) {
            f.life -= dt
            if (f.life <= 0.0) { f.view.removeFromParent(); floaters.remove(f); continue }
            f.y -= dt * 40.0
            f.view.y = f.y
            f.view.alpha = (f.life / 0.9).coerceIn(0.0, 1.0)
        }
    }

    private fun removeEnemy(e: Enemy) { e.view.removeFromParent(); enemies.remove(e) }
    private fun removeSoldier(s: Soldier) { s.view.removeFromParent(); soldiers.remove(s) }
    private fun removeProjectile(p: Proj) { p.view.removeFromParent(); projectiles.remove(p) }

    // ---- 저장/복원 (SharedPreferences 브리지) ----
    private fun persist() {
        val parts = nodes.mapIndexedNotNull { i, n -> n.building?.let { "$i:${it.type.name}:${it.level}:${it.invested}" } }
        val s = "v1;$gold;$lives;$wave;$castleLevel;${barricade?.level ?: 0};${parts.joinToString(",")}"
        NightfallSave.saver?.invoke(s)
    }

    private fun clearSave() { NightfallSave.saver?.invoke("") }

    private fun restore() {
        val s = NightfallSave.loader?.invoke()
        if (s.isNullOrBlank() || !s.startsWith("v1;")) return
        try {
            val seg = s.split(";")
            gold = seg[1].toInt(); lives = seg[2].toInt(); wave = seg[3].toInt(); castleLevel = seg[4].toInt()
            val barLvl = seg[5].toInt()
            applyBiome(biomeFor(wave))
            if (barLvl > 0) barricade = makeBarricade(barLvl)
            val blds = if (seg.size > 6) seg[6] else ""
            if (blds.isNotBlank()) for (item in blds.split(",")) {
                val f = item.split(":")
                val idx = f[0].toInt(); val type = Build.valueOf(f[1]); val lvl = f[2].toInt(); val inv = f[3].toInt()
                if (idx in nodes.indices) placeBuilding(nodes[idx], type, lvl, inv)
            }
        } catch (e: Exception) {
            // 손상된 저장 데이터는 무시하고 새 게임 시작
        }
    }
}
