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
 * Test profile runs with ecos-process.mongo.enabled=false, so the application must start
 * without any mongo bean at all. This is the target production mode.
 */
@SpringBootTest
class MongoBeansAbsentTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `context should not contain mongo client and template`() {
        assertThat(context.getBeanNamesForType(MongoClient::class.java)).isEmpty()
        assertThat(context.getBeanNamesForType(MongoTemplate::class.java)).isEmpty()
    }

    @Test
    fun `context should not contain mongo repositories and adapters`() {
        assertThat(context.getBeanNamesForType(TimerRepository::class.java)).isEmpty()
        assertThat(context.getBeanNamesForType(MongoProcDefRepoAdapter::class.java)).isEmpty()
        assertThat(context.getBeanNamesForType(MongoProcInstanceAdapter::class.java)).isEmpty()
    }
}
