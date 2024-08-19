package ru.citeck.ecos.process.config;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.mongock.driver.mongodb.sync.v4.driver.MongoSync4Driver;
import io.mongock.runner.core.executor.MongockRunner;
import io.mongock.runner.springboot.MongockSpringboot;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.citeck.ecos.webapp.lib.spring.context.utils.JSR310DateConverters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@Profile("!test")
public class MongoConfiguration implements ApplicationContextAware {

    private static final String MONGO_CHANGELOG_PACKAGE = "ru.citeck.ecos.process.mongo.changelog";

    @Value("${spring.data.mongodb.uri}")
    private String mongoDBURI;


    private ApplicationContext applicationContext;

    @Bean
    public MongockRunner mongock() {

        ConnectionString connectionString = new ConnectionString(mongoDBURI);
        MongoClient mongoclient = MongoClients.create(connectionString);
        migrateChangeLogs(connectionString, mongoclient);

        MongoSync4Driver mongoSync4Driver = MongoSync4Driver.withDefaultLock(
            mongoclient,
            connectionString.getDatabase()
        );

        return MongockSpringboot.builder()
            .setDriver(mongoSync4Driver)
            .addMigrationScanPackage(MONGO_CHANGELOG_PACKAGE)
            .setSpringContext(applicationContext)
            .buildRunner();
    }

    private void migrateChangeLogs(ConnectionString connectionString, MongoClient mongoclient) {
        MongoDatabase db = mongoclient.getDatabase(connectionString.getDatabase());
        boolean dbchangeLogIsEmpty = db.getCollection("dbchangelog").countDocuments() == 0L;
        boolean mongockChangeLogIsEmpty = db.getCollection("mongockChangeLog").countDocuments() == 0L;
        if (!dbchangeLogIsEmpty && mongockChangeLogIsEmpty) {
            log.info("Migrate rows from dbchangelog to mongockChangeLog");
            db.getCollection("dbchangelog")
                .aggregate(Collections.singletonList(new BasicDBObject("$out", "mongockChangeLog")))
                .toCollection();
        }
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(JSR310DateConverters.DateToZonedDateTimeConverter.INSTANCE);
        converters.add(JSR310DateConverters.ZonedDateTimeToDateConverter.INSTANCE);
        return new MongoCustomConversions(converters);
    }

    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean localValidatorFactoryBean) {
        return new ValidatingMongoEventListener(localValidatorFactoryBean);
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
