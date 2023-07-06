package ru.citeck.ecos.process.config;

import com.github.cloudyrock.mongock.SpringMongock;
import com.github.cloudyrock.mongock.SpringMongockBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.citeck.ecos.webapp.lib.spring.context.utils.JSR310DateConverters;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@Profile("!test")
public class MongoConfiguration {

    private static final String MONGO_CHANGELOG_PACKAGE = "ru.citeck.ecos.process.mongo.changelog";

    @Value("${spring.data.mongodb.uri}")
    private String mongoDBURI;

    @Bean
    public SpringMongock mongock() {
        MongoClient mongoclient = MongoClients.create(mongoDBURI);
        String dbName = getDBNameFromURI(mongoDBURI);

        MongoDatabase db = mongoclient.getDatabase(dbName);
        MongoTemplate template = new MongoTemplate(mongoclient, dbName);

        migrateChangeLogs(db);
        return new SpringMongockBuilder(template, MONGO_CHANGELOG_PACKAGE)
            .setLockQuickConfig()
            .build();
    }

    public static String getDBNameFromURI(String uriString) {
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cloud not parse MongoDB URI: " + uriString, e);
        }
        String path = uri.getPath();
        String dbName = path != null ? path.replaceAll("^/|/$", "") : null;
        if (StringUtils.isBlank(dbName)) {
            throw new RuntimeException("Could not parse MongoDB database name from URI: " + uriString);
        }

        return dbName;
    }

    private void migrateChangeLogs(MongoDatabase db) {
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
}
