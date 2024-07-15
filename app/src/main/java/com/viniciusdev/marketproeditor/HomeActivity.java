package com.viniciusdev.marketproeditor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class HomeActivity extends AppCompatActivity {

    private static final String LICENSE_FILE_PATH = "ProEditor/license.json";
    private static final String SECRET_KEY = "16CharSecretKey!"; // Chave secreta de 16 caracteres

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private static final int REQUEST_PERMISSION = 1001;
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private Handler handler;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView greetingText = findViewById(R.id.greeting_text);
        Button viewLicenseButton = findViewById(R.id.view_license_button);
        Button deleteLicenseButton = findViewById(R.id.delete_license_button);
        Button sendLicenseButton = findViewById(R.id.send_license_button);

        // Determinar a saudação
        String greeting = getGreeting();
        greetingText.setText(greeting);

        // Inicializar o lançador de permissões
        initializePermissionLauncher();

        // Verificar se o arquivo de licença existe
        if (licenseFileExists()) {
            String licenseInfo = readLicenseFile();
            greetingText.setText(greeting + " " + licenseInfo);
            deleteLicenseButton.setVisibility(View.VISIBLE);
            sendLicenseButton.setVisibility(View.VISIBLE);
            viewLicenseButton.setVisibility(View.VISIBLE);
        } else {
            // Verificar e solicitar permissões
            checkPermissions();
            deleteLicenseButton.setVisibility(View.GONE);
            sendLicenseButton.setVisibility(View.GONE);
            viewLicenseButton.setVisibility(View.GONE);
        }

        deleteLicenseButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        sendLicenseButton.setOnClickListener(v -> sendLicenseViaWhatsApp());
        viewLicenseButton.setOnClickListener(v -> showLicenseInfoDialog());

        handler = new Handler();
        random = new Random();
    }

    private void showLicenseInfoDialog() {
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

            String message = "Nome: " + username +
                    "\nTelefone: " + phoneNumber +
                    "\nID do Dispositivo: " + deviceId +
                    "\nModelo do Dispositivo: " + deviceModel +
                    "\nProduto do Dispositivo: " + deviceProduct +
                    "\nVersão do Android: " + androidVersion +
                    "\nTimestamp do JSON: " + jsonTimestamp +
                    "\nTimestamp do Arquivo: " + fileTimestamp +
                    "\nStatus de Integridade: ";

            SpannableString spannableMessage = new SpannableString(message + integrityStatus);
            int color = integrityStatus.equals("verified") ? ContextCompat.getColor(this, android.R.color.holo_green_dark) : ContextCompat.getColor(this, android.R.color.holo_red_dark);
            spannableMessage.setSpan(new ForegroundColorSpan(color), message.length(), message.length() + integrityStatus.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Informações da Licença")
                    .setMessage(spannableMessage)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializePermissionLauncher() {
        requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean manageExternalStorageGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manageExternalStorageGranted = Environment.isExternalStorageManager();
            }

            if (manageExternalStorageGranted) {
                showUserInfoDialog();
            } else {
                Toast.makeText(this, "Permissões de armazenamento negadas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_PERMISSION);
            } else {
                showUserInfoDialog();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            } else {
                showUserInfoDialog();
            }
        } else {
            showUserInfoDialog();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    showUserInfoDialog();
                } else {
                    Toast.makeText(this, "Permissão de gerenciamento de todos os arquivos negada", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getGreeting() {
        int hour = new java.util.Date().getHours();
        if (hour >= 5 && hour < 12) {
            return "Bom Dia";
        } else if (hour >= 12 && hour < 18) {
            return "Boa Tarde";
        } else {
            return "Boa Noite";
        }
    }

    private boolean licenseFileExists() {
        File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
        return file.exists();
    }

    private String readLicenseFile() {
        try {
            FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH));
            byte[] data = new byte[(int) fis.getChannel().size()];
            fis.read(data);
            fis.close();

            // Descriptografar dados
            String decryptedData = decrypt(new String(data, StandardCharsets.UTF_8), SECRET_KEY);
            JSONObject jsonObject = new JSONObject(decryptedData);
            return jsonObject.getString("username");
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao ler o arquivo de licença";
        }
    }

    private String readLicenseFile2() {
        try {
            File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
            FileInputStream fis = new FileInputStream(licenseFile);
            byte[] data = new byte[(int) fis.getChannel().size()];
            fis.read(data);
            fis.close();

            // Descriptografar dados
            String decryptedData = decrypt(new String(data, StandardCharsets.UTF_8), SECRET_KEY);
            JSONObject jsonObject = new JSONObject(decryptedData);

            // Obter o hash armazenado e remover do JSON
            String storedHash = jsonObject.getString("hash");
            jsonObject.remove("hash");

            // Obter o nome original do arquivo e o timestamp do JSON
            String originalFileName = jsonObject.getString("original_file_name");
            String jsonTimestamp = jsonObject.getString("timestamp");

            // Recalcular o hash dos dados JSON (sem o hash)
            String recalculatedHash = calculateHash(jsonObject.toString());

            // Obter o timestamp do arquivo
            String fileTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date(licenseFile.lastModified()));

            // Verificar a integridade do arquivo
            boolean isIntegrityViolated = false;

            if (!storedHash.equals(recalculatedHash)) {
                isIntegrityViolated = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Hash esperado: " + storedHash, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Hash atual: " + recalculatedHash, Toast.LENGTH_LONG).show();
                });
            }

            if (!jsonTimestamp.equals(fileTimestamp)) {
                isIntegrityViolated = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Timestamp do JSON esperado: " + jsonTimestamp, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Timestamp do arquivo atual: " + fileTimestamp, Toast.LENGTH_LONG).show();
                });
            }

            if (!originalFileName.equals(licenseFile.getName())) {
                isIntegrityViolated = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Nome do arquivo esperado: " + originalFileName, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Nome do arquivo atual: " + licenseFile.getName(), Toast.LENGTH_LONG).show();
                });
            }

            // Definir status de integridade
            jsonObject.put("integrity_status", isIntegrityViolated ? "violated" : "verified");

            // Adicionar o timestamp do arquivo no JSON
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



    private void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_info, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        EditText usernameInput = dialogView.findViewById(R.id.username);
        EditText phoneNumberInput = dialogView.findViewById(R.id.phone_number);
        TextView submitButton = dialogView.findViewById(R.id.submit_button);

        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                String phoneNumber = phoneNumberInput.getText().toString();
                showProgressDialog(); // Mostrar o diálogo de progresso
                new Thread(() -> {
                    saveLicenseFile(username, phoneNumber);
                    runOnUiThread(() -> {
                        dismissProgressDialog(); // Fechar o diálogo de progresso
                        dialog.dismiss(); // Fechar o diálogo de informações do usuário
                        restartActivity(); // Reiniciar a atividade para atualizar o nome do usuário
                    });
                }).start();
            }
        });

        dialog.show();
    }

    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_progress, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        progressBar = dialogView.findViewById(R.id.progress_bar);
        progressText = dialogView.findViewById(R.id.progress_text);

        progressDialog = builder.create();
        progressDialog.show();

        // Simulação de progresso com troca de texto aleatória
        new Thread(() -> {
            String[] messages = {
                    "Gerando licença...",
                    "Aguarde um momento...",
                    "Quase pronto...",
                    "Processando dados...",
                    "Finalizando...",
                    "Verificando informações...",
                    "Calculando dados...",
                    "Preparando arquivo...",
                    "Compactando informações...",
                    "Validando dados...",
                    "Atualizando sistema...",
                    "Lendo dados do dispositivo...",
                    "Aplicando configurações...",
                    "Carregando informações...",
                    "Quase lá...",
                    "Aguarde mais um pouco...",
                    "Progresso quase completo...",
                    "Aproximando-se da conclusão...",
                    "Finalizando detalhes finais...",
                    "Concluindo processo..."
            };

            for (int progress = 0; progress <= 100; progress += 10) {
                final int currentProgress = progress;
                final String currentMessage = messages[random.nextInt(messages.length)];
                runOnUiThread(() -> {
                    progressBar.setProgress(currentProgress);
                    progressText.setText(currentMessage + " " + currentProgress + "%");
                });
                try {
                    Thread.sleep(1000); // Atraso de 1 segundo
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void saveLicenseFile(String username, String phoneNumber) {
        try {
            // Verificar se os campos estão vazios
            if (username.trim().isEmpty() || phoneNumber.trim().isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show());
                return;
            }

            // Obter informações do dispositivo
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", username);
            jsonObject.put("phone_number", phoneNumber);
            jsonObject.put("device_id", Build.ID);
            jsonObject.put("device_model", Build.MODEL);
            jsonObject.put("device_product", Build.PRODUCT);
            jsonObject.put("android_version", Build.VERSION.RELEASE);

            // Adicionar timestamp
            String timeStamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
            jsonObject.put("timestamp", timeStamp);

            // Adicionar nome original do arquivo
            String originalFileName = LICENSE_FILE_PATH;
            jsonObject.put("original_file_name", "license.json");

            // Adicionar um salt (UUID)
            String salt = UUID.randomUUID().toString();
            jsonObject.put("salt", salt);

            // Calcular o hash dos dados JSON
            String jsonString = jsonObject.toString();
            String hash = calculateHash(jsonString);
            jsonObject.put("hash", hash);

            // Criptografar dados
            String encryptedData = encrypt(jsonObject.toString(), SECRET_KEY);

            File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(encryptedData.getBytes(StandardCharsets.UTF_8));
            fos.close();
            // Adicionar atraso simulado
            Thread.sleep(3000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
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

    private String encrypt(String data, String secretKey) throws Exception {
        Key key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedData, Base64.DEFAULT);
    }

    private String decrypt(String data, String secretKey) throws Exception {
        Key key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedData = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Situação Delicada")
                .setMessage("Se você já enviou essa licença ao desenvolvedor e ela já está ativa no ProEditor, e se você apagar você irá perder o acesso, tendo que aguardar o desenvolvedor aprovar sua licença manualmente no servidor novamente. Sendo assim, pense bem na espera que terá que você irá gerar para uma nova licença.")
                .setCancelable(false)
                .setPositiveButton("Sim", (dialog, id) -> {
                    new MaterialAlertDialogBuilder(this)
                            .setMessage("Você realmente quer apagar a licença?")
                            .setCancelable(false)
                            .setPositiveButton("Sim", (dialog1, id1) -> {
                                new MaterialAlertDialogBuilder(this)
                                        .setMessage("Essa é sua última chance. Apagar a licença?")
                                        .setCancelable(false)
                                        .setPositiveButton("Sim", (dialog2, id2) -> deleteLicenseFile())
                                        .setNegativeButton("Não", (dialog2, id2) -> dialog2.dismiss())
                                        .show();
                            })
                            .setNegativeButton("Não", (dialog1, id1) -> dialog1.dismiss())
                            .show();
                })
                .setNegativeButton("Não", (dialog, id) -> dialog.dismiss())
                .show();
    }

    private void deleteLicenseFile() {
        File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
        if (file.exists()) {
            if (file.delete()) {
                Toast.makeText(this, "Arquivo de licença deletado com sucesso", Toast.LENGTH_SHORT).show();
                restartActivity();
            } else {
                Toast.makeText(this, "Falha ao deletar o arquivo de licença", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Arquivo de licença não encontrado", Toast.LENGTH_SHORT).show();


        }
    }

    private void sendLicenseViaWhatsApp() {
        try {
            File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);

            if (!licenseFile.exists()) {
                Toast.makeText(this, "Arquivo de licença não encontrado", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", licenseFile);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.setType("application/json");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (sendIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(sendIntent);
            } else {
                Toast.makeText(this, "Nenhum aplicativo disponível para enviar o arquivo", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}