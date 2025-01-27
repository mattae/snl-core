package io.github.mattae.snl.core.api.domain;

import io.github.mattae.snl.core.api.id.UUIDV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = "name")
@Table(name = "fw_plugin")
public class Plugin implements Persistable<UUID> {
    @Id
    @UUIDV7
    private UUID id;

    @NotNull
    @Column(unique = true)
    private String name;

    private String version;

    private Boolean started = false;

    private Boolean enabled = true;

    private String hash;

    private String signature;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
