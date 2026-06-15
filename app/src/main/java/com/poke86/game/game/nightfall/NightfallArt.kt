package com.poke86.game.game.nightfall

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.circle
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.math.geom.Angle

/**
 * Nightfall 플랫-카툰 아트 헬퍼 — 전부 순수 Korge 도형으로 직접 그린 오리지널 아트.
 * 외부 스프라이트/에셋을 쓰지 않으므로 NightfallWorld 의 게임 로직과 독립적이며 가볍다.
 *
 * 공통 스타일: 어두운 아웃라인 + 바닥 그림자 + 하이라이트(플랫 셰이딩).
 */

internal val NF_OUTLINE = Colors["#241d2e"]

/** 유닛 스프라이트 묶음: view(=위치 컨테이너), body(=바브 애니메이션 대상), hpBar(=scaleX 체력바). */
internal class UnitArt(val view: Container, val body: Container, val hpBar: SolidRect)

/** 바닥에 깔리는 납작한 타원 그림자. 래퍼를 세로로 눌러 중심 고정. */
internal fun Container.dropShadow(rx: Double, ry: Double, cy: Double = 0.0, a: Double = 0.20) {
    container {
        position(0.0, cy)
        scaleY = ry / rx
        circle(rx, Colors.BLACK) {
            position(-rx, -rx)
            alpha = a
            hitTestEnabled = false
        }
    }
}

/** 외곽선이 있는 원(아웃라인 + 채움). */
private fun Container.outlinedCircle(r: Double, fill: RGBA, ox: Double = 0.0, oy: Double = 0.0, line: Double = 2.5) {
    circle(r + line, NF_OUTLINE) { position(ox - r - line, oy - r - line) }
    circle(r, fill) { position(ox - r, oy - r) }
}

/** 체력바(어두운 트랙 + 초록 게이지). 반환값은 scaleX 로 깎을 게이지. */
private fun Container.hpBar(w: Double, topY: Double): SolidRect {
    solidRect(w, 5.0, NF_OUTLINE) { position(-w / 2, topY); alpha = 0.65 }
    return solidRect(w, 5.0, Colors["#46d36a"]) { position(-w / 2, topY) }
}

// ---------------------------------------------------------------- 적/보스

internal fun Container.enemyArt(r: Double): UnitArt {
    val view = container()
    view.dropShadow(r, r * 0.42, r * 0.85)
    val body = view.container()
    body.outlinedCircle(r, Colors["#cf4b4b"])
    body.circle(r * 0.5, Colors["#e0796a"]) { position(-r * 0.5, -r * 0.25); alpha = 0.55 }
    body.solidRect(4.0, 7.0, NF_OUTLINE) { position(-r * 0.6, -r - 2.0) }
    body.solidRect(4.0, 7.0, NF_OUTLINE) { position(r * 0.6 - 4.0, -r - 2.0) }
    body.circle(3.4, Colors.WHITE) { position(-7.5, -3.0) }
    body.circle(3.4, Colors.WHITE) { position(1.0, -3.0) }
    body.circle(1.7, NF_OUTLINE) { position(-6.2, -2.0) }
    body.circle(1.7, NF_OUTLINE) { position(2.3, -2.0) }
    val hp = view.hpBar(r * 2, -r - 17.0)
    return UnitArt(view, body, hp)
}

internal fun Container.bossArt(r: Double): UnitArt {
    val view = container()
    view.dropShadow(r * 1.05, r * 0.45, r * 0.9, 0.26)
    val body = view.container()
    body.circle(r + 10.0, Colors["#b06bff"]) { position(-(r + 10.0), -(r + 10.0)); alpha = 0.18 }
    body.outlinedCircle(r, Colors["#7d3cc9"], line = 3.0)
    body.circle(r * 0.55, Colors["#9a5fe0"]) { position(-r * 0.55, -r * 0.3); alpha = 0.5 }
    body.solidRect(r * 1.1, 10.0, Colors["#ffd34d"]) { position(-r * 0.55, -r - 12.0) }
    body.solidRect(5.0, 9.0, Colors["#ffd34d"]) { position(-r * 0.5, -r - 20.0) }
    body.solidRect(5.0, 11.0, Colors["#ffd34d"]) { position(-2.5, -r - 22.0) }
    body.solidRect(5.0, 9.0, Colors["#ffd34d"]) { position(r * 0.5 - 5.0, -r - 20.0) }
    body.circle(4.2, Colors["#ffec99"]) { position(-9.0, -3.0) }
    body.circle(4.2, Colors["#ffec99"]) { position(1.0, -3.0) }
    body.circle(2.0, NF_OUTLINE) { position(-7.5, -2.0) }
    body.circle(2.0, NF_OUTLINE) { position(2.5, -2.0) }
    val hp = view.hpBar(r * 2, -r - 20.0)
    return UnitArt(view, body, hp)
}

// ---------------------------------------------------------------- 아군 병사

