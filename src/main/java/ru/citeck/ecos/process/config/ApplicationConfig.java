package ru.citeck.ecos.process.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.commands.CommandsProperties;
import ru.citeck.ecos.records3.RecordsProperties;

@Configuration
public class ApplicationConfig {

    @Value("${spring.application.name:}")
    private String appName;

    @Value("${eureka.instance.instanceId:}")
    private String appInstanceId;

    @Bean
    @ConfigurationProperties(prefix = "eproc.ecos-records")
    public RecordsProperties recordsProperties() {
        return new RecordsProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "eproc.ecos-commands")
    protected CommandsProperties createProperties() {
        CommandsProperties props = new CommandsProperties();
        props.setAppName(appName);
        props.setAppInstanceId(appInstanceId);
        return props;
    }
}
