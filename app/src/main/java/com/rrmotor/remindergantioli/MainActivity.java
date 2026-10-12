package com.rrmotor.remindergantioli;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private final int HIJAU = Color.rgb(20, 130, 65);
    private static final int PILIH_KONTAK = 1001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout layout;
    private EditText email, password;
    private EditText nama, wa, nopol, mesin, km;
    private EditText tanggalInputView;
    private Spinner interval, jenisMotor;

    private Calendar tanggalDipilih = Calendar.getInstance();
    private boolean sedangMenyimpan = false;

    private final SimpleDateFormat formatTanggal =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            tampilkanLogin();
        } else {
            tampilkanForm();
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

        layout.addView(e, new LinearLayout.LayoutParams(
                -1, -2));

        return e;
    }

    private Button tombol(String teks, Runnable aksi) {
        Button b = new Button(this);
        b.setText(teks);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(HIJAU));

        layout.addView(b, new LinearLayout.LayoutParams(
                -1, -2));

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
                .addOnSuccessListener(result -> tampilkanForm())
                .addOnFailureListener(error ->
                        pesan("Login gagal: " + error.getMessage()));
    }

    private void daftar() {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();

        if (e.isEmpty() || p.length() < 6) {
            pesan("Isi email yang benar dan password minimal 6 karakter.");
            return;
        }

        auth.createUserWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> tampilkanForm())
                .addOnFailureListener(error ->
                        pesan("Pendaftaran gagal: " +
                                error.getMessage()));
    }

    private void tampilkanForm() {
        buatLayout();
        judul("🏍️ RR MOTOR\nREMINDER GANTI OLI");

        TextView akun = new TextView(this);
        akun.setText("Login: " + auth.getCurrentUser().getEmail());
        layout.addView(akun);

        tambahLabel("Tanggal input data:");
        tanggalInputView = input("Pilih tanggal input");

        tanggalDipilih = Calendar.getInstance();
        tanggalInputView.setText(formatTanggal.format(
                tanggalDipilih.getTime()));

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
                }));
        layout.addView(interval);

        tambahLabel("Jenis motor untuk perhitungan KM:");

        jenisMotor = new Spinner(this);
        jenisMotor.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "Motor umum",
                        "NMAX & sejenisnya",
                        "Motor ojek"
                }));
        layout.addView(jenisMotor);

        tombol("SIMPAN DATA", this::simpanData);
        tombol("RIWAYAT REMINDER", this::riwayat);

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
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            );

            startActivityForResult(intent, PILIH_KONTAK);

        } catch (Exception e) {
            pesan("Tidak bisa membuka kontak di HP ini.");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

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
                                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                        },
                        null,
                        null,
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    String namaKontak = cursor.getString(0);
                    String nomorKontak = cursor.getString(1);

                    if (namaKontak != null && !namaKontak.trim().isEmpty()) {
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

        String nomor = wa.getText().toString().trim();

        if (nomor.isEmpty()) {
            pesan("Nomor WhatsApp wajib diisi.");
            return;
        }

        String kilometer = km.getText().toString().trim();

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

        String tanggalInput = formatTanggal.format(
                tanggalDasar.getTime());

        String tanggalTempo = formatTanggal.format(
                jatuhTempo.getTime());

        Map<String, Object> data = new HashMap<>();
        data.put("uid", auth.getCurrentUser().getUid());
        data.put("nama", nama.getText().toString().trim());
        data.put("whatsapp", nomor);
        data.put("nopol", nopol.getText().toString().trim());
        data.put("nomorMesin", mesin.getText().toString().trim());
        data.put("kmTerakhir", kilometer);
        data.put("interval", interval.getSelectedItem().toString());
        data.put("jenisMotor", jenisMotor.getSelectedItem().toString());
        data.put("tanggalInput", tanggalInput);
        data.put("jatuhTempo", tanggalTempo);
        data.put("status", "BELUM TERKIRIM");
        data.put("waktuSimpan", System.currentTimeMillis());

        sedangMenyimpan = true;

        db.collection("reminders")
                .add(data)
                .addOnSuccessListener(ref -> {
                    sedangMenyimpan = false;

                    new AlertDialog.Builder(this)
                            .setTitle("BERHASIL")
                            .setMessage(
                                    "Data pelanggan berhasil disimpan.\n\n" +
                                    "Tanggal input: " + tanggalInput +
                                    "\nJatuh tempo: " + tanggalTempo)
                            .setPositiveButton("OK", (d, w) -> {
                                tampilkanForm();
                            })
                            .show();
                })
                .addOnFailureListener(error -> {
                    sedangMenyimpan = false;
                    pesan("Gagal menyimpan data: " +
                            error.getMessage());
                });
    }

    private void riwayat() {
        if (auth.getCurrentUser() == null) {
            pesan("Silakan login kembali.");
            return;
        }

        db.collection("reminders")
                .whereEqualTo("uid",
                        auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    StringBuilder hasil = new StringBuilder();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        hasil.append("Tanggal input: ")
                                .append(nilai(doc, "tanggalInput"))
                                .append("\nNama: ")
                                .append(nilai(doc, "nama"))
                                .append("\nWA: ")
                                .append(nilai(doc, "whatsapp"))
                                .append("\nPlat: ")
                                .append(nilai(doc, "nopol"))
                                .append("\nJatuh tempo: ")
                                .append(nilai(doc, "jatuhTempo"))
                                .append("\nStatus: ")
                                .append(nilai(doc, "status"))
                                .append("\n\n");
                    }

                    if (hasil.length() == 0) {
                        hasil.append("Belum ada riwayat reminder.");
                    }

                    ScrollView scroll = new ScrollView(this);
                    TextView isi = new TextView(this);
                    isi.setText(hasil.toString());
                    isi.setTextSize(16);
                    isi.setPadding(24, 16, 24, 16);
                    scroll.addView(isi);

                    new AlertDialog.Builder(this)
                            .setTitle("RIWAYAT REMINDER")
                            .setView(scroll)
                            .setPositiveButton("TUTUP", null)
                            .show();
                })
                .addOnFailureListener(error ->
                        pesan("Gagal membuka riwayat: " +
                                error.getMessage()));
    }

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
