package com.viniciusdev.proeditor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.WindowManager;
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
    private boolean isShowingCompiled = false;
    private boolean isShowingNotCompiled = false;
    private int notCompiledCount = 0;
    private ItemAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Listar diretórios
        listDirectories();

        // Verificar projetos
        verifyProjects();

        adapter = new ItemAdapter(myprojects_listVerifyed, item -> {
            showProjectOptionsDialog(item);
        }, this);
        recyclerView.setAdapter(adapter);

        updateProjectCounts();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Atualizar a lista de projetos
            listDirectories();
            verifyProjects();
            adapter.updateList(myprojects_listVerifyed);
            updateProjectCounts();
            if (isShowingCompiled) {
                filterProjects(true);
            } else if (isShowingNotCompiled) {
                filterProjects(false);
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProjectsByQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProjectsByQuery(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.filter_all) {
            adapter.updateList(myprojects_listVerifyed);
            isShowingCompiled = false;
            isShowingNotCompiled = false;
        } else if (itemId == R.id.filter_compiled) {
            filterProjects(true);
            isShowingCompiled = true;
            isShowingNotCompiled = false;
        } else if (itemId == R.id.filter_not_compiled) {
            filterProjects(false);
            isShowingNotCompiled = true;
            isShowingCompiled = false;
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void listDirectories() {
        myprojects_string.clear();
        FileUtil.listDir(sketchware_mysc_list_path, myprojects_string);
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

    private void filterProjectsByQuery(String query) {
        List<Item> filteredList = new ArrayList<>();
        for (Item item : myprojects_listVerifyed) {
            if (item.getName().toLowerCase().contains(query.toLowerCase()) ||
                    item.getId().toLowerCase().contains(query.toLowerCase()) ||
                    item.getProjectPackage().toLowerCase().contains(query.toLowerCase())) {
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

    private void showProjectOptionsDialog(Item item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_project_options, null);

        builder.setView(dialogView);

        TextView optionEditColors = dialogView.findViewById(R.id.option_edit_colors);
        TextView optionFullEdit = dialogView.findViewById(R.id.option_full_edit);
        TextView optionCommonEdit = dialogView.findViewById(R.id.option_common_edit);

        optionEditColors.setOnClickListener(v -> {
            Toast.makeText(ListarActivity.this, "Edição de Cores selecionada para: " + item.getName(), Toast.LENGTH_SHORT).show();
        });

        optionFullEdit.setOnClickListener(v -> {
            if (item.getBuildInfo().startsWith("Compilado")) {
                Intent intent = new Intent(ListarActivity.this, ProjetoActivity.class);
                intent.putExtra("projectName", item.getId());
                startActivity(intent);
                Toast.makeText(ListarActivity.this, "Edição Completa selecionada para: " + item.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ListarActivity.this, "O projeto não está compilado: " + item.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        optionCommonEdit.setOnClickListener(v -> {
            Toast.makeText(ListarActivity.this, "Edição Comum selecionada para: " + item.getName(), Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = builder.create();

        if(dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            //dialog.getWindow().setDimAmount(0.1f);  // Opacidade do fundo fora do diálogo
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);  // Efeito de desfoque no fundo
        }

        dialog.show();
    }

    public String getSketchwareMyscListPath() {
        return sketchware_mysc_list_path;
    }
}
