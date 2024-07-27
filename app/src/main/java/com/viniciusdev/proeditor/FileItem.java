package com.viniciusdev.proeditor;

public class FileItem {
    private final String name;
    private final boolean isDirectory;
    private final String path;

    public FileItem(String name, boolean isDirectory, String path) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getPath() {
        return path;
    }
}
