package com.viniciusdev.proeditor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private final PermissionManager permissionManager;

    public MainActivity(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionManager.checkPermissions();
        setContentView(R.layout.activity_main);

    }
}