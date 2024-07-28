package com.viniciusdev.proeditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class ProjetoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private Stack<String> directoryHistory;
    private String currentPath;
    private String pasta;
    private FloatingActionButton fabDelete;
    private boolean multiSelectMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projeto);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.btn_run).setOnClickListener(v -> {
            Toast.makeText(this, "RUN clicked", Toast.LENGTH_SHORT).show();
        });

        toolbar.findViewById(R.id.btn_multi_select).setOnClickListener(v -> {
            toggleMultiSelectMode();
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_xml) {
                    Toast.makeText(ProjetoActivity.this, "XML selected", Toast.LENGTH_SHORT).show();
                    listFilesWithExtension(pasta, ".xml");
                    return true;
                }
                if (itemId == R.id.nav_java) {
                    Toast.makeText(ProjetoActivity.this, "Java selected", Toast.LENGTH_SHORT).show();
                    listFilesWithExtension(pasta, ".java");
                    return true;
                }
                if (itemId == R.id.nav_home) {
                    Toast.makeText(ProjetoActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
                    listFilesAndDirectories(pasta);
                    return true;
                }
                return false;
            }
        });

        fabDelete = findViewById(R.id.fab_delete);
        fabDelete.setOnClickListener(v -> showDeleteConfirmationDialogForSelectedItems());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        directoryHistory = new Stack<>();
        currentPath = getIntent().getStringExtra("projectName");
        pasta = "/sdcard/.sketchware/mysc/" + currentPath + "/app/";
        listFilesAndDirectories(pasta);
    }

    private void listFilesAndDirectories(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();
        List<FileItem> fileList = new ArrayList<>();

        if (files != null && files.length > 0) {
            for (File file : files) {
                fileList.add(new FileItem(file.getName(), file.isDirectory(), file.getAbsolutePath()));
            }
        } else {
            fileList.add(new FileItem("EMPTY", false, ""));
        }

        fileAdapter = new FileAdapter(fileList);
        fileAdapter.setOnItemClickListener(item -> {
            if (item.isDirectory()) {
                directoryHistory.push(currentPath);
                currentPath = item.getPath();
                listFilesAndDirectories(currentPath);
            } else if (item.getName().endsWith(".java") || item.getName().endsWith(".xml")) {
                openEditorActivity(item.getPath());
            }
            updateFabVisibility();
        });
        fileAdapter.setOnItemLongClickListener(item -> {
            if (!item.isDirectory()) {
                showDeleteConfirmationDialog(item);
            }
        });
        fileAdapter.setSelectionListener(selectedCount -> updateFabVisibility());
        recyclerView.setAdapter(fileAdapter);
        updateFabVisibility();
    }

    private void listFilesWithExtension(String path, String extension) {
        List<FileItem> fileList = new ArrayList<>();
        findFilesWithExtension(new File(path), extension, fileList);

        if (fileList.isEmpty()) {
            fileList.add(new FileItem("EMPTY", false, ""));
        }

        fileAdapter = new FileAdapter(fileList);
        fileAdapter.setOnItemClickListener(item -> {
            if (item.isDirectory()) {
                directoryHistory.push(currentPath);
                currentPath = item.getPath();
                listFilesWithExtension(currentPath, extension);
            } else if (item.getName().endsWith(".java") || item.getName().endsWith(".xml")) {
                openEditorActivity(item.getPath());
            }
            updateFabVisibility();
        });
        fileAdapter.setOnItemLongClickListener(item -> {
            if (!item.isDirectory()) {
                showDeleteConfirmationDialog(item);
            }
        });
        fileAdapter.setSelectionListener(selectedCount -> updateFabVisibility());
        recyclerView.setAdapter(fileAdapter);
        updateFabVisibility();
    }

    private void findFilesWithExtension(File directory, String extension, List<FileItem> fileList) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findFilesWithExtension(file, extension, fileList);
                } else if (file.getName().endsWith(extension)) {
                    fileList.add(new FileItem(file.getName(), false, file.getAbsolutePath()));
                }
            }
        }
    }

    private void showDeleteConfirmationDialog(FileItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Yes", (dialog, which) -> deleteFile(item))
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteConfirmationDialogForSelectedItems() {
        Set<FileItem> selectedItems = fileAdapter.getSelectedItems();
        StringBuilder fileNames = new StringBuilder("Are you sure you want to delete the following files?\n\n");
        for (FileItem item : selectedItems) {
            fileNames.append(item.getName()).append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Files")
                .setMessage(fileNames.toString())
                .setPositiveButton("Yes", (dialog, which) -> deleteSelectedFiles())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteFile(FileItem item) {
        File file = new File(item.getPath());
        if (file.delete()) {
            Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
            listFilesAndDirectories(currentPath);
        } else {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelectedFiles() {
        Set<FileItem> selectedItems = fileAdapter.getSelectedItems();
        boolean allDeleted = true;

        for (FileItem item : selectedItems) {
            File file = new File(item.getPath());
            if (!file.delete()) {
                allDeleted = false;
            }
        }

        if (allDeleted) {
            Toast.makeText(this, "All selected files deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete some files", Toast.LENGTH_SHORT).show();
        }

        listFilesAndDirectories(currentPath);
        fileAdapter.clearSelection();
        updateFabVisibility();
    }

    private void updateFabVisibility() {
        if (fileAdapter.getSelectedItems().isEmpty()) {
            fabDelete.setVisibility(View.GONE);
        } else {
            fabDelete.setVisibility(View.VISIBLE);
        }
    }

    private void toggleMultiSelectMode() {
        multiSelectMode = !multiSelectMode;
        fileAdapter.setMultiSelectMode(multiSelectMode);
        if (!multiSelectMode) {
            fileAdapter.clearSelection();
        }
        Toast.makeText(this, "Multi-select mode " + (multiSelectMode ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        updateFabVisibility();
    }

    private void openEditorActivity(String filePath) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (multiSelectMode) {
            toggleMultiSelectMode();
        } else if (!directoryHistory.isEmpty()) {
            currentPath = directoryHistory.pop();
            listFilesAndDirectories(currentPath);
        } else {
            super.onBackPressed();
        }
    }
}
