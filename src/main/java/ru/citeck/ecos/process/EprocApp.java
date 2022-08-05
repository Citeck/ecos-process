package ru.citeck.ecos.process;

import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import ru.citeck.ecos.process.config.ApplicationProperties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.citeck.ecos.webapp.lib.spring.EcosSpringApplication;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
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
