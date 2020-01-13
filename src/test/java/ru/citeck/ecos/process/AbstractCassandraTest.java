package ru.citeck.ecos.process;

import io.github.jhipster.config.JHipsterConstants;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for starting/stopping Cassandra during tests.
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles(JHipsterConstants.SPRING_PROFILE_TEST)
public class AbstractCassandraTest {

    public static final String CASSANDRA_UNIT_KEYSPACE = "cassandra_unit_keyspace";
    public static GenericContainer CASSANDRA_CONTAINER = null;
    public static final int CASSANDRA_TEST_PORT = 9042;
    private static boolean started = false;

    @BeforeAll
    public static void startServer() throws ConfigurationException, IOException, URISyntaxException {
        if (!started) {
            startTestcontainer();
            Cluster cluster = new Cluster.Builder()
                .addContactPoint("127.0.0.1")
                .withPort(CASSANDRA_CONTAINER.getMappedPort(CASSANDRA_TEST_PORT))
                .withoutMetrics()
                .withoutJMXReporting()
                .build();

            Session session = cluster.connect();
            createTestKeyspace(session);
            CQLDataLoader dataLoader = new CQLDataLoader(session);
            applyScripts(dataLoader, "config/cql/changelog/", "*.cql");
            started = true;
        }
    }

    private static void startTestcontainer() {
        CASSANDRA_CONTAINER =
            new GenericContainer("cassandra:3.11.5")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedPorts(CASSANDRA_TEST_PORT);

        CASSANDRA_CONTAINER.start();
    }

    private static void createTestKeyspace(Session session) {
        String createQuery = "CREATE KEYSPACE " + CASSANDRA_UNIT_KEYSPACE
            + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}";
        session.execute(createQuery);
        String useKeyspaceQuery = "USE " + CASSANDRA_UNIT_KEYSPACE;
        session.execute(useKeyspaceQuery);
    }

    private static void applyScripts(CQLDataLoader dataLoader,
                                     String cqlDir,
                                     String pattern) throws IOException, URISyntaxException {

        URL dirUrl = ClassLoader.getSystemResource(cqlDir);
        if (dirUrl == null) {
            return;
        }
        List<String> scripts = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dirUrl.toURI()), pattern)) {
            for (Path entry : stream) {
                scripts.add(entry.getFileName().toString());
            }
        }
        Collections.sort(scripts);

        for (String fileName : scripts) {
            dataLoader.load(new ClassPathCQLDataSet(
                cqlDir + fileName,
                false,
                false,
                CASSANDRA_UNIT_KEYSPACE)
            );
        }
    }
}
