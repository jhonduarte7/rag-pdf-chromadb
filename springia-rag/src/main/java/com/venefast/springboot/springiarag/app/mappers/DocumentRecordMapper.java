package com.venefast.springboot.springiarag.app.mappers;

import com.venefast.springboot.springiarag.app.models.dtos.DocumentRecordDto;
import com.venefast.springboot.springiarag.app.models.entities.DocumentRecord;
import org.springframework.stereotype.Component;

/**
 * Component responsible for mapping between {@link DocumentRecord} persistence entities
 * and {@link DocumentRecordDto} data transfer objects.
 */
@Component
public class DocumentRecordMapper {

    /**
     * Converts a {@link DocumentRecord} entity to its corresponding {@link DocumentRecordDto}.
     *
     * @param entity The document record entity to convert.
     * @return The populated {@link DocumentRecordDto}, or {@code null} if the entity is null.
     */
    public DocumentRecordDto toDto(DocumentRecord entity) {
        if (entity == null) {
            return null;
        }
        return new DocumentRecordDto(
                entity.getId(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getTotalChunks(),
                entity.getStatus()
        );
    }

    /**
     * Converts a {@link DocumentRecordDto} into a {@link DocumentRecord} entity.
     *
     * @param dto The document record DTO to convert.
     * @return The populated {@link DocumentRecord} entity, or {@code null} if the DTO is null.
     */
    public DocumentRecord toEntity(DocumentRecordDto dto) {
        if (dto == null) {
            return null;
        }
        DocumentRecord entity = new DocumentRecord();
        entity.setId(dto.id());
        entity.setFileName(dto.fileName());
        entity.setContentType(dto.contentType());
        entity.setFileSize(dto.fileSize());
        entity.setTotalChunks(dto.totalChunks());
        entity.setStatus(dto.status());
        return entity;
    }
}
