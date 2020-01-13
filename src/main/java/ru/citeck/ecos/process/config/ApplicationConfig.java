package ru.citeck.ecos.process.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.records2.RecordsProperties;

@Configuration
public class ApplicationConfig {

    @Bean
    @ConfigurationProperties(prefix = "eproc.ecos-records")
    public RecordsProperties recordsProperties() {
        return new RecordsProperties();
    }
}
