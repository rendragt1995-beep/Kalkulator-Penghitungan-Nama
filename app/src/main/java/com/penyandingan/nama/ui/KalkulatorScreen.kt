package com.penyandingan.nama.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penyandingan.nama.data.ItemRiwayat
import com.penyandingan.nama.domain.*
import com.penyandingan.nama.util.PrintHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TabUtama(val label: String) {
    KALKULATOR("Kalkulator"),
    RIWAYAT("Riwayat")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalkulatorScreen(viewModel: KalkulatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var tabAktif by remember { mutableStateOf(TabUtama.KALKULATOR) }
    var dialogHapusSemua by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Kalkulator Penyandingan Nama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tabAktif == TabUtama.KALKULATOR,
                    onClick = { tabAktif = TabUtama.KALKULATOR },
                    icon = { Icon(Icons.Rounded.Calculate, contentDescription = null) },
                    label = { Text(TabUtama.KALKULATOR.label) }
                )
                NavigationBarItem(
                    selected = tabAktif == TabUtama.RIWAYAT,
                    onClick = { tabAktif = TabUtama.RIWAYAT },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.riwayat.isNotEmpty()) {
                                    Badge { Text("${uiState.riwayat.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null)
                        }
                    },
                    label = { Text(TabUtama.RIWAYAT.label) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (tabAktif) {
                TabUtama.KALKULATOR -> {
                    HalamanKalkulator(
                        uiState = uiState,
                        onNama1Change = viewModel::ubahNama1,
                        onNama2Change = viewModel::ubahNama2,
                        onHitung = {
                            focusManager.clearFocus()
                            viewModel.hitung()
                        },
                        onBersihkan = {
                            focusManager.clearFocus()
                            viewModel.bersihkan()
                        },
                        onSalin = { hasil ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val teks = KalkulatorPenyandingan.formatLaporanTeks(hasil)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hasil Penyandingan Nama", teks))
                            scope.launch {
                                snackbarHostState.showSnackbar("Hasil berhasil disalin ke papan klip.")
                            }
                        },
                        onCetak = { hasil ->
                            PrintHelper.cetakHasil(context, hasil)
                        }
                    )
                }

                TabUtama.RIWAYAT -> {
                    HalamanRiwayat(
                        riwayat = uiState.riwayat,
                        onPilihItem = { item ->
                            viewModel.muatDariRiwayat(item)
                            tabAktif = TabUtama.KALKULATOR
                        },
                        onHapusItem = viewModel::hapusRiwayat,
                        onMintaHapusSemua = { dialogHapusSemua = true }
                    )
                }
            }
        }
    }

    if (dialogHapusSemua) {
        AlertDialog(
            onDismissRequest = { dialogHapusSemua = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Seluruh riwayat perhitungan lokal akan dihapus permanen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hapusSemuaRiwayat()
                        dialogHapusSemua = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogHapusSemua = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun HalamanKalkulator(
    uiState: KalkulatorUiState,
    onNama1Change: (String) -> Unit,
    onNama2Change: (String) -> Unit,
    onHitung: () -> Unit,
    onBersihkan: () -> Unit,
    onSalin: (HasilPenyandinganLengkap) -> Unit,
    onCetak: (HasilPenyandinganLengkap) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Masukkan Kedua Nama",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = uiState.inputNama1,
                    onValueChange = onNama1Change,
                    label = { Text("Nama Pertama") },
                    placeholder = { Text("Contoh: Budi Santoso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = uiState.inputNama2,
                    onValueChange = onNama2Change,
                    label = { Text("Nama Kedua") },
                    placeholder = { Text("Contoh: Siti Aminah") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onHitung() })
                )

                if (uiState.pesanError != null) {
                    Text(
                        text = uiState.pesanError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBersihkan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bersihkan")
                    }

                    Button(
                        onClick = onHitung,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hitung")
                    }
                }
            }
        }

        val hasil = uiState.hasil
        if (hasil != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { onSalin(hasil) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Hasil")
                }

                FilledTonalButton(
                    onClick = { onCetak(hasil) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak")
                }
            }

            KartuRingkasan(hasil)
            KartuHasilPerKata(judul = "Tabel Hasil Per Kata — Nama 1", hasilNama = hasil.nama1)
            KartuHasilPerKata(judul = "Tabel Hasil Per Kata — Nama 2", hasilNama = hasil.nama2)
            KartuUrutanAngka(hasil)
            KartuHurufDihapus(hasil.daftarHurufDihapus)
            KartuTabelPenyandingan(hasil.tabelPenyandingan)
        } else {
            KartuInfoKosong()
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun KartuRingkasan(hasil: HasilPenyandinganLengkap) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ringkasan Hasil Kedua Nama",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BoxRingkasanNama(
                    label = "Nama 1",
                    nama = hasil.nama1.namaAsli,
                    angka = hasil.nama1.ringkasanAngka,
                    sisa = hasil.nama1.totalSisaHuruf,
                    modifier = Modifier.weight(1f)
                )
                BoxRingkasanNama(
                    label = "Nama 2",
                    nama = hasil.nama2.namaAsli,
                    angka = hasil.nama2.ringkasanAngka,
                    sisa = hasil.nama2.totalSisaHuruf,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Pasangan huruf yang dihapus: ${hasil.daftarHurufDihapus.size} pasangan (${hasil.daftarHurufDihapus.size * 2} huruf)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BoxRingkasanNama(
    label: String,
    nama: String,
    angka: String,
    sisa: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            text = nama,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = angka,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sisa $sisa huruf",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KartuHasilPerKata(judul: String, hasilNama: HasilNama) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = judul,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hasilNama.namaAsli,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("Kata", Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Sisa Huruf", Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Jml", Modifier.weight(0.6f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Hasil", Modifier.weight(0.7f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }

            hasilNama.hasilPerKata.forEachIndexed { idx, kata ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(kata.kataAsli, Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = kata.sisaHurufTeks,
                        modifier = Modifier.weight(1.3f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${kata.hurufBernomor.size}",
                        modifier = Modifier.weight(0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = kata.angkaAkhirTeks,
                        modifier = Modifier.weight(0.7f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (kata.angkaAkhir != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                if (idx < hasilNama.hasilPerKata.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun KartuUrutanAngka(hasil: HasilPenyandinganLengkap) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sisa Huruf & Urutan Angka 1–4 Berkesinambungan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            BlokUrutanNama(label = "Nama 1: ${hasil.nama1.namaAsli}", hasilNama = hasil.nama1)
            HorizontalDivider()
            BlokUrutanNama(label = "Nama 2: ${hasil.nama2.namaAsli}", hasilNama = hasil.nama2)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlokUrutanNama(label: String, hasilNama: HasilNama) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        hasilNama.hasilPerKata.forEach { kata ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${kata.kataAsli} → Hasil: ${kata.angkaAkhirTeks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (kata.hurufBernomor.isEmpty()) {
                    Text(
                        text = "— (tidak ada huruf tersisa)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        kata.hurufBernomor.forEach { hb ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = hb.huruf.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = hb.angka.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KartuHurufDihapus(daftarDihapus: List<HurufDihapus>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Daftar Huruf yang Dihapus (${daftarDihapus.size} Pasangan)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (daftarDihapus.isEmpty()) {
                Text(
                    text = "Tidak ada huruf yang sama pada posisi yang sama.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    daftarDihapus.forEach { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Pos ${item.posisi}: ${item.token1.karakter} = ${item.token2.karakter}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuTabelPenyandingan(tabel: List<BarisPenyandingan>) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tabel Penyandingan Lengkap",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text("Pos", Modifier.weight(0.55f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Nama 1", Modifier.weight(1.1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Nama 2", Modifier.weight(1.1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Status", Modifier.weight(1.25f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            tabel.forEachIndexed { idx, baris ->
                val dihapus = baris.status == StatusPenyandingan.SAMA_DIHAPUS
                val bgRow = if (dihapus) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgRow)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${baris.posisi}",
                        modifier = Modifier.weight(0.55f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = baris.token1?.let { "${it.karakter} (${it.kataAsli})" } ?: "—",
                        modifier = Modifier.weight(1.1f),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (dihapus) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = baris.token2?.let { "${it.karakter} (${it.kataAsli})" } ?: "—",
                        modifier = Modifier.weight(1.1f),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (dihapus) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = baris.status.label,
                        modifier = Modifier.weight(1.25f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dihapus) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
                if (idx < tabel.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun KartuInfoKosong() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Cara Kerja Penyandingan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "1. Huruf kedua nama dibandingkan mulai posisi 1.\n" +
                    "2. Huruf yang sama pada posisi yang sama akan dihapus keduanya.\n" +
                    "3. Huruf yang tersisa dikembalikan ke kata aslinya.\n" +
                    "4. Huruf tersisa dihitung 1–4 secara berkesinambungan per nama.\n" +
                    "5. Angka terakhir tiap kata menjadi hasil kata tersebut (— jika habis).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HalamanRiwayat(
    riwayat: List<ItemRiwayat>,
    onPilihItem: (ItemRiwayat) -> Unit,
    onHapusItem: (String) -> Unit,
    onMintaHapusSemua: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    if (riwayat.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Belum ada riwayat perhitungan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Perhitungan yang Anda lakukan akan tersimpan otomatis secara lokal di perangkat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat Lokal (${riwayat.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onMintaHapusSemua) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hapus Semua")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(riwayat, key = { it.id }) { item ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPilihItem(item) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${item.nama1}  ↔  ${item.nama2}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hasil: [${item.ringkasan1}] & [${item.ringkasan2}]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatter.format(Date(item.waktuMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onHapusItem(item.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Hapus riwayat",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
