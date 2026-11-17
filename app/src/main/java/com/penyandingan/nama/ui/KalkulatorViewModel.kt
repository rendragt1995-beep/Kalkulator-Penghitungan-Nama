package com.penyandingan.nama.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.penyandingan.nama.data.ItemRiwayat
import com.penyandingan.nama.data.RiwayatRepository
import com.penyandingan.nama.domain.HasilPenyandinganLengkap
import com.penyandingan.nama.domain.KalkulatorPenyandingan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KalkulatorUiState(
    val inputNama1: String = "",
    val inputNama2: String = "",
    val pesanError: String? = null,
    val hasil: HasilPenyandinganLengkap? = null,
    val riwayat: List<ItemRiwayat> = emptyList()
)

class KalkulatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RiwayatRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        KalkulatorUiState(riwayat = repository.ambilSemua())
    )
    val uiState: StateFlow<KalkulatorUiState> = _uiState.asStateFlow()

    fun ubahNama1(nilai: String) {
        _uiState.update { it.copy(inputNama1 = nilai, pesanError = null) }
    }

    fun ubahNama2(nilai: String) {
        _uiState.update { it.copy(inputNama2 = nilai, pesanError = null) }
    }

    fun hitung(simpanRiwayat: Boolean = true) {
        val n1 = _uiState.value.inputNama1.trim()
        val n2 = _uiState.value.inputNama2.trim()

        if (n1.isEmpty() || n2.isEmpty()) {
            _uiState.update {
                it.copy(pesanError = "Masukkan kedua nama terlebih dahulu sebelum menghitung.")
            }
            return
        }

        val hasHuruf1 = n1.any { it.isLetter() }
        val hasHuruf2 = n2.any { it.isLetter() }
        if (!hasHuruf1 || !hasHuruf2) {
            _uiState.update {
                it.copy(pesanError = "Setiap nama harus mengandung minimal satu huruf.")
            }
            return
        }

        val hasilBaru = KalkulatorPenyandingan.hitung(n1, n2)
        val riwayatBaru = if (simpanRiwayat) {
            repository.simpan(
                nama1 = hasilBaru.nama1.namaAsli,
                nama2 = hasilBaru.nama2.namaAsli,
                ringkasan1 = hasilBaru.nama1.ringkasanAngka,
                ringkasan2 = hasilBaru.nama2.ringkasanAngka
            )
        } else {
            _uiState.value.riwayat
        }

        _uiState.update {
            it.copy(
                hasil = hasilBaru,
                pesanError = null,
                riwayat = riwayatBaru
            )
        }
    }

    fun bersihkan() {
        _uiState.update {
            it.copy(
                inputNama1 = "",
                inputNama2 = "",
                pesanError = null,
                hasil = null
            )
        }
    }

    fun muatDariRiwayat(item: ItemRiwayat) {
        _uiState.update {
            it.copy(
                inputNama1 = item.nama1,
                inputNama2 = item.nama2,
                pesanError = null
            )
        }
        hitung(simpanRiwayat = false)
    }

    fun hapusRiwayat(id: String) {
        val baru = repository.hapusSatu(id)
        _uiState.update { it.copy(riwayat = baru) }
    }

    fun hapusSemuaRiwayat() {
        val baru = repository.hapusSemua()
        _uiState.update { it.copy(riwayat = baru) }
    }
}
