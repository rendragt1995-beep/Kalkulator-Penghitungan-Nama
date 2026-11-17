package com.penyandingan.nama.domain

enum class StatusPenyandingan(val label: String) {
    SAMA_DIHAPUS("Sama — Dihapus"),
    BERBEDA_TETAP("Berbeda — Tetap"),
    SISA_NAMA_1("Nama 2 Habis — Tetap"),
    SISA_NAMA_2("Nama 1 Habis — Tetap")
}

data class TokenHuruf(
    val kataIndex: Int,
    val kataAsli: String,
    val posisiDalamKata: Int,
    val karakter: Char
)

data class BarisPenyandingan(
    val posisi: Int,
    val token1: TokenHuruf?,
    val token2: TokenHuruf?,
    val status: StatusPenyandingan
)

data class HurufDihapus(
    val posisi: Int,
    val token1: TokenHuruf,
    val token2: TokenHuruf
)

data class HurufBernomor(
    val kataIndex: Int,
    val kataAsli: String,
    val huruf: Char,
    val angka: Int
)

data class HasilKata(
    val kataIndex: Int,
    val kataAsli: String,
    val hurufBernomor: List<HurufBernomor>,
    val angkaAkhir: Int? // null berarti tidak ada huruf tersisa -> tampilkan "—"
) {
    val sisaHurufTeks: String
        get() = if (hurufBernomor.isEmpty()) "—" else hurufBernomor.joinToString("") { it.huruf.toString() }

    val angkaAkhirTeks: String
        get() = angkaAkhir?.toString() ?: "—"
}

data class HasilNama(
    val namaAsli: String,
    val hasilPerKata: List<HasilKata>,
    val urutanAngka: List<HurufBernomor>
) {
    val ringkasanAngka: String
        get() = if (hasilPerKata.isEmpty()) "—" else hasilPerKata.joinToString(" - ") { it.angkaAkhirTeks }

    val totalSisaHuruf: Int
        get() = urutanAngka.size
}

data class HasilPenyandinganLengkap(
    val nama1: HasilNama,
    val nama2: HasilNama,
    val tabelPenyandingan: List<BarisPenyandingan>,
    val daftarHurufDihapus: List<HurufDihapus>
)

object KalkulatorPenyandingan {

    fun hitung(inputNama1: String, inputNama2: String): HasilPenyandinganLengkap {
        // Aturan 1: Pertahankan ejaan, urutan, dan batas setiap kata.
        val kataList1 = pecahKata(inputNama1)
        val kataList2 = pecahKata(inputNama2)

        // Aturan 2: Gabungkan huruf untuk proses penyandingan posisi saja.
        val tokens1 = ekstrakTokenHuruf(kataList1)
        val tokens2 = ekstrakTokenHuruf(kataList2)

        val maxPanjang = maxOf(tokens1.size, tokens2.size)
        val tabelPenyandingan = mutableListOf<BarisPenyandingan>()
        val daftarDihapus = mutableListOf<HurufDihapus>()
        val sisa1 = mutableListOf<TokenHuruf>()
        val sisa2 = mutableListOf<TokenHuruf>()

        // Aturan 3: Bandingkan huruf pada posisi yang sama mulai dari posisi 1.
        for (i in 0 until maxPanjang) {
            val posisi = i + 1
            val t1 = tokens1.getOrNull(i)
            val t2 = tokens2.getOrNull(i)

            when {
                t1 != null && t2 != null -> {
                    if (t1.karakter.equals(t2.karakter, ignoreCase = true)) {
                        // Aturan 4: Jika huruf sama, hapus kedua huruf tersebut.
                        tabelPenyandingan.add(
                            BarisPenyandingan(posisi, t1, t2, StatusPenyandingan.SAMA_DIHAPUS)
                        )
                        daftarDihapus.add(HurufDihapus(posisi, t1, t2))
                    } else {
                        // Aturan 5: Jika berbeda, pertahankan keduanya.
                        tabelPenyandingan.add(
                            BarisPenyandingan(posisi, t1, t2, StatusPenyandingan.BERBEDA_TETAP)
                        )
                        sisa1.add(t1)
                        sisa2.add(t2)
                    }
                }
                t1 != null -> {
                    // Aturan 6: Jika salah satu nama habis, huruf nama lainnya tetap tersisa.
                    tabelPenyandingan.add(
                        BarisPenyandingan(posisi, t1, null, StatusPenyandingan.SISA_NAMA_1)
                    )
                    sisa1.add(t1)
                }
                t2 != null -> {
                    // Aturan 6
                    tabelPenyandingan.add(
                        BarisPenyandingan(posisi, null, t2, StatusPenyandingan.SISA_NAMA_2)
                    )
                    sisa2.add(t2)
                }
            }
        }

        val hasilNama1 = prosesSisaDanHitung(kataList1, sisa1)
        val hasilNama2 = prosesSisaDanHitung(kataList2, sisa2)

        return HasilPenyandinganLengkap(
            nama1 = hasilNama1,
            nama2 = hasilNama2,
            tabelPenyandingan = tabelPenyandingan,
            daftarHurufDihapus = daftarDihapus
        )
    }

