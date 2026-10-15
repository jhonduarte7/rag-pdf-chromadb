package com.venefast.springboot.springiarag.app.repositories;

import com.venefast.springboot.springiarag.app.models.entities.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for managing {@link DocumentRecord} persistence entities.
 */
@Repository
public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {

    /**
     * Finds a document record matching the specified file name.
     *
     * @param fileName the original uploaded file name
     * @return Optional containing the matched DocumentRecord, or empty if not found
     */
    Optional<DocumentRecord> findByFileName(String fileName);

    /**
     * Finds all document records matching the specified status (e.g., "INDEXED", "FAILED").
     *
     * @param status the ingestion status string
     * @return list of matching DocumentRecord entities
     */
    List<DocumentRecord> findByStatus(String status);
}
