package com.microfinance.borrower.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BulkKycVerificationResponse {
    private int totalProcessed;
    private int successfulUpdates;
    private int failedUpdates;
    private List<BorrowerUpdateResult> results = new ArrayList<>();
    private String summary;
    private Long performedBy;
    private String performedByName;

    @Data
    public static class BorrowerUpdateResult {
        private Long borrowerId;
        private String borrowerName;
        private String borrowerNumber;
        private boolean success;
        private String message;
        private String previousKycStatus;
        private String newKycStatus;

        public static BorrowerUpdateResult success(Long borrowerId, String borrowerName, String borrowerNumber, 
                                                 String previousStatus, String newStatus) {
            BorrowerUpdateResult result = new BorrowerUpdateResult();
            result.setBorrowerId(borrowerId);
            result.setBorrowerName(borrowerName);
            result.setBorrowerNumber(borrowerNumber);
            result.setSuccess(true);
            result.setMessage("KYC status updated successfully");
            result.setPreviousKycStatus(previousStatus);
            result.setNewKycStatus(newStatus);
            return result;
        }

        public static BorrowerUpdateResult failure(Long borrowerId, String borrowerName, String borrowerNumber, 
                                                  String errorMessage) {
            BorrowerUpdateResult result = new BorrowerUpdateResult();
            result.setBorrowerId(borrowerId);
            result.setBorrowerName(borrowerName);
            result.setBorrowerNumber(borrowerNumber);
            result.setSuccess(false);
            result.setMessage(errorMessage);
            return result;
        }
    }

    // Helper methods
    public void addSuccessResult(BorrowerUpdateResult result) {
        this.results.add(result);
        this.successfulUpdates++;
        this.totalProcessed++;
    }

    public void addFailureResult(BorrowerUpdateResult result) {
        this.results.add(result);
        this.failedUpdates++;
        this.totalProcessed++;
    }

    public void generateSummary() {
        this.summary = String.format(
            "Processed %d borrowers: %d successful, %d failed", 
            totalProcessed, successfulUpdates, failedUpdates
        );
    }

    public boolean hasFailures() {
        return failedUpdates > 0;
    }

    public boolean isCompleteSuccess() {
        return failedUpdates == 0 && totalProcessed > 0;
    }
}