package com.examprep.model;

public class GoogleDriveFile {

    public static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    private String id;
    private String name;
    private String mimeType;
    private String modifiedTime;
    private Long sizeBytes;

    public GoogleDriveFile() {
    }

    public GoogleDriveFile(String id, String name, String mimeType) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
    }

    public boolean isFolder() {
        return FOLDER_MIME.equals(mimeType);
    }

    public String getKindLabel() {
        if (mimeType == null || mimeType.isBlank()) {
            return "File";
        }
        return switch (mimeType) {
            case "application/pdf" -> "PDF";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "Word";
            case "application/msword" -> "Word";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "Excel";
            case "text/plain" -> "Text";
            case "image/png" -> "PNG";
            case "image/jpeg" -> "JPEG";
            case "application/vnd.google-apps.document" -> "Google Doc";
            case "application/vnd.google-apps.spreadsheet" -> "Google Sheet";
            case "application/vnd.google-apps.presentation" -> "Google Slides";
            case FOLDER_MIME -> "Folder";
            default -> "File";
        };
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
