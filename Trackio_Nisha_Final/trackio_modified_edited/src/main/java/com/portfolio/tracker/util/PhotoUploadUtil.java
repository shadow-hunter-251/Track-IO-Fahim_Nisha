package com.portfolio.tracker.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Saves uploaded profile photos to a stable uploads directory and returns
 * the web-accessible path.  Uses Files.copy(inputStream) instead of
 * transferTo() to avoid Spring's temp-file-move race condition.
 */
public final class PhotoUploadUtil {

    /**
     * Absolute, stable upload directory: <user.home>/trackio-uploads
     * This is predictable on every OS and survives restarts.
     */
    public static final Path UPLOAD_DIR =
            Paths.get(System.getProperty("user.home"), "trackio-uploads");

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB

    private PhotoUploadUtil() {}

    /**
     * Saves a multipart image and returns a URL like "/uploads/avatar-uuid.jpg".
     * Falls back to fallbackUrl (existing URL) or null if nothing is supplied.
     * Throws IllegalArgumentException with a user-facing message on validation failure.
     */
    public static String save(MultipartFile photoFile, String fallbackUrl) throws IOException {

        if (photoFile != null && !photoFile.isEmpty()) {

            // Size guard
            if (photoFile.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Profile photo is too large. Maximum size is 5 MB.");
            }

            // MIME type guard
            String ct = photoFile.getContentType();
            if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png")
                             || ct.equals("image/webp") || ct.equals("image/gif"))) {
                throw new IllegalArgumentException("Profile photo must be a JPG, PNG, GIF, or WebP image.");
            }

            // Ensure upload directory exists
            Files.createDirectories(UPLOAD_DIR);

            // Determine extension from original filename
            String ext = ".jpg";
            String original = photoFile.getOriginalFilename();
            if (original != null && original.lastIndexOf('.') > -1) {
                String e = original.substring(original.lastIndexOf('.')).toLowerCase();
                if (e.matches("\\.(jpg|jpeg|png|gif|webp)")) ext = e.equals(".jpeg") ? ".jpg" : e;
            }

            String filename = "avatar-" + UUID.randomUUID() + ext;
            Path target = UPLOAD_DIR.resolve(filename);

            // KEY FIX: use Files.copy(inputStream) -- never transferTo(File).
            // transferTo() tries to move the OS temp file which can fail silently
            // if Tomcat already cleaned it up between request parsing and controller execution.
            try (InputStream in = photoFile.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + filename;
        }

        // No new file uploaded -- preserve existing URL if present
        if (fallbackUrl != null && !fallbackUrl.trim().isEmpty()) {
            return fallbackUrl.trim();
        }
        return null;
    }
}
