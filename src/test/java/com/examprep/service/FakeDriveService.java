package com.examprep.service;

import com.examprep.model.GoogleDriveFile;

import java.net.http.HttpClient;
import java.util.List;

final class FakeDriveService extends GoogleDriveService {

    private final List<GoogleDriveFile> files;

    static FakeDriveService withFiles(GoogleDriveFile... files) {
        return new FakeDriveService(List.of(files));
    }

    private FakeDriveService(List<GoogleDriveFile> files) {
        super("folder-abc", "{}", HttpClient.newHttpClient(), "http://127.0.0.1");
        this.files = files;
    }

    @Override
    public List<GoogleDriveFile> listFolderFiles() {
        return files;
    }
}
