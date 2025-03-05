package io.github.mattae.snl.core.api.domain;

import com.blazebit.persistence.view.CreatableEntityView;
import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.UpdatableEntityView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.mattae.snl.core.api.id.UUIDV7;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.jdbc.Expectation;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fw_party_identifiers")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SQLDelete(sql = "update fw_party_identifiers set archived = true, lastlast_modified_date = now() where id = ?",
        verify = Expectation.RowCount.class)
@SQLRestriction("archived = false")
@Getter
@Setter
public class Identifier {
    @Id
    @UUIDV7
    @EqualsAndHashCode.Include
    UUID id;

    @Column(name = "type", nullable = false)
    @EqualsAndHashCode.Include
    private String type;

    @Column(name = "value", nullable = false)
    @EqualsAndHashCode.Include
    private String value;

    @Column(name = "register", nullable = false)
    @EqualsAndHashCode.Include
    private String register;

    private String lifecycleStatus;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private Boolean archived = false;

    @ManyToOne
    @NotNull
    private Party party;

    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @PreUpdate
    @PreRemove
    public void update() {
        lastModifiedDate = LocalDateTime.now();
    }

    @EntityView(Identifier.class)
    public interface View {
        @IdMapping
        UUID getId();

        String getType();

        String getValue();

        String getRegister();

        LocalDateTime getFromDate();

        LocalDateTime getToDate();
    }

    @EntityView(Identifier.class)
    @CreatableEntityView
    @UpdatableEntityView
    public interface IdentifierView extends Identifier.View {
        void setId(UUID id);

        void setValue(String value);

        void setRegister(String register);

        void setType(String type);

        void setFromDate(LocalDateTime fromDate);

        void setToDate(LocalDateTime toDate);

        Party.View getParty();

        void setParty(Party.View party);

        LocalDateTime getLastModifiedDate();

        void setLastModifiedDate(LocalDateTime date);

        @JsonIgnore
        Boolean getArchived();

        void setArchived(Boolean archived);

        @com.blazebit.persistence.view.PreRemove
        default boolean preRemove() {
            setArchived(true);
            setLastModifiedDate(LocalDateTime.now());
            return false;
        }

        @com.blazebit.persistence.view.PreUpdate
        default void preUpdate() {
            setLastModifiedDate(LocalDateTime.now());
        }

        @com.blazebit.persistence.view.PrePersist
        default void prePersist() {
            setLastModifiedDate(LocalDateTime.now());
            setArchived(false);
        }
    }
}
