package io.github.mattae.snl.core.api.domain;

import com.blazebit.persistence.view.PrePersist;
import com.blazebit.persistence.view.PreRemove;
import com.blazebit.persistence.view.*;
import io.github.mattae.snl.core.api.config.AuditEntityListener;
import io.github.mattae.snl.core.api.id.UUIDV7;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Entity
@Table(name = "fw_party_addresses")
@SoftDelete(columnName = "archived")
@SQLRestriction("archived = false")
@EntityListeners(AuditEntityListener.class)
public class Address {
    @Id
    @UUIDV7
    @EqualsAndHashCode.Include
    UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @NotNull
    private String line1;

    @EqualsAndHashCode.Include
    @ToString.Include
    private String line2;

    @EqualsAndHashCode.Include
    @ToString.Include
    @NotNull
    private String city;

    @EqualsAndHashCode.Include
    @ToString.Include
    @NotNull
    private String state;

    @NotNull
    @EqualsAndHashCode.Include
    @ToString.Include
    private String country;

    @ToString.Include
    private String postalCode;

    @EqualsAndHashCode.Include
    private String addressType;

    @ManyToOne
    @NotNull
    private Party party;

    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @PreUpdate
    @jakarta.persistence.PreRemove
    public void update() {
        lastModifiedDate = LocalDateTime.now();
    }

    @EntityView(Address.class)
    public interface View {
        @IdMapping
        UUID getId();

        String getLine1();

        String getLine2();

        String getCity();

        String getState();

        String getCountry();

        String getPostalCode();

        String getAddressType();
    }

    @EntityView(Address.class)
    @CreatableEntityView
    @UpdatableEntityView
    public interface AddressView extends Address.View {
        void setId(UUID id);

        void setLine1(String line1);

        void setLine2(String line2);

        void setCity(String city);

        void setState(String state);

        void setCountry(String country);

        void setPostalCode(String postalCode);

        void setAddressType(String addressType);

        Party.View getParty();

        void setParty(Party.View party);

        @PostCreate
        default void init() {
            setAddressType("Residential");
        }

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
