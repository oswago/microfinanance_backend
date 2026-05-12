// ReschedulingDocumentRepository.java
package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ReschedulingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReschedulingDocumentRepository extends JpaRepository<ReschedulingDocument, Long> {

    //List<ReschedulingDocument> findByReschedulingRequestId(Long requestId);
    
   // void deleteByReschedulingRequestId(Long requestId);
}