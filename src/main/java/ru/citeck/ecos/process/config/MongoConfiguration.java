package ru.citeck.ecos.process.config;

import com.github.cloudyrock.mongock.SpringMongock;
import com.github.cloudyrock.mongock.SpringMongockBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import io.github.jhipster.domain.util.JSR310DateConverters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@EnableMongoRepositories("ru.citeck.ecos.process.repository")
@Profile("!test")
public class MongoConfiguration {

    private static final String MONGO_CHANGELOG_PACKAGE = "ru.citeck.ecos.process.mongo.changelog";

    @Value("${spring.data.mongodb.uri}")
    private String mongoDBURI;

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.data.mongodb")
    public DataSource domainDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public SpringMongock mongock() {
        MongoClientURI mongoClientURI = new MongoClientURI(mongoDBURI);
        MongoClient mongoclient = new MongoClient(mongoClientURI);
        migrateChangeLogs(mongoClientURI, mongoclient);
        return new SpringMongockBuilder(mongoclient, mongoClientURI.getDatabase(), MONGO_CHANGELOG_PACKAGE)
            .setLockQuickConfig()
            .build();
    }

    private void migrateChangeLogs(MongoClientURI mongoClientURI, MongoClient mongoclient) {
        MongoDatabase db = mongoclient.getDatabase(mongoClientURI.getDatabase());
        boolean dbchangeLogIsEmpty = db.getCollection("dbchangelog").count() == 0L;
        boolean mongockChangeLogIsEmpty = db.getCollection("mongockChangeLog").count() == 0L;
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
}
