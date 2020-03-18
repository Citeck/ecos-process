package ru.citeck.ecos.process.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class TestMongoConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.data.mongodb")
    public DataSource domainDataSource() {
        return DataSourceBuilder.create().build();
    }
}
