package com.viniciusdev.marketproeditor;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

public class KeyGeneratorUtil {

    private static final String KEY_PAIR_DIRECTORY = "ProEditor/keys";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public static void saveKeyPair(Context context, KeyPair keyPair) throws Exception {
        File keyPairDir = new File(context.getFilesDir(), KEY_PAIR_DIRECTORY);
        if (!keyPairDir.exists()) {
            keyPairDir.mkdirs();
        }

        // Salvar chave pública
        File publicKeyFile = new File(keyPairDir, PUBLIC_KEY_FILE);
        try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
            fos.write(keyPair.getPublic().getEncoded());
        }

        // Salvar chave privada
        File privateKeyFile = new File(keyPairDir, PRIVATE_KEY_FILE);
        try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
            fos.write(keyPair.getPrivate().getEncoded());
        }
    }

    public static PublicKey loadPublicKey(Context context) throws Exception {
        File publicKeyFile = new File(context.getFilesDir(), KEY_PAIR_DIRECTORY + "/" + PUBLIC_KEY_FILE);
        try (FileInputStream fis = new FileInputStream(publicKeyFile)) {
            byte[] encodedPublicKey = new byte[(int) publicKeyFile.length()];
            fis.read(encodedPublicKey);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        }
    }

    public static PrivateKey loadPrivateKey(Context context) throws Exception {
        File privateKeyFile = new File(context.getFilesDir(), KEY_PAIR_DIRECTORY + "/" + PRIVATE_KEY_FILE);
        try (FileInputStream fis = new FileInputStream(privateKeyFile)) {
            byte[] encodedPrivateKey = new byte[(int) privateKeyFile.length()];
            fis.read(encodedPrivateKey);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }

    public static boolean keyPairExists(Context context) {
        File publicKeyFile = new File(context.getFilesDir(), KEY_PAIR_DIRECTORY + "/" + PUBLIC_KEY_FILE);
        File privateKeyFile = new File(context.getFilesDir(), KEY_PAIR_DIRECTORY + "/" + PRIVATE_KEY_FILE);
        return publicKeyFile.exists() && privateKeyFile.exists();
    }
}