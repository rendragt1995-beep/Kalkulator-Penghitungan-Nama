package com.penyandingan.nama

import com.penyandingan.nama.domain.KalkulatorPenyandingan
import com.penyandingan.nama.domain.StatusPenyandingan
import org.junit.Assert.*
import org.junit.Test

class KalkulatorPenyandinganTest {

    @Test
    fun `kasus 1 - penyandingan dasar dua nama multikata dan kelanjutan angka antar kata`() {
        val hasil = KalkulatorPenyandingan.hitung("Budi Santoso", "Siti Aminah")

        assertEquals(1, hasil.daftarHurufDihapus.size)
        assertEquals(4, hasil.daftarHurufDihapus[0].posisi)
        assertEquals('i', hasil.daftarHurufDihapus[0].token1.karakter.lowercaseChar())

        assertEquals("Bud", hasil.nama1.hasilPerKata[0].sisaHurufTeks)
        assertEquals(3, hasil.nama1.hasilPerKata[0].angkaAkhir)
        assertEquals("Santoso", hasil.nama1.hasilPerKata[1].sisaHurufTeks)
        assertEquals(2, hasil.nama1.hasilPerKata[1].angkaAkhir)
        assertEquals("3 - 2", hasil.nama1.ringkasanAngka)

        assertEquals("Sit", hasil.nama2.hasilPerKata[0].sisaHurufTeks)
        assertEquals(3, hasil.nama2.hasilPerKata[0].angkaAkhir)
        assertEquals("Aminah", hasil.nama2.hasilPerKata[1].sisaHurufTeks)
        assertEquals(1, hasil.nama2.hasilPerKata[1].angkaAkhir)
        assertEquals("3 - 1", hasil.nama2.ringkasanAngka)
    }

    @Test
    fun `kasus 2 - aturan 11 kata tanpa huruf tersisa tetap ditampilkan dengan tanda em-dash`() {
        val hasil = KalkulatorPenyandingan.hitung("Adi Putra", "Adi")

        assertEquals(3, hasil.daftarHurufDihapus.size)

        val kataAdi1 = hasil.nama1.hasilPerKata[0]
        assertEquals("Adi", kataAdi1.kataAsli)
        assertEquals("—", kataAdi1.sisaHurufTeks)
        assertNull(kataAdi1.angkaAkhir)
        assertEquals("—", kataAdi1.angkaAkhirTeks)

        val kataPutra = hasil.nama1.hasilPerKata[1]
        assertEquals("Putra", kataPutra.kataAsli)
        assertEquals("Putra", kataPutra.sisaHurufTeks)
        assertEquals(1, kataPutra.angkaAkhir)

        assertEquals("— - 1", hasil.nama1.ringkasanAngka)
        assertEquals("—", hasil.nama2.ringkasanAngka)
    }

    @Test
    fun `kasus 3 - kata tengah habis seluruh hurufnya tidak memutus kontinuitas 1 sampai 4`() {
        val hasil = KalkulatorPenyandingan.hitung("Budi Adi Putra", "Siti Adi")

        assertEquals(3, hasil.nama1.hasilPerKata[0].angkaAkhir)
        assertEquals("—", hasil.nama1.hasilPerKata[1].angkaAkhirTeks)
        assertEquals(4, hasil.nama1.hasilPerKata[2].angkaAkhir)
        assertEquals("3 - — - 4", hasil.nama1.ringkasanAngka)

        assertEquals("3 - —", hasil.nama2.ringkasanAngka)
    }

    @Test
    fun `kasus 4 - aturan 1 dan 2 batas kata dan ejaan asli dipertahankan walau huruf berbeda kapital`() {
        val hasil = KalkulatorPenyandingan.hitung("  Andi   Wijaya ", "ani")

        assertEquals("Andi Wijaya", hasil.nama1.namaAsli)
        assertEquals("di", hasil.nama1.hasilPerKata[0].sisaHurufTeks)
        assertEquals("Wijaya", hasil.nama1.hasilPerKata[1].sisaHurufTeks)
        assertEquals("i", hasil.nama2.hasilPerKata[0].sisaHurufTeks)

        assertEquals("2 - 4", hasil.nama1.ringkasanAngka)
        assertEquals("1", hasil.nama2.ringkasanAngka)
    }

    @Test
    fun `kasus 5 - seluruh huruf sama persis menghasilkan semua tanda em-dash`() {
        val hasil = KalkulatorPenyandingan.hitung("Rahmat Hidayat", "Rahmat Hidayat")

        assertEquals(13, hasil.daftarHurufDihapus.size)
        assertEquals("— - —", hasil.nama1.ringkasanAngka)
        assertEquals("— - —", hasil.nama2.ringkasanAngka)
        assertTrue(hasil.nama1.urutanAngka.isEmpty())
        assertTrue(hasil.nama2.urutanAngka.isEmpty())
    }

    @Test
    fun `kasus 6 - kata tunggal 1 huruf berulang menguji siklus 1 2 3 4`() {
        val hasil = KalkulatorPenyandingan.hitung("A B C D E", "Z")

        assertEquals("1 - 2 - 3 - 4 - 1", hasil.nama1.ringkasanAngka)
        assertEquals("1", hasil.nama2.ringkasanAngka)
        assertEquals(StatusPenyandingan.BERBEDA_TETAP, hasil.tabelPenyandingan[0].status)
        assertEquals(StatusPenyandingan.SISA_NAMA_1, hasil.tabelPenyandingan[1].status)
    }
}
