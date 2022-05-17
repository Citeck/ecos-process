package ru.citeck.ecos.process.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Eproc.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
}
