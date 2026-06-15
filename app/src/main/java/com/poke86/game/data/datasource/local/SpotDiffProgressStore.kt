package com.poke86.game.data.datasource.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.spotDiffDataStore by preferencesDataStore(name = "spot_diff_progress")

/**
 * 틀린그림찾기 스테이지 진행도.
 * clearedOrder: 마지막으로 클리어한 order 값 (1~N). 0 = 아무것도 클리어 안 함.
 */
@Singleton
class SpotDiffProgressStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val CLEARED_ORDER = intPreferencesKey("cleared_order")

    fun observeClearedOrder(): Flow<Int> =
        context.spotDiffDataStore.data.map { it[CLEARED_ORDER] ?: 0 }

    suspend fun loadClearedOrder(): Int =
        context.spotDiffDataStore.data.first()[CLEARED_ORDER] ?: 0

    /** 클리어한 order가 기존보다 클 때만 저장 (퇴보 방지). */
    suspend fun saveClearedOrder(order: Int) {
        context.spotDiffDataStore.edit { prefs ->
            val current = prefs[CLEARED_ORDER] ?: 0
            if (order > current) prefs[CLEARED_ORDER] = order
        }
    }

    suspend fun reset() {
        context.spotDiffDataStore.edit { it[CLEARED_ORDER] = 0 }
    }
}
