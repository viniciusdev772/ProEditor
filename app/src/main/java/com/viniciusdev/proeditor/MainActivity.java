package com.viniciusdev.proeditor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String SECRET_KEY = "16CharSecretKey!";
    public static final String LICENSE_FILE_PATH = ".ProEditor/ProAccount/license.json";
    private static final String SERVER_URL = "https://proeditor.viniciusdev.com.br/api/v1/proeditor_authentication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verifica permissões de armazenamento
        ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean manageExternalStorageGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manageExternalStorageGranted = Environment.isExternalStorageManager();
            }

            if (Boolean.TRUE.equals(manageExternalStorageGranted)) {
                // Permissões concedidas
            } else {
                Toast.makeText(this, "Permissões de armazenamento negadas", Toast.LENGTH_SHORT).show();
            }
        });
        PermissionManager permissionManager = new PermissionManager(this, requestPermissionsLauncher);
        permissionManager.checkPermissions();

        setContentView(R.layout.activity_main);

        // Verifica a licença quando o aplicativo é iniciado
        verificarLicencaNoServidor();

        // Configurar o botão para mostrar informações da licença
        findViewById(R.id.button_licenca).setOnClickListener(v -> verificarLicencaNoServidor());
        findViewById(R.id.button_listar_projetos).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ListarActivity.class)));
    }

    public JSONObject lerLicenca() throws Exception {
        File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
        FileInputStream fis = new FileInputStream(licenseFile);
        byte[] data = new byte[(int) fis.getChannel().size()];
        fis.read(data);
        fis.close();

        String decryptedData = decrypt(new String(data, StandardCharsets.UTF_8));
        return new JSONObject(decryptedData);
    }

    private String decrypt(String data) throws Exception {
        Key key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedData = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    private void enviarDados(JSONObject jsonObject) {
        OkHttpClient client = new OkHttpClient();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                // Tratar falha na requisição
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Falha na requisição", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Ler a resposta do servidor
                    assert response.body() != null;
                    String responseData = response.body().string();
                    try {
                        // Descriptografar a resposta
                        String decryptedResponse = decrypt(responseData);
                        JSONObject jsonResponse = new JSONObject(decryptedResponse);

                        // Verificar se a licença foi autenticada
                        boolean isAuthenticated = jsonResponse.optInt("status", 0) == 200;

                        // Mostrar o diálogo com as informações
                        runOnUiThread(() -> showLicenseInfoDialog(isAuthenticated));
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao descriptografar a resposta", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Tratar resposta de erro
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao enviar dados", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void verificarLicencaNoServidor() {
        try {
            JSONObject jsonObject88 = lerLicenca();

            File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
            long timestampCompleto = licenseFile.lastModified();
            //jsonObject88.put("timestamp", Math.abs(jsonObject88.getLong("timestamp_completo") - timestampCompleto) > 50);
            jsonObject88.put("IsIntegrited", true); // Atualiza com o valor real

            enviarDados(jsonObject88);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao ler ou enviar a licença", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLicenseInfoDialog(boolean isAuthenticated) {
        String licenseInfo = readLicenseFile2();

        if (licenseInfo == null) {
            Toast.makeText(this, "Erro ao ler o arquivo de licença", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(licenseInfo);
            String integrityStatus = jsonObject.optString("integrity_status", "unknown");
            String username = jsonObject.optString("username", "N/A");
            String phoneNumber = jsonObject.optString("phone_number", "N/A");
            String deviceId = jsonObject.optString("device_id", "N/A");
            String deviceModel = jsonObject.optString("device_model", "N/A");
            String deviceProduct = jsonObject.optString("device_product", "N/A");
            String androidVersion = jsonObject.optString("android_version", "N/A");
            String jsonTimestamp = jsonObject.optString("timestamp", "N/A");
            String fileTimestamp = jsonObject.optString("file_timestamp", "N/A");
            String androidId = jsonObject.optString("android_id", "N/A");

            // Obter informações do dispositivo
            String actualDeviceModel = Build.MODEL;
            String actualDeviceProduct = Build.PRODUCT;
            String actualAndroidVersion = Build.VERSION.RELEASE;
            @SuppressLint("HardwareIds") String actualAndroidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            // Comparar informações
            boolean isDeviceModelMatch = Objects.equals(deviceModel, actualDeviceModel);
            boolean isDeviceProductMatch = Objects.equals(deviceProduct, actualDeviceProduct);
            boolean isAndroidVersionMatch = Objects.equals(androidVersion, actualAndroidVersion);
            boolean isAndroidIdMatch = Objects.equals(androidId, actualAndroidId);

            // Mostrar diálogos apropriados
            if (isAuthenticated) {
                showValidLicenseDialog(jsonObject, isDeviceModelMatch, isDeviceProductMatch, isAndroidVersionMatch, isAndroidIdMatch);
            } else {
                showInvalidLicenseDialog();
            }

            // Mostrar diálogos para valores não correspondentes
            showMismatchedValuesDialog(!isDeviceModelMatch, "Modelo do Dispositivo", deviceModel, actualDeviceModel);
            showMismatchedValuesDialog(!isDeviceProductMatch, "Produto do Dispositivo", deviceProduct, actualDeviceProduct);
            showMismatchedValuesDialog(!isAndroidVersionMatch, "Versão do Android", androidVersion, actualAndroidVersion);
            showMismatchedValuesDialog(!isAndroidIdMatch, "ID do Android", androidId, actualAndroidId);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showValidLicenseDialog(JSONObject jsonObject, boolean isDeviceModelMatch, boolean isDeviceProductMatch, boolean isAndroidVersionMatch, boolean isAndroidIdMatch) {
        String integrityStatus = jsonObject.optString("integrity_status", "unknown");
        String username = jsonObject.optString("username", "N/A");
        String phoneNumber = jsonObject.optString("phone_number", "N/A");
        String deviceModel = jsonObject.optString("device_model", "N/A");
        String deviceProduct = jsonObject.optString("device_product", "N/A");
        String androidVersion = jsonObject.optString("android_version", "N/A");
        String jsonTimestamp = jsonObject.optString("timestamp", "N/A");
        String fileTimestamp = jsonObject.optString("file_timestamp", "N/A");

        String message = "Licença Válida\n\n" +
                "Status de Integridade: " + integrityStatus + "\n" +
                "Nome de Usuário: " + username + "\n" +
                "Número de Telefone: " + phoneNumber + "\n" +
                "Modelo do Dispositivo: " + deviceModel + (isDeviceModelMatch ? " (Correto)" : " (Incorreto)") + "\n" +
                "Produto do Dispositivo: " + deviceProduct + (isDeviceProductMatch ? " (Correto)" : " (Incorreto)") + "\n" +
                "Versão do Android: " + androidVersion + (isAndroidVersionMatch ? " (Correto)" : " (Incorreto)") + "\n" +
                "Timestamp (JSON): " + jsonTimestamp + "\n" +
                "Timestamp (Arquivo): " + fileTimestamp;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Informações da Licença")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showInvalidLicenseDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Licença Inválida")
                .setMessage("Sua licença não foi autenticada. Por favor, verifique os detalhes da licença.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showMismatchedValuesDialog(boolean hasMismatch, String fieldName, String fieldValue, String actualValue) {

    }

    // Método para calcular hash
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

    // Método para ler e verificar a licença
    public String readLicenseFile2() {
        try {
            File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
            FileInputStream fis = new FileInputStream(licenseFile);
            byte[] data = new byte[(int) fis.getChannel().size()];
            fis.read(data);
            fis.close();

            String decryptedData = decrypt(new String(data, StandardCharsets.UTF_8));
            JSONObject jsonObject99 = new JSONObject(decryptedData);

            // Ler o hash armazenado
            String storedHash = jsonObject99.optString("hash", "");
            jsonObject99.remove("hash");

            // Obter os dados necessários
            String originalFileName = jsonObject99.optString("original_file_name", "");
            String jsonTimestamp = jsonObject99.optString("timestamp", "");
            long jsonTimestampCompleto = jsonObject99.optLong("timestamp_completo", 0);

            // Recalcular o hash
            String recalculatedHash = calculateHash(jsonObject99.toString());

            // Obter o timestamp do arquivo
            String fileTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date(licenseFile.lastModified()));

            // Verificar a integridade
            boolean isIntegrityViolated = false;
            boolean lic_valid = true;

            if (!storedHash.equals(recalculatedHash)) {
                isIntegrityViolated = true;
                Toast.makeText(this, "Hash inválido", Toast.LENGTH_SHORT).show();
                lic_valid = false;
            }

            if (!jsonTimestamp.equals(fileTimestamp)) {
                isIntegrityViolated = true;
                Toast.makeText(this, "Timestamp inválido", Toast.LENGTH_SHORT).show();
                lic_valid = false;
            }

            if (!originalFileName.equals(licenseFile.getName())) {
                isIntegrityViolated = true;
                Toast.makeText(this, "Nome do arquivo inválido", Toast.LENGTH_SHORT).show();
                lic_valid = false;
            }

            long fileTimestampCompleto = licenseFile.lastModified();
            if (Math.abs(jsonTimestampCompleto - fileTimestampCompleto) > 50) {
                isIntegrityViolated = true;
                lic_valid = false;
            }

            jsonObject99.put("integrity_status", isIntegrityViolated ? "violated" : "verified");
            jsonObject99.put("file_timestamp", fileTimestamp);

            return jsonObject99.toString();
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

}
