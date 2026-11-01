package com.examprep.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    public void addText(String name, String value) {
        if (value == null) {
            return;
        }
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        write(value);
        write("\r\n");
    }

    public void addFile(String name, String filename, String contentType, byte[] bytes) {
        write("--" + boundary + "\r\n");
        write("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + sanitizeFilename(filename) + "\"\r\n");
        write("Content-Type: " + (contentType == null || contentType.isBlank()
                ? "application/octet-stream" : contentType) + "\r\n\r\n");
        try {
            body.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        write("\r\n");
    }

    public byte[] finish() {
        write("--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private void write(String text) {
        try {
            body.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload.bin";
        }
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
