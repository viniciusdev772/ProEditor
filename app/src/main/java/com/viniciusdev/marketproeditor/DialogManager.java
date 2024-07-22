package com.viniciusdev.marketproeditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class DialogManager {

    public static final String LICENSE_FILE_PATH = ".ProEditor/ProAccount/licence.json";
    private final Context context;
    private final LicenseManager licenseManager;
    private ProgressBar progressBar;
    private TextView progressText;
    private AlertDialog progressDialog;

    public DialogManager(Context context, LicenseManager licenseManager) {
        this.context = context;
        this.licenseManager = licenseManager;
    }

    public void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_user_info, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        EditText usernameInput = dialogView.findViewById(R.id.username);
        EditText phoneNumberInput = dialogView.findViewById(R.id.phone_number);
        TextView submitButton = dialogView.findViewById(R.id.submit_button);

        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String phoneNumber = phoneNumberInput.getText().toString();
            showProgressDialog(); // Mostrar o diálogo de progresso
            new Thread(() -> {
                licenseManager.saveLicenseFile(username, phoneNumber);
                sendLicenseToServer();
                ((HomeActivity) context).runOnUiThread(() -> {
                    dismissProgressDialog(); // Fechar o diálogo de progresso
                    dialog.dismiss(); // Fechar o diálogo de informações do usuário
                     // Reiniciar a atividade para atualizar o nome do usuário
                });
            }).start();
        });

        dialog.show();
    }

    @SuppressLint("HardwareIds")
    public void showLicenseInfoDialog() {
        String licenseInfo = licenseManager.readLicenseFile2();

        if (licenseInfo == null) {
            Toast.makeText(context, "Erro ao ler o arquivo de licença", Toast.LENGTH_SHORT).show();
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
            String actualAndroidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            // Comparar informações
            boolean isDeviceModelMatch = Objects.equals(deviceModel, actualDeviceModel);
            boolean isDeviceProductMatch = Objects.equals(deviceProduct, actualDeviceProduct);
            boolean isAndroidVersionMatch = Objects.equals(androidVersion, actualAndroidVersion);
            boolean isAndroidIdMatch = Objects.equals(androidId, actualAndroidId);

            String message = "Nome: " + username +
                    "\nTelefone: " + phoneNumber +
                    "\nID do Dispositivo: " + deviceId +
                    "\nModelo do Dispositivo: " + deviceModel + (isDeviceModelMatch ? " (correspondente)" : " (não correspondente)") +
                    "\nProduto do Dispositivo: " + deviceProduct + (isDeviceProductMatch ? " (correspondente)" : " (não correspondente)") +
                    "\nVersão do Android: " + androidVersion + (isAndroidVersionMatch ? " (correspondente)" : " (não correspondente)") +
                    "\nID Unico do Dispositivo: " + androidId + (isAndroidIdMatch ? " (correspondente)" : " (não correspondente)") +
                    "\nTimestamp do JSON: " + jsonTimestamp +
                    "\nTimestamp do Arquivo: " + fileTimestamp +
                    "\nStatus de Integridade: ";

            SpannableString spannableMessage = new SpannableString(message + integrityStatus);
            int color = integrityStatus.equals("verified") ? ContextCompat.getColor(context, android.R.color.holo_green_dark) : ContextCompat.getColor(context, android.R.color.holo_red_dark);
            spannableMessage.setSpan(new ForegroundColorSpan(color), message.length(), message.length() + integrityStatus.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            new MaterialAlertDialogBuilder(context)
                    .setTitle("Informações da Licença")
                    .setMessage(spannableMessage)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    public void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
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

            Random random = new Random();

            for (int progress = 0; progress <= 100; progress += 10) {
                final int currentProgress = progress;
                final String currentMessage = messages[random.nextInt(messages.length)];
                ((HomeActivity) context).runOnUiThread(() -> {
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

    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Situação Delicada")
                .setMessage("Se você já enviou essa licença ao desenvolvedor e ela já está ativa no ProEditor, e se você apagar você irá perder o acesso, tendo que aguardar o desenvolvedor aprovar sua licença manualmente no servidor novamente. Sendo assim, pense bem na espera que terá que você irá gerar para uma nova licença.")
                .setCancelable(false)
                .setPositiveButton("Sim", (dialog, id) -> new MaterialAlertDialogBuilder(context)
                        .setMessage("Você realmente quer apagar a licença?")
                        .setCancelable(false)
                        .setPositiveButton("Sim", (dialog1, id1) -> new MaterialAlertDialogBuilder(context)
                                .setMessage("Essa é sua última chance. Apagar a licença?")
                                .setCancelable(false)
                                .setPositiveButton("Sim", (dialog2, id2) -> {
                                    licenseManager.deleteLicenseFile();
                                    ((HomeActivity) context).restartActivity();
                                })
                                .setNegativeButton("Não", (dialog2, id2) -> dialog2.dismiss())
                                .show())
                        .setNegativeButton("Não", (dialog1, id1) -> dialog1.dismiss())
                        .show())
                .setNegativeButton("Não", (dialog, id) -> dialog.dismiss())
                .show();
    }

    private void sendLicenseToServer() {
        new Thread(() -> {
            try {
                // Caminho do arquivo de licença
                File licenseFile = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);

                // Ler o conteúdo do arquivo
                FileInputStream fis = new FileInputStream(licenseFile);
                byte[] data = new byte[(int) fis.getChannel().size()];
                fis.read(data);
                fis.close();

                // O conteúdo do arquivo já está em Base64, não precisa codificar
                String licenseContentBase64 = new String(data);

                // Criar o objeto JSON com a chave "license_base64"
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("license_base64", licenseContentBase64);

                // Enviar o objeto JSON para a API
                URL url = new URL("https://proeditor.viniciusdev.com.br/api/licenses"); // Substitua pela URL da sua API
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setDoOutput(true);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(jsonObject.toString());
                writer.flush();
                writer.close();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == 201) {
                    BufferedReader responseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String responseLine;
                    while ((responseLine = responseReader.readLine()) != null) {
                        responseBuilder.append(responseLine);
                    }
                    responseReader.close();

                    // Decodificar a resposta do servidor de Base64
                    //String decodedResponse = new String(Base64.decode(responseBuilder.toString(), Base64.DEFAULT));
                    JSONObject decodedResponseJson = new JSONObject(responseBuilder.toString());
                    ((HomeActivity) context).runOnUiThread(() -> {
                        try {
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Resposta do Servidor")
                                    .setMessage(decodedResponseJson.getString("message") + "\nCódigo: " + decodedResponseJson.getString("code"))
                                    .setPositiveButton("COPIAR", (dialog, which) -> {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = null;
                                        try {
                                            clip = ClipData.newPlainText("code", decodedResponseJson.getString("code"));
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(context, "Copiado para a área de transferência", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        ((HomeActivity) context).restartActivity();
                                    })
                                    .show();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    ((HomeActivity) context).runOnUiThread(() -> Toast.makeText(context, "Erro na resposta do servidor: " + responseCode, Toast.LENGTH_SHORT).show());
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                ((HomeActivity) context).runOnUiThread(() -> Toast.makeText(context, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}