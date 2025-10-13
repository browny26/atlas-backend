package com.back.atlas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String supabaseServiceKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String folder) throws IOException {

        try {
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "." + fileExtension;
            String filePath = folder + "/" + fileName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            headers.set("Authorization", "Bearer " + supabaseServiceKey);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(
                    file.getBytes(),
                    headers
            );

            String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
                    supabaseUrl, bucketName, filePath);

            RestTemplate restTemplate = new RestTemplate();

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        uploadUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    String publicUrl = getPublicUrl(filePath);
                    return publicUrl;
                } else {
                    throw new RuntimeException("Upload failed: " + response.getBody());
                }

            } catch (Exception httpException) {
                httpException.printStackTrace();

                throw new RuntimeException("HTTP request failed: " + httpException.getMessage(), httpException);
            }

        } catch (Exception e) {
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }

            e.printStackTrace();
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    private String getPublicUrl(String filePath) {
        return String.format("%s/storage/v1/object/public/%s/%s",
                supabaseUrl, bucketName, filePath);
    }

    private String getFileExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf(".") + 1)
                : "jpg";
    }
}
