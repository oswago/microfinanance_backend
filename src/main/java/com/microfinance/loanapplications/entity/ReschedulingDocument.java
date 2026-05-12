// ReschedulingDocument.java
package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rescheduling_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReschedulingDocument extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_type", length = 100)
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;

    @Lob
    @Column(name = "file_content", columnDefinition = "BYTEA")
    private byte[] fileContent;
    
    @Column(name = "uploaded_by")
    private String uploadedBy;
    
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_reschedule_id", nullable = false)
    private LoanReschedule loanReschedule;  // This is correct!

}