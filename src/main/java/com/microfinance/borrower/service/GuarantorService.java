package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.GuarantorDto;
import com.microfinance.borrower.dto.GuarantorCreateRequest;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerGuarantor;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.borrower.repository.GuarantorRepository;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuarantorService {

    private final GuarantorRepository guarantorRepository;
    private final BorrowerRepository borrowerRepository;

    public Page<GuarantorDto> getGuarantorsByBorrower(Long borrowerId, Pageable pageable) {
        return guarantorRepository.findByBorrowerId(borrowerId, pageable)
                .map(this::convertToDto);
    }

    public Page<GuarantorDto> searchGuarantors(Long borrowerId, String query, Pageable pageable) {
        return guarantorRepository.searchByBorrowerId(borrowerId, query, pageable)
                .map(this::convertToDto);
    }

    public Map<String, Object> getGuarantorsSummary(Long borrowerId) {
        long totalGuarantors = guarantorRepository.countByBorrowerId(borrowerId);
        long activeGuarantors = guarantorRepository.countByBorrowerIdAndStatus(borrowerId, GeneralConfig.GuarantorStatus.ACTIVE);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGuarantors", totalGuarantors);
        summary.put("activeGuarantors", activeGuarantors);
        
        return summary;
    }

    public GuarantorDto getGuarantorById(Long id) {
        BorrowerGuarantor guarantor = guarantorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guarantor not found with id: " + id));
        return convertToDto(guarantor);
    }

    @Transactional
    public GuarantorDto createGuarantor(Long borrowerId, GuarantorCreateRequest request) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        BorrowerGuarantor guarantor = new BorrowerGuarantor();
        guarantor.setBorrower(borrower);
        updateGuarantorFromRequest(guarantor, request);

        BorrowerGuarantor savedGuarantor = guarantorRepository.save(guarantor);
        log.info("Created guarantor {} for borrower {}", savedGuarantor.getFullName(), borrower.getFullName());

        return convertToDto(savedGuarantor);
    }

    @Transactional
    public GuarantorDto updateGuarantor(Long id, GuarantorCreateRequest request) {
        BorrowerGuarantor guarantor = guarantorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guarantor not found with id: " + id));

        updateGuarantorFromRequest(guarantor, request);
        BorrowerGuarantor updatedGuarantor = guarantorRepository.save(guarantor);
        log.info("Updated guarantor {}", updatedGuarantor.getFullName());

        return convertToDto(updatedGuarantor);
    }

    @Transactional
    public void deleteGuarantor(Long id) {
        BorrowerGuarantor guarantor = guarantorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guarantor not found with id: " + id));
        
        guarantorRepository.delete(guarantor);
        log.info("Deleted guarantor {}", guarantor.getFullName());
    }

    @Transactional
    public GuarantorDto updateGuarantorStatus(Long id, GeneralConfig.GuarantorStatus status) {
        BorrowerGuarantor guarantor = guarantorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guarantor not found with id: " + id));

        guarantor.setStatus(status);
        BorrowerGuarantor updatedGuarantor = guarantorRepository.save(guarantor);
        log.info("Updated guarantor {} status to {}", updatedGuarantor.getFullName(), status);

        return convertToDto(updatedGuarantor);
    }

    private void updateGuarantorFromRequest(BorrowerGuarantor guarantor, GuarantorCreateRequest request) {
        guarantor.setFullName(request.getFullName());
        guarantor.setPhoneNumber(request.getPhoneNumber());
        guarantor.setEmail(request.getEmail());
        guarantor.setRelationship(request.getRelationship());
        guarantor.setOccupation(request.getOccupation());
        guarantor.setEmployer(request.getEmployer());
        guarantor.setMonthlyIncome(request.getMonthlyIncome());
        guarantor.setAddress(request.getAddress());
        guarantor.setIdentificationType(request.getIdentificationType());
        guarantor.setIdentificationNumber(request.getIdentificationNumber());
        guarantor.setNotes(request.getNotes());
        
        if (request.getStatus() != null) {
            guarantor.setStatus(request.getStatus());
        }
    }

    private GuarantorDto convertToDto(BorrowerGuarantor guarantor) {
        GuarantorDto dto = new GuarantorDto();
        dto.setId(guarantor.getId());
        dto.setFullName(guarantor.getFullName());
        dto.setPhoneNumber(guarantor.getPhoneNumber());
        dto.setEmail(guarantor.getEmail());
        dto.setRelationship(guarantor.getRelationship());
        dto.setOccupation(guarantor.getOccupation());
        dto.setEmployer(guarantor.getEmployer());
        dto.setMonthlyIncome(guarantor.getMonthlyIncome());
        dto.setAddress(guarantor.getAddress());
        dto.setIdentificationType(guarantor.getIdentificationType());
        dto.setIdentificationNumber(guarantor.getIdentificationNumber());
        dto.setStatus(guarantor.getStatus());
        dto.setNotes(guarantor.getNotes());
        dto.setCreatedAt(guarantor.getCreatedAt());
        dto.setUpdatedAt(guarantor.getUpdatedAt());
        
        return dto;
    }
}