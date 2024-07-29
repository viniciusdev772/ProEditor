package com.viniciusdev.proeditor;

import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileUtil {

    public static String getExternalStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static void listDir(String dirPath, List<String> list) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    list.add(file.getAbsolutePath());
                }
            }
        }
    }

    public static boolean isExistFile(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    public static String readFile(String path) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return new String(Files.readAllBytes(Paths.get(path)));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        return path;
    }



    static void excluirRecursivamente(File arquivoOuPasta) {
        if (arquivoOuPasta.isDirectory()) {
            File[] conteudo = arquivoOuPasta.listFiles();
            if (conteudo != null) {
                for (File file : conteudo) {
                    excluirRecursivamente(file);
                }
            }
        }
        arquivoOuPasta.delete();
    }

    public static String decryptProjectFile(String path) {
        try {
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] bytes = "sketchwaresecure".getBytes();
            instance.init(Cipher.DECRYPT_MODE, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
            RandomAccessFile randomAccessFile = new RandomAccessFile(path, "r");
            byte[] bArr = new byte[(int) randomAccessFile.length()];
            randomAccessFile.readFully(bArr);
            return new String(instance.doFinal(bArr));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
