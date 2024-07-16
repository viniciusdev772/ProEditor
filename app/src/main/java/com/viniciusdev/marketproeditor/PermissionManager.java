package com.viniciusdev.marketproeditor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class PermissionManager {

    private static final int REQUEST_PERMISSION = 1001;
    private Context context;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    public PermissionManager(Context context, ActivityResultLauncher<String[]> requestPermissionsLauncher) {
        this.context = context;
        this.requestPermissionsLauncher = requestPermissionsLauncher;
    }

    public void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                ((Activity) context).startActivityForResult(intent, REQUEST_PERMISSION);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(context, "Permissões concedidas", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Permissão de gerenciamento de todos os arquivos negada", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
