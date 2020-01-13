package ru.citeck.ecos.process.config;

import io.github.jhipster.config.JHipsterConstants;
import org.springframework.context.annotation.Profile;
import ru.citeck.ecos.process.AbstractCassandraTest;
import ru.citeck.ecos.process.config.cassandra.CassandraConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_TEST)
public class CassandraConfigurationIT extends CassandraConfiguration {

    private final Logger log = LoggerFactory.getLogger(CassandraConfigurationIT.class);

    /**
     * Override how to get the port to connect to the Cassandra cluster.
     * <p>
     * This uses the TestContainers API to get the mapped port in Docker.
     */
    @Override
    protected int getPort(CassandraProperties properties) {
        return AbstractCassandraTest.CASSANDRA_CONTAINER.getMappedPort(AbstractCassandraTest.CASSANDRA_TEST_PORT);
    }
}
