package com.poke86.game.network

import com.poke86.game.domain.model.td.TDCharacterSave
import com.poke86.game.domain.model.td.TDProgressSave
import com.poke86.game.domain.model.td.TDResourcesSave
import com.poke86.game.domain.model.td.TDSaveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

class TDApi(
    private val baseUrl: String = "https://game.poke86.com",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** 최초 실행 시 UUID 등록. 이미 등록된 경우도 ok:true 반환. */
    suspend fun register(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = """{"userId":"$userId"}""".toRequestBody(JSON_MEDIA)
            val req = Request.Builder().url("$baseUrl/td/users").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
            }
        }
    }

    /** 서버에서 전체 저장 데이터 로드. 없으면 null. */
    suspend fun fetch(userId: String): Result<TDSaveData?> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$baseUrl/td/users/$userId/save").get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@runCatching null
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val bodyStr = resp.body?.string().orEmpty()
                json.decodeFromString(TDSaveData.serializer(), bodyStr)
            }
        }
    }

    /** 전체 저장 데이터 업로드. */
    suspend fun uploadAll(userId: String, data: TDSaveData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyStr = json.encodeToString(TDSaveData.serializer(), data)
            val body = bodyStr.toRequestBody(JSON_MEDIA)
            val req = Request.Builder().url("$baseUrl/td/users/$userId/save").put(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
            }
        }
    }

    /** 진행 상태만 부분 업데이트. */
    suspend fun uploadProgress(userId: String, progress: TDProgressSave): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            patch("$baseUrl/td/users/$userId/save/progress",
                json.encodeToString(TDProgressSave.serializer(), progress))
        }
    }

    /** 자원(골드·라이프)만 부분 업데이트. */
    suspend fun uploadResources(userId: String, resources: TDResourcesSave): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            patch("$baseUrl/td/users/$userId/save/resources",
                json.encodeToString(TDResourcesSave.serializer(), resources))
        }
    }

    /** 캐릭터 목록 부분 업데이트. */
    suspend fun uploadCharacters(userId: String, characters: List<TDCharacterSave>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            patch("$baseUrl/td/users/$userId/save/characters",
                json.encodeToString(ListSerializer(TDCharacterSave.serializer()), characters))
        }
    }

    private fun patch(url: String, bodyStr: String) {
        val body = bodyStr.toRequestBody(JSON_MEDIA)
        val req = Request.Builder().url(url).patch(body).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
        }
    }
}
