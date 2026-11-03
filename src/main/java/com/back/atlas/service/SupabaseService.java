package com.back.atlas.service;

import com.back.atlas.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/** Uploads files to Supabase Storage and returns their public URL. */
@Slf4j
@Service
public class SupabaseService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String bucketName;

    public SupabaseService(RestClient.Builder builder,
                           @Value("${supabase.url}") String supabaseUrl,
                           @Value("${supabase.service-key}") String serviceKey,
                           @Value("${supabase.bucket}") String bucketName) {
        this.restClient = builder
                .baseUrl(supabaseUrl)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .build();
        this.supabaseUrl = supabaseUrl;
        this.bucketName = bucketName;
    }

    public String uploadFile(MultipartFile file, String folder) {
        String filePath = folder + "/" + UUID.randomUUID() + "." + getFileExtension(file.getOriginalFilename());
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{path}", bucketName, filePath)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | IOException e) {
            log.error("Supabase upload of {} failed", filePath, e);
            throw new ExternalServiceException("File upload failed", e);
        }
        return "%s/storage/v1/object/public/%s/%s".formatted(supabaseUrl, bucketName, filePath);
    }

    private static String getFileExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1)
                : "jpg";
    }
}
