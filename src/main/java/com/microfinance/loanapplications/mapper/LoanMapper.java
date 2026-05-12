package com.microfinance.loanapplications.mapper;

import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.LoanSummaryDto;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.system.entity.Branch;
import com.microfinance.base.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoanMapper {

    @Autowired
    private final UserService userService;

    public LoanMapper(UserService userService) {
        this.userService = userService;
    }

    /**
     * Convert Loan entity to LoanDto
     */
    public LoanDto toDto(Loan loan) {
        if (loan == null) {
            return null;
        }

        LoanDto dto = new LoanDto();

        // Basic fields
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTenureMonths(loan.getTenureMonths());
        dto.setStatus(loan.getStatus() != null ? loan.getStatus().name() : null);
        dto.setDisbursementDate(loan.getDisbursementDate());
        dto.setMaturityDate(loan.getMaturityDate());
        dto.setClosedDate(loan.getClosedDate());
        dto.setTotalDue(loan.getTotalDue());
        dto.setTotalPaid(loan.getTotalPaid());
        dto.setOutstandingBalance(loan.getOutstandingBalance());
        dto.setPenaltyAccrued(loan.getPenaltyAccrued());
        dto.setDaysDelinquent(loan.getDaysDelinquent());
        dto.setDisbursementMethod(loan.getDisbursementMethod());
        dto.setTransactionReference(loan.getTransactionReference());
        dto.setDisbursementNotes(loan.getDisbursementNotes());
        dto.setNetDisbursementAmount(loan.getNetDisbursementAmount());
        dto.setCreatedAt(loan.getCreatedAt());
        dto.setUpdatedAt(loan.getUpdatedAt());

        // Borrower fields - flattened
        if (loan.getBorrower() != null) {
            dto.setBorrowerId(loan.getBorrower().getId());
            dto.setBorrowerName(getBorrowerFullName(loan.getBorrower()));
            dto.setBorrowerNumber(loan.getBorrower().getBorrowerNumber());
            dto.setBorrowerPhoneNumber(loan.getBorrower().getPhoneNumber());
        }

        // Loan product fields - flattened
        if (loan.getLoanProduct() != null) {
            dto.setLoanProductId(loan.getLoanProduct().getId());
            dto.setLoanProductName(loan.getLoanProduct().getName());
        }

        // Branch fields - flattened
        if (loan.getBranch() != null) {
            dto.setBranchId(loan.getBranch().getId());
            dto.setBranchName(loan.getBranch().getName());
        }

        // User fields - flattened
        if (loan.getDisbursedBy() != null) {
            // Get username directly from the entity
            String username = loan.getDisbursedBy().getUsername();
            dto.setDisbursedByName(username);
            dto.setDisbursedById(loan.getDisbursedBy().getId());

            // If username is empty or you need full name, fetch from service
            if (username == null || username.isEmpty()) {
                User user = userService.getUserById(loan.getDisbursedBy().getId());
                dto.setDisbursedByName(user.getFullName());
            }
        } else {
            // Loan not disbursed yet
            dto.setDisbursedByName(null);
            dto.setDisbursedById(null);
        }

        if (loan.getLoanApplication() != null) {
            dto.setLoanApplicationId(loan.getLoanApplication().getId());
        }

        return dto;
    }

    /**
     * Convert list of Loan entities to list of LoanDtos
     */
    public List<LoanDto> toDtoList(List<Loan> loans) {
        if (loans == null) {
            return null;
        }
        return loans.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert Loan entity to LoanSummaryDto
     */
    public LoanSummaryDto toSummaryDto(Loan loan) {
        if (loan == null) {
            return null;
        }

        LoanSummaryDto dto = new LoanSummaryDto();
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setOutstandingBalance(loan.getOutstandingBalance());
        dto.setStatus(loan.getStatus() != null ? loan.getStatus().name() : null);
        dto.setDisbursementDate(loan.getDisbursementDate());
        dto.setDaysDelinquent(loan.getDaysDelinquent());
        dto.setIsDelinquent(loan.getDaysDelinquent() != null && loan.getDaysDelinquent() > 0);

        return dto;
    }

    /**
     * Convert list of Loan entities to list of LoanSummaryDtos
     */
    public List<LoanSummaryDto> toSummaryDtoList(List<Loan> loans) {
        if (loans == null) {
            return null;
        }
        return loans.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Update Loan entity from LoanDto (partial update)
     */
    public void updateEntityFromDto(LoanDto dto, Loan loan, Long currentUserId) {
        if (dto == null || loan == null) {
            return;
        }

        // Only update fields that should be modifiable
        if (dto.getStatus() != null) {
            try {
                loan.setStatus(com.microfinance.common.config.GeneralConfig.LoanStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        if (dto.getDisbursementNotes() != null) {
            loan.setDisbursementNotes(dto.getDisbursementNotes());
        }

        if (dto.getDisbursementMethod() != null) {
            loan.setDisbursementMethod(dto.getDisbursementMethod());
        }

        if (dto.getTransactionReference() != null) {
            loan.setTransactionReference(dto.getTransactionReference());
        }

        // Set updated by
        if (currentUserId != null) {
            loan.setUpdatedBy(currentUserId);
        }
    }

    // ==================== HELPER METHODS ====================

    private String getBorrowerFullName(Borrower borrower) {
        if (borrower == null) {
            return null;
        }
        StringBuilder fullName = new StringBuilder();
        if (borrower.getFirstName() != null) {
            fullName.append(borrower.getFirstName());
        }
        if (borrower.getLastName() != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(borrower.getLastName());
        }
        return fullName.length() > 0 ? fullName.toString() : null;
    }
}