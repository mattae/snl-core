package io.github.mattae.snl.core.api.domain;

import com.blazebit.persistence.view.PrePersist;
import com.blazebit.persistence.view.PreRemove;
import com.blazebit.persistence.view.*;
import com.blazebit.persistence.view.filter.EqualFilter;
import io.github.mattae.snl.core.api.id.UUIDV7;
import jakarta.persistence.CascadeType;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.jdbc.Expectation;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
//@SoftDelete(columnName = "archived")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Table(name = "fw_party")
@SQLRestriction("archived = false")
@SQLDelete(sql = "update fw_party set archived = true, lastlast_modified_date = now() where id = ?",
        verify = Expectation.RowCount.class)
public class Party {
    @Id
    @UUIDV7
    private UUID id;

    private String type;

    private String legalType = "";

    private String displayName = "";

    @OneToMany(mappedBy = "party", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    @SQLRestriction("archived = false")
    private Set<Address> addresses = new HashSet<>();

    @OneToMany(mappedBy = "party", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    @SQLRestriction("archived = false")
    private Set<Identifier> identifiers = new HashSet<>();

    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @PreUpdate
    public void update() {
        lastModifiedDate = LocalDateTime.now();
    }

    @EntityView(Party.class)
    public interface View {
        @IdMapping
        UUID getId();

        @AttributeFilter(EqualFilter.class)
        String getType();
    }

    @CreatableEntityView
    @UpdatableEntityView
    @EntityView(Party.class)
    public interface PartyView extends Party.View {
        void setType(String type);

        //@UpdatableMapping(cascade = {com.blazebit.persistence.view.CascadeType.UPDATE, com.blazebit.persistence.view.CascadeType.PERSIST, com.blazebit.persistence.view.CascadeType.DELETE})
        @MappingInverse(removeStrategy = InverseRemoveStrategy.REMOVE)
        List<Address.AddressView> getAddresses();

        void setAddresses(List<Address.AddressView> addresses);

        //@UpdatableMapping(cascade = {com.blazebit.persistence.view.CascadeType.UPDATE, com.blazebit.persistence.view.CascadeType.PERSIST, com.blazebit.persistence.view.CascadeType.DELETE})
        @MappingInverse(removeStrategy = InverseRemoveStrategy.REMOVE)
        List<Identifier.IdentifierView> getIdentifiers();

        void setIdentifiers(List<Identifier.IdentifierView> identifiers);


        LocalDateTime getLastModifiedDate();

        void setLastModifiedDate(LocalDateTime date);

        @PreRemove
        default boolean preRemove() {
            setLastModifiedDate(LocalDateTime.now());
            return false;
        }

        @com.blazebit.persistence.view.PreUpdate
        default void preUpdate() {
            setLastModifiedDate(LocalDateTime.now());
        }

        @PrePersist
        default void prePersist() {
            setLastModifiedDate(LocalDateTime.now());
        }
    }
}
