package ru.citeck.ecos.process.domain.proctask

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.createAttsSync
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttributeType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskSyncMutationRestrictionTest {

    @Autowired
    private lateinit var recordsService: RecordsService

    private val taskAttsSync = mutableListOf<EntityRef>()

    @AfterEach
    fun clean() {
        recordsService.delete(taskAttsSync)
    }

    @Test
    fun `create atts sync with exists att id not allowed`() {

        taskAttsSync.add(
            createAttSync(
                id = "test-task-atts-sync-exists-att-id",
                source = TaskAttsSyncSource.RECORD
            )
        )

        assertThrows<IllegalArgumentException> {
            createAttSync(
                id = "test-task-atts-sync-exists-att-id-2",
                source = TaskAttsSyncSource.RECORD
            )
        }
    }

    @Test
    fun `create atts sync with exists att id of different source not allowed`() {

        taskAttsSync.add(
            createAttSync(
                id = "test-task-atts-sync-exists-att-id-diff-source",
                source = TaskAttsSyncSource.RECORD
            )
        )

        assertThrows<IllegalArgumentException> {
            taskAttsSync.add(
                createAttSync(
                    id = "test-task-atts-sync-exists-att-id-diff-source-2",
                    source = TaskAttsSyncSource.TYPE
                )
            )
        }
    }

    @Test
    fun `create atts sync with exists id in same sync not allowed `() {

        assertThrows<IllegalArgumentException> {
            taskAttsSync.add(
                createAttsSync(
                    id = "test-task-atts-sync-exists-att-id-on-object",
                    enabled = true,
                    source = TaskAttsSyncSource.RECORD,
                    name = "test-task-atts-sync-exists-att-id-on-object",
                    attributesSync = listOf(
                        TaskSyncAttribute(
                            id = "name",
                            type = AttributeType.TEXT,
                            ecosTypes = listOf(
                                TaskSyncAttributeType(
                                    typeRef = "someType".toEntityRef(),
                                    attribute = "name"
                                )
                            )
                        ),
                        TaskSyncAttribute(
                            id = "name",
                            type = AttributeType.DATE,
                            ecosTypes = listOf(
                                TaskSyncAttributeType(
                                    typeRef = "someType".toEntityRef(),
                                    attribute = "name"
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `create atts sync with exists id in same sync with different type not allowed `() {

        assertThrows<IllegalArgumentException> {
            taskAttsSync.add(
                createAttsSync(
                    id = "test-task-atts-sync-exists-att-id-on-object-diff-type",
                    enabled = true,
                    source = TaskAttsSyncSource.RECORD,
                    name = "test-task-atts-sync-exists-att-id-on-object-diff-type",
                    attributesSync = listOf(
                        TaskSyncAttribute(
                            id = "name",
                            type = AttributeType.TEXT,
                            ecosTypes = listOf(
                                TaskSyncAttributeType(
                                    typeRef = "someType".toEntityRef(),
                                    attribute = "name"
                                )
                            )
                        ),
                        TaskSyncAttribute(
                            id = "name",
                            type = AttributeType.DATE,
                            ecosTypes = listOf(
                                TaskSyncAttributeType(
                                    typeRef = "someType2".toEntityRef(),
                                    attribute = "name"
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    private fun createAttSync(id: String, source: TaskAttsSyncSource): EntityRef {
        val attSync = createAttsSync(
            id = id,
            enabled = true,
            source = source,
            name = id,
            attributesSync = listOf(
                TaskSyncAttribute(
                    id = "name",
                    type = AttributeType.TEXT,
                    ecosTypes = listOf(
                        TaskSyncAttributeType(
                            typeRef = "someType".toEntityRef(),
                            attribute = "name"
                        )
                    )
                )
            )
        )
        taskAttsSync.add(attSync)
        return attSync
    }

}