internal fun Container.soldierArt(r: Double): UnitArt {
    val view = container()
    view.dropShadow(r * 0.95, r * 0.4, r * 0.8, 0.18)
    val body = view.container()
    body.outlinedCircle(r, Colors["#3f7bd0"], line = 2.0)
    body.circle(r * 0.9, Colors["#cdd7e6"]) { position(-r * 0.9, -r * 1.1) }
    body.solidRect(r * 1.5, 4.0, Colors["#9fb0c8"]) { position(-r * 0.75, -r * 0.25) }
    body.circle(r * 0.5, Colors["#ffd34d"]) { position(r * 0.3, -r * 0.1) }
    val hp = view.hpBar(r * 2, -r - 13.0)
    return UnitArt(view, body, hp)
}

// ---------------------------------------------------------------- 건물(노드 위에 그림)

/** 화살탑 */
internal fun Container.towerArt() {
    dropShadow(30.0, 13.0, 22.0)
    solidRect(56.0, 60.0, NF_OUTLINE) { position(-28.0, -36.0) }
    solidRect(50.0, 54.0, Colors["#8a8f98"]) { position(-25.0, -33.0) }
    solidRect(50.0, 10.0, Colors["#6b7079"]) { position(-25.0, -33.0) }
    solidRect(12.0, 18.0, NF_OUTLINE) { position(-6.0, -14.0) }
    for (dx in intArrayOf(-25, -9, 7)) solidRect(12.0, 12.0, Colors["#6b7079"]) { position(dx.toDouble(), -46.0) }
    solidRect(3.0, 22.0, NF_OUTLINE) { position(-1.5, -66.0) }
    solidRect(20.0, 13.0, Colors["#c0392b"]) { position(1.5, -64.0) }
}

/** 막사 */
internal fun Container.barracksArt() {
    dropShadow(34.0, 14.0, 20.0)
    solidRect(64.0, 52.0, NF_OUTLINE) { position(-32.0, -30.0) }
    solidRect(58.0, 46.0, Colors["#b5793a"]) { position(-29.0, -27.0) }
    solidRect(70.0, 18.0, NF_OUTLINE) { position(-35.0, -44.0) }
    solidRect(64.0, 14.0, Colors["#8a5a28"]) { position(-32.0, -42.0) }
    solidRect(20.0, 26.0, NF_OUTLINE) { position(-10.0, -3.0) }
    solidRect(14.0, 22.0, Colors["#5e4426"]) { position(-7.0, -1.0) }
    solidRect(4.0, 18.0, Colors["#dfe6ee"]) { position(13.0, -22.0) }
    solidRect(12.0, 4.0, Colors["#9aa3ad"]) { position(9.0, -10.0) }
}

/** 집 */
internal fun Container.houseArt() {
    dropShadow(30.0, 13.0, 18.0)
    solidRect(58.0, 46.0, NF_OUTLINE) { position(-29.0, -24.0) }
    solidRect(52.0, 40.0, Colors["#caa46a"]) { position(-26.0, -21.0) }
    solidRect(66.0, 20.0, NF_OUTLINE) { position(-33.0, -38.0) }
    solidRect(60.0, 16.0, Colors["#9c5b3a"]) { position(-30.0, -36.0) }
    solidRect(10.0, 16.0, NF_OUTLINE) { position(12.0, -46.0) }
    solidRect(16.0, 22.0, Colors["#6e4a2a"]) { position(-8.0, -3.0) }
    solidRect(12.0, 12.0, Colors["#f2d68a"]) { position(8.0, -16.0) }
}

// ---------------------------------------------------------------- 성

/** 중앙 성을 (cx,cy) 에 그린다. 반환값은 흔들리는 깃발 컨테이너. */
internal fun Container.castleArt(cx: Double, cy: Double): Container {
    val c = container { position(cx, cy) }
    c.dropShadow(104.0, 42.0, 44.0, 0.22)
    for (sx in doubleArrayOf(-78.0, 50.0)) {
        c.solidRect(34.0, 130.0, NF_OUTLINE) { position(sx - 2.0, -76.0) }
        c.solidRect(30.0, 126.0, Colors["#8a7f6c"]) { position(sx, -74.0) }
        for (dx in 0..1) c.solidRect(10.0, 12.0, Colors["#6f6657"]) { position(sx + dx * 18.0, -84.0) }
    }
    c.solidRect(154.0, 150.0, NF_OUTLINE) { position(-77.0, -62.0) }
    c.solidRect(148.0, 144.0, Colors["#9a8f7a"]) { position(-74.0, -59.0) }
    for (dx in -72..60 step 22) c.solidRect(14.0, 16.0, Colors["#7d7464"]) { position(dx.toDouble(), -70.0) }
    c.solidRect(70.0, 100.0, NF_OUTLINE) { position(-35.0, -106.0) }
    c.solidRect(64.0, 96.0, Colors["#a89c86"]) { position(-32.0, -104.0) }
    c.container {
        position(0.0, -108.0)
        rotation = Angle.fromDegrees(45.0)
        solidRect(58.0, 58.0, NF_OUTLINE) { position(-29.0, -29.0) }
        solidRect(48.0, 48.0, Colors["#b23b3b"]) { position(-24.0, -24.0) }
    }
    for (wy in intArrayOf(-30, 6)) for (wx in intArrayOf(-44, 30)) {
        c.solidRect(18.0, 24.0, NF_OUTLINE) { position(wx.toDouble(), wy.toDouble()) }
        c.solidRect(12.0, 18.0, Colors["#f4d987"]) { position(wx + 3.0, wy + 3.0) }
    }
    c.solidRect(40.0, 54.0, NF_OUTLINE) { position(-20.0, 36.0) }
    c.solidRect(32.0, 48.0, Colors["#574a36"]) { position(-16.0, 40.0) }
    c.solidRect(3.0, 40.0, NF_OUTLINE) { position(-1.5, -188.0) }
    val banner = c.container { position(0.0, -184.0) }
    banner.solidRect(34.0, 22.0, NF_OUTLINE) { position(2.0, -2.0) }
    banner.solidRect(28.0, 16.0, Colors["#2f6fb0"]) { position(4.0, 0.0) }
    return banner
}

