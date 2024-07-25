package ru.citeck.ecos.process.domain.proctask

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttributeType
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskSyncMutationRestrictionTest {

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @AfterEach
    @BeforeEach
    fun clean() {
        helper.cleanTaskAttsSyncSettings()
    }

    @Test
    fun `create atts sync with exists att id not allowed`() {
        createAttSync(
            id = "test-task-atts-sync-exists-att-id",
            source = TaskAttsSyncSource.RECORD
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
        createAttSync(
            id = "test-task-atts-sync-exists-att-id-diff-source",
            source = TaskAttsSyncSource.RECORD
        )

        assertThrows<IllegalArgumentException> {
            createAttSync(
                id = "test-task-atts-sync-exists-att-id-diff-source-2",
                source = TaskAttsSyncSource.TYPE
            )
        }
    }

    @Test
    fun `create atts sync with exists id in same sync not allowed `() {

        assertThrows<IllegalArgumentException> {
            helper.createAttsSync(
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
        }
    }

    @Test
    fun `create atts sync with exists id in same sync with different type not allowed `() {

        assertThrows<IllegalArgumentException> {
            helper.createAttsSync(
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
        }
    }

    private fun createAttSync(id: String, source: TaskAttsSyncSource): EntityRef {
        val attSync = helper.createAttsSync(
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
        return attSync
    }
}
