package com.back.atlas.controller;

import com.back.atlas.dto.UploadResponse;
import com.back.atlas.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/storage")
public class StorageController {

    private final SupabaseService supabaseService;

    @PostMapping("/upload")
    public UploadResponse uploadFile(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "folder", defaultValue = "profiles") String folder) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!isValidImageFile(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type");
        }
        if (!folder.matches("[a-zA-Z0-9_-]{1,50}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid folder name");
        }
        return new UploadResponse(supabaseService.uploadFile(file, folder));
    }

    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}