    private fun pecahKata(input: String): List<String> {
        return input.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
    }

    private fun ekstrakTokenHuruf(daftarKata: List<String>): List<TokenHuruf> {
        val tokens = mutableListOf<TokenHuruf>()
        daftarKata.forEachIndexed { kataIdx, kata ->
            kata.forEachIndexed { chIdx, ch ->
                if (ch.isLetter()) {
                    tokens.add(
                        TokenHuruf(
                            kataIndex = kataIdx,
                            kataAsli = kata,
                            posisiDalamKata = chIdx,
                            karakter = ch
                        )
                    )
                }
            }
        }
        return tokens
    }

    private fun prosesSisaDanHitung(
        daftarKata: List<String>,
        sisaTokens: List<TokenHuruf>
    ): HasilNama {
        // Aturan 7: Kembalikan huruf tersisa ke kata aslinya. Jangan memindahkan huruf antarkata.
        val sisaPerKata = sisaTokens.groupBy { it.kataIndex }

        var penghitung = 0
        val urutanAngkaGlobal = mutableListOf<HurufBernomor>()
        val hasilPerKata = mutableListOf<HasilKata>()

        daftarKata.forEachIndexed { kataIdx, kataAsli ->
            val hurufDiKata = sisaPerKata[kataIdx].orEmpty()
            if (hurufDiKata.isEmpty()) {
                // Aturan 11: Kata tanpa huruf tersisa tetap ditampilkan dengan tanda —.
                hasilPerKata.add(
                    HasilKata(
                        kataIndex = kataIdx,
                        kataAsli = kataAsli,
                        hurufBernomor = emptyList(),
                        angkaAkhir = null
                    )
                )
            } else {
                // Aturan 8 & 9: Hitung angka 1, 2, 3, 4 berulang dan berkesinambungan tanpa mengulang tiap kata.
                val daftarBernomor = hurufDiKata.map { token ->
                    penghitung = (penghitung % 4) + 1
                    val item = HurufBernomor(
                        kataIndex = kataIdx,
                        kataAsli = kataAsli,
                        huruf = token.karakter,
                        angka = penghitung
                    )
                    urutanAngkaGlobal.add(item)
                    item
                }

                // Aturan 10: Hasil setiap kata adalah angka pada huruf terakhir yang tersisa.
                hasilPerKata.add(
                    HasilKata(
                        kataIndex = kataIdx,
                        kataAsli = kataAsli,
                        hurufBernomor = daftarBernomor,
                        angkaAkhir = daftarBernomor.last().angka
                    )
                )
            }
        }

        return HasilNama(
            namaAsli = daftarKata.joinToString(" "),
            hasilPerKata = hasilPerKata,
            urutanAngka = urutanAngkaGlobal
        )
    }

    fun formatLaporanTeks(hasil: HasilPenyandinganLengkap): String {
        val sb = StringBuilder()
        sb.appendLine("=== KALKULATOR PENYANDINGAN NAMA ===")
        sb.appendLine("Nama 1: ${hasil.nama1.namaAsli}")
        sb.appendLine("Nama 2: ${hasil.nama2.namaAsli}")
        sb.appendLine()
        sb.appendLine("--- RINGKASAN HASIL ---")
        sb.appendLine("Hasil ${hasil.nama1.namaAsli}: ${hasil.nama1.ringkasanAngka} (Sisa ${hasil.nama1.totalSisaHuruf} huruf)")
        sb.appendLine("Hasil ${hasil.nama2.namaAsli}: ${hasil.nama2.ringkasanAngka} (Sisa ${hasil.nama2.totalSisaHuruf} huruf)")
        sb.appendLine("Total Pasangan Huruf Dihapus: ${hasil.daftarHurufDihapus.size}")
        sb.appendLine()

        sb.appendLine("--- HURUF YANG DIHAPUS ---")
        if (hasil.daftarHurufDihapus.isEmpty()) {
            sb.appendLine("Tidak ada huruf yang dihapus.")
        } else {
            hasil.daftarHurufDihapus.forEach {
                sb.appendLine("Posisi ${it.posisi}: ${it.token1.karakter} (${it.token1.kataAsli}) = ${it.token2.karakter} (${it.token2.kataAsli})")
            }
        }
        sb.appendLine()

        sb.appendLine("--- TABEL PENYANDINGAN ---")
        hasil.tabelPenyandingan.forEach { b ->
            val k1 = b.token1?.let { "${it.karakter} [${it.kataAsli}]" } ?: "—"
            val k2 = b.token2?.let { "${it.karakter} [${it.kataAsli}]" } ?: "—"
            sb.appendLine("Pos ${b.posisi}: $k1 | $k2 -> ${b.status.label}")
        }
        sb.appendLine()

        fun tulisDetailNama(judul: String, h: HasilNama) {
            sb.appendLine("--- $judul: ${h.namaAsli} ---")
            sb.appendLine("Sisa huruf & urutan 1–4:")
            h.hasilPerKata.forEach { kata ->
                val urutan = if (kata.hurufBernomor.isEmpty()) {
                    "—"
                } else {
                    kata.hurufBernomor.joinToString(" ") { "${it.huruf}(${it.angka})" }
                }
                sb.appendLine("• ${kata.kataAsli}: Sisa [${kata.sisaHurufTeks}] | Urutan: $urutan | Hasil: ${kata.angkaAkhirTeks}")
            }
            sb.appendLine()
        }

        tulisDetailNama("HASIL NAMA 1", hasil.nama1)
        tulisDetailNama("HASIL NAMA 2", hasil.nama2)

        return sb.toString().trimEnd()
    }

