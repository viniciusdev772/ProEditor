package com.viniciusdev.proeditor;

public class Item {
    private String name;
    private String id;
    private String projectPackage;
    private String iconPath;
    private String buildInfo;

    public Item(String name, String id, String projectPackage, String iconPath, String buildInfo) {
        this.name = name;
        this.id = id;
        this.projectPackage = projectPackage;
        this.iconPath = iconPath;
        this.buildInfo = buildInfo;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getProjectPackage() {
        return projectPackage;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getBuildInfo() {
        return buildInfo;
    }
}
