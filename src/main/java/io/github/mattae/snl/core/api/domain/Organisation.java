package io.github.mattae.snl.core.api.domain;

import com.blazebit.persistence.view.CascadeType;
import com.blazebit.persistence.view.PrePersist;
import com.blazebit.persistence.view.PreRemove;
import com.blazebit.persistence.view.*;
import com.blazebit.persistence.view.filter.ContainsIgnoreCaseFilter;
import com.blazebit.persistence.view.filter.EqualFilter;
import io.github.mattae.snl.core.api.id.UUIDV7;
import jakarta.persistence.*;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(of = "id", callSuper = false)
@SoftDelete(columnName = "archived")
@Table(name = "fw_organisation")
public class Organisation {
    @Id
    @UUIDV7
    private UUID id;

    @OneToOne(cascade = jakarta.persistence.CascadeType.ALL)
    private Party party;

    private String name;

    private String email;

    private String phone;

    private LocalDate establishmentDate;

    private LocalDate validityDate;

    @ManyToOne
    private Organisation parent;

    @NotNull
    private String type;

    private Boolean active = true;

    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @PreUpdate
    public void update() {
        lastModifiedDate = LocalDateTime.now();
    }

    @EntityView(Organisation.class)
    public interface IdView {
        @IdMapping
        UUID getId();

        String getName();
    }

    @EntityView(Organisation.class)
    public interface View extends IdView {
        @AttributeFilter(ContainsIgnoreCaseFilter.class)
        String getEmail();

        @AttributeFilter(ContainsIgnoreCaseFilter.class)
        String getPhone();

        LocalDate getEstablishmentDate();

        LocalDate getValidityDate();

        @Mapping("party.identifiers")
        Set<Identifier.View> getIdentifiers();

        @AttributeFilter(EqualFilter.class)
        Boolean getActive();
    }

    @EntityView(Organisation.class)
    @CreatableEntityView
    public interface CreateView extends IdView {
        @NotNull
        String getName();

        void setName(String name);

        String getEmail();

        void setEmail(String email);

        String getPhone();

        void setPhone(String phone);

        LocalDate getEstablishmentDate();

        void setEstablishmentDate(LocalDate date);

        LocalDate getValidityDate();

        void setValidityDate(LocalDate date);

        IdView getParent();

        void setParent(IdView parent);

        @NotNull
        String getType();

        void setType(String type);

        @AllowUpdatableEntityViews
        @UpdatableMapping(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.UPDATE, CascadeType.DELETE})
        @NotNull
        Party.PartyView getParty();

        void setParty(Party.PartyView party);

        Boolean getActive();

        void setActive(Boolean active);

        LocalDateTime getLastModifiedDate();

        void setLastModifiedDate(LocalDateTime date);

        @PreRemove
        default boolean preRemove() {
            setLastModifiedDate(LocalDateTime.now());
            return false;
        }

        @PrePersist
        default void prePersist() {
            getParty().setType("ORGANISATION");
            setActive(true);
            setLastModifiedDate(LocalDateTime.now());
        }
    }

    @EntityView(Organisation.class)
    @UpdatableEntityView
    public interface UpdateView extends CreateView {
        @IdMapping
        @NotNull
        UUID getId();

        void setId(UUID id);

        @com.blazebit.persistence.view.PreUpdate
        default void preUpdate() {
            setLastModifiedDate(LocalDateTime.now());
        }
    }

    @EntityView(Organisation.class)
    @CreatableEntityView
    public interface ShortView extends IdView {

        void setId(UUID id);

        void setName(String name);

        String getType();

        void setType(String type);
    }
}
