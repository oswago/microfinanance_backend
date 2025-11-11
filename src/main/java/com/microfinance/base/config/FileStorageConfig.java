package com.microfinance.base.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class FileStorageConfig {

    private final FileStorageProperties fileStorageProperties;

    @Bean
    public Path fileStorageLocation() {
        Path fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        
        try {
            java.nio.file.Files.createDirectories(fileStorageLocation);
            return fileStorageLocation;
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
}