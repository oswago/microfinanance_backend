package com.microfinance.common.config;

public class GeneralConfig {

    //1. Borrower related////////////////////////////////////////////////////////

    public enum ClientType {
        INDIVIDUAL, GROUP_MEMBER, SME, CORPORATE
    }

    public enum RiskRating {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED
    }

    public enum BorrowerStatus {
        ACTIVE, INACTIVE, BLACKLISTED, DECEASED
    }

    public enum KycStatus {
        PENDING, VERIFIED, REJECTED, NOT_STARTED, SUSPENDED, EXPIRED
    }

    // In CasePriority enum (if not exists)
    public enum CasePriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }



    //2. Borrower Activity////////////////////////////////////////////////////////////////

    public enum BorrowerActivityType {
        // Borrower Management Activities
        BORROWER_CREATED,
        BORROWER_UPDATED,
        BORROWER_STATUS_CHANGED,
        BORROWER_KYC_INITIATED,
        BORROWER_KYC_VERIFIED,
        BORROWER_KYC_REJECTED,
        BORROWER_KYC_EXPIRED,

        // Document Activities
        DOCUMENT_UPLOADED,
        DOCUMENT_VERIFIED,
        DOCUMENT_REJECTED,
        DOCUMENT_DELETED,

        // Group Management Activities
        GROUP_ASSIGNED,
        GROUP_REMOVED,
        GROUP_LEADER_ASSIGNED,
        GROUP_LEADER_REMOVED,

        // Loan Application Activities
        LOAN_APPLICATION_SUBMITTED,
        LOAN_APPLICATION_APPROVED,
        LOAN_APPLICATION_REJECTED,
        LOAN_APPLICATION_WITHDRAWN,

        // Loan Disbursement Activities
        LOAN_DISBURSED,
        LOAN_DISBURSEMENT_FAILED,

        // Repayment Activities
        REPAYMENT_MADE,
        REPAYMENT_SCHEDULED,
        REPAYMENT_OVERDUE,
        REPAYMENT_PARTIAL,
        REPAYMENT_BOUNCE,

        // Savings Activities
        SAVINGS_DEPOSIT,
        SAVINGS_WITHDRAWAL,
        SAVINGS_INTEREST_APPLIED,

        // Communication Activities
        SMS_SENT,
        EMAIL_SENT,
        NOTIFICATION_SENT,
        REMINDER_SENT,

        // System Activities
        PROFILE_VIEWED,
        PASSWORD_CHANGED,
        CONTACT_UPDATED,
        EMPLOYMENT_UPDATED,

        // Risk Management Activities
        RISK_RATING_UPDATED,
        CREDIT_SCORE_UPDATED,
        BLACKLISTED,
        BLACKLIST_REMOVED,

        // Guarantor Activities
        GUARANTOR_ADDED,
        GUARANTOR_REMOVED,
        GUARANTOR_VERIFIED,

        // Meeting Activities
        MEETING_ATTENDED,
        MEETING_MISSED,

        // Miscellaneous
        NOTE_ADDED,
        FILE_UPLOADED,
        SYSTEM_AUTO_UPDATE, OTHER_ACTIVITY,
        COLLECTION_ACTIVITY, EARLY_REPAYMENT_REQUEST_ACTIVITY, FIELD_VISIT_ACTIVITY,

        RECOVERY_CASE_ACTIVITY, LEGAL_NOTICE_ACTIVITY, LOAN_APPLICATION_ACTIVITY,
        LOAN_APPLICATION_APPROVAL_ACTIVITY, LOAN_APPLICATION_RETURNED, LOAN_CLOSED,
        LOAN_WRITE_OFF, LOAN_WRITE_OFF_APPROVED, LOAN_WRITE_OFF_REJECTED,
        LOAN_REPAYMENT, LOAN_REPAYMENT_REVERSED, LOAN_REPAYMENT_WAIVED;

    }



 //3. Borrower Group//////////////////////////////////////////////////////////////////

    public enum GroupType {
        JOINT_LIABILITY, SAVINGS, AGRICULTURAL, WOMEN, YOUTH, COMMUNITY
    }

    public enum GroupStatus {
        ACTIVE, INACTIVE, DISSOLVED
    }

    public enum JointLiabilityType {
        FULL, PARTIAL, NONE
    }




    //4. Document Verification//////////////////////////////////////////////////////////

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }



    //5. KCYWorkflowStepStatus////////////////////////////////////////////////////////////

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED
    }

    //Guarantor/////

    public enum Relationship {
        SPOUSE, PARENT, SIBLING, FRIEND, BUSINESS_PARTNER, OTHER
    }

    public enum GuarantorStatus {
        ACTIVE, INACTIVE, BLACKLISTED
    }

    //Loan///
    public enum LoanStatus {
        DRAFT,
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        ACTIVE,
        CLOSED,
        DEFAULTED,
        COMPLETED, WRITTEN_OFF,
        PENDING_DISBURSEMENT,
        DELINQUENT,
        RESTRUCTURED, OVERDUE, DISBURSED, RESCHEDULED
    }


    public enum LoanApplicationStatus {
        DRAFT, PENDING_APPROVAL,APPROVED, REJECTED, CANCELLED, SUBMITTED, UNDER_REVIEW, DISBURSED, NEEDS_REVISION, PENDING_FINAL_APPROVAL;

        public boolean isTerminalState() {
            return false;
        }
    }

    public enum ApplicationStage {
        APPLICATION, UNDER_REVIEW, CREDIT_ASSESSMENT, APPROVAL, DISBURSEMENT, CLOSED, ESCALATED, ACTIVE
    }


    public enum InstallmentStatus {
        PENDING, PAID, PARTIAL, OVERDUE
    }

    public enum PaymentMethod {
        CASH, BANK_TRANSFER, MOBILE_MONEY, CHEQUE, WAIVER, POS,DIRECT_DEBIT,CREDIT_CARD
    }

    public enum RepaymentStatus {
        PENDING, COMPLETED, FAILED, WAIVED, REVERSED
    }

    public enum RescheduleStatus {
        PENDING, APPROVED, REJECTED, PENDING_APPROVAL, UNDER_REVIEW, CANCELLED
    }


    public enum ActionType {
        PHONE_CALL,
        SMS,
        EMAIL,
        FIELD_VISIT,
        HOME_VISIT,
        OFFICE_VISIT,
        LETTER,
        LEGAL_NOTICE,
        FOLLOW_UP,
        MEETING,
        PROMISE_TO_PAY,
        PAYMENT_COLLECTION,
        NEGOTIATION,
        ESCALATION
    }

    public enum ActionStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        RESCHEDULED,
        PENDING, FAILED
    }


    public enum FollowUpAction {
        CALL_AGAIN,
        SEND_REMINDER,
        FIELD_VISIT,
        SEND_LETTER,
        ESCALATE_TO_MANAGER,
        ESCALATE_TO_LEGAL,
        NEGOTIATE_PAYMENT_PLAN,
        OFFER_SETTLEMENT,
        SEND_FINAL_NOTICE,
        INITIATE_RECOVERY_PROCESS
    }


    public enum ContactMethod {
        PHONE,
        MOBILE,
        EMAIL,
        SMS,
        WHATSAPP,
        IN_PERSON,
        MAIL,
        FIELD_VISIT, COURIER
    }

    public enum Outcome {
        CONTACTED,
        NO_ANSWER,
        WRONG_NUMBER,
        NUMBER_DISCONNECTED,
        PROMISED_TO_PAY,
        PARTIAL_PAYMENT,
        FULL_PAYMENT,
        REFUSED_TO_PAY,
        REQUESTED_FOLLOW_UP,
        DISPUTED_AMOUNT,
        REQUESTED_WRITTEN_AGREEMENT,
        UNABLE_TO_CONTACT,
        DECEASED,
        LOCATION_CLOSED,
        BUSINESS_CLOSED,
        CONTACTED_WILL_PAY,
        CONTACTED_NEEDS_TIME,
        SUCCESSFUL, UNSUCCESSFUL, PARTIAL, PENDING, POSTPONED, CONTACTED_DISPUTES
    }





    public enum ApprovalDecision {
        APPROVED, REJECTED, PENDING, RETURNED_FOR_REVISION
    }


    public enum ConditionStatus {
        PENDING,
        COMPLETED,
        WAIVED,
        EXPIRED
    }


    public enum InterestMethod {
        FLAT,
        REDUCING_BALANCE,
        COMPOUND
    }

    public enum TenureUnit {
        DAYS,
        WEEKS,
        MONTHS,
        YEARS
    }

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }


    public static enum RestructureStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED,
        COMPLETED
    }

    public static enum DocumentStatus {
        PENDING,
        UPLOADED,
        VERIFIED,
        REJECTED,
        EXPIRED
    }

    public static enum DocumentType {
        LOAN_AGREEMENT,
        DISBURSEMENT_RECEIPT,
        REPAYMENT_RECEIPT,
        COLLATERAL_DOCUMENT,
        IDENTIFICATION,
        INCOME_PROOF,
        BANK_STATEMENT,
        BUSINESS_LICENSE,
        OTHER
    }


    // Enum for write-off status
    public enum WriteOffStatus {
        PENDING, APPROVED, REJECTED
    }


    public static enum EarlyRepaymentStatus {
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        PAID,
        CANCELLED
    }

}
