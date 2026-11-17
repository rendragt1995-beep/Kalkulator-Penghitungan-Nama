package com.penyandingan.nama.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ItemRiwayat(
    val id: String,
    val nama1: String,
    val nama2: String,
    val ringkasan1: String,
    val ringkasan2: String,
    val waktuMillis: Long
)

class RiwayatRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ambilSemua(): List<ItemRiwayat> {
        val raw = prefs.getString(KEY_RIWAYAT, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        ItemRiwayat(
                            id = obj.getString("id"),
                            nama1 = obj.getString("nama1"),
                            nama2 = obj.getString("nama2"),
                            ringkasan1 = obj.getString("ringkasan1"),
                            ringkasan2 = obj.getString("ringkasan2"),
                            waktuMillis = obj.getLong("waktuMillis")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun simpan(nama1: String, nama2: String, ringkasan1: String, ringkasan2: String): List<ItemRiwayat> {
        val sekarang = ambilSemua().toMutableList()
        // Hindari duplikat identik berurutan
        val itemBaru = ItemRiwayat(
            id = UUID.randomUUID().toString(),
            nama1 = nama1,
            nama2 = nama2,
            ringkasan1 = ringkasan1,
            ringkasan2 = ringkasan2,
            waktuMillis = System.currentTimeMillis()
        )
        sekarang.removeAll {
            it.nama1.equals(nama1, ignoreCase = false) &&
                it.nama2.equals(nama2, ignoreCase = false)
        }
        sekarang.add(0, itemBaru)
        val terbatas = sekarang.take(MAX_RIWAYAT)
        tulisKePrefs(terbatas)
        return terbatas
    }

    fun hapusSatu(id: String): List<ItemRiwayat> {
        val baru = ambilSemua().filterNot { it.id == id }
        tulisKePrefs(baru)
        return baru
    }

    fun hapusSemua(): List<ItemRiwayat> {
        prefs.edit().remove(KEY_RIWAYAT).apply()
        return emptyList()
    }

    private fun tulisKePrefs(items: List<ItemRiwayat>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nama1", item.nama1)
                put("nama2", item.nama2)
                put("ringkasan1", item.ringkasan1)
                put("ringkasan2", item.ringkasan2)
                put("waktuMillis", item.waktuMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RIWAYAT, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "kalkulator_penyandingan_prefs"
        const val KEY_RIWAYAT = "daftar_riwayat"
        const val MAX_RIWAYAT = 50
    }
}
