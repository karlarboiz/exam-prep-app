package com.examprep.model;

import java.util.ArrayList;
import java.util.List;

public class GoogleDriveListing {

    private String folderId;
    private String folderName;
    private String parentId;
    private final List<GoogleDriveFile> folders = new ArrayList<>();
    private final List<GoogleDriveFile> files = new ArrayList<>();

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<GoogleDriveFile> getFolders() {
        return folders;
    }

    public List<GoogleDriveFile> getFiles() {
        return files;
    }

    public boolean isEmpty() {
        return folders.isEmpty() && files.isEmpty();
    }
}
