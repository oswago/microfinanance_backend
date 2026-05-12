package com.microfinance.loanapplications.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.loanapplications.dto.repayment.ReceiptDto;
import com.microfinance.loanapplications.entity.EarlyRepaymentRequest;
import com.microfinance.loanapplications.entity.Loan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

    private static final Font SUBHEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font AMOUNT_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.GREEN);

    public byte[] generateReceiptPdf(ReceiptDto receipt) {
        log.info("Generating PDF receipt for: {}", receipt.getReceiptNumber());

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addHeader(document, receipt);
            addBorrowerInfo(document, receipt);
            addPaymentDetails(document, receipt);
            addInstallmentDetails(document, receipt);
            addFooter(document, receipt);

            document.close();
        } catch (DocumentException e) {
            log.error("Error generating PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF receipt", e);
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document, ReceiptDto receipt) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        // Left side - Logo and Title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("MICROFINANCE SYSTEM", TITLE_FONT));
        leftCell.addElement(new Paragraph("Payment Receipt", HEADER_FONT));
        table.addCell(leftCell);

        // Right side - Receipt details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph("Receipt No: " + receipt.getReceiptNumber(), NORMAL_FONT));
        rightCell.addElement(new Paragraph("Date: " + receipt.getReceiptDate()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), NORMAL_FONT));
        table.addCell(rightCell);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addBorrowerInfo(Document document, ReceiptDto receipt) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addTableRow(table, "Borrower Name:", receipt.getBorrowerName());
        addTableRow(table, "ID Number:", receipt.getBorrowerIdNumber());
        addTableRow(table, "Phone:", receipt.getBorrowerPhone());
        addTableRow(table, "Email:", receipt.getBorrowerEmail());
        addTableRow(table, "Loan Account:", receipt.getLoanAccountNumber());
        addTableRow(table, "Branch:", receipt.getBranchName() + " (" + receipt.getBranchCode() + ")");

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addPaymentDetails(Document document, ReceiptDto receipt) throws DocumentException {
        Paragraph title = new Paragraph("Payment Details", HEADER_FONT);
        title.setSpacingBefore(10);
        title.setSpacingAfter(5);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addTableRow(table, "Amount Paid:", formatCurrency(receipt.getAmountPaid()));
        addTableRow(table, "Principal:", formatCurrency(receipt.getPrincipalAmount()));
        addTableRow(table, "Interest:", formatCurrency(receipt.getInterestAmount()));

        if (receipt.getPenaltyAmount() != null && receipt.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Penalty:", formatCurrency(receipt.getPenaltyAmount()));
        }

        if (receipt.getFeesAmount() != null && receipt.getFeesAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Fees:", formatCurrency(receipt.getFeesAmount()));
        }

        addTableRow(table, "Payment Method:", receipt.getPaymentMethod());
        addTableRow(table, "Transaction Ref:", receipt.getTransactionReference());
        addTableRow(table, "Payment Date:", receipt.getPaymentDate()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        addTableRow(table, "Received By:", receipt.getReceivedBy());

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addInstallmentDetails(Document document, ReceiptDto receipt) throws DocumentException {
        if (receipt.getInstallmentNumber() != null) {
            Paragraph title = new Paragraph("Installment Details", HEADER_FONT);
            title.setSpacingBefore(10);
            title.setSpacingAfter(5);
            document.add(title);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableRow(table, "Installment #:", String.valueOf(receipt.getInstallmentNumber()));
            addTableRow(table, "Due Date:", receipt.getDueDate()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addFooter(Document document, ReceiptDto receipt) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        // Left side - Signature
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Authorized Signature", SMALL_FONT));
        leftCell.addElement(new Paragraph("____________________", SMALL_FONT));
        table.addCell(leftCell);

        // Right side - QR Code or Verification
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph("Verification URL:", SMALL_FONT));
        rightCell.addElement(new Paragraph(receipt.getVerificationUrl(), SMALL_FONT));
        table.addCell(rightCell);

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("This is a computer generated receipt. No signature required.",
                SMALL_FONT));
    }

    public void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) return "N/A";
        return String.format("KES %,.2f", amount);
    }


        /**
         * Generate a settlement letter for early repayment
         */
        public byte[] generateSettlementLetter(EarlyRepaymentRequest request) {
            log.info("Generating settlement letter for early repayment request: {}", request.getRequestNumber());

            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                PdfWriter writer = PdfWriter.getInstance(document, baos);
                document.open();

                // Add letterhead
                addLetterhead(document);

                // Add title
                Paragraph title = new Paragraph("LOAN SETTLEMENT LETTER", TITLE_FONT);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Add date and reference
                addReferenceInfo(document, request);

                // Add borrower information
                addBorrowerInfo(document, request);

                // Add loan information
                addLoanInfo(document, request);

                // Add settlement calculation table
                addSettlementTable(document, request);

                // Add terms and conditions
                addTermsAndConditions(document);

                // Add signature section
                addSignatureSection(document, writer, request);

                // Add footer
                addFooter(document);

                document.close();
            } catch (DocumentException e) {
                log.error("Error generating settlement letter: {}", e.getMessage());
                throw new RuntimeException("Failed to generate settlement letter", e);
            }

            return baos.toByteArray();
        }

        private void addLetterhead(Document document) throws DocumentException {
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);

            // Left side - Company Logo/Name
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            Paragraph companyName = new Paragraph("MICROFINANCE SYSTEM", TITLE_FONT);
            companyName.setAlignment(Element.ALIGN_LEFT);
            leftCell.addElement(companyName);
            leftCell.addElement(new Paragraph("Financial Services Limited", NORMAL_FONT));
            leftCell.addElement(new Paragraph("P.O. Box 12345 - 00100", NORMAL_FONT));
            leftCell.addElement(new Paragraph("Nairobi, Kenya", NORMAL_FONT));
            headerTable.addCell(leftCell);

            // Right side - Contact Info
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(new Paragraph("Tel: +254 700 000000", NORMAL_FONT));
            rightCell.addElement(new Paragraph("Email: info@microfinance.co.ke", NORMAL_FONT));
            rightCell.addElement(new Paragraph("Website: www.microfinance.co.ke", NORMAL_FONT));
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Add horizontal line
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(BaseColor.GRAY);
            lineCell.setBorderWidth(1);
            lineCell.setPaddingBottom(5);
            line.addCell(lineCell);
            document.add(line);

            document.add(new Paragraph(" "));
        }

        private void addReferenceInfo(Document document, EarlyRepaymentRequest request) throws DocumentException {
            PdfPTable refTable = new PdfPTable(2);
            refTable.setWidthPercentage(100);

            // Left side - Date
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.addElement(new Paragraph("Date: " + LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), NORMAL_FONT));
            refTable.addCell(dateCell);

            // Right side - Reference
            PdfPCell refCell = new PdfPCell();
            refCell.setBorder(Rectangle.NO_BORDER);
            refCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            refCell.addElement(new Paragraph("Ref: " + request.getRequestNumber(), BOLD_FONT));
            refTable.addCell(refCell);

            document.add(refTable);
            document.add(new Paragraph(" "));
        }

        private void addBorrowerInfo(Document document, EarlyRepaymentRequest request) throws DocumentException {
            Paragraph borrowerHeader = new Paragraph("BORROWER INFORMATION", SUBHEADER_FONT);
            borrowerHeader.setSpacingBefore(10);
            borrowerHeader.setSpacingAfter(5);
            document.add(borrowerHeader);

            Borrower borrower = request.getBorrower();
            PdfPTable borrowerTable = new PdfPTable(2);
            borrowerTable.setWidthPercentage(100);

            addTableRow(borrowerTable, "Full Name:", borrower.getFullName());
            addTableRow(borrowerTable, "ID Number:", borrower.getBorrowerNumber());
            addTableRow(borrowerTable, "Phone:", borrower.getPhoneNumber() != null ? borrower.getPhoneNumber() : "N/A");
            addTableRow(borrowerTable, "Email:", borrower.getEmail() != null ? borrower.getEmail() : "N/A");
            addTableRow(borrowerTable, "Address:", borrower.getAddress() != null ? borrower.getAddress() : "N/A");

            document.add(borrowerTable);
            document.add(new Paragraph(" "));
        }

        private void addLoanInfo(Document document, EarlyRepaymentRequest request) throws DocumentException {
            Paragraph loanHeader = new Paragraph("LOAN INFORMATION", SUBHEADER_FONT);
            loanHeader.setSpacingBefore(10);
            loanHeader.setSpacingAfter(5);
            document.add(loanHeader);

            Loan loan = request.getLoan();
            PdfPTable loanTable = new PdfPTable(2);
            loanTable.setWidthPercentage(100);

            addTableRow(loanTable, "Loan Account:", loan.getLoanAccountNumber());
            addTableRow(loanTable, "Loan Product:", loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : "N/A");
            addTableRow(loanTable, "Disbursement Date:", formatDate(loan.getDisbursementDate()));
            addTableRow(loanTable, "Original Loan Amount:", formatCurrency(loan.getPrincipalAmount()));
            addTableRow(loanTable, "Original Tenure:", request.getOriginalTenure() + " months");
            addTableRow(loanTable, "Remaining Tenure:", request.getRemainingTenure() + " months");

            document.add(loanTable);
            document.add(new Paragraph(" "));
        }

        private void addSettlementTable(Document document, EarlyRepaymentRequest request) throws DocumentException {
            Paragraph settlementHeader = new Paragraph("EARLY SETTLEMENT CALCULATION", SUBHEADER_FONT);
            settlementHeader.setSpacingBefore(10);
            settlementHeader.setSpacingAfter(5);
            document.add(settlementHeader);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{60f, 40f});

            // Add table headers
            PdfPCell descHeader = new PdfPCell(new Phrase("Description", BOLD_FONT));
            descHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
            descHeader.setPadding(8);
            table.addCell(descHeader);

            PdfPCell amountHeader = new PdfPCell(new Phrase("Amount (KES)", BOLD_FONT));
            amountHeader.setBackgroundColor(BaseColor.LIGHT_GRAY);
            amountHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amountHeader.setPadding(8);
            table.addCell(amountHeader);

            // Add calculation rows
            addAmountRow(table, "Outstanding Principal", request.getOutstandingPrincipal());
            addAmountRow(table, "Accrued Interest", request.getAccruedInterest());

            if (request.getPenaltyCharges().compareTo(BigDecimal.ZERO) > 0) {
                addAmountRow(table, "Penalty Charges", request.getPenaltyCharges());
            }

            // Subtotal
            PdfPCell subtotalLabel = new PdfPCell(new Phrase("Subtotal", BOLD_FONT));
            subtotalLabel.setPadding(8);
            table.addCell(subtotalLabel);

            PdfPCell subtotalValue = new PdfPCell(new Phrase(formatCurrency(request.getTotalPayable()), BOLD_FONT));
            subtotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            subtotalValue.setPadding(8);
            table.addCell(subtotalValue);

            // Discount
            PdfPCell discountLabel = new PdfPCell(new Phrase(
                    String.format("Early Settlement Discount (%.2f%%)", request.getDiscountPercentage()),
                    BOLD_FONT));
            discountLabel.setPadding(8);
            table.addCell(discountLabel);

            PdfPCell discountValue = new PdfPCell(new Phrase("- " + formatCurrency(request.getDiscountAmount()), BOLD_FONT));
            discountValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            discountValue.setPadding(8);
            table.addCell(discountValue);

            // Final amount
            PdfPCell finalLabel = new PdfPCell(new Phrase("FINAL SETTLEMENT AMOUNT", SUBHEADER_FONT));
            finalLabel.setBackgroundColor(BaseColor.GREEN);
            finalLabel.setPadding(10);
            table.addCell(finalLabel);

            PdfPCell finalValue = new PdfPCell(new Phrase(formatCurrency(request.getEarlyRepaymentAmount()), AMOUNT_FONT));
            finalValue.setBackgroundColor(BaseColor.GREEN);
            finalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            finalValue.setPadding(10);
            table.addCell(finalValue);

            // Interest savings
            PdfPCell savingsLabel = new PdfPCell(new Phrase("Total Interest Saved", NORMAL_FONT));
            savingsLabel.setPadding(8);
            table.addCell(savingsLabel);

            PdfPCell savingsValue = new PdfPCell(new Phrase(formatCurrency(request.getInterestSavings()), NORMAL_FONT));
            savingsValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            savingsValue.setPadding(8);
            table.addCell(savingsValue);

            document.add(table);
            document.add(new Paragraph(" "));
        }

        private void addTermsAndConditions(Document document) throws DocumentException {
            Paragraph termsHeader = new Paragraph("TERMS AND CONDITIONS", SUBHEADER_FONT);
            termsHeader.setSpacingBefore(15);
            termsHeader.setSpacingAfter(5);
            document.add(termsHeader);

            String[] terms = {
                    "1. This settlement amount must be paid by the target settlement date specified in the request.",
                    "2. Upon receipt of full payment, the loan will be marked as CLOSED and no further interest will accrue.",
                    "3. This settlement letter is valid for 30 days from the date of issuance.",
                    "4. Any adjustments to this calculation require approval from the Branch Manager.",
                    "5. The borrower acknowledges that early settlement may affect their credit score.",
                    "6. This document serves as an official settlement offer and acceptance letter."
            };

            for (String term : terms) {
                document.add(new Paragraph(term, NORMAL_FONT));
                document.add(new Paragraph(" "));
            }
        }

        private void addSignatureSection(Document document, PdfWriter writer, EarlyRepaymentRequest request) throws DocumentException {
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);

            // Borrower signature
            PdfPCell borrowerCell = new PdfPCell();
            borrowerCell.setBorder(Rectangle.NO_BORDER);
            borrowerCell.addElement(new Paragraph("BORROWER'S ACCEPTANCE", BOLD_FONT));
            borrowerCell.addElement(new Paragraph(" "));
            borrowerCell.addElement(new Paragraph("Signature: __________________________", NORMAL_FONT));
            borrowerCell.addElement(new Paragraph("Date: " + LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), NORMAL_FONT));
            signatureTable.addCell(borrowerCell);

            // Authorized signatory
            PdfPCell authCell = new PdfPCell();
            authCell.setBorder(Rectangle.NO_BORDER);
            authCell.addElement(new Paragraph("AUTHORIZED SIGNATORY", BOLD_FONT));
            authCell.addElement(new Paragraph(" "));
            authCell.addElement(new Paragraph("Name: _______________________________", NORMAL_FONT));
            authCell.addElement(new Paragraph("Signature: __________________________", NORMAL_FONT));
            authCell.addElement(new Paragraph("Date: " + LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), NORMAL_FONT));
            signatureTable.addCell(authCell);

            document.add(signatureTable);
        }

        private void addFooter(Document document) throws DocumentException {
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{100f});

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBorder(Rectangle.NO_BORDER);
            footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph note = new Paragraph(
                    "This is a computer-generated document. No signature is required for electronic versions.",
                    SMALL_FONT);
            note.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(note);

            Paragraph disclaimer = new Paragraph(
                    "This settlement letter is confidential and intended solely for the borrower.",
                    SMALL_FONT);
            disclaimer.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(disclaimer);

            footerTable.addCell(footerCell);
            document.add(footerTable);
        }

        private void addAmountRow(PdfPTable table, String label, BigDecimal amount) {
            PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
            labelCell.setPadding(5);
            table.addCell(labelCell);

            PdfPCell amountCell = new PdfPCell(new Phrase(formatCurrency(amount), NORMAL_FONT));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amountCell.setPadding(5);
            table.addCell(amountCell);
        }


        public String formatDate(LocalDate date) {
            if (date == null) return "N/A";
            return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        }



    // Add these public methods to PdfGenerationService.java

    public Font getTitleFont() {
        return TITLE_FONT;
    }

    public Font getHeaderFont() {
        return HEADER_FONT;
    }

    public Font getSubheaderFont() {
        return SUBHEADER_FONT;
    }

    public Font getNormalFont() {
        return NORMAL_FONT;
    }

    public Font getSmallFont() {
        return SMALL_FONT;
    }

    public Font getBoldFont() {
        return BOLD_FONT;
    }

    public Font getAmountFont() {
        return AMOUNT_FONT;
    }


        private static final BaseColor HEADER_BG_COLOR = BaseColor.LIGHT_GRAY;

        /**
         * Add document header with company info and generation details
         */
        public void addDocumentHeader(Document document, String title, String subtitle) throws DocumentException {
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);

            // Left side - Company Name
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            Paragraph companyName = new Paragraph("MICROFINANCE SYSTEM", TITLE_FONT);
            companyName.setAlignment(Element.ALIGN_LEFT);
            leftCell.addElement(companyName);
            if (subtitle != null) {
                leftCell.addElement(new Paragraph(subtitle, HEADER_FONT));
            }
            headerTable.addCell(leftCell);

            // Right side - Generation Info
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(new Paragraph("Generated: " + formatDateTime(LocalDateTime.now()), SMALL_FONT));
            if (title != null) {
                rightCell.addElement(new Paragraph(title, SMALL_FONT));
            }
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Add separator line
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(BaseColor.GRAY);
            lineCell.setBorderWidth(0.5f);
            lineCell.setPaddingBottom(5);
            line.addCell(lineCell);
            document.add(line);

            document.add(new Paragraph(" "));
        }

        /**
         * Add document footer with generation timestamp and disclaimer
         */
        public void addDocumentFooter(Document document) throws DocumentException {
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBorder(Rectangle.NO_BORDER);
            footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph footer = new Paragraph("Generated on: " + formatDateTime(LocalDateTime.now()), SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(footer);

            Paragraph disclaimer = new Paragraph("This is a computer-generated report.", SMALL_FONT);
            disclaimer.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(disclaimer);

            footerTable.addCell(footerCell);
            document.add(footerTable);
        }

        /**
         * Add section title with consistent styling
         */
        public void addSectionTitle(Document document, String title) throws DocumentException {
            Paragraph sectionTitle = new Paragraph(title, SUBHEADER_FONT);
            sectionTitle.setSpacingBefore(10);
            sectionTitle.setSpacingAfter(5);
            document.add(sectionTitle);
        }

        /**
         * Add table header with background color
         */
        public void addTableHeader(PdfPTable table, String header) {
            PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
            cell.setBackgroundColor(HEADER_BG_COLOR);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        /**
         * Add a regular table cell
         */
        public void addTableCell(PdfPTable table, String value) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "N/A", NORMAL_FONT));
            cell.setPadding(5);
            table.addCell(cell);
        }

        /**
         * Add amount cell with right alignment
         */
        public void addAmountCell(PdfPTable table, BigDecimal amount) {
            PdfPCell cell = new PdfPCell(new Phrase(formatCurrency(amount), NORMAL_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            table.addCell(cell);
        }

        /**
         * Add percentage cell with right alignment
         */
        public void addPercentageCell(PdfPTable table, double percentage) {
            String text = String.format("%.2f%%", percentage);
            PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            table.addCell(cell);
        }

        /**
         * Add a summary card (used for KPI metrics)
         */
        public void addSummaryCard(Document document, String title, String value, String unit) throws DocumentException {
            PdfPTable cardTable = new PdfPTable(1);
            cardTable.setWidthPercentage(100);
            cardTable.setWidths(new float[]{100f});

            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.BOX);
            cell.setBorderColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph titlePara = new Paragraph(title, SMALL_FONT);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(titlePara);

            Paragraph valuePara = new Paragraph(value, BOLD_FONT);
            valuePara.setAlignment(Element.ALIGN_CENTER);
            valuePara.setFont(new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
            cell.addElement(valuePara);

            if (unit != null) {
                Paragraph unitPara = new Paragraph(unit, SMALL_FONT);
                unitPara.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(unitPara);
            }

            cardTable.addCell(cell);
            document.add(cardTable);
        }

        /**
         * Format LocalDateTime for PDF display
         */
        public String formatDateTime(LocalDateTime dateTime) {
            if (dateTime == null) return "N/A";
            return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        }








    }

