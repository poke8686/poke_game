package com.poke86.game.game.nightfall

import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.math.geom.Size

/**
 * Korge 엔진(순수 Kotlin)과 안드로이드 저장소(SharedPreferences) 사이의 브리지.
 * [com.poke86.game.ui.games.nightfall.NightfallScreen] 이 람다를 주입한다.
 */
object NightfallSave {
    var saver: ((String) -> Unit)? = null
    var loader: (() -> String?)? = null
}

/**
 * Nightfall(왕국 국경 TD) Korge 엔진 진입점 — 순수 게임 코드(안드로이드/Compose 비의존).
 *
 * [com.poke86.game.ui.games.nightfall.NightfallScreen] 의 KorgeAndroidView 가 [config] 를 로드한다.
 * 실제 게임 로직은 [NightfallWorld] 에 위치한다.
 */
object NightfallKorge {

    private val VIRTUAL = Size(720, 1280)

    /** [korlibs.korge.android.KorgeAndroidView.loadModule] 에 전달할 설정(KorgeConfig = Korge). */
    fun config(): Korge = Korge(
        windowSize = VIRTUAL,
        virtualSize = VIRTUAL,
        backgroundColor = Colors["#4c9a4c"],
        main = { NightfallWorld(this).setup() },
    )
}
