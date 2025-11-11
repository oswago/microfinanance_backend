package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.DocumentTypeDto;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.common.config.DocumentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {

    @GetMapping
    public ResponseEntity<List<DocumentTypeDto>> getAllDocumentTypes() {
        List<DocumentTypeDto> types = Arrays.stream(DocumentConfig.DocumentType.values())
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/required")
    public ResponseEntity<List<DocumentTypeDto>> getRequiredDocumentTypes() {
        List<DocumentTypeDto> types = Arrays.stream(DocumentConfig.DocumentType.values())
                .filter(DocumentConfig.DocumentType::isRequired)
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<DocumentTypeDto>> getDocumentTypesByCategory(@PathVariable String category) {
        List<DocumentTypeDto> types = Arrays.stream(DocumentConfig.DocumentType.values())
                .filter(type -> type.getCategory().equalsIgnoreCase(category))
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getDocumentCategories() {
        List<String> categories = Arrays.stream(DocumentConfig.DocumentType.values())
                .map(DocumentConfig.DocumentType::getCategory)
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    private DocumentTypeDto convertToDto(DocumentConfig.DocumentType documentType) {
        DocumentTypeDto dto = new DocumentTypeDto();
        dto.setCode(documentType.name());
        dto.setDisplayName(documentType.getDisplayName());
        dto.setCategory(documentType.getCategory());
        dto.setRequired(documentType.isRequired());
        dto.setDescription(getDocumentTypeDescription(documentType));
        return dto;
    }

    private String getDocumentTypeDescription(DocumentConfig.DocumentType type) {
        switch (type) {
            case NATIONAL_ID:
                return "Government-issued national identification card";
            case PASSPORT:
                return "International passport with photo";
            case UTILITY_BILL:
                return "Recent electricity, water, or telephone bill (not older than 3 months)";
            case PAYSLIP:
                return "Recent salary slip or employment income proof";
            case BANK_STATEMENT:
                return "Last 3 months bank statements";
            case PHOTOGRAPH:
                return "Recent passport-sized photograph";
            default:
                return "Supporting document";
        }
    }
}