// ---------------------------------------------------------------- 성문 방벽(받는 컨테이너에 그림)

/** 받는 컨테이너에 나무 방벽을 그리고 체력바를 반환. */
internal fun Container.barricadeArt(): SolidRect {
    solidRect(120.0, 26.0, NF_OUTLINE) { position(-60.0, -13.0) }
    solidRect(114.0, 20.0, Colors["#6f675b"]) { position(-57.0, -10.0) }
    for (dx in -54..40 step 24) {
        solidRect(6.0, 20.0, Colors["#564f45"]) { position(dx.toDouble(), -10.0) }
        solidRect(10.0, 8.0, NF_OUTLINE) { position(dx.toDouble() - 2.0, -18.0) }
    }
    return solidRect(110.0, 5.0, Colors["#46d36a"]) { position(-55.0, -24.0) }
}

// ---------------------------------------------------------------- 화살(투사체)

internal fun Container.arrowArt(): Container {
    val a = container()
    a.solidRect(18.0, 4.0, Colors["#6b4a2a"]) { position(-9.0, -2.0) }
    a.solidRect(7.0, 7.0, Colors["#e7e2d6"]) { position(7.0, -3.5) }
    a.solidRect(5.0, 6.0, Colors["#c0392b"]) { position(-10.0, -3.0) }
    return a
}

// ---------------------------------------------------------------- 지형 장식

internal fun Container.treeArt(snowy: Boolean) {
    dropShadow(20.0, 9.0, 16.0, 0.18)
    solidRect(8.0, 18.0, Colors["#6b4a2a"]) { position(-4.0, 0.0) }
    val leaf = if (snowy) Colors["#dfe9ef"] else Colors["#3f8a45"]
    val leafD = if (snowy) Colors["#c2d2db"] else Colors["#357a3b"]
    circle(18.0, NF_OUTLINE) { position(-18.0, -34.0); alpha = 0.9 }
    circle(16.0, leaf) { position(-16.0, -32.0) }
    circle(11.0, leafD) { position(-3.0, -24.0); alpha = 0.6 }
}

internal fun Container.rockArt(lava: Boolean) {
    dropShadow(16.0, 7.0, 8.0, 0.16)
    val base = if (lava) Colors["#3a2a22"] else Colors["#9aa0a6"]
    circle(15.0, NF_OUTLINE) { position(-15.0, -12.0) }
    circle(13.0, base) { position(-13.0, -10.0) }
    if (lava) circle(5.0, Colors["#ff7a3c"]) { position(-3.0, -6.0); alpha = 0.8 }
}

internal fun Container.bushArt() {
    circle(12.0, NF_OUTLINE) { position(-12.0, -10.0) }
    circle(10.0, Colors["#3f8a45"]) { position(-10.0, -8.0) }
    circle(7.0, Colors["#4f9d4f"]) { position(0.0, -6.0) }
}

// ---------------------------------------------------------------- HUD 아이콘

internal fun Container.coinIcon(r: Double = 13.0) {
    circle(r + 2.0, NF_OUTLINE) { position(-(r + 2.0), -(r + 2.0)) }
    circle(r, Colors["#ffd34d"]) { position(-r, -r) }
    circle(r * 0.6, Colors["#ffe98a"]) { position(-r * 0.6, -r * 0.6); alpha = 0.8 }
}

internal fun Container.heartIcon(s: Double = 12.0) {
    circle(s * 0.5, Colors["#ff6b6b"]) { position(-s * 0.55, -s * 0.5) }
    circle(s * 0.5, Colors["#ff6b6b"]) { position(s * 0.05, -s * 0.5) }
    container {
        rotation = Angle.fromDegrees(45.0)
        solidRect(s * 0.8, s * 0.8, Colors["#ff6b6b"]) { position(-s * 0.4, -s * 0.4) }
    }
}
