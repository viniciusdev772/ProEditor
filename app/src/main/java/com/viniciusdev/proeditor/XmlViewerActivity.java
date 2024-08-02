package com.viniciusdev.proeditor;

import android.os.Bundle;
import android.util.Xml;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class XmlViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_xml_viewer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o caminho do arquivo XML do Intent Extra
        String xmlPath = getIntent().getStringExtra("XML_PATH");
        if (xmlPath != null) {
            showFilePathAlert(xmlPath);
            if (xmlPath.endsWith(".xml")) {
                // Carrega e infla o layout XML dinamicamente
                if (!loadAndInflateXml(xmlPath)) {
                    showLoadErrorAlert("Erro desconhecido ao carregar o arquivo XML.");
                }
            } else {
                // Mostra um diálogo de alerta se o arquivo não for um XML válido
                showInvalidFileAlert();
            }
        } else {
            // Mostra um diálogo de alerta se o caminho do arquivo for nulo
            showInvalidFileAlert();
        }
    }

    private boolean loadAndInflateXml(String xmlPath) {
        try {
            File xmlFile = new File(xmlPath);
            InputStream inputStream = new FileInputStream(xmlFile);

            // Cria um XmlPullParser para ler o XML
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "UTF-8");

            // Analisar o XML e construir as views dinamicamente
            LinearLayout rootLayout = findViewById(R.id.main);
            parseAndBuildViews(parser, rootLayout);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            showLoadErrorAlert(e.getMessage());
            return false;
        }
    }

    private void parseAndBuildViews(XmlPullParser parser, ViewGroup rootLayout) throws Exception {
        int eventType = parser.getEventType();
        ViewGroup currentLayout = rootLayout;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("LinearLayout")) {
                    LinearLayout linearLayout = new LinearLayout(this);
                    linearLayout.setOrientation(parser.getAttributeValue(null, "android:orientation").equals("vertical") ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    linearLayout.setLayoutParams(layoutParams);
                    currentLayout.addView(linearLayout);
                    currentLayout = linearLayout;
                } else if (tagName.equals("TextView")) {
                    TextView textView = new TextView(this);
                    textView.setText(parser.getAttributeValue(null, "android:text"));
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    textView.setLayoutParams(layoutParams);
                    currentLayout.addView(textView);
                }
                // Adicione mais casos conforme necessário para outros tipos de views
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("LinearLayout")) {
                    currentLayout = (ViewGroup) currentLayout.getParent();
                }
            }
            eventType = parser.next();
        }
    }

    private void showFilePathAlert(String xmlPath) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Caminho do Arquivo")
                .setMessage("Caminho: " + xmlPath)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showInvalidFileAlert() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Arquivo Inválido")
                .setMessage("O arquivo especificado não é um XML válido.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLoadErrorAlert(String errorMessage) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Erro de Carregamento")
                .setMessage("Ocorreu um erro ao carregar o arquivo XML: " + errorMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}