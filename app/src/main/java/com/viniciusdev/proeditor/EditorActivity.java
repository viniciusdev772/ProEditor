package com.viniciusdev.proeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;

public class EditorActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private CodeEditor codeEditor;
    private Map<String, String> openFiles;
    private Map<String, Boolean> fileChanged;
    private Map<String, String> editedFiles;
    private String currentFilePath;
    private static final String PREFS_NAME = "RecentFiles";
    private static final String EDITED_FILES_DIR = Environment.getExternalStorageDirectory().getPath() + "/ProEditor/edited/";
    private DrawerLayout drawerLayout;
    private float textSize = 14;
    private TabLayout tabLayout;
    private Handler handler;
    private Runnable changeChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        codeEditor = findViewById(R.id.codeEditor);
        drawerLayout = findViewById(R.id.drawer_layout);
        tabLayout = findViewById(R.id.tabLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar o ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Ajustar Toolbar para não invadir a StatusBar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );
            return insets;
        });

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        openFiles = new HashMap<>();
        fileChanged = new HashMap<>();
        editedFiles = new HashMap<>();

        // Definir o tamanho da fonte para 14
        codeEditor.setTextSize(textSize);

        // Carregar arquivos recentes
        loadRecentFiles();

        // Abrir o arquivo passado pela Intent
        String filePath = getIntent().getStringExtra("filePath");
        if (filePath != null) {
            openFile(filePath);
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> showDialog());

        setupErrorDetectionAndAutoComplete();

        // Iniciar verificação periódica de alterações no texto
        handler = new Handler();
        changeChecker = new Runnable() {
            @Override
            public void run() {
                checkForChanges();
                handler.postDelayed(this, 500); // Verificar a cada 500ms
            }
        };
        handler.postDelayed(changeChecker, 500);

        // Atualizar o número de arquivos recentes no menu
        updateRecentFilesCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveRecentFiles();
        handler.removeCallbacks(changeChecker); // Parar a verificação quando a atividade for destruída
    }

    private void openFile(String filePath) {
        if (openFiles.containsKey(filePath)) {
            switchToFile(filePath);
            return;
        }

        try {
            String code = new String(Files.readAllBytes(new File(filePath).toPath()));
            codeEditor.setText(code);
            setLanguage(filePath);
            addTab(filePath);
            currentFilePath = filePath;
            openFiles.put(filePath, code); // Adiciona o conteúdo ao mapa
            fileChanged.put(filePath, false);
            editedFiles.put(filePath, code); // Adiciona o conteúdo editado ao mapa
            updateToolbarTitle(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setLanguage(String filePath) {
        if (filePath.endsWith(".java")) {
            codeEditor.setEditorLanguage(new JavaLanguage());
        } else if (filePath.endsWith(".xml")) {
            // Adicionar suporte para XML ou outras linguagens aqui, se necessário
        } else {
            // Adicionar suporte para outras linguagens aqui
        }
    }

    private void setupErrorDetectionAndAutoComplete() {
        // Placeholder para futuras implementações de detecção de erros e autocompletar
        // Adicionar futuras configurações conforme necessário
    }

    private void showDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_save_send, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        Button btnSave = dialogView.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            saveCodeToFile();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveCodeToFile() {
        if (currentFilePath != null) {
            try (FileWriter writer = new FileWriter(new File(currentFilePath))) {
                writer.write(codeEditor.getText().toString());
                openFiles.put(currentFilePath, codeEditor.getText().toString()); // Atualiza o conteúdo salvo no mapa
                fileChanged.put(currentFilePath, false);
                updateTabTitle(currentFilePath, false);
                Toast.makeText(this, "Arquivo salvo", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erro ao salvar arquivo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveEditedFile(String filePath, String content) {
        File editedFileDir = new File(EDITED_FILES_DIR);
        if (!editedFileDir.exists()) {
            editedFileDir.mkdirs();
        }

        File editedFile = new File(editedFileDir, new File(filePath).getName());
        try (FileWriter writer = new FileWriter(editedFile)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar arquivo editado", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTab(String filePath) {
        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText(new File(filePath).getName());
        tab.setTag(filePath); // Associa o caminho do arquivo à aba
        tabLayout.addTab(tab);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchToFile((String) tab.getTag());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        tab.view.setOnLongClickListener(v -> {
            showDeleteFileDialog(filePath, tab);
            return true;
        });
    }

    private void switchToFile(String filePath) {
        if (filePath != null && editedFiles.containsKey(filePath)) {
            codeEditor.setText(editedFiles.get(filePath));
            currentFilePath = filePath;
            setLanguage(filePath);
            updateToolbarTitle(filePath);
            selectTab(filePath); // Seleciona a aba correspondente ao arquivo
        }
    }

    private void selectTab(String filePath) {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && filePath.equals(tab.getTag())) {
                tab.select();
                break;
            }
        }
    }

    private void saveRecentFiles() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, String> entry : openFiles.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    private void loadRecentFiles() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String filePath = entry.getKey();
            String code = (String) entry.getValue();
            openFiles.put(filePath, code);
            fileChanged.put(filePath, false);
            editedFiles.put(filePath, code);
            addTab(filePath);
        }
        if (!openFiles.isEmpty()) {
            switchToFile(openFiles.keySet().iterator().next());
        }
    }

    private void checkForChanges() {
        if (currentFilePath != null && editedFiles.containsKey(currentFilePath)) {
            String currentText = codeEditor.getText().toString();
            if (!currentText.equals(editedFiles.get(currentFilePath))) {
                editedFiles.put(currentFilePath, currentText);
                fileChanged.put(currentFilePath, true);
                updateTabTitle(currentFilePath, true);
                saveEditedFile(currentFilePath, currentText);
            }
        }
    }

    private void updateTabTitle(String filePath, boolean changed) {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && filePath.equals(tab.getTag())) {
                String title = new File(filePath).getName();
                if (changed) {
                    title = "*" + title;
                }
                tab.setText(title);
                break;
            }
        }
    }

    private void showDeleteFileDialog(String filePath, TabLayout.Tab tab) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Excluir Arquivo Recente")
                .setMessage("Deseja excluir este arquivo dos recentes?")
                .setPositiveButton("Sim", (dialog, which) -> removeFileFromRecent(filePath, tab))
                .setNegativeButton("Não", null)
                .show();
    }

    private void removeFileFromRecent(String filePath, TabLayout.Tab tab) {
        openFiles.remove(filePath);
        fileChanged.remove(filePath);
        editedFiles.remove(filePath);
        tabLayout.removeTab(tab);
        saveRecentFiles();
        if (filePath.equals(currentFilePath) && !openFiles.isEmpty()) {
            switchToFile(openFiles.keySet().iterator().next());
        } else if (filePath.equals(currentFilePath)) {
            codeEditor.setText("");
            currentFilePath = null;
            updateToolbarTitle(null);
        }
        Toast.makeText(this, "Arquivo removido dos recentes", Toast.LENGTH_SHORT).show();
        updateRecentFilesCount(); // Atualizar o número de arquivos recentes no menu
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_recent_files) {
            openRecentFilesDialog();
            return true;
        }
        if (itemId == R.id.nav_increase_font_size) {
            changeFontSize(true);
            return true;
        }
        if (itemId == R.id.nav_decrease_font_size) {
            changeFontSize(false);
            return true;
        }
        if (itemId == R.id.nav_lock_toolbar) {
            toggleToolbarLock();
            return true;
        }
        if (itemId == R.id.nav_clear_recent_files) {
            clearRecentFiles();
            return true;
        }
        return false;
    }

    private void updateRecentFilesCount() {
        NavigationView navigationView = findViewById(R.id.navigation_view);
        Menu menu = navigationView.getMenu();
        MenuItem recentFilesItem = menu.findItem(R.id.nav_recent_files);

        if (recentFilesItem != null) {
            int recentFilesCount = openFiles.size();
            String title = "Arquivos Recentes (" + recentFilesCount + ")";
            recentFilesItem.setTitle(title);
        }
    }

    private void openRecentFilesDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Arquivos Recentes");

        String[] filePaths = openFiles.keySet().toArray(new String[0]);
        builder.setItems(filePaths, (dialog, which) -> {
            openFile(filePaths[which]);
            drawerLayout.closeDrawer(GravityCompat.START); // Fechar o DrawerLayout ao selecionar um arquivo
        });

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void changeFontSize(boolean increase) {
        textSize += increase ? 1 : -1;
        codeEditor.setTextSize(textSize);
        Toast.makeText(this, "Tamanho da fonte: " + textSize, Toast.LENGTH_SHORT).show();
    }

    private void toggleToolbarLock() {
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null) {
            behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                @Override
                public boolean canDrag(AppBarLayout appBarLayout) {
                    return false; // Mudar para true para habilitar novamente o arraste
                }
            });
            Toast.makeText(this, "Toolbar bloqueada", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearRecentFiles() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        openFiles.clear();
        fileChanged.clear();
        editedFiles.clear();
        tabLayout.removeAllTabs();
        Toast.makeText(this, "Arquivos recentes limpos", Toast.LENGTH_SHORT).show();
        updateRecentFilesCount(); // Atualizar o número de arquivos recentes no menu
    }

    private void updateToolbarTitle(String filePath) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(filePath != null ? new File(filePath).getName() : "Editor de Código");
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            checkModifiedFilesBeforeExit();
        }
    }

    private void checkModifiedFilesBeforeExit() {
        boolean hasModifiedFiles = false;
        for (Boolean changed : fileChanged.values()) {
            if (changed) {
                hasModifiedFiles = true;
                break;
            }
        }

        if (hasModifiedFiles) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Salvar alterações")
                    .setMessage("Existem arquivos modificados. Deseja salvar todas as alterações?")
                    .setPositiveButton("Salvar todos", (dialog, which) -> {
                        saveAllModifiedFiles();
                        finish();
                    })
                    .setNegativeButton("Descartar todos", (dialog, which) -> finish())
                    .setNeutralButton("Cancelar", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void saveAllModifiedFiles() {
        for (String filePath : fileChanged.keySet()) {
            if (fileChanged.get(filePath)) {
                try (FileWriter writer = new FileWriter(new File(filePath))) {
                    writer.write(editedFiles.get(filePath));
                    openFiles.put(filePath, editedFiles.get(filePath)); // Atualiza o conteúdo salvo no mapa
                    fileChanged.put(filePath, false);
                    updateTabTitle(filePath, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erro ao salvar arquivo: " + filePath, Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Deletar a pasta edited após salvar todas as modificações
        File editedDir = new File(EDITED_FILES_DIR);
        if (editedDir.exists()) {
            for (File file : Objects.requireNonNull(editedDir.listFiles())) {
                file.delete();
            }
            editedDir.delete();
        }
        Toast.makeText(this, "Todas as modificações foram salvas.", Toast.LENGTH_SHORT).show();
    }
}