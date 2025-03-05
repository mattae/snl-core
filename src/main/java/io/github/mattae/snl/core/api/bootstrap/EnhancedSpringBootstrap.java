package io.github.mattae.snl.core.api.bootstrap;

import com.blazebit.persistence.integration.jackson.EntityViewIdValueAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.scalars.ExtendedScalars;
import io.github.mattae.snl.core.api.services.*;
import io.github.mattae.snl.core.api.services.errors.ExceptionTranslator;
import io.micrometer.core.instrument.MeterRegistry;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.aopalliance.intercept.MethodInterceptor;
import org.laxture.sbp.SpringBootPlugin;
import org.laxture.sbp.spring.boot.SpringBootstrap;
import org.laxture.spring.util.ApplicationContextProvider;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import javax.sql.DataSource;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class EnhancedSpringBootstrap extends SpringBootstrap {
    private final SpringBootPlugin plugin;

    public EnhancedSpringBootstrap(SpringBootPlugin plugin, Class<?>... primarySources) {
        super(plugin, primarySources);
        this.plugin = plugin;
    }

    @Override
    public ConfigurableApplicationContext createApplicationContext() {
        AnnotationConfigApplicationContext applicationContext =
                (AnnotationConfigApplicationContext) super.createApplicationContext();

        importBeanFromMainContext(applicationContext, EntityViewIdValueAccessor.class);
        importBeanFromMainContext(applicationContext, RouterFunctionMapping.class);
        importBeanFromMainContext(applicationContext, HttpSecurity.class);
        importBeanFromMainContext(applicationContext, PluginManager.class);
        importBeanFromMainContext(applicationContext, AuthenticationManagerBuilder.class);
        importBeanFromMainContext(applicationContext, MethodInterceptor.class);
        importBeanFromMainContext(applicationContext, ConfigurationService.class);
        importBeanFromMainContext(applicationContext, MailService.class);
        importBeanFromMainContext(applicationContext, PersonService.class);
        importBeanFromMainContext(applicationContext, OrganisationService.class);
        importBeanFromMainContext(applicationContext, TranslationService.class);
        importBeanFromMainContext(applicationContext, ValueSetService.class);
        importBeanFromMainContext(applicationContext, DataSource.class);
        importBeanFromMainContext(applicationContext, ExceptionTranslator.class);
        importBeanFromMainContext(applicationContext, ExtensionService.class);
        importBeanFromMainContext(applicationContext, PreferenceService.class);
        importBeanFromMainContext(applicationContext, ConversionService.class);
        importBeanFromMainContext(applicationContext, TransactionManager.class);
        importBeanFromMainContext(applicationContext, TransactionHandler.class);
        importBeanFromMainContext(applicationContext, MeterRegistry.class);
        importBeanFromMainContext(applicationContext, BeanFactoryCacheOperationSourceAdvisor.class);
        importBeanFromMainContext(applicationContext, CacheManager.class);
        importBeanFromMainContext(applicationContext, ExecutionGraphQlService.class);
        importBeanFromMainContext(applicationContext, HandlerMappingIntrospector.class);
        importBeanFromMainContext(applicationContext, AnnotationTemplateExpressionDefaults.class);
        importBeanFromMainContext(applicationContext, SslBundles.class);
        getGraphqlControllers(plugin.getMainApplicationContext())
                .forEach(controller -> importBeanFromMainContext(applicationContext, controller.getClass()));

        registerSupportingBeans(applicationContext);

        return applicationContext;
    }

    @Override
    protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
        super.configurePropertySources(environment, args);

        ApplicationContextProvider.getApplicationContext(ConfigurationService.class).getBean(ConfigurationService.class)
                .getValueAsStringForKey("PLUGIN.SPRING.APPLICATION_CONFIGURATION", plugin.getWrapper().getPluginId())
                .ifPresent(config -> {
                    Map<String, Object> properiesMap = new HashMap<>();

                    try {
                        properiesMap = new YamlToProperties(config).asProperties();
                    } catch (Exception ex) {
                        try {
                            var properties = new Properties();
                            properties.load(new StringReader(config));
                            properiesMap = properties.entrySet().stream().collect(
                                    Collectors.toMap(
                                            e -> String.valueOf(e.getKey()),
                                            Map.Entry::getValue,
                                            (prev, next) -> next, HashMap::new
                                    ));
                        } catch (Exception ignored) {
                        }
                    }

                    if (!properiesMap.isEmpty()) {
                        environment.getPropertySources().addLast(
                                new MapPropertySource("Override configurations", properiesMap) {
                                });
                    }
                });
    }

    private void registerSupportingBeans(AnnotationConfigApplicationContext applicationContext) {
        ThymeleafProperties properties = getMainApplicationContext().getBean(ThymeleafProperties.class);
        SpringResourceTemplateResolver resolver = getSpringResourceTemplateResolver(applicationContext, properties);
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(SpringResourceTemplateResolver.class);
        beanDefinition.setInstanceSupplier(() -> resolver);
        applicationContext.registerBeanDefinition("pluginTemplateResolver", beanDefinition);


        SpringTemplateEngine engine = getSpringTemplateEngine(properties, resolver, applicationContext);
        beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(SpringResourceTemplateResolver.class);
        beanDefinition.setInstanceSupplier(() -> engine);
        beanDefinition.setPrimary(true);
        applicationContext.registerBeanDefinition("springTemplateEngine", beanDefinition);

        beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(LiquibaseConfiguration.class);
        applicationContext.registerBeanDefinition("liquibaseConfiguration", beanDefinition);


        ParameterValueMapper parameterValueMapper = getMainApplicationContext().getBean(ParameterValueMapper.class);
        EndpointMediaTypes endpointMediaTypes = getMainApplicationContext().getBean(EndpointMediaTypes.class);
        ObjectProvider<PathMapper> endpointPathMappers = getMainApplicationContext().getBeanProvider(PathMapper.class);
        ObjectProvider<AdditionalPathsMapper> additionalPathsMappers = getMainApplicationContext().getBeanProvider(AdditionalPathsMapper.class);
        ObjectProvider<OperationInvokerAdvisor> invokerAdvisors = getMainApplicationContext().getBeanProvider(OperationInvokerAdvisor.class);
        ObjectProvider<EndpointFilter<ExposableWebEndpoint>> filters = getMainApplicationContext()
                .getBeanProvider(ResolvableType.forClass(EndpointFilter.class));
        ObjectProvider<OperationFilter<WebOperation>> operationFilters = getMainApplicationContext()
                .getBeanProvider(ResolvableType.forType(WebOperation.class));

        var discoverer = new WebEndpointDiscoverer(applicationContext, parameterValueMapper, endpointMediaTypes,
                endpointPathMappers.orderedStream().toList(), additionalPathsMappers.stream().toList(), invokerAdvisors.orderedStream().toList(),
                filters.orderedStream().toList(), operationFilters.orderedStream().toList());
        beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(WebEndpointDiscoverer.class);
        beanDefinition.setInstanceSupplier(() -> discoverer);
        applicationContext.registerBeanDefinition("webEndpointDiscoverer", beanDefinition);

        beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(RuntimeWiringConfigurer.class);
        beanDefinition.setInstanceSupplier(this::runtimeWiringConfigurer);
        applicationContext.registerBeanDefinition("runtimeWiringConfigurer", beanDefinition);
    }

    private RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> {
            wiringBuilder.scalar(ExtendedScalars.LocalTime);
            wiringBuilder.scalar(ExtendedScalars.GraphQLChar);
            wiringBuilder.scalar(ExtendedScalars.CountryCode);
            wiringBuilder.scalar(ExtendedScalars.Currency);
            wiringBuilder.scalar(ExtendedScalars.Date);
            wiringBuilder.scalar(ExtendedScalars.GraphQLBigDecimal);
            wiringBuilder.scalar(ExtendedScalars.GraphQLBigInteger);
            wiringBuilder.scalar(ExtendedScalars.GraphQLByte);
            wiringBuilder.scalar(ExtendedScalars.GraphQLLong);
            wiringBuilder.scalar(ExtendedScalars.GraphQLShort);
            wiringBuilder.scalar(ExtendedScalars.Locale);
            wiringBuilder.scalar(ExtendedScalars.NegativeFloat);
            wiringBuilder.scalar(ExtendedScalars.NegativeInt);
            wiringBuilder.scalar(ExtendedScalars.NonNegativeFloat);
            wiringBuilder.scalar(ExtendedScalars.NonNegativeInt);
            wiringBuilder.scalar(ExtendedScalars.NonPositiveFloat);
            wiringBuilder.scalar(ExtendedScalars.NonPositiveInt);
            wiringBuilder.scalar(ExtendedScalars.Object);
            wiringBuilder.scalar(ExtendedScalars.PositiveFloat);
            wiringBuilder.scalar(ExtendedScalars.PositiveInt);
            wiringBuilder.scalar(ExtendedScalars.Time);
            wiringBuilder.scalar(ExtendedScalars.Url);
            wiringBuilder.scalar(ExtendedScalars.UUID);
        };
    }

    private SpringTemplateEngine getSpringTemplateEngine(ThymeleafProperties properties,
                                                         SpringResourceTemplateResolver resolver,
                                                         ApplicationContext applicationContext) {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setResourceLoader(applicationContext);
        messageSource.addBasenames("messages/messages");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setEnableSpringELCompiler(properties.isEnableSpringElCompiler());
        engine.setRenderHiddenMarkersBeforeCheckboxes(properties.isRenderHiddenMarkersBeforeCheckboxes());
        engine.addTemplateResolver(resolver);
        ObjectProvider<ITemplateResolver> templateResolvers = getMainApplicationContext().getBeanProvider(ITemplateResolver.class);
        ObjectProvider<IDialect> dialects = getMainApplicationContext().getBeanProvider(IDialect.class);
        templateResolvers.orderedStream().forEach(engine::addTemplateResolver);
        dialects.orderedStream().forEach(engine::addDialect);
        engine.setTemplateEngineMessageSource(messageSource);
        return engine;
    }

    private SpringResourceTemplateResolver getSpringResourceTemplateResolver(ApplicationContext applicationContext,
                                                                             ThymeleafProperties properties) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix(properties.getPrefix());
        resolver.setSuffix(properties.getSuffix());
        resolver.setTemplateMode(properties.getMode());
        if (properties.getEncoding() != null) {
            resolver.setCharacterEncoding(properties.getEncoding().name());
        }
        resolver.setCacheable(properties.isCache());
        resolver.setOrder(1);
        resolver.setCheckExistence(properties.isCheckTemplate());
        return resolver;
    }

    protected static Set<Object> getGraphqlControllers(ApplicationContext applicationContext) {
        Set<Object> beans = new LinkedHashSet<>();
        beans.addAll(applicationContext.getBeansWithAnnotation(Controller.class)
                .values().stream().toList());
        beans.addAll(applicationContext.getBeansWithAnnotation(RestController.class)
                .values().stream().toList());
        beans = beans.stream().filter(bean -> {
            Set<Method> sets = MethodIntrospector.selectMethods(ClassUtils.getUserClass(bean),
                    (ReflectionUtils.MethodFilter) method -> {
                        Set<Annotation> annotations = AnnotatedElementUtils.findAllMergedAnnotations(
                                method, new LinkedHashSet<>(Arrays.asList(BatchMapping.class, SchemaMapping.class)));
                        return !annotations.isEmpty();
                    });
            return !sets.isEmpty();
        }).collect(Collectors.toSet());
        return beans;
    }

    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled")
    static class LiquibaseConfiguration implements InitializingBean {
        private final LiquibaseProperties properties;
        private final DataSource dataSource;
        private final ApplicationContext applicationContext;

        LiquibaseConfiguration(Optional<LiquibaseProperties> properties, DataSource dataSource, ApplicationContext applicationContext) {
            this.properties = properties.orElse(null);
            this.dataSource = dataSource;
            this.applicationContext = applicationContext;
        }

        public void initializeLiquibase() throws LiquibaseException {
            if (properties != null) {
                SpringLiquibase liquibase = new SpringLiquibase();
                liquibase.setDataSource(dataSource);
                liquibase.setChangeLog(properties.getChangeLog());
                liquibase.setClearCheckSums(properties.isClearChecksums());
                liquibase.setDefaultSchema(properties.getDefaultSchema());
                liquibase.setLiquibaseSchema(properties.getLiquibaseSchema());
                liquibase.setLiquibaseTablespace(properties.getLiquibaseTablespace());
                liquibase.setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
                liquibase.setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());
                liquibase.setDropFirst(properties.isDropFirst());
                liquibase.setShouldRun(properties.isEnabled());
                liquibase.setChangeLogParameters(properties.getParameters());
                liquibase.setRollbackFile(properties.getRollbackFile());
                liquibase.setTestRollbackOnUpdate(properties.isTestRollbackOnUpdate());
                liquibase.setTag(properties.getTag());
                liquibase.setResourceLoader(applicationContext);

                liquibase.afterPropertiesSet();
            }
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            initializeLiquibase();
        }
    }
}
