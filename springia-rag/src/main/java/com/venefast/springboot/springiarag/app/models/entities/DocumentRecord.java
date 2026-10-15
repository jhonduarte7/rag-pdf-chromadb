package com.venefast.springboot.springiarag.app.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping the relational <code>documents</code> table.
 *
 * <p>Tracks uploaded PDF metadata, file size, generated chunk count, and ingestion lifecycle status.</p>
 */
@Entity
@Table(name = "documents")
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Default no-argument constructor required by JPA.
     */
    public DocumentRecord() {
    }

    /**
     * Parameterized constructor for creating a new DocumentRecord instance.
     *
     * @param fileName original name of the uploaded document
     * @param contentType MIME type of the uploaded file
     * @param fileSize size of the file in bytes
     * @param totalChunks number of token chunks generated
     * @param status processing status (e.g. "INDEXED", "FAILED")
     */
    public DocumentRecord(String fileName, String contentType, Long fileSize, Integer totalChunks, String status) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.totalChunks = totalChunks;
        this.status = status;
    }

    /**
     * Entity lifecycle callback executed prior to persistence.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Entity lifecycle callback executed prior to update.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
