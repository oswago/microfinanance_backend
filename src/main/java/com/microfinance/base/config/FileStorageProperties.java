package com.microfinance.base.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {
    private String uploadDir;
    private long maxSize;
    private List<String> allowedTypes;
    
    public String getUploadDir() {
        return uploadDir != null ? uploadDir : "uploads/documents";
    }
    
    public long getMaxSize() {
        return maxSize > 0 ? maxSize : 10485760L; // 10MB default
    }
}