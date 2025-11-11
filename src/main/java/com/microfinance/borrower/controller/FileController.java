package com.microfinance.borrower.controller;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final BorrowerDocumentRepository documentRepository;

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        try {
            BorrowerDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

            Path filePath = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found: " + document.getFileName());
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + document.getFileName() + "\"")
                    .body(resource);

        } catch (Exception ex) {
            throw new RuntimeException("Error downloading file: " + ex.getMessage(), ex);
        }
    }

    @GetMapping("/documents/{documentId}/info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocument> getDocumentInfo(@PathVariable Long documentId) {
        BorrowerDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));
        return ResponseEntity.ok(document);
    }
}