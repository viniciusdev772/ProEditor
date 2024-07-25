package com.viniciusdev.proeditor;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean manageExternalStorageGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manageExternalStorageGranted = Environment.isExternalStorageManager();
            }

            if (Boolean.TRUE.equals(manageExternalStorageGranted)) {

            } else {
                Toast.makeText(this, "Permissões de armazenamento negadas", Toast.LENGTH_SHORT).show();
            }
        });
        PermissionManager permissionManager = new PermissionManager(this, requestPermissionsLauncher);
        permissionManager.checkPermissions();
        setContentView(R.layout.activity_main);

    }
}