    fun formatLaporanHtml(hasil: HasilPenyandinganLengkap): String {
        fun esc(text: String): String = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

        val barisPenyandinganHtml = hasil.tabelPenyandingan.joinToString("") { b ->
            val k1 = b.token1?.let { "${esc(it.karakter.toString())} <small>(${esc(it.kataAsli)})</small>" } ?: "—"
            val k2 = b.token2?.let { "${esc(it.karakter.toString())} <small>(${esc(it.kataAsli)})</small>" } ?: "—"
            val rowStyle = if (b.status == StatusPenyandingan.SAMA_DIHAPUS) "background:#fde8e8;color:#9b1c1c;" else ""
            "<tr style=\"$rowStyle\"><td>${b.posisi}</td><td>$k1</td><td>$k2</td><td>${esc(b.status.label)}</td></tr>"
        }

        val dihapusHtml = if (hasil.daftarHurufDihapus.isEmpty()) {
            "<p>Tidak ada huruf yang dihapus.</p>"
        } else {
            "<ul>" + hasil.daftarHurufDihapus.joinToString("") {
                "<li>Posisi ${it.posisi}: <strong>${esc(it.token1.karakter.toString())}</strong> (${esc(it.token1.kataAsli)}) = <strong>${esc(it.token2.karakter.toString())}</strong> (${esc(it.token2.kataAsli)})</li>"
            } + "</ul>"
        }

        fun tabelKataHtml(h: HasilNama): String {
            return h.hasilPerKata.joinToString("") { k ->
                val urutan = if (k.hurufBernomor.isEmpty()) "—" else k.hurufBernomor.joinToString(" ") {
                    "${esc(it.huruf.toString())}(${it.angka})"
                }
                "<tr><td>${esc(k.kataAsli)}</td><td>${esc(k.sisaHurufTeks)}</td><td>$urutan</td><td><strong>${esc(k.angkaAkhirTeks)}</strong></td></tr>"
            }
        }

        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: sans-serif; color: #1f2937; padding: 20px; font-size: 13px; }
                    h1 { font-size: 18px; margin-bottom: 4px; color: #1e3a8a; }
                    h2 { font-size: 14px; margin-top: 18px; margin-bottom: 6px; border-bottom: 1px solid #cbd5e1; padding-bottom: 4px; }
                    .summary-box { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 12px; margin: 12px 0; }
                    table { width: 100%; border-collapse: collapse; margin-top: 6px; }
                    th, td { border: 1px solid #cbd5e1; padding: 6px 8px; text-align: left; }
                    th { background: #f1f5f9; }
                </style>
            </head>
            <body>
                <h1>Kalkulator Penyandingan Nama</h1>
                <p><strong>Nama 1:</strong> ${esc(hasil.nama1.namaAsli)}<br/>
                <strong>Nama 2:</strong> ${esc(hasil.nama2.namaAsli)}</p>

                <div class="summary-box">
                    <strong>Ringkasan Hasil:</strong><br/>
                    • ${esc(hasil.nama1.namaAsli)}: <strong>${esc(hasil.nama1.ringkasanAngka)}</strong> (Sisa ${hasil.nama1.totalSisaHuruf} huruf)<br/>
                    • ${esc(hasil.nama2.namaAsli)}: <strong>${esc(hasil.nama2.ringkasanAngka)}</strong> (Sisa ${hasil.nama2.totalSisaHuruf} huruf)<br/>
                    • Pasangan Huruf Dihapus: <strong>${hasil.daftarHurufDihapus.size}</strong>
                </div>

                <h2>Daftar Huruf yang Dihapus</h2>
                $dihapusHtml

                <h2>Tabel Hasil Per Kata — ${esc(hasil.nama1.namaAsli)}</h2>
                <table>
                    <tr><th>Kata Asli</th><th>Sisa Huruf</th><th>Urutan 1–4</th><th>Hasil</th></tr>
                    ${tabelKataHtml(hasil.nama1)}
                </table>

                <h2>Tabel Hasil Per Kata — ${esc(hasil.nama2.namaAsli)}</h2>
                <table>
                    <tr><th>Kata Asli</th><th>Sisa Huruf</th><th>Urutan 1–4</th><th>Hasil</th></tr>
                    ${tabelKataHtml(hasil.nama2)}
                </table>

                <h2>Tabel Penyandingan Lengkap</h2>
                <table>
                    <tr><th>Pos</th><th>Nama 1</th><th>Nama 2</th><th>Status</th></tr>
                    $barisPenyandinganHtml
                </table>
            </body>
            </html>
        """.trimIndent()
    }
}
