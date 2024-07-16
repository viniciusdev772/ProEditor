package com.viniciusdev.marketproeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import android.util.Base64;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class LicenseManager {

    public static final String LICENSE_FILE_PATH = "ProEditor/license.json";
    private static final String SECRET_KEY = "16CharSecretKey!";
    private final Context context;
    public boolean lic_valid = false;

    public LicenseManager(Context context) {
        this.context = context;
    }

    public boolean licenseFileExists() {
        File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
        return file.exists();
    }

    public boolean isLicenseValid() {
        return lic_valid;
    }

    public String readLicenseFile() {
        try {
            FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH));
            byte[] data = new byte[(int) fis.getChannel().size()];
            fis.read(data);
            fis.close();

            String decryptedData = decrypt(new String(data, StandardCharsets.UTF_8));
            JSONObject jsonObject = new JSONObject(decryptedData);
            return jsonObject.getString("username");
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao ler o arquivo de licença";
        }
    }

    public String readLicenseFile2() {
        try {
            File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
            FileInputStream fis = new FileInputStream(licenseFile);
            byte[] data = new byte[(int) fis.getChannel().size()];
            fis.read(data);
            fis.close();

            String decryptedData = decrypt(new String(data, StandardCharsets.UTF_8));
            JSONObject jsonObject = new JSONObject(decryptedData);

            String storedHash = jsonObject.getString("hash");
            jsonObject.remove("hash");

            String originalFileName = jsonObject.getString("original_file_name");
            String jsonTimestamp = jsonObject.getString("timestamp");
            long jsonTimestampCompleto = jsonObject.getLong("timestamp_completo");

            String recalculatedHash = calculateHash(jsonObject.toString());

            String fileTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date(licenseFile.lastModified()));

            boolean isIntegrityViolated = false;
            lic_valid = true;

            if (!storedHash.equals(recalculatedHash)) {
                isIntegrityViolated = true;
                lic_valid = false;
            }

            if (!jsonTimestamp.equals(fileTimestamp)) {
                isIntegrityViolated = true;
                lic_valid = false;
            }

            if (!originalFileName.equals(licenseFile.getName())) {
                isIntegrityViolated = true;
                lic_valid = false;
            }

            long fileTimestampCompleto = licenseFile.lastModified();
            if (Math.abs(jsonTimestampCompleto - fileTimestampCompleto) > 50) {
                isIntegrityViolated = true;
                lic_valid = false;
            }

            jsonObject.put("integrity_status", isIntegrityViolated ? "violated" : "verified");
            jsonObject.put("file_timestamp", fileTimestamp);

            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorJson = new JSONObject();
            try {
                errorJson.put("error", "Erro ao ler o arquivo de licença");
            } catch (Exception jsonException) {
                jsonException.printStackTrace();
            }
            return errorJson.toString();
        }
    }

    public void saveLicenseFile(String username, String phoneNumber) {
        try {
            if (username.trim().isEmpty() || phoneNumber.trim().isEmpty()) {
                showToast("Preencha todos os campos");
                return;
            }

            File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
            Objects.requireNonNull(file.getParentFile()).mkdirs();
            FileOutputStream fos = new FileOutputStream(file);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", username);
            jsonObject.put("phone_number", phoneNumber);
            jsonObject.put("device_id", Build.ID);
            jsonObject.put("device_model", Build.MODEL);
            jsonObject.put("device_product", Build.PRODUCT);
            jsonObject.put("android_version", Build.VERSION.RELEASE);

            String timeStamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
            jsonObject.put("timestamp", timeStamp);

            String originalFileName = LICENSE_FILE_PATH;
            jsonObject.put("original_file_name", "license.json");

            String salt = UUID.randomUUID().toString();
            jsonObject.put("salt", salt);

            jsonObject.put("timestamp_completo", System.currentTimeMillis());

            String jsonString = jsonObject.toString();
            String hash = calculateHash(jsonString);
            jsonObject.put("hash", hash);

            String encryptedData = encrypt(jsonObject.toString());
            fos.write(encryptedData.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Thread.sleep(3000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String calculateHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String encrypt(String data) throws Exception {
        Key key = new SecretKeySpec(LicenseManager.SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedData, Base64.DEFAULT);
    }

    private String decrypt(String data) throws Exception {
        Key key = new SecretKeySpec(LicenseManager.SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedData = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public void deleteLicenseFile() {
        File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
        if (file.exists()) {
            if (file.delete()) {
                showToast("Arquivo de licença deletado com sucesso");
            } else {
                showToast("Falha ao deletar o arquivo de licença");
            }
        } else {
            showToast("Arquivo de licença não encontrado");
        }
    }
}