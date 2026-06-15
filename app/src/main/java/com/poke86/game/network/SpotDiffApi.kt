package com.poke86.game.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SpotDiffStageSummary(
    val id: String,
    val title: String,
    val difficulty: Int,
    val diffCount: Int,
    val order: Int = 1,
)

data class SpotDiffStageDetail(
    val id: String,
    val title: String,
    val difficulty: Int,
    val imageAUrl: String,
    val imageBUrl: String,
    val diffs: List<SpotDiffPoint>,
)

data class SpotDiffPoint(val x: Float, val y: Float, val r: Float)

class SpotDiffApi(
    private val baseUrl: String = "https://game.poke86.com",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun listStages(): Result<List<SpotDiffStageSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$baseUrl/spotdiff/stages").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val arr = JSONObject(body).optJSONArray("stages") ?: JSONArray()
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            SpotDiffStageSummary(
                                id = o.getString("id"),
                                title = o.optString("title", o.getString("id")),
                                difficulty = o.optInt("difficulty", 1),
                                diffCount = o.optInt("diffCount", 0),
                                order = o.optInt("order", i + 1),
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun getStage(id: String): Result<SpotDiffStageDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$baseUrl/spotdiff/stages/$id").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val o = JSONObject(body)
                val diffsArr = o.optJSONArray("diffs") ?: JSONArray()
                val diffs = buildList {
                    for (i in 0 until diffsArr.length()) {
                        val d = diffsArr.getJSONObject(i)
                        add(
                            SpotDiffPoint(
                                x = d.getDouble("x").toFloat(),
                                y = d.getDouble("y").toFloat(),
                                r = d.optDouble("r", 0.07).toFloat(),
                            )
                        )
                    }
                }
                SpotDiffStageDetail(
                    id = o.getString("id"),
                    title = o.optString("title", id),
                    difficulty = o.optInt("difficulty", 1),
                    imageAUrl = o.getString("imageAUrl"),
                    imageBUrl = o.getString("imageBUrl"),
                    diffs = diffs,
                )
            }
        }
    }
}
