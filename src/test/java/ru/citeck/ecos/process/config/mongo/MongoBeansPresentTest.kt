package ru.citeck.ecos.process.config.mongo

import com.mongodb.client.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceAdapter
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepoAdapter
import ru.citeck.ecos.process.domain.timer.repo.TimerRepository

/**
 * Mirrors [MongoBeansAbsentTest], but for the opposite (default in production) mode:
 * ecos-process.mongo.enabled=true. This is the mode every existing installation runs in
 * right after the upgrade, so the mongo wiring (moved @EnableMongoRepositories in
 * MongoRepositoriesConfig, mongo repo adapters) must be exercised by a real Spring context,
 * not only reasoned about.
 *
 * No MongoDB server is required: MongoClient connects lazily, and Spring Data Mongo's
 * auto-index-creation is disabled by default, so the context comes up fully without any
 * network access to Mongo.
 */
@SpringBootTest(
    properties = [
        "ecos-process.mongo.enabled=true",
        "spring.data.mongodb.uri=mongodb://localhost:27017/eproc-test"
    ]
)
class MongoBeansPresentTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `context should contain mongo client and template`() {
        assertThat(context.getBeanNamesForType(MongoClient::class.java)).isNotEmpty()
        assertThat(context.getBeanNamesForType(MongoTemplate::class.java)).isNotEmpty()
    }

    @Test
    fun `context should contain mongo repositories and adapters`() {
        assertThat(context.getBeanNamesForType(TimerRepository::class.java)).isNotEmpty()
        assertThat(context.getBeanNamesForType(MongoProcDefRepoAdapter::class.java)).isNotEmpty()
        assertThat(context.getBeanNamesForType(MongoProcInstanceAdapter::class.java)).isNotEmpty()
    }
}
