package com.poke86.game.data.datasource.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tdDataStore by preferencesDataStore(name = "td_save")

/**
 * DataStore 기반 로컬 저장소.
 * 각 섹션을 별도 JSON 문자열로 저장하여 부분 업데이트를 지원합니다.
 */
@Singleton
class TDLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val PROGRESS   = stringPreferencesKey("td_progress")
        val RESOURCES  = stringPreferencesKey("td_resources")
        val CHARACTERS = stringPreferencesKey("td_characters")
        val SAVED_AT   = stringPreferencesKey("td_saved_at")
    }

    fun observe(): Flow<TDSaveData> = context.tdDataStore.data.map { it.toSaveData() }

    suspend fun load(): TDSaveData = context.tdDataStore.data.first().toSaveData()

    suspend fun saveAll(data: TDSaveData) {
        context.tdDataStore.edit { prefs ->
            prefs[Keys.PROGRESS]   = json.encodeToString(TDProgressSave.serializer(), data.progress)
            prefs[Keys.RESOURCES]  = json.encodeToString(TDResourcesSave.serializer(), data.resources)
            prefs[Keys.CHARACTERS] = json.encodeToString(ListSerializer(TDCharacterSave.serializer()), data.characters)
            prefs[Keys.SAVED_AT]   = data.savedAt.toString()
        }
    }

    suspend fun saveProgress(progress: TDProgressSave) {
        context.tdDataStore.edit { prefs ->
            prefs[Keys.PROGRESS] = json.encodeToString(TDProgressSave.serializer(), progress)
            prefs[Keys.SAVED_AT] = System.currentTimeMillis().toString()
        }
    }

    suspend fun saveResources(resources: TDResourcesSave) {
        context.tdDataStore.edit { prefs ->
            prefs[Keys.RESOURCES] = json.encodeToString(TDResourcesSave.serializer(), resources)
            prefs[Keys.SAVED_AT]  = System.currentTimeMillis().toString()
        }
    }

    suspend fun saveCharacters(characters: List<TDCharacterSave>) {
        context.tdDataStore.edit { prefs ->
            prefs[Keys.CHARACTERS] = json.encodeToString(ListSerializer(TDCharacterSave.serializer()), characters)
            prefs[Keys.SAVED_AT]   = System.currentTimeMillis().toString()
        }
    }

    // ─── Preferences → TDSaveData ─────────────────────────────────────────────

    private fun Preferences.toSaveData() = TDSaveData(
        progress = this[Keys.PROGRESS]?.decode(TDProgressSave.serializer()) ?: TDProgressSave(),
        resources = this[Keys.RESOURCES]?.decode(TDResourcesSave.serializer()) ?: TDResourcesSave(),
        characters = this[Keys.CHARACTERS]?.decode(ListSerializer(TDCharacterSave.serializer())) ?: emptyList(),
        savedAt = this[Keys.SAVED_AT]?.toLongOrNull() ?: 0L,
    )

    private fun <T> String.decode(deserializer: kotlinx.serialization.DeserializationStrategy<T>): T? =
        runCatching { json.decodeFromString(deserializer, this) }.getOrNull()
}
