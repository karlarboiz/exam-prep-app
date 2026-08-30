package com.examprep.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Minimal multipart/form-data body for outbound HttpClient calls.
 */
public final class MultipartForm {

    private final String boundary = "----examprep" + UUID.randomUUID().toString().replace("-", "");
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    public void addText(String name, String value) throws IOException {
        if (value == null) {
            return;
        }
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        write(value);
        write("\r\n");
    }

    public void addFile(String name, String filename, String contentType, byte[] bytes) throws IOException {
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + sanitizeFilename(filename) + "\"\r\n");
        write("Content-Type: " + (contentType == null || contentType.isBlank()
                ? "application/octet-stream" : contentType) + "\r\n\r\n");
        body.write(bytes);
        write("\r\n");
    }

    public byte[] finish() throws IOException {
        write("--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private void write(String text) throws IOException {
        body.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload.bin";
        }
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
