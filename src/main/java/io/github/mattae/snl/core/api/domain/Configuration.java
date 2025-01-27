package io.github.mattae.snl.core.api.domain;

import com.blazebit.persistence.view.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "fw_configuration")
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @NotNull
    @EqualsAndHashCode.Include
    private String category;

    private String description;

    private Boolean personalized;

    @Column(name = "_order")
    private Integer order = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Set<Data> data;

    @ManyToOne
    private Plugin plugin;

    @EntityView(Configuration.class)
    @CreatableEntityView
    public interface CreateView {
        @IdMapping
        Long getId();

        @NotNull
        String getCategory();

        void setCategory(String category);

        String getDescription();

        void setDescription(String description);

        Integer getOrder();

        void setOrder(Integer order);

        PluginView getPlugin();

        Boolean getPersonalized();

        void setPersonalized(Boolean personalized);

        @NotEmpty
        @MappingSingular
        Set<Data> getData();

        void setData(Set<Data> configurations);
    }

    @EntityView(Configuration.class)
    @UpdatableEntityView
    public interface UpdateView extends CreateView {
        @IdMapping
        @NotNull
        Long getId();

        void setId(Long id);
    }

    @Getter
    @Setter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Data {
        @NotNull
        @EqualsAndHashCode.Include
        private String key;

        @NotNull
        private String value;

        @NotNull
        private Type type;

        private String regex;

        private Boolean masked;

        private String system;

        private String name;

        private List<DataOption> options;

        public enum Type {
            string, numeric, bool, date
        }

        @Getter
        @Setter
        public static class DataOption {
            private String value;
            private String label;
        }
    }

    @EntityView(Plugin.class)
    public interface PluginView {
        @IdMapping
        UUID getId();
    }

    @EntityView(Configuration.class)
    public interface ConfigurationPluginView {
        @IdMapping
        Long getId();

        PluginView getPlugin();
    }
}
