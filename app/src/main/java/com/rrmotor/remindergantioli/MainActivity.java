package com.rrmotor.remindergantioli;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private final int HIJAU = Color.rgb(20, 130, 65);

    private static final int PILIH_KONTAK = 1001;
    private static final int IZIN_NOTIFIKASI = 2001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout layout;

    private EditText email, password;
    private EditText nama, wa, nopol, mesin, km;
    private EditText tanggalInputView;

    private Spinner interval, jenisMotor;

    private Calendar tanggalDipilih = Calendar.getInstance();

    private boolean sedangMenyimpan = false;
    private boolean sedangMenghapus = false;

    private final SimpleDateFormat formatTanggal =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        formatTanggal.setLenient(false);

        FirebaseApp.initializeApp(this);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ReminderReceiver.buatChannel(this);

        if (auth.getCurrentUser() == null) {
            tampilkanLogin();
        } else {
            mintaIzinNotifikasi();
            tampilkanForm();
        }
    }

    private void mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    IZIN_NOTIFIKASI
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == IZIN_NOTIFIKASI) {
            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(
                        this,
                        "Izin notifikasi aktif.",
                        Toast.LENGTH_SHORT
                ).show();

                periksaDanJadwalkanReminder();

            } else {
                Toast.makeText(
                        this,
                        "Aktifkan izin notifikasi di Pengaturan HP.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void buatLayout() {
        ScrollView scroll = new ScrollView(this);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 32);
        layout.setBackgroundColor(Color.WHITE);

        scroll.addView(layout);

        setContentView(scroll);
    }

    private void judul(String teks) {
        TextView t = new TextView(this);

        t.setText(teks);
        t.setTextSize(23);
        t.setTextColor(HIJAU);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, 1);
        t.setPadding(0, 12, 0, 20);

        layout.addView(t);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);

        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);

        layout.addView(
                e,
                new LinearLayout.LayoutParams(-1, -2));

        return e;
    }

    private Button tombol(String teks, Runnable aksi) {
        Button b = new Button(this);

        b.setText(teks);
        b.setTextColor(Color.WHITE);

        b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(HIJAU));

        layout.addView(
                b,
                new LinearLayout.LayoutParams(-1, -2));

        b.setOnClickListener(v -> aksi.run());

        return b;
    }

    private void tampilkanLogin() {
        buatLayout();

        judul("🏍️ RR MOTOR\nREMINDER GANTI OLI");

        email = input("Email");
        email.setInputType(33);

        password = input("Password");
        password.setInputType(129);

        tombol("MASUK", this::masuk);
        tombol("DAFTAR AKUN BARU", this::daftar);

        TextView info = new TextView(this);

        info.setText(
                "Gunakan email dan password milik Anda sendiri. " +
                "Akun diperlukan untuk menyimpan riwayat pelanggan."
        );

        info.setPadding(0, 20, 0, 0);

        layout.addView(info);
    }

    private void masuk() {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();

        if (e.isEmpty() || p.isEmpty()) {
            pesan("Email dan password harus diisi.");
            return;
        }

        auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {
                    mintaIzinNotifikasi();
                    tampilkanForm();
                })
                .addOnFailureListener(error ->
                        pesan("Login gagal: " + error.getMessage()));
    }

    private void daftar() {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();

        if (e.isEmpty() || p.length() < 6) {
            pesan(
                    "Isi email yang benar dan password minimal 6 karakter.");
            return;
        }

        auth.createUserWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {
                    mintaIzinNotifikasi();
                    tampilkanForm();
                })
                .addOnFailureListener(error ->
                        pesan("Pendaftaran gagal: " + error.getMessage()));
    }

    private void tampilkanForm() {
        if (auth.getCurrentUser() == null) {
            tampilkanLogin();
            return;
        }

        buatLayout();

        judul("🏍️ RR MOTOR\nREMINDER GANTI OLI");

        TextView akun = new TextView(this);

        akun.setText(
                "Login: " + auth.getCurrentUser().getEmail());

        layout.addView(akun);

        tambahLabel("Tanggal input data:");

        tanggalInputView = input("Pilih tanggal input");

        tanggalDipilih = Calendar.getInstance();

        tanggalInputView.setText(
                formatTanggal.format(tanggalDipilih.getTime()));

        tanggalInputView.setFocusable(false);
        tanggalInputView.setClickable(true);

        tanggalInputView.setOnClickListener(v -> pilihTanggal());

        tombol("📅 PILIH TANGGAL", this::pilihTanggal);

        nama = input("Nama pelanggan (opsional)");

        wa = input("Nomor WhatsApp (wajib)");
        wa.setInputType(3);

        tombol("📱 PILIH KONTAK", this::bukaKontak);

        nopol = input("Nomor polisi / plat motor (opsional)");

        mesin = input("Nomor mesin (opsional)");

        km = input("KM terakhir (opsional)");
        km.setInputType(2);

        tambahLabel("Interval pengingat:");

        interval = new Spinner(this);

        interval.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "1 BULAN",
                        "1,5 BULAN",
                        "2 BULAN"
                }
        ));

        layout.addView(interval);

        tambahLabel("Jenis motor:");

        jenisMotor = new Spinner(this);

        jenisMotor.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "Motor umum",
                        "NMAX & sejenisnya",
                        "Motor ojek"
                }
        ));

        layout.addView(jenisMotor);

        tombol("SIMPAN DATA", this::simpanData);

        tombol("RIWAYAT REMINDER", this::riwayat);

        tombol("PERIKSA JADWAL NOTIFIKASI", () -> {
            periksaDanJadwalkanReminder();

            pesan(
                    "Pemeriksaan jadwal dimulai. " +
                    "Pastikan internet dan izin notifikasi aktif."
            );
        });

        tombol("KELUAR AKUN", () -> {
            new AlertDialog.Builder(this)
                    .setTitle("Keluar akun?")
                    .setMessage("Anda akan kembali ke halaman login.")
                    .setNegativeButton("BATAL", null)
                    .setPositiveButton("KELUAR", (d, w) -> {
                        auth.signOut();
                        tampilkanLogin();
                    })
                    .show();
        });

        periksaDanJadwalkanReminder();
    }

    private void pilihTanggal() {
        Calendar awal = (Calendar) tanggalDipilih.clone();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, tahun, bulan, hari) -> {
                    tanggalDipilih.set(Calendar.YEAR, tahun);
                    tanggalDipilih.set(Calendar.MONTH, bulan);
                    tanggalDipilih.set(Calendar.DAY_OF_MONTH, hari);

                    tanggalInputView.setText(
                            formatTanggal.format(tanggalDipilih.getTime()));
                },
                awal.get(Calendar.YEAR),
                awal.get(Calendar.MONTH),
                awal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void bukaKontak() {
        try {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.ContactsContract
                            .CommonDataKinds.Phone.CONTENT_URI
            );

            startActivityForResult(intent, PILIH_KONTAK);

        } catch (Exception e) {
            pesan("Tidak bisa membuka kontak di HP ini.");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PILIH_KONTAK
                && resultCode == RESULT_OK
                && data != null) {

            Uri kontakUri = data.getData();

            if (kontakUri == null) {
                pesan("Kontak tidak ditemukan.");
                return;
            }

            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(
                        kontakUri,
                        new String[]{
                                android.provider.ContactsContract
                                        .CommonDataKinds.Phone.DISPLAY_NAME,
                                android.provider.ContactsContract
                                        .CommonDataKinds.Phone.NUMBER
                        },
                        null,
                        null,
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    String namaKontak = cursor.getString(0);
                    String nomorKontak = cursor.getString(1);

                    if (namaKontak != null
                            && !namaKontak.trim().isEmpty()) {
                        nama.setText(namaKontak.trim());
                    }

                    if (nomorKontak != null) {
                        wa.setText(nomorKontak.trim());
                    }

                    Toast.makeText(
                            this,
                            "Kontak berhasil dipilih",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {
                    pesan("Nama atau nomor kontak tidak dapat dibaca.");
                }

            } catch (Exception e) {
                pesan("Gagal membaca kontak: " + e.getMessage());

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void tambahLabel(String teks) {
        TextView t = new TextView(this);

        t.setText(teks);
        t.setTextSize(15);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(0, 12, 0, 4);

        layout.addView(t);
    }

    private void simpanData() {
        if (sedangMenyimpan) return;

        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            tampilkanLogin();
            return;
        }

        String namaPelanggan = nama.getText().toString().trim();
        String nomor = wa.getText().toString().trim();
        String plat = nopol.getText().toString().trim();
        String nomorMesin = mesin.getText().toString().trim();
        String kilometer = km.getText().toString().trim();
        String intervalPilihan = interval.getSelectedItem().toString();
        String motor = jenisMotor.getSelectedItem().toString();

        if (nomor.isEmpty()) {
            pesan("Nomor WhatsApp wajib diisi.");
            return;
        }

        String nomorAngka = nomor.replaceAll("[^0-9]", "");

        if (nomorAngka.length() < 9 || nomorAngka.length() > 15) {
            pesan("Periksa kembali nomor WhatsApp pelanggan.");
            return;
        }

        if (!kilometer.isEmpty()) {
            try {
                if (Long.parseLong(kilometer) < 0) {
                    pesan("KM tidak boleh negatif.");
                    return;
                }
            } catch (NumberFormatException ex) {
                pesan("Isi KM dengan angka yang benar.");
                return;
            }
        }

        Calendar tanggalDasar = (Calendar) tanggalDipilih.clone();
        Calendar jatuhTempo = (Calendar) tanggalDasar.clone();

        int pilihan = interval.getSelectedItemPosition();

        if (pilihan == 0) {
            jatuhTempo.add(Calendar.MONTH, 1);
        } else if (pilihan == 1) {
            jatuhTempo.add(Calendar.MONTH, 1);
            jatuhTempo.add(Calendar.DAY_OF_MONTH, 15);
        } else {
            jatuhTempo.add(Calendar.MONTH, 2);
        }

        String tanggalInput =
                formatTanggal.format(tanggalDasar.getTime());

        String tanggalTempo =
                formatTanggal.format(jatuhTempo.getTime());

        Map<String, Object> data = new HashMap<>();

        data.put("uid", auth.getCurrentUser().getUid());
        data.put("nama", namaPelanggan);
        data.put("whatsapp", nomor);
        data.put("nopol", plat);
        data.put("nomorMesin", nomorMesin);
        data.put("kmTerakhir", kilometer);
        data.put("interval", intervalPilihan);
        data.put("jenisMotor", motor);
        data.put("tanggalInput", tanggalInput);
        data.put("jatuhTempo", tanggalTempo);
        data.put("status", "BELUM TERKIRIM");
        data.put("waktuSimpan", System.currentTimeMillis());

        sedangMenyimpan = true;

        db.collection("reminders")
                .add(data)
                .addOnSuccessListener(ref -> {
                    sedangMenyimpan = false;

                    Calendar waktuNotifikasi =
                            (Calendar) jatuhTempo.clone();

                    waktuNotifikasi.set(Calendar.HOUR_OF_DAY, 9);
                    waktuNotifikasi.set(Calendar.MINUTE, 0);
                    waktuNotifikasi.set(Calendar.SECOND, 0);
                    waktuNotifikasi.set(Calendar.MILLISECOND, 0);

                    ReminderReceiver.jadwalkan(
                            MainActivity.this,
                            ref.getId(),
                            namaPelanggan,
                            plat,
                            nomor,
                            nomorMesin,
                            kilometer,
                            intervalPilihan,
                            motor,
                            tanggalInput,
                            tanggalTempo,
                            waktuNotifikasi.getTimeInMillis()
                    );

                    new AlertDialog.Builder(this)
                            .setTitle("BERHASIL")
                            .setMessage(
                                    "Data pelanggan berhasil disimpan.\n\n" +
                                    "Tanggal input: " + tanggalInput +
                                    "\nJatuh tempo: " + tanggalTempo +
                                    "\nInterval: " + intervalPilihan +
                                    "\nNotifikasi dijadwalkan pukul 09.00."
                            )
                            .setPositiveButton(
                                    "OK",
                                    (d, w) -> tampilkanForm())
                            .show();
                })
                .addOnFailureListener(error -> {
                    sedangMenyimpan = false;

                    pesan("Gagal menyimpan data: "
                            + error.getMessage());
                });
    }

    private void periksaDanJadwalkanReminder() {
        if (auth.getCurrentUser() == null) return;

        db.collection("reminders")
                .whereEqualTo(
                        "uid",
                        auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String status = nilai(doc, "status");

                        if ("TERKIRIM".equalsIgnoreCase(status)
                                || "SUDAH TERKIRIM".equalsIgnoreCase(status)) {

                            ReminderReceiver.batalkan(
                                    MainActivity.this,
                                    doc.getId());

                            continue;
                        }

                        String tanggal = doc.getString("jatuhTempo");

                        if (tanggal == null || tanggal.trim().isEmpty()) {
                            continue;
                        }

                        try {
                            Date tanggalParsed = formatTanggal.parse(tanggal);

                            if (tanggalParsed == null) continue;

                            Calendar waktu = Calendar.getInstance();
                            waktu.setTime(tanggalParsed);

                            waktu.set(Calendar.HOUR_OF_DAY, 9);
                            waktu.set(Calendar.MINUTE, 0);
                            waktu.set(Calendar.SECOND, 0);
                            waktu.set(Calendar.MILLISECOND, 0);

                            ReminderReceiver.jadwalkan(
                                    MainActivity.this,
                                    doc.getId(),
                                    doc.getString("nama"),
                                    doc.getString("nopol"),
                                    doc.getString("whatsapp"),
                                    doc.getString("nomorMesin"),
                                    doc.getString("kmTerakhir"),
                                    doc.getString("interval"),
                                    doc.getString("jenisMotor"),
                                    doc.getString("tanggalInput"),
                                    doc.getString("jatuhTempo"),
                                    waktu.getTimeInMillis()
                            );

                        } catch (Exception e) {
                            android.util.Log.e(
                                    "RR_MOTOR",
                                    "Gagal membaca tanggal jatuh tempo",
                                    e);
                        }
                    }
                })
                .addOnFailureListener(error ->
                        android.util.Log.e(
                                "RR_MOTOR",
                                "Gagal memeriksa reminder Firebase",
                                error));
    }

    // =========================
    // HALAMAN RIWAYAT REMINDER
    // =========================

    private void riwayat() {
        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            tampilkanLogin();
            return;
        }

        db.collection("reminders")
                .whereEqualTo(
                        "uid",
                        auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    ScrollView scroll = new ScrollView(this);

                    LinearLayout daftar = new LinearLayout(this);
                    daftar.setOrientation(LinearLayout.VERTICAL);
                    daftar.setPadding(16, 12, 16, 12);

                    scroll.addView(daftar);

                    int jumlahTerkirim = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = nilai(doc, "status");

                        if (sudahTerkirim(status)) {
                            jumlahTerkirim++;
                        }
                    }

                    TextView ringkasan = new TextView(this);
                    ringkasan.setText(
                            "Jumlah riwayat terkirim: " + jumlahTerkirim
                            + "\nData belum terkirim akan tetap dipertahankan."
                    );
                    ringkasan.setTextSize(15);
                    ringkasan.setTextColor(HIJAU);
                    ringkasan.setPadding(0, 0, 0, 16);

                    daftar.addView(ringkasan);

                    if (snapshot.isEmpty()) {
                        TextView kosong = new TextView(this);

                        kosong.setText("Belum ada riwayat reminder.");
                        kosong.setTextSize(16);

                        daftar.addView(kosong);
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String namaPelanggan = nilai(doc, "nama");
                        String nomorWA = nilai(doc, "whatsapp");
                        String plat = nilai(doc, "nopol");
                        String tanggal = nilai(doc, "tanggalInput");
                        String tempo = nilai(doc, "jatuhTempo");
                        String status = nilai(doc, "status");

                        LinearLayout kartu = new LinearLayout(this);
                        kartu.setOrientation(LinearLayout.VERTICAL);
                        kartu.setPadding(12, 12, 12, 20);

                        TextView informasi = new TextView(this);

                        informasi.setText(
                                "Nama: " + namaPelanggan +
                                "\nWA: " + nomorWA +
                                "\nPlat: " + plat +
                                "\nTanggal input: " + tanggal +
                                "\nJatuh tempo: " + tempo +
                                "\nStatus: " + status
                        );

                        informasi.setTextSize(16);
                        kartu.addView(informasi);

                        if (sudahTerkirim(status)) {

                            Button hapus = new Button(this);

                            hapus.setText("HAPUS RIWAYAT");
                            hapus.setTextColor(Color.WHITE);

                            hapus.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            Color.rgb(190, 35, 35)));

                            kartu.addView(hapus);

                            hapus.setOnClickListener(v ->
                                    konfirmasiHapusSatu(
                                            doc.getId(),
                                            namaPelanggan));

                        } else {

                            Button tandai = new Button(this);

                            tandai.setText("TANDAI SUDAH TERKIRIM");
                            tandai.setTextColor(Color.WHITE);

                            tandai.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            HIJAU));

                            kartu.addView(tandai);

                            tandai.setOnClickListener(v ->
                                    new AlertDialog.Builder(this)
                                            .setTitle("Konfirmasi")
                                            .setMessage(
                                                    "Apakah reminder pelanggan " +
                                                    namaPelanggan +
                                                    " benar-benar sudah terkirim?")
                                            .setNegativeButton("BELUM", null)
                                            .setPositiveButton(
                                                    "SUDAH TERKIRIM",
                                                    (dialog, which) ->
                                                            tandaiTerkirim(doc.getId()))
                                            .show());
                        }

                        daftar.addView(kartu);

                        View garis = new View(this);
                        garis.setBackgroundColor(Color.LTGRAY);

                        daftar.addView(
                                garis,
                                new LinearLayout.LayoutParams(-1, 2));
                    }

                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle("RIWAYAT REMINDER")
                            .setView(scroll)
                            .setNeutralButton("HAPUS SEMUA", null)
                            .setPositiveButton("KEMBALI", null)
                            .create();

                    dialog.setOnShowListener(d -> {
                        Button hapusSemua =
                                dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

                        Button kembali =
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                        hapusSemua.setOnClickListener(v -> {
                            dialog.dismiss();
                            konfirmasiHapusSemuaRiwayat();
                        });

                        kembali.setOnClickListener(v -> {
                            dialog.dismiss();
                            tampilkanForm();
                        });
                    });

                    dialog.show();
                })
                .addOnFailureListener(error ->
                        pesan("Gagal membuka riwayat: "
                                + error.getMessage()));
    }

    private boolean sudahTerkirim(String status) {
        return "TERKIRIM".equalsIgnoreCase(status)
                || "SUDAH TERKIRIM".equalsIgnoreCase(status);
    }

    private void konfirmasiHapusSatu(
            String documentId,
            String namaPelanggan) {

        new AlertDialog.Builder(this)
                .setTitle("Hapus riwayat?")
                .setMessage(
                        "Hapus riwayat pelanggan " + namaPelanggan
                                + " dari Firebase?\n\n"
                                + "Jadwal alarm tidak dibatalkan oleh "
                                + "fungsi hapus riwayat ini.\n\n"
                                + "Data yang dihapus tidak dapat dikembalikan."
                )
                .setNegativeButton("BATAL", null)
                .setPositiveButton("HAPUS", (dialog, which) ->
                        hapusRiwayat(documentId))
                .show();
    }

    // =========================
    // TANDAI SUDAH TERKIRIM
    // =========================

    private void tandaiTerkirim(String documentId) {
        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            return;
        }

        db.collection("reminders")
                .document(documentId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        pesan("Data pelanggan tidak ditemukan.");
                        return;
                    }

                    String pemilik = doc.getString("uid");

                    if (!auth.getCurrentUser().getUid().equals(pemilik)) {
                        pesan("Anda tidak memiliki izin mengubah data ini.");
                        return;
                    }

                    db.collection("reminders")
                            .document(documentId)
                            .update("status", "TERKIRIM")
                            .addOnSuccessListener(unused -> {
                                // Alarm pelanggan ini dibatalkan karena
                                // statusnya sudah terkirim, bukan karena
                                // penghapusan riwayat.
                                ReminderReceiver.batalkan(
                                        this, documentId);

                                pesan("Status berhasil diubah menjadi TERKIRIM.");

                                riwayat();
                            })
                            .addOnFailureListener(error ->
                                    pesan("Gagal mengubah status: "
                                            + error.getMessage()));
                })
                .addOnFailureListener(error ->
                        pesan("Gagal memeriksa data: "
                                + error.getMessage()));
    }

    // =========================
    // HAPUS SATU RIWAYAT
    // =========================

    private void hapusRiwayat(String documentId) {
        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            tampilkanLogin();
            return;
        }

        db.collection("reminders")
                .document(documentId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        pesan("Riwayat tidak ditemukan.");
                        riwayat();
                        return;
                    }

                    String pemilik = doc.getString("uid");
                    String status = doc.getString("status");

                    if (!auth.getCurrentUser().getUid().equals(pemilik)) {
                        pesan("Anda tidak memiliki izin menghapus data ini.");
                        return;
                    }

                    if (!sudahTerkirim(status)) {
                        pesan(
                                "Hanya riwayat yang sudah terkirim "
                                        + "yang dapat dihapus."
                        );
                        return;
                    }

                    doc.getReference()
                            .delete()
                            .addOnSuccessListener(unused -> {
                                // Tidak memanggil ReminderReceiver.batalkan()
                                // saat menghapus riwayat.
                                pesan("Riwayat berhasil dihapus.");
                                riwayat();
                            })
                            .addOnFailureListener(error ->
                                    pesan("Gagal menghapus riwayat: "
                                            + error.getMessage()));
                })
                .addOnFailureListener(error ->
                        pesan("Gagal memeriksa riwayat: "
                                + error.getMessage()));
    }

    // =========================
    // KONFIRMASI HAPUS SEMUA
    // =========================

    private void konfirmasiHapusSemuaRiwayat() {
        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            tampilkanLogin();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("HAPUS SEMUA RIWAYAT?")
                .setMessage(
                        "Semua riwayat yang sudah berstatus TERKIRIM "
                                + "akan dihapus dari Firebase.\n\n"
                                + "Data pelanggan yang belum terkirim "
                                + "tidak ikut dihapus.\n\n"
                                + "Tidak ada penghapusan otomatis. "
                                + "Data yang dihapus tidak dapat dikembalikan."
                )
                .setNegativeButton("BATAL", null)
                .setPositiveButton("HAPUS SEMUA", (dialog, which) ->
                        hapusSemuaRiwayatTerkirim())
                .show();
    }

    // =========================
    // HAPUS SEMUA RIWAYAT TERKIRIM
    // =========================

    private void hapusSemuaRiwayatTerkirim() {
        if (sedangMenghapus) {
            pesan("Proses penghapusan sedang berjalan.");
            return;
        }

        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            tampilkanLogin();
            return;
        }

        sedangMenghapus = true;

        String uid = auth.getCurrentUser().getUid();

        db.collection("reminders")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<DocumentSnapshot> daftarHapus =
                            new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String pemilik = doc.getString("uid");
                        String status = nilai(doc, "status");

                        if (uid.equals(pemilik)
                                && sudahTerkirim(status)) {
                            daftarHapus.add(doc);
                        }
                    }

                    if (daftarHapus.isEmpty()) {
                        sedangMenghapus = false;

                        new AlertDialog.Builder(this)
                                .setTitle("RIWAYAT KOSONG")
                                .setMessage(
                                        "Tidak ada riwayat terkirim "
                                                + "untuk dihapus."
                                )
                                .setPositiveButton("OK",
                                        (d, w) -> riwayat())
                                .show();

                        return;
                    }

                    hapusDokumenRiwayat(
                            daftarHapus,
                            0,
                            0,
                            0
                    );
                })
                .addOnFailureListener(error -> {
                    sedangMenghapus = false;

                    pesan("Gagal membaca riwayat: "
                            + error.getMessage());
                });
    }

    private void hapusDokumenRiwayat(
            List<DocumentSnapshot> daftar,
            int indeks,
            int berhasil,
            int gagal) {

        if (indeks >= daftar.size()) {
            sedangMenghapus = false;

            new AlertDialog.Builder(this)
                    .setTitle("HASIL PENGHAPUSAN")
                    .setMessage(
                            "Berhasil dihapus: " + berhasil
                                    + "\nGagal dihapus: " + gagal
                                    + "\n\nData pelanggan yang belum "
                                    + "terkirim tidak ikut dihapus."
                    )
                    .setPositiveButton("OK", (dialog, which) ->
                            riwayat())
                    .show();

            return;
        }

        DocumentSnapshot doc = daftar.get(indeks);

        if (auth.getCurrentUser() == null) {
            sedangMenghapus = false;
            pesan("Sesi login berakhir. Silakan login kembali.");
            tampilkanLogin();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Periksa kembali pemilik dan status sebelum menghapus.
        doc.getReference()
                .get()
                .addOnSuccessListener(dataTerbaru -> {

                    if (!dataTerbaru.exists()) {
                        hapusDokumenRiwayat(
                                daftar, indeks + 1, berhasil, gagal);
                        return;
                    }

                    String pemilik = dataTerbaru.getString("uid");
                    String status = nilai(dataTerbaru, "status");

                    if (!uid.equals(pemilik)
                            || !sudahTerkirim(status)) {

                        // Data yang tidak lagi memenuhi syarat
                        // tidak dihapus.
                        hapusDokumenRiwayat(
                                daftar, indeks + 1, berhasil, gagal);
                        return;
                    }

                    dataTerbaru.getReference()
                            .delete()
                            .addOnSuccessListener(unused ->
                                    hapusDokumenRiwayat(
                                            daftar,
                                            indeks + 1,
                                            berhasil + 1,
                                            gagal))
                            .addOnFailureListener(error -> {
                                android.util.Log.e(
                                        "RR_MOTOR",
                                        "Gagal menghapus riwayat "
                                                + dataTerbaru.getId(),
                                        error);

                                hapusDokumenRiwayat(
                                        daftar,
                                        indeks + 1,
                                        berhasil,
                                        gagal + 1);
                            });
                })
                .addOnFailureListener(error -> {
                    android.util.Log.e(
                            "RR_MOTOR",
                            "Gagal memeriksa riwayat sebelum menghapus",
                            error);

                    hapusDokumenRiwayat(
                            daftar,
                            indeks + 1,
                            berhasil,
                            gagal + 1);
                });
    }

    // =========================
    // UTILITAS
    // =========================

    private String nilai(DocumentSnapshot doc, String kolom) {
        String value = doc.getString(kolom);

        return value == null || value.trim().isEmpty()
                ? "-"
                : value;
    }

    private void pesan(String teks) {
        new AlertDialog.Builder(this)
                .setMessage(teks)
                .setPositiveButton("OK", null)
                .show();
    }
}
