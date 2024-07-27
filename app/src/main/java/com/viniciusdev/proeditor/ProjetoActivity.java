package com.viniciusdev.proeditor;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ProjetoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private Stack<String> directoryHistory;
    private String currentPath;
    private String pasta ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projeto);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.btn_run).setOnClickListener(v -> {
            Toast.makeText(this, "RUN clicked", Toast.LENGTH_SHORT).show();
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

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        directoryHistory = new Stack<>();
        currentPath = getIntent().getStringExtra("projectName");
        pasta = "/sdcard/.sketchware/mysc/" + currentPath + "/app/";
        listFilesAndDirectories("/sdcard/.sketchware/mysc/" + currentPath + "/app/");
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
            }
        });
        recyclerView.setAdapter(fileAdapter);
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
            }
        });
        recyclerView.setAdapter(fileAdapter);
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

    @Override
    public void onBackPressed() {
        if (!directoryHistory.isEmpty()) {
            currentPath = directoryHistory.pop();
            listFilesAndDirectories(currentPath);
        } else {
            super.onBackPressed();
        }
    }
}
