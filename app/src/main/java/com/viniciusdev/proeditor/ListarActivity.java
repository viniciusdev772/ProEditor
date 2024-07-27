package com.viniciusdev.proeditor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ListarActivity extends AppCompatActivity {

    private final String sketchware_data_path = FileUtil.getExternalStorageDir().concat("/.sketchware/data/");
    private final String sketchware_mysc_path = FileUtil.getExternalStorageDir().concat("/.sketchware/mysc/");
    private final String sketchware_mysc_list_path = FileUtil.getExternalStorageDir().concat("/.sketchware/mysc/list");
    private final String sketchware_resources_icons_path = FileUtil.getExternalStorageDir().concat("/.sketchware/resources/icons/");
    private final String sketchware_resources_images_path = FileUtil.getExternalStorageDir().concat("/.sketchware/resources/images/");

    private final List<Item> myprojects_list = new ArrayList<>();
    private final List<Item> myprojects_listVerifyed = new ArrayList<>();
    private final List<String> myprojects_string = new ArrayList<>();
    private int myprojects_number = 0;

    private TextView totalProjects;
    private int compiledCount = 0;
    private int notCompiledCount = 0;
    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        totalProjects = findViewById(R.id.totalProjects);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Listar diretórios
        FileUtil.listDir(sketchware_mysc_list_path, myprojects_string);

        // Verificar projetos
        verifyProjects();

        adapter = new ItemAdapter(myprojects_listVerifyed, item -> {
            // Handle item click
            Toast.makeText(ListarActivity.this, "Clicou no item: " + item.getName(), Toast.LENGTH_SHORT).show();
        }, this);
        recyclerView.setAdapter(adapter);

        updateProjectCounts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.filter_all) {
            adapter.updateList(myprojects_listVerifyed);
        } else if (itemId == R.id.filter_compiled) {
            filterProjects(true);
        } else if (itemId == R.id.filter_not_compiled) {
            filterProjects(false);
            float initialColor = -1.6740915E7f; // Exemplo de cor inicial no formato float
            ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor);
            colorPickerDialog.show();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    private void verifyProjects() {
        myprojects_list.clear();
        myprojects_listVerifyed.clear();
        myprojects_number = 0;
        compiledCount = 0;
        notCompiledCount = 0;

        for (String projectPath : myprojects_string) {
            File projectDir = new File(projectPath);
            String projectName = projectDir.getName();

            String id = projectName;
            boolean isCompiled = false;
            String compilationTime = "";
            File binDir = new File(sketchware_mysc_path, id + "/bin");
            if (binDir.exists() && binDir.isDirectory()) {
                File[] listOfFiles = binDir.listFiles();
                if (listOfFiles != null) {
                    for (File file : listOfFiles) {
                        if (file.getName().endsWith(".apk.res")) {
                            isCompiled = true;
                            long lastModified = file.lastModified();
                            long currentTime = System.currentTimeMillis();
                            long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime - lastModified);
                            long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime - lastModified);
                            long diffInHours = TimeUnit.MILLISECONDS.toHours(currentTime - lastModified);

                            if (diffInSeconds < 60) {
                                compilationTime = "Compilado há " + diffInSeconds + " segundos atrás";
                            } else if (diffInMinutes < 60) {
                                compilationTime = "Compilado há " + diffInMinutes + " minutos atrás";
                            } else if (diffInHours < 24) {
                                compilationTime = "Compilado há " + diffInHours + " horas atrás";
                            } else {
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                                compilationTime = "Compilado em " + dateFormat.format(new Date(lastModified));
                            }
                            break;
                        }
                    }
                }
            }

            String title = isCompiled ? "Projeto Compilado" : "Projeto Não Compilado";
            String iconPath = sketchware_resources_icons_path + "/" + id + "/icon.png"; // Caminho do ícone

            // Desencriptar o arquivo /project e obter o nome do aplicativo e o pacote
            String projectDataPath = sketchware_mysc_list_path + "/" + id + "/project";
            String decryptedJson = FileUtil.decryptProjectFile(projectDataPath);
            String appName = title; // Valor padrão caso o decrypt falhe
            String projectPackage = "No package";

            if (decryptedJson != null) {
                try {
                    JSONObject jsonObject = new JSONObject(decryptedJson);
                    appName = jsonObject.getString("my_app_name");
                    projectPackage = jsonObject.getString("my_sc_pkg_name");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String buildInfo = isCompiled ? compilationTime : "Não Compilado";
            Item item = new Item(appName, id, projectPackage, iconPath, buildInfo);
            myprojects_list.add(item);
            myprojects_number++;

            if (isCompiled) {
                compiledCount++;
            } else {
                notCompiledCount++;
            }

            // Verificação adicional não necessária
            myprojects_listVerifyed.add(item);
        }
    }

    private void filterProjects(boolean compiled) {
        List<Item> filteredList = new ArrayList<>();
        for (Item item : myprojects_listVerifyed) {
            if ((compiled && item.getBuildInfo().startsWith("Compilado")) || (!compiled && "Não Compilado".equals(item.getBuildInfo()))) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }

    private void updateProjectCounts() {
        @SuppressLint("DefaultLocale") String projectCountText = String.format("Total de Projetos: %d (Compilados: %d, Não Compilados: %d)",
                myprojects_listVerifyed.size(), compiledCount, notCompiledCount);
        totalProjects.setText(projectCountText);
    }

    public String getSketchwareMyscListPath() {
        return sketchware_mysc_list_path;
    }
}