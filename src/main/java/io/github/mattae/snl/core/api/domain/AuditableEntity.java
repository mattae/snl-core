package io.github.mattae.snl.core.api.domain;

import io.github.mattae.snl.core.api.config.AuditEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Setter
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
public abstract class AuditableEntity implements Auditable<String> {
    @Column(name = "created_by", nullable = true)
    private String createdBy;

    @Column(name = "created_date", nullable = true)
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by", nullable = true)
    private String lastModifiedBy;

    @Column(name = "last_modified_date", nullable = true)
    private LocalDateTime lastModifiedDate;

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }
}
