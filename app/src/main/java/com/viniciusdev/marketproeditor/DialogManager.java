package com.viniciusdev.marketproeditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.util.Random;

public class DialogManager {

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
                ((HomeActivity) context).runOnUiThread(() -> {
                    dismissProgressDialog(); // Fechar o diálogo de progresso
                    dialog.dismiss(); // Fechar o diálogo de informações do usuário
                    ((HomeActivity) context).restartActivity(); // Reiniciar a atividade para atualizar o nome do usuário
                });
            }).start();
        });

        dialog.show();
    }

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
}
