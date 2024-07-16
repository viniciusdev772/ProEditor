package com.viniciusdev.marketproeditor;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private LicenseManager licenseManager;
    private PermissionManager permissionManager;
    private DialogManager dialogManager;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        licenseManager = new LicenseManager(this);
        dialogManager = new DialogManager(this, licenseManager);

        requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean manageExternalStorageGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manageExternalStorageGranted = Environment.isExternalStorageManager();
            }

            if (manageExternalStorageGranted) {
                dialogManager.showUserInfoDialog();
            } else {
                Toast.makeText(this, "Permissões de armazenamento negadas", Toast.LENGTH_SHORT).show();
            }
        });

        permissionManager = new PermissionManager(this, requestPermissionsLauncher);

        TextView greetingText = findViewById(R.id.greeting_text);
        Button viewLicenseButton = findViewById(R.id.view_license_button);
        Button deleteLicenseButton = findViewById(R.id.delete_license_button);
        Button sendLicenseButton = findViewById(R.id.send_license_button);

        String greeting = getGreeting();
        greetingText.setText(greeting);

        if (licenseManager.licenseFileExists()) {
            String licenseInfo = licenseManager.readLicenseFile2();
            if(!licenseManager.isLicenseValid()){
                sendLicenseButton.setVisibility(View.GONE);
                viewLicenseButton.setVisibility(View.GONE);
                deleteLicenseButton.setText("Arrumar Licença");
                deleteLicenseButton.setOnClickListener(v -> dialogManager.showDeleteConfirmationDialog());
            }else{
                ///String licenseInfo = licenseManager.readLicenseFile();
                greetingText.setText(greeting + " " + licenseManager.readLicenseFile());
                deleteLicenseButton.setVisibility(View.VISIBLE);
                sendLicenseButton.setVisibility(View.VISIBLE);
                viewLicenseButton.setVisibility(View.VISIBLE);
            }

        } else {
            permissionManager.checkPermissions();
            dialogManager.showUserInfoDialog();
            deleteLicenseButton.setVisibility(View.GONE);
            sendLicenseButton.setVisibility(View.GONE);
            viewLicenseButton.setVisibility(View.GONE);
        }

        deleteLicenseButton.setOnClickListener(v -> dialogManager.showDeleteConfirmationDialog());
        sendLicenseButton.setOnClickListener(v -> sendLicenseViaWhatsApp());
        viewLicenseButton.setOnClickListener(v -> dialogManager.showLicenseInfoDialog());

        random = new Random();
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

    private void sendLicenseViaWhatsApp() {
        try {
            File licenseFile = new File(Environment.getExternalStorageDirectory(), LicenseManager.LICENSE_FILE_PATH);

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

    public void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionManager.onActivityResult(requestCode, resultCode, data);
    }
}
