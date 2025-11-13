package ru.citeck.ecos.process;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import ru.citeck.ecos.process.config.ApplicationProperties;
import ru.citeck.ecos.webapp.lib.spring.EcosSpringApplication;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
@EnableMethodSecurity(securedEnabled = true)
@EnableMongoRepositories("ru.citeck.ecos.process.domain.*.repo")
public class EprocApp {

    public static final String NAME = "eproc";

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        new EcosSpringApplication(EprocApp.class).run(args);
    }
}
