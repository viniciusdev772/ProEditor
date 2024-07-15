package com.viniciusdev.marketproeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String LICENSE_FILE_PATH = "ProEditor/license.json";
    private static final int REQUEST_PERMISSION = 1001;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializePermissionLauncher();

        if (licenseFileExists()) {
            // Se o arquivo de licença existir, vá diretamente para HomeActivity
            navigateToHome();
        } else {
            // Verificar e solicitar permissões
            checkPermissions();
        }
    }

    private void initializePermissionLauncher() {
        requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean manageExternalStorageGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manageExternalStorageGranted = Environment.isExternalStorageManager();
            }

            if (manageExternalStorageGranted) {
                // Se as permissões foram concedidas, mostre a tela inicial
                showMainActivityLayout();
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
                showMainActivityLayout();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            } else {
                showMainActivityLayout();
            }
        } else {
            showMainActivityLayout();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    showMainActivityLayout();
                } else {
                    Toast.makeText(this, "Permissão de gerenciamento de todos os arquivos negada", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean licenseFileExists() {
        File file = new File(Environment.getExternalStorageDirectory(), LICENSE_FILE_PATH);
        return file.exists();
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish(); // Finaliza MainActivity para que o usuário não possa voltar para ela
    }

    private void showMainActivityLayout() {
        setContentView(R.layout.activity_main);

        Button getStartedButton = findViewById(R.id.get_started_button);
        getStartedButton.setOnClickListener(v -> navigateToHome());
    }
}
