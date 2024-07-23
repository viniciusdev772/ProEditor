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
    private static final String LICENSE_FILE_PATH = ".ProEditor/ProAccount/license.json";

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<Intent> manageAllFilesPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializePermissionLaunchers();
        checkPermissions();
    }

    //kk
    private void initializePermissionLaunchers() {
        requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allPermissionsGranted = true;
            for (Boolean granted : result.values()) {
                if (!granted) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                if (licenseFileExists()) {
                    navigateToHome();
                } else {
                    showMainActivityLayout();
                }
            } else {
                Toast.makeText(this, "Permissões de armazenamento negadas", Toast.LENGTH_SHORT).show();
            }
        });

        manageAllFilesPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    if (licenseFileExists()) {
                        navigateToHome();
                    } else {
                        showMainActivityLayout();
                    }
                } else {
                    Toast.makeText(this, "Permissão de gerenciamento de todos os arquivos negada", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                manageAllFilesPermissionLauncher.launch(intent);
            } else {
                if (licenseFileExists()) {
                    navigateToHome();
                } else {
                    showMainActivityLayout();
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            } else {
                if (licenseFileExists()) {
                    navigateToHome();
                } else {
                    showMainActivityLayout();
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
